package me.ghosttypes.ghostware.modules.combat;

import me.ghosttypes.ghostware.modules.hud.visual.NotificationsHUD;
import me.ghosttypes.ghostware.modules.misc.Notifications;
import me.ghosttypes.ghostware.utils.Categories;
import me.ghosttypes.ghostware.utils.Wrapper;
import me.ghosttypes.ghostware.utils.player.ArmorUtil;
import me.ghosttypes.ghostware.utils.world.BlockHelper;
import me.ghosttypes.ghostware.utils.world.Loader;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class SelfTrapPlus extends Module {

    public enum PlaceMode {
        AntiFacePlace,
        Full,
        Top,
        None
    }

    public enum Mode {
        Normal,
        Smart
    }


    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>().name("mode").description("Which mode to use").defaultValue(Mode.Normal).build());
    private final Setting<Boolean> smartBeds = sgGeneral.add(new BoolSetting.Builder().name("consider-beds").description("Fully trap yourself if a player has beds nearby").defaultValue(true).build());
    public final Setting<Double> smartRangeBeds = sgGeneral.add(new DoubleSetting.Builder().name("bed-check-range").description("How far to check for players holding beds.").defaultValue(5).min(0).sliderMax(5).build());
    private final Setting<Double> smartDura = sgGeneral.add(new DoubleSetting.Builder().name("smart-dura").description("How low an armor piece needs to be to fully trap.").defaultValue(2).min(1).sliderMin(1).sliderMax(100).max(100).build());
    private final Setting<PlaceMode> placeMode = sgGeneral.add(new EnumSetting.Builder<PlaceMode>().name("place-mode").description("Which positions to place at.").defaultValue(PlaceMode.Top).build());
    private final Setting<Boolean> antiCev = sgGeneral.add(new BoolSetting.Builder().name("anti-cev-breaker").description("Protect yourself from cev breaker.").defaultValue(true).build());
    private final Setting<Integer> blockPerTick = sgGeneral.add(new IntSetting.Builder().name("blocks-per-tick").description("How many block placements per tick.").defaultValue(4).sliderMin(1).sliderMax(10).build());
    private final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder().name("center").description("Centers you on the block you are standing on before placing.").defaultValue(true).build());
    private final Setting<Boolean> turnOff = sgGeneral.add(new BoolSetting.Builder().name("turn-off").description("Turns off after placing.").defaultValue(true).build());
    private final Setting<Boolean> toggleOnMove = sgGeneral.add(new BoolSetting.Builder().name("toggle-on-move").description("Turns off if you move (chorus, pearl phase etc).").defaultValue(true).build());
    private final Setting<Boolean> onlyInHole = sgGeneral.add(new BoolSetting.Builder().name("only-in-hole").description("Won't place unless you're in a hole").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Sends rotation packets to the server when placing.").defaultValue(true).build());
    private final Setting<Notifications.mode> notifyMode = sgGeneral.add(new EnumSetting.Builder<Notifications.mode>().name("notification-mode").description("How notifications are displayed.").defaultValue(Notifications.mode.Chat).build());

    // Render

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders a block overlay where the obsidian will be placed.").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The color of the sides of the blocks being rendered.").defaultValue(new SettingColor(204, 0, 0, 10)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The color of the lines of the blocks being rendered.").defaultValue(new SettingColor(204, 0, 0, 255)).build());

    private final List<BlockPos> placePositions = new ArrayList<>();
    private BlockPos startPos;
    private int bpt;

    private final ArrayList<Vec3d> full = new ArrayList<Vec3d>() {{
        add(new Vec3d(0, 2, 0));
        add(new Vec3d(1, 1, 0));
        add(new Vec3d(-1, 1, 0));
        add(new Vec3d(0, 1, 1));
        add(new Vec3d(0, 1, -1));
    }};

    private final ArrayList<Vec3d> antiFacePlace = new ArrayList<Vec3d>() {{
        add(new Vec3d(1, 1, 0));
        add(new Vec3d(-1, 1, 0));
        add(new Vec3d(0, 1, 1));
        add(new Vec3d(0, 1, -1));
    }};


    public SelfTrapPlus(){
        super(Categories.Combat, "self-trap-plus", "Places obsidian around your head.");
    }

    @Override
    public void onActivate() {
        Loader.moduleAuth();
        if (!placePositions.isEmpty()) placePositions.clear();
        if (center.get()) PlayerUtils.centerPlayer();
        startPos = mc.player.getBlockPos();
        bpt = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        bpt = 0;
        FindItemResult obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);
        if (!obsidian.found()) { notify("No obsidian in hotbar!"); toggle(); return; }
        if (BlockHelper.isVecComplete(getTrapDesign()) && turnOff.get()) { notify("Finished self trap."); toggle(); return;}
        if (toggleOnMove.get() && startPos != mc.player.getBlockPos()) { toggle(); return; }
        if (onlyInHole.get() && !Wrapper.isInHole(mc.player)) { toggle(); return; }
        if (antiCev.get()) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof EndCrystalEntity) {
                    BlockPos crystalPos = entity.getBlockPos();
                    if (crystalPos.equals(mc.player.getBlockPos().up(3)) || crystalPos.equals(mc.player.getBlockPos().up(4))) {
                        mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
                        mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    }
                    break;
                }
            }
        }
        for (Vec3d b : getTrapDesign()) {
            if (bpt >= blockPerTick.get()) return;
            BlockPos ppos = mc.player.getBlockPos();
            BlockPos bb = ppos.add(b.x, b.y, b.z);
            if (BlockHelper.getBlock(bb) == Blocks.AIR) {
                me.ghosttypes.ghostware.utils.world.BlockUtils.place(bb, obsidian, rotate.get(), 100, true);
                bpt++;
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get() || BlockHelper.isVecComplete(getTrapDesign())) return;
        for (Vec3d b: getTrapDesign()) {
            BlockPos ppos = mc.player.getBlockPos();
            BlockPos bb = ppos.add(b.x, b.y, b.z);
            if (BlockHelper.getBlock(bb) == Blocks.AIR) event.renderer.box(bb, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }

    private void notify(String msg) {
        String title = "[" + this.name + "] ";
        switch (notifyMode.get()) {
            case Chat -> info(msg);
            case Hud -> NotificationsHUD.addNotification(title + msg);
        }
    }

    private ArrayList<Vec3d> getTrapDesign() {
        ArrayList<Vec3d> trapDesign = new ArrayList<Vec3d>();
        if (mode.get() == Mode.Normal) {
            switch (placeMode.get()) {
                case Full -> { trapDesign.addAll(full); }
                case Top -> { trapDesign.add(new Vec3d(0, 2, 0)); }
                case AntiFacePlace -> { trapDesign.addAll(antiFacePlace); }
            }
        }
        if (mode.get() == Mode.Smart) {
            if (smartBeds.get()) {
                boolean usingFull = false;
                // Armor Check
                Iterable<ItemStack> armorPieces = mc.player.getArmorItems();
                for (ItemStack armorPiece : armorPieces) {
                    if (ArmorUtil.checkThreshold(armorPiece, smartDura.get())) {
                        trapDesign.addAll(full);
                        usingFull = true;
                        break;
                    }
                }
                // Beds Check
                if (!usingFull) {
                    for (Entity entity : mc.world.getEntities()) {
                        if (entity instanceof PlayerEntity player) {
                            if (player.getMainHandStack().getItem() instanceof BedItem && mc.player.distanceTo(player) <= smartRangeBeds.get()) {
                                trapDesign.addAll(full);
                                usingFull = true;
                                break;
                            }
                        }
                    }
                }
                // Use top mode if checks passed
                if (!usingFull) trapDesign.add(new Vec3d(0, 2, 0));
            }
        }
        if (antiCev.get()) { trapDesign.add(new Vec3d(0, 3, 0));}
        return trapDesign;
    }
}
