package me.ghosttypes.ghostware.modules.combat;

import me.ghosttypes.ghostware.utils.Categories;
import me.ghosttypes.ghostware.utils.world.BlockUtils;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.util.math.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static me.ghosttypes.ghostware.utils.world.BlockUtils.isSurrounded;

public class SurroundPlus extends Module {

    public SurroundPlus() {
        super(Categories.Combat, "surround-plus", "surrounds you in blocks to prevent massive cope");
    }

    public enum CrystalMode {Always, Legs, None}
    public enum HitMode {Default, Packet, Both}
    public enum TpMode {Default, Smooth, None}

    private final SettingGroup sgGeneral = settings.createGroup("General", true);
    private final SettingGroup sgForce = settings.createGroup("Force", true);
    private final SettingGroup sgCenter = settings.createGroup("Center", true);
    private final SettingGroup sgCrystalBreaker = settings.createGroup("Crystal Breaker", true);
    private final SettingGroup sgMisc = settings.createGroup("Misc", true);
    private final SettingGroup sgRender = settings.createGroup("Render", true);

    //general
    private final Setting<Integer> bpt = sgGeneral.add(new IntSetting.Builder().name("blocks-per-tick").description("how many blocks can be placed per tick").defaultValue(3).min(1).sliderMax(10).build());
    private final Setting<Boolean> doubleH = sgGeneral.add(new BoolSetting.Builder().name("double").description("placing obsidian to anti face place positions").defaultValue(false).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Automatically faces towards the obsidian being placed.").defaultValue(false).build());
    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder().name("block").description("What blocks to use for surround.").defaultValue(Collections.singletonList(Blocks.OBSIDIAN)).filter(this::blockFilter).build());

    //force
    private final Setting<Boolean> forceSurround = sgForce.add(new BoolSetting.Builder().name("force-surround").description("force placing main surround blocks(cool for ping players or bad servers)").defaultValue(false).build());
    private final Setting<Keybind> forceDouble = sgForce.add(new KeybindSetting.Builder().name("force-doube").description("force double height surround").defaultValue(Keybind.fromKey(-1)).build());
    private final Setting<Keybind> forceTrap = sgForce.add(new KeybindSetting.Builder().name("force-trap").description("force self trap").defaultValue(Keybind.fromKey(-1)).build());
    private final Setting<Keybind> forceAntiCity = sgForce.add(new KeybindSetting.Builder().name("force-anti-city").description("force force anti city blocks").defaultValue(Keybind.fromKey(-1)).build());

    //center
    private final Setting<TpMode> centerMode = sgCenter.add(new EnumSetting.Builder<TpMode>().name("center-mode").description("Teleports you to the center of the block.").defaultValue(TpMode.Default).build());
    private final Setting<Integer> centerDelay = sgCenter.add(new IntSetting.Builder().name("delay").description("Delay for teleporting to center.").defaultValue(5).min(1).sliderMax(20).visible(() -> centerMode.get() == TpMode.Smooth).build());
    private final Setting<Boolean> stop = sgCenter.add(new BoolSetting.Builder().name("stop-moving").description("stop all movings").defaultValue(false).build());
    private final Setting<Boolean> anchor = sgCenter.add(new BoolSetting.Builder().name("anchor").description("slowing you to prevent massive cope").defaultValue(true).build());

    //crystal breaker
    private final Setting<CrystalMode> mode = sgCrystalBreaker.add(new EnumSetting.Builder<CrystalMode>().name("crystal-breaker").description("Breaks crystals in range.").defaultValue(CrystalMode.Legs).build());
    private final Setting<CrystalMode> placeMode = sgCrystalBreaker.add(new EnumSetting.Builder<CrystalMode>().name("place-mode").description("Places obsidian in crystal position.").defaultValue(CrystalMode.Legs).visible(() -> mode.get() != CrystalMode.None).build());
    private final Setting<HitMode> hitMode = sgCrystalBreaker.add(new EnumSetting.Builder<HitMode>().name("hit-mode").description("The way to interact with crystal.").defaultValue(HitMode.Default).visible(() -> mode.get() != CrystalMode.None).build());
    private final Setting<Boolean> onlyHole = sgCrystalBreaker.add(new BoolSetting.Builder().name("hole-only").description("Woks only if player is in hole.").defaultValue(false).visible(() -> mode.get() != CrystalMode.None).build());
    private final Setting<Boolean> antiCev = sgCrystalBreaker.add(new BoolSetting.Builder().name("anti-cev").description("Ignore range if crystal was placed in cev position").defaultValue(false).visible(() -> mode.get() != CrystalMode.None).build());
    private final Setting<Double> breakRange = sgCrystalBreaker.add(new DoubleSetting.Builder().name("range").description("The speed at which you rotate.").defaultValue(2.7).min(0).sliderMax(7).visible(() -> mode.get() != CrystalMode.None).build());
    private final Setting<Integer> crystalAge = sgCrystalBreaker.add(new IntSetting.Builder().name("crystal-age").description("The speed at which you rotate.").defaultValue(2).min(0).sliderMax(10).visible(() -> mode.get() != CrystalMode.None).build());
    private final Setting<Integer> crystalDelay = sgCrystalBreaker.add(new IntSetting.Builder().name("delay").description("Delay between hits").defaultValue(2).min(1).sliderMax(10).visible(() -> mode.get() != CrystalMode.None).build());

