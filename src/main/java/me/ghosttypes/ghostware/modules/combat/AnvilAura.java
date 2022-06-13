package me.ghosttypes.ghostware.modules.combat;

import me.ghosttypes.ghostware.utils.Categories;
import me.ghosttypes.ghostware.utils.Wrapper;
import me.ghosttypes.ghostware.utils.combat.AnvilUtil;
import me.ghosttypes.ghostware.utils.combat.MineUtil;
import me.ghosttypes.ghostware.utils.player.AutomationUtils;
import me.ghosttypes.ghostware.utils.player.InvUtil;
import me.ghosttypes.ghostware.utils.world.BlockHelper;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;


// Better auto anvil was the first thing i wrote for java
// so this is a proper recode of that now that i have 2% more understanding
// of the shit language known as java :fancytroll:
public class AnvilAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAutomation = settings.createGroup("Automation");

    // General
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder().name("range").description("Maximum targeting range.").defaultValue(4).min(0).build());
    public final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>().name("target-priority").description("How to filter the players to target.").defaultValue(SortPriority.LowestHealth).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").description("Delay between anvil placements.").min(1).defaultValue(4).sliderMax(50).build());
    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder().name("blocks-per-tick").description("Maximum blocks per tick.").defaultValue(4).min(2).max(8).sliderMin(2).sliderMax(8).build());
    private final Setting<RotationMode> rotateMode = sgGeneral.add(new EnumSetting.Builder<RotationMode>().name("rotation-mode").description("When to rotate.").defaultValue(RotationMode.Anvils).build());
    private final Setting<Integer> rotatePrio = sgGeneral.add(new IntSetting.Builder().name("rotation-priority").description("Rotation priority.").min(1).defaultValue(50).sliderMax(100).max(100).build());
    private final Setting<AnvilMode> anvilMode = sgGeneral.add(new EnumSetting.Builder<AnvilMode>().name("place-mode").description("How to place anvils.").defaultValue(AnvilMode.Airplace).build());
    private final Setting<Integer> airplaceHeight = sgGeneral.add(new IntSetting.Builder().name("height").description("How high to place anvils.").min(3).defaultValue(4).sliderMax(5).max(5).visible(() -> anvilMode.get() == AnvilMode.Airplace).build());
    private final Setting<Boolean> multiPlace = sgGeneral.add(new BoolSetting.Builder().name("multi-place").description("Place multiple anvils at once.").defaultValue(false).visible(() -> anvilMode.get() == AnvilMode.Airplace).build());
    private final Setting<ButtonMode> buttonMode = sgGeneral.add(new EnumSetting.Builder<ButtonMode>().name("place-mode").description("How to place anvils.").defaultValue(ButtonMode.Before).build());
    public final Setting<LegacyMode> legacyMode = sgGeneral.add(new EnumSetting.Builder<LegacyMode>().name("legacy-mode").description("Which support design to use.").defaultValue(LegacyMode.Speed).build());

    // Automation
    private final Setting<Boolean> stopOnAntiAnvil = sgAutomation.add(new BoolSetting.Builder().name("toggle-on-anti-anvil").description("Disable if the target places blocks above their head.").defaultValue(false).build());
    private final Setting<Boolean> breakAntiAnvil = sgAutomation.add(new BoolSetting.Builder().name("break-anti-anvil").description("Break blocks the target puts above their head.").defaultValue(true).build());
    public final Setting<MineUtil.MineMode> mineMode = sgAutomation.add(new EnumSetting.Builder<MineUtil.MineMode>().name("mine-mode").description("How to mine blocks.").defaultValue(MineUtil.MineMode.Server).build());


    private PlayerEntity target;
    private int bpt, timer;
    private boolean canPlaceAnvil;


    public AnvilAura() {
        super(Categories.Combat, "anvil-aura", "Nice helmet");
    }


    @Override
    public void onActivate() {
        target = null;
        bpt = 0;
        timer = delay.get();
        canPlaceAnvil = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        bpt = 0;

        // Targeting
        if (target == null) {
            target = TargetUtils.getPlayerTarget(range.get(), priority.get());
            if (TargetUtils.isBadTarget(target, range.get())) target = null;
            if (target == null) {
                error("No targets in range.");
                toggle();
                return;
            }
        }

        // Material Checks
        FindItemResult anvils = InvUtil.findAnvil();
        FindItemResult floor = InvUtil.findButton();
        if (!anvils.found()) {
            error("No anvils in hotbar.");
            toggle();
            return;
        }
        if (!floor.found()) {
            error("No buttons or pressure plates in hotbar.");
            toggle();
            return;
        }

        // Anti Anvil Stuff
        if (stopOnAntiAnvil.get() || breakAntiAnvil.get()) {
            BlockPos antiAnvilBlock = getAntiAnvilBlock(target);
            if (stopOnAntiAnvil.get() && antiAnvilBlock != null) {
                toggle();
                return;
            }
            boolean rotate = rotateMode.get() == RotationMode.Always || rotateMode.get() == RotationMode.Blocks;
            if (breakAntiAnvil.get() && antiAnvilBlock != null) {
                if (mineMode.get() == MineUtil.MineMode.Client) {
                    FindItemResult pick = InvUtil.findPick();
                    if (!pick.found()) {
                        error("No pickaxe in hotbar.");
                        toggle();
                    } else {
                        Wrapper.updateSlot(pick.getSlot());
                        AutomationUtils.doRegularMine(antiAnvilBlock, rotate, rotatePrio.get());
                    }
                } else {
                    MineUtil.handlePacketMine(antiAnvilBlock, mineMode.get(), rotate, rotatePrio.get());
                }
                return;
            }
        }

        // Before button mode
        if (buttonMode.get() == ButtonMode.Before && !AnvilUtil.isValidFloorBlock(target.getBlockPos())) {
            boolean rotate = rotateMode.get() == RotationMode.Blocks || rotateMode.get() == RotationMode.Always;
            BlockUtils.place(target.getBlockPos(), floor, rotate, rotatePrio.get(), true);
            bpt++;
        }

        // Anvil place timer
        if (timer <= 0) {
            timer = delay.get();
            canPlaceAnvil = true;
        } else {
            timer--;
        }

        // Legacy mode (comes first since we need to ensure the target has been trapped)
        if (anvilMode.get() == AnvilMode.Legacy) {
            FindItemResult obsidian = InvUtil.findObby();
            boolean rotate = rotateMode.get() == RotationMode.Blocks || rotateMode.get() == RotationMode.Always;
            if (!obsidian.found()) {
                error("No obsidian in hotbar.");
                toggle();
                return;
            }
            // get vec3d list design for which legacy mode is being used
            ArrayList<Vec3d> design = AnvilUtil.getDesign(target);
            if (!BlockHelper.isTargetVecComplete(target, design)) {
                // build the trap
                for (Vec3d b : design) {
                    if (bpt >= blocksPerTick.get()) return;
                    BlockPos ppos = target.getBlockPos();
                    BlockPos bb = ppos.add(b.x, b.y, b.z);
                    if (BlockHelper.getBlock(bb) == Blocks.AIR) {
                        BlockUtils.place(bb, obsidian, rotate, rotatePrio.get(), true);
                        bpt++;
                    }
                }
                return;
            } else {
                if (canPlaceAnvil) {
                    canPlaceAnvil = false;
                    // After button mode
                    if (buttonMode.get() == ButtonMode.After && !AnvilUtil.isValidFloorBlock(target.getBlockPos())) BlockUtils.place(target.getBlockPos(), floor, rotate, rotatePrio.get(), true);
                    rotate = rotateMode.get() == RotationMode.Anvils || rotateMode.get() == RotationMode.Always; // update rotation after placing button
                    int height = AnvilUtil.getLegacyAnvilPos(legacyMode.get()); // get the height to place anvils at based on which legacy mode is being used
                    BlockPos anvilPos = target.getBlockPos().up(height);
                    BlockUtils.place(anvilPos, anvils, rotate, rotatePrio.get());
                }
            }
        }


        // Airplace mode
        if (anvilMode.get() == AnvilMode.Airplace && canPlaceAnvil) {
            canPlaceAnvil = false;
            boolean rotate = rotateMode.get() == RotationMode.Anvils || rotateMode.get() == RotationMode.Always;
            BlockPos pp = target.getBlockPos();
            BlockPos anvilPos = pp.up(airplaceHeight.get());
            BlockUtils.place(anvilPos, anvils, rotate, rotatePrio.get());
            if (multiPlace.get()) {
                //int nextPlace = findMultiPlace(airplaceHeight.get());
                BlockUtils.place(anvilPos.up(2), anvils, rotate, rotatePrio.get());
            }
        }
    }


    private BlockPos getAntiAnvilBlock(PlayerEntity p) {
        BlockPos pp = p.getBlockPos();
        if (AutomationUtils.isTrapBlock(pp.up(2))) return pp.up(2);
        if (AutomationUtils.isTrapBlock(pp.up(3))) return pp.up(3);
        return null;
    }



    public enum AnvilMode {
        Airplace,
        Legacy
    }

    public enum LegacyMode {
        Speed,
        Damage
    }

    public enum RotationMode {
        Anvils,
        Blocks,
        Always,
        None
    }

    public enum ButtonMode {
        Before,
        After
    }
}
