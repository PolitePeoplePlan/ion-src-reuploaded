package me.ghosttypes.ghostware.modules.combat;

import me.ghosttypes.ghostware.modules.hud.visual.NotificationsHUD;
import me.ghosttypes.ghostware.modules.misc.Notifications;
import me.ghosttypes.ghostware.utils.Categories;
import me.ghosttypes.ghostware.utils.Wrapper;
import me.ghosttypes.ghostware.utils.world.Loader;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IClientPlayerInteractionManager;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

public class CevBreaker extends Module {
    //TODO rewrite the entire module lmao

    // This was more or less pasted from meteor+ because i cbf to make a cevbreaker but with all the self trap apes
    // I might as well code a working one. This is broken and only works for the first iteration. basically have to toggle it each time for it to work
    private final SettingGroup sgDefault = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> targetRange = sgDefault.add(new IntSetting.Builder().name("target-range").description("Maximum targetting range.").defaultValue(4).min(0).sliderMax(6).build());
    private final Setting<Integer> tickDelay = sgDefault.add(new IntSetting.Builder().name("tick-delay").description("The delay in ticks in between actions.").defaultValue(2).min(0).sliderMax(10).build());
    public final Setting<MineMode> mineMode = sgDefault.add(new EnumSetting.Builder<MineMode>().name("mine-mode").description("How to mine blocks.").defaultValue(MineMode.Server).build());
    private final Setting<Boolean> antiStuck = sgDefault.add(new BoolSetting.Builder().name("anti-stuck").description("Replaces a crystal above the obsidian block if it's broken.").defaultValue(true).build());
    private final Setting<Boolean> instant = sgDefault.add(new BoolSetting.Builder().name("instant").description("Attempts to break the obsidian instantly.").defaultValue(true).visible(() -> mineMode.get() == MineMode.Server).build());
    private final Setting<Boolean> rotate = sgDefault.add(new BoolSetting.Builder().name("rotate").description("Rotate on block interactions.").defaultValue(true).build());
    private final Setting<Notifications.mode> notifyMode = sgDefault.add(new EnumSetting.Builder<Notifications.mode>().name("notification-mode").description("How notifications are displayed.").defaultValue(Notifications.mode.Chat).build());