    //misc
    private final Setting<Boolean> checkEntity = sgMisc.add(new BoolSetting.Builder().name("check-entity").description("Checks entity lol").defaultValue(false).build());
    private final Setting<Boolean> onlyOnGround = sgMisc.add(new BoolSetting.Builder().name("only-on-ground").description("Works only when you standing on blocks.").defaultValue(true).build());
    private final Setting<Boolean> disableOnJump = sgMisc.add(new BoolSetting.Builder().name("disable-on-jump").description("Automatically disables when you jump.").defaultValue(true).build());
    private final Setting<Boolean> disableOnTp = sgMisc.add(new BoolSetting.Builder().name("disable-on-tp").description("Automatically disables when you teleporting (like using chorus or pearl).").defaultValue(true).build());
    private final Setting<Boolean> disableOnYChange = sgMisc.add(new BoolSetting.Builder().name("disable-on-y-change").description("Automatically disables when your y level (step, jumping, atc).").defaultValue(true).build());

    // Render
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders an overlay where blocks will be placed.").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color of the target block rendering.").defaultValue(new SettingColor(200, 0, 0, 30)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color of the target block rendering.").defaultValue(new SettingColor(255, 0, 0)).build());

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();
    private static List<BlockPos> posPlaceBlocks = new ArrayList<>();
    private int ticks;
    private int centerDelayLeft;
    BlockPos pos;
    private boolean crystalRemoved = false;
    private int crystalTicks;

    @Override
    public void onActivate() {
        centerDelayLeft = 0;
        switch (centerMode.get()) {
            case Default -> {
                PlayerUtils.centerPlayer();
                pause();
            }
            case Smooth -> {
                centerDelayLeft = centerDelay.get();
                if (inCenter()) {centerDelayLeft = 0;}
                if (anchor.get()) ((IVec3d) mc.player.getVelocity()).set(0, mc.player.getVelocity().y, 0);
            }
        }
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @Override
    public void onDeactivate() {
        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (onlyOnGround.get() && !mc.player.isOnGround()) return;
        if ((disableOnJump.get() && (mc.options.keyJump.isPressed() || mc.player.input.jumping)) || (disableOnYChange.get() && mc.player.prevY < mc.player.getY())) {
            toggle();
            return;
        }
        //center
        if (centerMode.get() == TpMode.Smooth) {
            if (centerDelayLeft > 0) {
                pause();
                assert mc.player != null;
                double decrX = MathHelper.floor(mc.player.getX()) + 0.5 - mc.player.getX();
                double decrZ = MathHelper.floor(mc.player.getZ()) + 0.5 - mc.player.getZ();
                double sqrtPos = Math.sqrt(Math.pow(decrX, 2.0) + Math.pow(decrZ, 2.0));
                double div = Math.sqrt(0.5) / centerDelay.get();
                if (sqrtPos <= div) {
                    centerDelayLeft = 0;
                    double x = MathHelper.floor(mc.player.getX()) + 0.5;
                    double z = MathHelper.floor(mc.player.getZ()) + 0.5;
                    mc.player.setPosition(x, mc.player.getY(), z);
                    return;
                }
                double x = mc.player.getX();
                double z = mc.player.getZ();
                double incX = MathHelper.floor(mc.player.getX()) + 0.5;
                double incZ = MathHelper.floor(mc.player.getZ()) + 0.5;
                double incResult = 0.0;
                double decrResult = 0.0;
                double x_ = mc.player.getX();
                double z_ = mc.player.getZ();
                if (Math.sqrt(Math.pow(decrX, 2.0)) > Math.sqrt(Math.pow(decrZ, 2.0))) {
                    if (decrX > 0.0) {
                        incResult = 0.5 / centerDelay.get();
                    } else if (decrX < 0.0) {
                        incResult = -0.5 / centerDelay.get();
                    }
                    x_ = mc.player.getX() + incResult;
                    z_ = z(x, z, incX, incZ, x_);
                } else if (Math.sqrt(Math.pow(decrX, 2.0)) < Math.sqrt(Math.pow(decrZ, 2.0))) {
                    if (decrZ > 0.0) {
                        decrResult = 0.5 / centerDelay.get();
                    } else if (decrZ < 0.0) {
                        decrResult = -0.5 / centerDelay.get();
                    }
                    z_ = mc.player.getZ() + decrResult;
                    x_ = x(x, z, incX, incZ, z_);
                } else if (Math.sqrt(Math.pow(decrX, 2.0)) == Math.sqrt(Math.pow(decrZ, 2.0))) {
                    if (decrX > 0.0) {
                        incResult = 0.5 / (double) centerDelay.get();
                    } else if (decrX < 0.0) {
                        incResult = -0.5 / (double) centerDelay.get();
                    }
                    x_ = mc.player.getX() + incResult;
                    if (decrZ > 0.0) {
                        decrResult = 0.5 / (double) centerDelay.get();
                    } else if (decrZ < 0.0) {
                        decrResult = -0.5 / (double) centerDelay.get();
                    }
                    z_ = mc.player.getZ() + decrResult;
                }
                pause();
                mc.player.setPosition(x_, mc.player.getY(), z_);
            }
        }

        ticks = 0;
        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);

        posPlaceBlocks.clear();
        if (!onlyOnGround.get()) posPlaceBlocks.add(mc.player.getBlockPos().down());
        posPlaceBlocks.add(mc.player.getBlockPos().west());
        posPlaceBlocks.add(mc.player.getBlockPos().east());
        posPlaceBlocks.add(mc.player.getBlockPos().south());
        posPlaceBlocks.add(mc.player.getBlockPos().north());
        if (doubleH.get() || forceDouble.get().isPressed() || forceTrap.get().isPressed()){
            posPlaceBlocks.add(mc.player.getBlockPos().west().up());
            posPlaceBlocks.add(mc.player.getBlockPos().east().up());
            posPlaceBlocks.add(mc.player.getBlockPos().south().up());
            posPlaceBlocks.add(mc.player.getBlockPos().north().up());
            if (forceTrap.get().isPressed()) posPlaceBlocks.add(mc.player.getBlockPos().up(2));
        }
        if (forceAntiCity.get().isPressed()){
            posPlaceBlocks.add(mc.player.getBlockPos().west(2));
            posPlaceBlocks.add(mc.player.getBlockPos().east(2));
            posPlaceBlocks.add(mc.player.getBlockPos().south(2));
            posPlaceBlocks.add(mc.player.getBlockPos().north(2));
            posPlaceBlocks.add(mc.player.getBlockPos().add(1,0,1));
            posPlaceBlocks.add(mc.player.getBlockPos().add(1,0,-1));
            posPlaceBlocks.add(mc.player.getBlockPos().add(-1,0,1));
            posPlaceBlocks.add(mc.player.getBlockPos().add(-1,0,-1));
        }
        for (Direction side : Direction.values()) {
            if (side == Direction.UP || side == Direction.DOWN) continue;
            if (forceSurround.get()) {
                BlockUtils.place(mc.player.getBlockPos().offset(side), InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem()))), rotate.get(), 20, checkEntity.get());
                renderBlocks.add(renderBlockPool.get().set(mc.player.getBlockPos().offset(side)));
            }
        }
        for (BlockPos p : posPlaceBlocks) {
            if (BlockUtils.canPlace(p, checkEntity.get()) && ticks <= bpt.get()) {
                if (anchor.get()) ((IVec3d) mc.player.getVelocity()).set(0, mc.player.getVelocity().y, 0);
                BlockUtils.place(p, InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem()))), rotate.get(), 20, checkEntity.get());
                renderBlocks.add(renderBlockPool.get().set(p));
                ticks++;
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        assert mc.player != null;
        assert mc.world != null;

            if (mode.get() == CrystalMode.None || (onlyHole.get() && !isSurrounded(mc.player))) return;
            for (Entity crystal : mc.world.getEntities()) {
                if (crystalTicks >= crystalDelay.get()) {
                    crystalTicks = 0;
                if (crystal instanceof EndCrystalEntity && crystal.age >= crystalAge.get() && (mc.player.distanceTo(crystal) < breakRange.get() || antiCev.get() && mc.player.getEyePos().add(0, 1, 0).distanceTo(crystal.getPos()) < 1.2)) {
                    pos = crystal.getBlockPos();
                    switch (mode.get()) {
                        case Always -> attack(crystal);
                        case Legs -> {
                            if (pos.getY() <= mc.player.getBlockPos().getY() || antiCev.get() && mc.player.getEyePos().add(0, 1, 0).distanceTo(crystal.getPos()) < 1.2)
                                attack(crystal);
                        }
                    }
                    crystalRemoved = true;
                } else if (crystalRemoved) {
                    switch (placeMode.get()) {
                        case Always -> place();
                        case Legs -> {
                            if (pos.getY() <= mc.player.getBlockPos().getY() || antiCev.get() && mc.player.getEyePos().add(0, 1, 0).distanceTo(crystal.getPos()) < 1.2)
                                place();
                        }
                    }
                    crystalRemoved = false;
                }
            } else crystalTicks++;
        }
    }

    private void attack(Entity target) {
        switch (hitMode.get()) {
            case Default -> mc.interactionManager.attackEntity(mc.player, target);
            case Packet -> mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
            case Both -> {
                mc.interactionManager.attackEntity(mc.player, target);
                mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
            }
        }
    }

    public void place() {
            BlockUtils.place(pos, InvUtils.find(Items.OBSIDIAN), false, 50, checkEntity.get());
    }

    private boolean blockFilter(Block block) {
        return block.getBlastResistance() >= 600;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get()) {
            renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
            renderBlocks.forEach(renderBlock -> renderBlock.render(event, sideColor.get(), lineColor.get(), shapeMode.get()));
        }
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof TeleportConfirmC2SPacket && disableOnTp.get()) toggle();
    }

    public static class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public int ticks;
        public RenderBlock set(BlockPos blockPos) {
            pos.set(blockPos);
            ticks = 8;
            return this;
        }
        public void tick() {
            ticks--;
        }
        public void render(Render3DEvent event, Color sides, Color lines, ShapeMode shapeMode) {
            int preSideA = sides.a;
            int preLineA = lines.a;
            sides.a *= (double) ticks / 8;
            lines.a *= (double) ticks / 8;
            event.renderer.box(pos, sides, lines, shapeMode, 0);
            sides.a = preSideA;
            lines.a = preLineA;
        }
    }

    private boolean inCenter() {
        if (mc.player == null) {
            return false;
        }
        if (mc.world == null) {
            return false;
        }
        if (mc.interactionManager == null) {
            return false;
        }
        int count = 0;
        if (mc.player.getBlockPos().equals(new BlockPos(mc.player.getX() - (mc.player.getWidth() + 0.1) / 2.0, mc.player.getY(), mc.player.getZ() - (mc.player.getWidth() + 0.1) / 2.0))) {
            count++;
        }
        if (mc.player.getBlockPos().equals(new BlockPos(mc.player.getX() + (mc.player.getWidth() + 0.1) / 2.0, mc.player.getY(), mc.player.getZ() + (mc.player.getWidth() + 0.1) / 2.0))) {
            count++;
        }
        if (mc.player.getBlockPos().equals(new BlockPos(mc.player.getX() - (mc.player.getWidth() + 0.1) / 2.0, mc.player.getY(), mc.player.getZ() + (mc.player.getWidth() + 0.1) / 2.0))) {
            count++;
        }
        if (mc.player.getBlockPos().equals(new BlockPos(mc.player.getX() + (mc.player.getWidth() + 0.1) / 2.0, mc.player.getY(), mc.player.getZ() - (mc.player.getWidth() + 0.1) / 2.0))) {
            count++;
        }
        return count == 4;
    }

    private double z(double a, double b, double c, double d, double e) {
        return (e - a) * (d - b) / (c - a) + b;
    }

    private double x(double a, double b, double c, double d, double e) {
        return (e - b) * (c - a) / (d - b) + a;
    }

    private void pause() {
        if (stop.get()) {
            mc.options.keyJump.setPressed(false);
            mc.options.keySprint.setPressed(false);
            mc.options.keyForward.setPressed(false);
            mc.options.keyBack.setPressed(false);
            mc.options.keyLeft.setPressed(false);
            mc.options.keyRight.setPressed(false);
        }
    }
}