    // Render
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Render where the obsidian will be placed.").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The color of the sides of the blocks being rendered.").defaultValue(new SettingColor(204, 0, 0, 45)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The color of the lines of the blocks being rendered.").defaultValue(new SettingColor(204, 0, 0, 255)).build());

    private int tickDelayLeft, stage;
    private boolean shouldAttack;
    private PlayerEntity target;
    private BlockPos blockPos;

    public CevBreaker() {
        super(Categories.Combat, "cev-breaker", "Automatically funny crystals the closest target. Doesn't work on strict servers.");
    }

    @Override
    public void onActivate() {
        Loader.moduleAuth();
        stage = 1;
        tickDelayLeft = 0;
        shouldAttack = true;
    }

    @EventHandler
    private void onPreTick(TickEvent.Pre event) {
        target = TargetUtils.getPlayerTarget(targetRange.get(), SortPriority.LowestDistance);
        if (target == null) {
            notify("No target in range.");
            toggle();
            return;
        }
        if (tickDelayLeft <= 0) {
            funnyCrystal();
            tickDelayLeft = tickDelay.get();
        }
        tickDelayLeft--;
    }

    private void funnyCrystal() {
        blockPos = target.getBlockPos().add(0, 2, 0);

        switch (stage) {
            case 1 -> {
                if (tickDelayLeft <= 0) {
                    FindItemResult obbySlot = InvUtils.findInHotbar(Items.OBSIDIAN);
                    if (!obbySlot.found()) {
                        notify("No obsidian in hotbar.");
                        toggle();
                        return;
                    }
                    BlockUtils.place(blockPos, obbySlot, rotate.get(), 50, true);
                    stage = 2;
                }
            }
            case 2 -> {
                if (tickDelayLeft <= 0) {
                    FindItemResult crystalSlot = InvUtils.findInHotbar(Items.END_CRYSTAL);
                    int prevSlot = mc.player.getInventory().selectedSlot;
                    if (!crystalSlot.found() && mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL) {
                        notify("No crystals in hotbar.");
                        toggle();
                        return;
                    }
                    if (mc.world.getBlockState(blockPos).isAir()) stage = 1;
                    if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 50, () -> placeCrystal(crystalSlot, prevSlot));
                    else placeCrystal(crystalSlot, prevSlot);
                    stage = 3;
                }
            }
            case 3 -> {
                if (tickDelayLeft <= 0) {
                    FindItemResult pickSlot = InvUtils.findInHotbar(itemStack -> {
                            Item item = itemStack.getItem();
                            return (item instanceof PickaxeItem);
                        }
                    );
                    if (!pickSlot.found()) {
                        toggle();
                        notify("No pickaxe in hotbar.");
                        return;
                    }
                    if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 50, () -> attackBlock(pickSlot));
                    else attackBlock(pickSlot);

                    if (mc.world.getBlockState(blockPos).isAir()) stage = 4;
                }
            }
            case 4 -> {
                if (tickDelayLeft <= 0) {
                    if (mc.world.getBlockState(blockPos).isAir()) {
                        for (Entity entity : mc.world.getEntities()) {
                            if (mc.player.distanceTo(entity) <= targetRange.get() && entity instanceof EndCrystalEntity) {
                                if (rotate.get()) {
                                    Rotations.rotate(Rotations.getYaw(entity), Rotations.getPitch(entity, Target.Feet), 50, () -> hitCrystal(entity));
                                } else hitCrystal(entity);
                                stage = 1;
                            }
                        }
                    }
                    if (BlockUtils.canPlace(blockPos.add(0, 1, 0), true) && antiStuck.get()) {
                        FindItemResult crystalSlot = InvUtils.findInHotbar(Items.END_CRYSTAL);
                        int prevSlot = mc.player.getInventory().selectedSlot;
                        if (crystalSlot.found()) {
                            if (rotate.get()) Rotations.rotate(Rotations.getYaw(blockPos), Rotations.getPitch(blockPos), 50, () -> placeCrystal(crystalSlot, prevSlot));
                            else placeCrystal(crystalSlot, prevSlot);
                        }
                    }
                }
            }
        }
    }

    private void notify(String msg) {
        String title = "[" + this.name + "] ";
        switch (notifyMode.get()) {
            case Chat -> info(msg);
            case Hud -> NotificationsHUD.addNotification(title + msg);
        }
    }

    private void placeCrystal(FindItemResult crystalSlot, int prevSlot) {
        if (mc.player.getOffHandStack().getItem() != Items.END_CRYSTAL) {
            mc.player.getInventory().selectedSlot = crystalSlot.getSlot();
            ((IClientPlayerInteractionManager) mc.interactionManager).syncSelected();
        }
        Hand hand = crystalSlot.getHand();
        mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(hand, new BlockHitResult(mc.player.getPos(), getDirection(blockPos), blockPos, false)));
        mc.player.swingHand(Hand.MAIN_HAND);
        if (hand == Hand.MAIN_HAND) mc.player.getInventory().selectedSlot = prevSlot;
    }

    private void attackBlock(FindItemResult pickSlot) {
        Wrapper.updateSlot(pickSlot.getSlot());
        if (shouldAttack) {
            if (mineMode.get() == MineMode.Server) mc.interactionManager.attackBlock(blockPos, Direction.DOWN);
            else mc.interactionManager.updateBlockBreakingProgress(blockPos, Direction.DOWN);
            if (instant.get() && mineMode.get() == MineMode.Server) shouldAttack = false;
        }
        if (!mc.world.getBlockState(blockPos).isAir() && mineMode.get() == MineMode.Server) mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.DOWN));
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private void hitCrystal(Entity entity) {
        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private Direction getDirection(BlockPos pos) {
        Vec3d eyesPos = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()), mc.player.getZ());
        for (Direction direction : Direction.values()) {
            RaycastContext raycastContext = new RaycastContext(eyesPos, new Vec3d(pos.getX() + 0.5 + direction.getVector().getX() * 0.5,
                pos.getY() + 0.5 + direction.getVector().getY() * 0.5,
                pos.getZ() + 0.5 + direction.getVector().getZ() * 0.5), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, mc.player);
            BlockHitResult result = mc.world.raycast(raycastContext);
            if (result != null && result.getType() == HitResult.Type.BLOCK && result.getBlockPos().equals(pos)) return direction;
        }
        if ((double) pos.getY() > eyesPos.y) return Direction.DOWN;
        return Direction.UP;
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onRender(Render3DEvent event) {
        if (!render.get() || blockPos == null || blockPos.getY() == -1 || mc.world.getBlockState(blockPos).isAir()) return;
        event.renderer.box(blockPos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    @Override
    public String getInfoString() {
        if (target != null) return target.getEntityName();
        return null;
    }

    public enum MineMode {
        Client,
        Server
    }
}
