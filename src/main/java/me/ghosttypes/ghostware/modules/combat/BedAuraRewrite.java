package me.ghosttypes.ghostware.modules.combat;

import me.ghosttypes.ghostware.modules.hud.visual.NotificationsHUD;
import me.ghosttypes.ghostware.modules.misc.Notifications;
import me.ghosttypes.ghostware.utils.Categories;
import me.ghosttypes.ghostware.utils.Wrapper;
import me.ghosttypes.ghostware.utils.combat.BedUtils;
import me.ghosttypes.ghostware.utils.combat.MineUtil;
import me.ghosttypes.ghostware.utils.misc.ItemCounter;
import me.ghosttypes.ghostware.utils.player.AutomationUtils;
import me.ghosttypes.ghostware.utils.player.InvUtil;
import me.ghosttypes.ghostware.utils.world.Loader;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BedBlock;
import net.minecraft.block.entity.BedBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.Item;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class BedAuraRewrite extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRotation = settings.createGroup("Rotation");
    private final SettingGroup sgTargeting = settings.createGroup("Targeting");
    private final SettingGroup sgPopOverride = settings.createGroup("PopOverride");
    private final SettingGroup sgTrap = settings.createGroup("Trap");
    private final SettingGroup sgAutoMove = settings.createGroup("Inventory");
    private final SettingGroup sgAutomation = settings.createGroup("Automation");
    private final SettingGroup sgSafety = settings.createGroup("Safety");
    private final SettingGroup sgPause = settings.createGroup("Pause");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General
    public final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder().name("debug").description("Spam chat with shit you won't understand.").defaultValue(false).build());
    public final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").description("The delay between placing beds in ticks.").defaultValue(9).min(0).sliderMax(20).build());
    public final Setting<Boolean> strictDirection = sgGeneral.add(new BoolSetting.Builder().name("strict-direction").description("Only places beds in the direction you are facing.").defaultValue(false).build());
    public final Setting<BreakHand> breakHand = sgGeneral.add(new EnumSetting.Builder<BreakHand>().name("break-hand").description("Which hand to break beds with.").defaultValue(BreakHand.Offhand).build());
    public final Setting<Notifications.mode> notifyMode = sgGeneral.add(new EnumSetting.Builder<Notifications.mode>().name("notification-mode").description("How notifications are displayed.").defaultValue(Notifications.mode.Chat).build());

    // Delay
    public final Setting<Integer> placeDelayHole = sgGeneral.add(new IntSetting.Builder().name("place-delay-hole").description("The delay between placing beds in ticks if the target is in a hole.").defaultValue(9).min(0).sliderMax(20).build());
    public final Setting<Integer> placeDelayMoving = sgGeneral.add(new IntSetting.Builder().name("place-delay-moving").description("The delay between placing beds in ticks if the target is moving.").defaultValue(7).min(0).sliderMax(20).build());
    public final Setting<Boolean> instantBreak = sgGeneral.add(new BoolSetting.Builder().name("instant-break").description("Break beds in the same tick they are placed in.").defaultValue(false).build());
    public final Setting<Integer> breakDelayHole = sgGeneral.add(new IntSetting.Builder().name("break-delay-hole").description("The delay between placing beds in ticks if the target is in a hole.").defaultValue(9).min(0).sliderMax(20).build());
    public final Setting<Integer> breakDelayMoving = sgGeneral.add(new IntSetting.Builder().name("place-delay-moving").description("The delay between placing beds in ticks if the target is moving.").defaultValue(7).min(0).sliderMax(20).build());

    // Rotation
    public final Setting<BedRotate> bedRotation = sgRotation.add(new EnumSetting.Builder<BedRotate>().name("bed-rotations").description("How to rotate on bed place/break.").defaultValue(BedRotate.Place).build());
    public final Setting<MineUtil.BlockRotate> blockRotation = sgRotation.add(new EnumSetting.Builder<MineUtil.BlockRotate>().name("block-rotations").description("How to rotate on block place/break.").defaultValue(MineUtil.BlockRotate.None).build());
    public final Setting<Integer> rotatePrio = sgRotation.add(new IntSetting.Builder().name("rotate-priority").description("Rotation priority").defaultValue(50).min(1).sliderMax(100).max(100).build());

    // Targeting
    // I feel that it's better to have separate modes for moving and hole, bc Fast is faster when the target is in a hole, but Strong is faster on moving targets. Can also depend on ping so it's
    // just more config stuff. The less stuff left up to the module to decide which is best, the better
    public final Setting<DamageCalc> holeDamageMode = sgTargeting.add(new EnumSetting.Builder<DamageCalc>().name("hole-damage-calc").description("How to calculate damage to targets in a hole.").defaultValue(DamageCalc.Fast).build());
    public final Setting<DamageCalc> movingDamgeMode = sgTargeting.add(new EnumSetting.Builder<DamageCalc>().name("moving-damage-calc").description("How to calculate damage to moving targets.").defaultValue(DamageCalc.Strong).build());
    //public final Setting<DamageCalc> damageMode = sgTargeting.add(new EnumSetting.Builder<DamageCalc>().name("damage-mode").description("Which damage calculation mode to use.").defaultValue(DamageCalc.Single).build());
    public final Setting<Boolean> predictMovement = sgTargeting.add(new BoolSetting.Builder().name("predict").description("Predict where to place next.").defaultValue(false).build());
    public final Setting<Boolean> predictIgnoreElytra = sgTargeting.add(new BoolSetting.Builder().name("ignore-elytra").description("Ignore predict if you or the target is in an elytra.").defaultValue(false).build());
    public final Setting<Double> targetRange = sgTargeting.add(new DoubleSetting.Builder().name("target-range").description("The range at which players can be targeted.").defaultValue(4).min(0).sliderMax(5).build());
    public final Setting<SortPriority> priority = sgTargeting.add(new EnumSetting.Builder<SortPriority>().name("target-priority").description("How to filter the players to target.").defaultValue(SortPriority.LowestHealth).build());
    public final Setting<Double> minDamage = sgTargeting.add(new DoubleSetting.Builder().name("min-damage").description("The minimum damage to inflict on your target.").defaultValue(7).min(0).max(36).sliderMax(36).build());
    public final Setting<Double> maxSelfDamage = sgTargeting.add(new DoubleSetting.Builder().name("max-self-damage").description("The maximum damage to inflict on yourself.").defaultValue(7).min(0).max(36).sliderMax(36).build());
    public final Setting<Boolean> antiSuicide = sgTargeting.add(new BoolSetting.Builder().name("anti-suicide").description("Will not place and break beds if they will kill you.").defaultValue(true).build());

    // Pop Override
    public final Setting<Boolean> selfDamageBypass = sgPopOverride.add(new BoolSetting.Builder().name("pop-override").description("Ignore max self damage if the target pops and you wont.").defaultValue(false).build());
    public final Setting<Double> selfDamageBypassHPbefore = sgPopOverride.add(new DoubleSetting.Builder().name("min-health").description("How much health you must have.").defaultValue(10).min(1).max(36).sliderMax(36).build());
    public final Setting<Double> selfDamageBypassHPafter = sgPopOverride.add(new DoubleSetting.Builder().name("min-health-after").description("How much health you must have after placing.").defaultValue(6).min(1).max(36).sliderMax(36).build());

    // Inventory
    public final Setting<Boolean> autoMove = sgAutoMove.add(new BoolSetting.Builder().name("auto-move").description("Moves beds into a selected hotbar slot.").defaultValue(false).build());
    public final Setting<Integer> autoMoveSlot = sgAutoMove.add(new IntSetting.Builder().name("auto-move-slot").description("The slot auto move moves beds to.").defaultValue(9).min(1).max(9).sliderMin(1).sliderMax(9).visible(autoMove::get).build());
    public final Setting<Boolean> autoSwitch = sgAutoMove.add(new BoolSetting.Builder().name("auto-switch").description("Switches to and from beds automatically.").defaultValue(true).build());
    public final Setting<Boolean> restoreOnDisable = sgAutoMove.add(new BoolSetting.Builder().name("restore-on-disable").description("Put whatever was in your auto move slot back after disabling.").defaultValue(true).build());
    public final Setting<Integer> minBeds = sgAutoMove.add(new IntSetting.Builder().name("min-beds").description("How many beds are required in your inventory to place.").defaultValue(2).min(1).build());

    // Trap
    public final Setting<Boolean> autoTrap = sgTrap.add(new BoolSetting.Builder().name("auto-trap").description("Prevent the target from escaping before placing beds.").defaultValue(true).build());
    public final Setting<Boolean> autoTrapHoleOnly = sgTrap.add(new BoolSetting.Builder().name("hole-only").description("Only trap the target if they are in a hole.").defaultValue(false).build());
    public final Setting<Boolean> autoTrapHold = sgTrap.add(new BoolSetting.Builder().name("hold").description("Wait for the target to be trapped before placing beds.").defaultValue(false).build());
    public final Setting<Boolean> autoTrapBypassObby = sgTrap.add(new BoolSetting.Builder().name("bypass-on-no-obby").description("Will place normally rather than stopping if you're out of obby.").defaultValue(false).build());

    // Automation
    public final Setting<MineUtil.MineMode> mineMode = sgTargeting.add(new EnumSetting.Builder<MineUtil.MineMode>().name("mine-mode").description("How to mine blocks.").defaultValue(MineUtil.MineMode.Server).build());
    public final Setting<Boolean> breakSelfTrap = sgAutomation.add(new BoolSetting.Builder().name("break-self-trap").description("Break target's self-trap automatically.").defaultValue(true).build());
    public final Setting<Boolean> randomizeSelfTrap = sgAutomation.add(new BoolSetting.Builder().name("randomize").description("Break a random block from the target's self trap. Can improve success rate.").defaultValue(false).build());
    public final Setting<Boolean> breakBurrow = sgAutomation.add(new BoolSetting.Builder().name("break-burrow").description("Break target's burrow automatically.").defaultValue(true).build());
    public final Setting<Boolean> breakWeb = sgAutomation.add(new BoolSetting.Builder().name("break-web").description("Break target's webs/string automatically.").defaultValue(true).build());
    public final Setting<Boolean> renderAutomation = sgAutomation.add(new BoolSetting.Builder().name("render-break").description("Render mining self-trap/burrow.").defaultValue(false).build());
    public final Setting<Boolean> disableOnNoBeds = sgAutomation.add(new BoolSetting.Builder().name("disable-on-no-beds").description("Disable if you run out of beds.").defaultValue(false).build());

    // Safety
    public final Setting<Boolean> disableOnSafety = sgSafety.add(new BoolSetting.Builder().name("disable-on-safety").description("Disable BedAuraPlus when safety activates.").defaultValue(true).build());
    public final Setting<Double> safetyHP = sgSafety.add(new DoubleSetting.Builder().name("safety-hp").description("What health safety activates at.").defaultValue(10).min(1).max(36).sliderMax(36).build());
    public final Setting<Boolean> safetyGapSwap = sgSafety.add(new BoolSetting.Builder().name("swap-to-gap").description("Swap to egaps after activating safety.").defaultValue(false).build());

    // Pause
    public final Setting<Boolean> pauseOnEat = sgPause.add(new BoolSetting.Builder().name("pause-on-eat").description("Pauses while eating.").defaultValue(true).build());
    public final Setting<Boolean> pauseOnDrink = sgPause.add(new BoolSetting.Builder().name("pause-on-drink").description("Pauses while drinking.").defaultValue(true).build());
    public final Setting<Boolean> pauseOnMine = sgPause.add(new BoolSetting.Builder().name("pause-on-mine").description("Pauses while mining.").defaultValue(true).build());
    public final Setting<Boolean> pauseOnCa = sgPause.add(new BoolSetting.Builder().name("pause-on-ca").description("Pause while Crystal Aura is active.").defaultValue(false).build());
    public final Setting<Boolean> pauseOnCraft = sgPause.add(new BoolSetting.Builder().name("pause-on-crafting").description("Pauses while you're in a crafting table.").defaultValue(false).build());

    // Render
    public final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").description("Whether to swing hand clientside clientside.").defaultValue(true).build());
    public final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders the block where it is placing a bed.").defaultValue(true).build());
    public final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    public final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211,75)).build());
    public final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color for positions to be placed.").defaultValue(new SettingColor(15, 255, 211)).build());
    public final Setting<Boolean> renderDamage = sgRender.add(new BoolSetting.Builder().name("render-damage").description("Renders bed damage.").defaultValue(true).build());
    public final Setting<Integer> roundDamage = sgRender.add(new IntSetting.Builder().name("round-damage").description("Round damage to x decimal places.").defaultValue(2).min(0).max(3).sliderMax(3).build());
    public final Setting<Double> damageScale = sgSafety.add(new DoubleSetting.Builder().name("damage-scale").description("The scale of the damage text.").defaultValue(1.4).min(0).max(5.0).sliderMax(5.0).build());
    public final Setting<SettingColor> damageColor = sgRender.add(new ColorSetting.Builder().name("damage-color").description("The color of the damage text.").defaultValue(new SettingColor(15, 255, 211)).build());


    public CardinalDirection direction;
    private PlayerEntity target;
    private BlockPos placePos, breakPos, stb;
    private int placeTimer, breakTimer, webTimer;
    private Item ogItem;
    private boolean sentTrapMine, sentBurrowMine, safetyToggled, clientTrapMining, clientBurrowMining;
    public double nextDamage;


    public BedAuraRewrite() {
        super(Categories.Combat, "bed-aura-rewrite", "The famous troll module.");
    }

    @Override
    public void onActivate() {
        Loader.moduleAuth();
        target = null;
        placePos = null;
        breakPos = null;
        ogItem = Wrapper.getItemFromSlot(autoMoveSlot.get() - 1);
        if (ogItem instanceof BedItem) ogItem = null; //ignore if we already have a bed there.
        safetyToggled = false;
        sentTrapMine = false;
        sentBurrowMine = false;
        clientTrapMining = false;
        clientBurrowMining = false;
        placeTimer = 0;
        breakTimer = 0;
        webTimer = 0;
        direction = CardinalDirection.North;
        stb = null;
    }

    @Override
    public void onDeactivate() {
        if (safetyToggled) {
            notify("Your health is too low!");
            if (safetyGapSwap.get()) {
                FindItemResult gap = InvUtil.findEgap();
                if (gap.found()) mc.player.getInventory().selectedSlot = gap.getSlot();
            }
        }
        if (!safetyToggled && restoreOnDisable.get() && ogItem != null) {
            FindItemResult ogItemInv = InvUtils.find(ogItem);
            if (ogItemInv.found()) InvUtils.move().from(ogItemInv.getSlot()).toHotbar(autoMoveSlot.get() - 1);
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        CrystalAura ca = Modules.get().get(CrystalAura.class);
        if (PlayerUtils.getTotalHealth() <= safetyHP.get()) {
            if (disableOnSafety.get()) {
                safetyToggled = true;
                toggle();
            }
            return;
        }

        // Initial Dimension Check + Pauses
        if (debug.get()) info("Checking pauses");
        if (mc.world.getDimension().isBedWorking()) { notify( "Beds don't work here monke!"); toggle(); return; }
        if (PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) return;
        if (pauseOnCraft.get() && mc.player.currentScreenHandler instanceof CraftingScreenHandler) return;
        if (ItemCounter.beds() < minBeds.get()) return;
        if (pauseOnCa.get() && ca.isActive()) return;
        if (MineUtil.override) return;

        // AutoEz check
        if (debug.get()) info("Checking autoEz");

        // Targeting
        if (debug.get()) info("Targeting");
        target = TargetUtils.getPlayerTarget(targetRange.get(), priority.get());
        if (TargetUtils.isBadTarget(target, targetRange.get())) target = null;
        if (target == null) { placePos = null; breakPos = null; stb = null; sentTrapMine = false; sentBurrowMine = false; clientTrapMining = false; clientBurrowMining = false; if (debug.get()) info("Target is null"); return; }

        // Auto Move
        if (autoMove.get()) {
            if (debug.get()) info("Auto move");
            FindItemResult bed = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
            if (bed.found() && bed.getSlot() != autoMoveSlot.get() - 1) { InvUtils.move().from(bed.getSlot()).toHotbar(autoMoveSlot.get() - 1); }
            if (!bed.found() && disableOnNoBeds.get()) { notify("You've run out of beds! Disabling."); toggle(); return; }
        }

        // Find Place
        if (breakPos == null) {
            if (debug.get()) info("Finding break pos");
            placePos = BedUtils.findPlaceWrapper(target);
            //switch (damageMode.get()) {
            //    case Single -> placePos = BedUtils.findPlace(target);
            //    case Strong -> placePos = BedUtils.findPlaceStrong(target);
            //}
        }

        // Auto Trap
        if (autoTrap.get() && BlockUtils.canPlace(target.getBlockPos().up(2))) {
            if (debug.get()) info("Checking auto trap");
            boolean doTrap = true;
            if (placePos != null) {
                if (autoTrapHoleOnly.get() && !Wrapper.isInHole(target)) doTrap = false;
            } else {
                doTrap = false;
            }
            if (doTrap) {
                if (debug.get()) info("Doing trap");
                FindItemResult obby = InvUtil.findObby();
                if (obby.found()) {
                    // rotation check
                    boolean rotate = false;
                    switch (blockRotation.get()) { case Place, Both -> rotate = true;}
                    if (debug.get()) info("Placing trap block");
                    boolean placed = BlockUtils.place(target.getBlockPos().up(2), obby, rotate, rotatePrio.get(), true, true, true);
                    if (!placed && autoTrapHold.get()) {
                        if (debug.get()) info("Didn't place, holding because trapHold=true");
                        return;
                    }
                } else {
                    if (debug.get()) info("No obby to trap with");
                    if (!autoTrapBypassObby.get()) return;
                    if (debug.get()) info("Ignoring because bypassObby=true");
                }
            }
        }

        // Automation
        if (debug.get()) info("Automation");
        // resetting vars if the mining job is finished
        if (sentBurrowMine && !AutomationUtils.isBurrowed(target, false)) resetBurrowVars();

        // rotation check
        boolean rotate = false;
        switch (blockRotation.get()) { case Place, Both -> rotate = true;}

        // Self Trap Breaking (rewrite)
        if (breakSelfTrap.get()) {
            FindItemResult pick = InvUtil.findPick();
            // first, check for a pick
            if (!pick.found()) {
                notify("No pickaxe in hotbar.");
                toggle();
                return;
            }
            // check if we have no valid placement for beds + check if they are self trapped
            if (placePos == null && BedUtils.isSelfTrapped(target)) {
                // notify the user we are breaking the trap
                if (!clientBurrowMining && !sentTrapMine) notify("Breaking " + target.getEntityName() + "'s sent trap");
                // client mining mode
                if (mineMode.get() == MineUtil.MineMode.Client && BedUtils.isValidTrapBlock(BedUtils.currentTrapBlockClient)) {
                    Wrapper.updateSlot(pick.getSlot()); // force hold a pick
                    AutomationUtils.doRegularMine(BedUtils.currentTrapBlockClient, rotate, rotatePrio.get());
                    clientTrapMining = true;
                } else { // if the current trap block is invalid or we have none yet, get one
                    BedUtils.currentTrapBlockClient = BedUtils.getSelfTrapBlock(target, autoTrap.get(), randomizeSelfTrap.get());
                }
                // packet mining mode
                if (mineMode.get() != MineUtil.MineMode.Client && BedUtils.isValidTrapBlock(BedUtils.currentTrapBlockPacket)) {
                    if (!sentTrapMine) {
                        MineUtil.handlePacketMine(stb, mineMode.get(), rotate, rotatePrio.get());
                        sentTrapMine = true;
                    }
                } else { // if the current trap block is invalid or we have none yet, get one
                    BedUtils.currentTrapBlockPacket = BedUtils.getSelfTrapBlock(target, autoTrap.get(), randomizeSelfTrap.get());
                }
            } else {
                resetTrapVars(); // if they aren't self trapped / we can place, reset vars
            }
        }

        // Self Trap Breaking (old)
        //if (breakSelfTrap.get() && shouldTrapMine()) {
        //    if (debug.get()) info("Self trap mining");
        //    FindItemResult pick = InvUtil.findPick();
        //    if (pick.found()) {
        //        //Wrapper.updateSlot(pick.getSlot());
        //        notify("Breaking " + target.getEntityName() + "'s self-trap.");
        //        stb = BedUtils.getSelfTrapBlock(target, autoTrap.get());
        //        if (mineMode.get() != MineUtil.MineMode.Client) { // check current mine mode
        //            if (debug.get()) info("Doing packet mine");
        //            MineUtil.handlePacketMine(stb, mineMode.get(), rotate, rotatePrio.get());
        //            sentTrapMine = true;
        //        } else {
        //            if (debug.get()) info("Doing client mine");
        //            AutomationUtils.doRegularMine(stb, rotate, rotatePrio.get()); // handle client mining
        //            clientTrapMining = true;
        //        }
        //        return;
        //  }
        //}
        // Burrow Breaking
        if (placePos == null && AutomationUtils.isBurrowed(target, false) && breakBurrow.get()) {
            if (debug.get()) info("Burrow mining");
            if (mineMode.get() != MineUtil.MineMode.Client) {
                if (sentBurrowMine || sentTrapMine) {
                    if (debug.get()) info("Already self trap mining or burrow mining, returning");
                    return;
                }
            } else {
                if (clientTrapMining) {
                    if (debug.get()) info("Already self trap mining or burrow mining, returning");
                    return;
                }
            }
            FindItemResult pick = InvUtil.findPick();
            if (pick.found()) {
                //Wrapper.updateSlot(pick.getSlot());
                notify("Breaking " + target.getEntityName() + "'s burrow.");
                if (mineMode.get() != MineUtil.MineMode.Client) { // check current mine mode
                    if (debug.get()) info("Doing packet mine");
                    MineUtil.handlePacketMine(target.getBlockPos(), mineMode.get(), rotate, rotatePrio.get()); // handle packet mining
                } else {
                    if (debug.get()) info("Doing burrow mine");
                    AutomationUtils.doRegularMine(target.getBlockPos(), rotate, rotatePrio.get()); // handle client mining
                    clientBurrowMining = true;
                }
                return;
                //AutomationUtils.doPacketMine(target.getBlockPos(), rotate, rotatePrio.get());
            }
        }
        // Web Breaking
        if (placePos == null && AutomationUtils.isWebbed(target) && breakWeb.get()) {
            if (debug.get()) info("Doing web mine");
            // check if we are mining self trap or burrow. check which mining mode we are using first, then check the appropriate booleans
            if (mineMode.get() != MineUtil.MineMode.Client) {
                if (sentBurrowMine || sentTrapMine) {
                    if (debug.get()) info("Already mining self trap or burrow, returning");
                    return;
                }
            } else {
                if (clientTrapMining || clientBurrowMining) {
                    if (debug.get()) info("Already mining self trap or burrow, returning");
                    return;
                }
            }
            FindItemResult sword = InvUtil.findSword();
            if (sword.found()) {
                Wrapper.updateSlot(sword.getSlot());
                if (webTimer <= 0) { // webTimer is to prevent getting stuck on apes spamming webs or string
                    notify("Breaking " + target.getEntityName() + "'s web.");
                    webTimer = 100;
                } else {
                    webTimer--;
                }
                AutomationUtils.mineWeb(target, sword.getSlot(), rotate, rotatePrio.get()); // handle client mining (no point in packet mining string/webs due to how short it takes to break. tested and its slower than
                // client side mining anyways.
                return;
            }
        }

        // Place
        if (debug.get()) info("Place");
        if (placeTimer <= 0 && placeBed(placePos)) {
            if (debug.get()) info("Place timer is 0");
            placeTimer = getNextPlaceTimer(target);
            if (debug.get()) info("Next place timer is " + placeTimer);
        } else {
            placeTimer--;
        }
        // Break
        if (debug.get()) info("Break");
        if (breakPos == null) breakPos = findBreak();
        if (breakTimer <= 0 || instantBreak.get()) {
            if (debug.get()) info("Break timer is 0 or instant");
            breakBed(breakPos);
            breakTimer = getNextBreakTimer(target);
            if (instantBreak.get()) {
                if (debug.get()) info("Next break is instant");
            } else {
                if (debug.get()) info("Next break timer is " + breakTimer);
            }
        } else {
            breakTimer--;
        }
    }


    private void resetTrapVars() {
        clientTrapMining = false;
        sentTrapMine = false;
        BedUtils.currentTrapBlockClient = null;
        BedUtils.currentTrapBlockPacket = null;
    }

    public void resetBurrowVars() {
        clientBurrowMining = false;
        sentBurrowMine = false;
    }

    private int getNextPlaceTimer(PlayerEntity target) {
        if (Wrapper.isPlayerMoving(target)) return placeDelayMoving.get();
        return placeDelayHole.get();
    }

    private int getNextBreakTimer(PlayerEntity target) {
        if (Wrapper.isPlayerMoving(target)) return breakDelayMoving.get();
        return breakDelayHole.get();
    }

    private void notify(String msg) {
        String title = "[" + this.name + "] ";
        switch (notifyMode.get()) {
            case Chat -> info(msg);
            case Hud -> NotificationsHUD.addNotification(title + msg);
        }
    }

    // Find a break pos
    private BlockPos findBreak() {
        boolean valid = false;
        boolean bypassSelfDmg;
        for (BlockEntity blockEntity : Utils.blockEntities()) {
            if (!(blockEntity instanceof BedBlockEntity)) continue;
            BlockPos bedPos = blockEntity.getPos();
            Vec3d bedVec = Utils.vec3d(bedPos);
            // check if the bed is in reach distance
            if (PlayerUtils.distanceTo(bedVec) <= mc.interactionManager.getReachDistance()) {
                // store damages
                double targetDamage = DamageUtils.bedDamage(target, bedVec);
                double selfDamage = DamageUtils.bedDamage(mc.player, bedVec);

                if (targetDamage >= minDamage.get()) { // check that it meets minDmg
                    if (selfDamageBypass.get()) {
                        bypassSelfDmg = BedUtils.shouldBypassSelfDmg(PlayerUtils.getTotalHealth(), selfDamage, selfDamage, targetDamage, target.getHealth() + target.getAbsorptionAmount(), antiSuicide.get(), true);
                    } else {
                        bypassSelfDmg = false;
                    }
                    if (!bypassSelfDmg) { // if we aren't bypassing minDmg, check selfDmg + anti suicide
                        if (selfDamage <= maxSelfDamage.get()) {
                            valid = !antiSuicide.get() || PlayerUtils.getTotalHealth() - selfDamage > 0;
                        }
                    } else { // if we are bypassing minDmg, just check anti suicide
                        valid = !antiSuicide.get() || PlayerUtils.getTotalHealth() - selfDamage > 0;
                    }
                }
                if (valid) return bedPos;
            }
        }
        return null;
    }

    private boolean placeBed(BlockPos pos) {
        if (pos == null) return false;
        FindItemResult bed = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BedItem);
        if (bed.getHand() == null && !autoSwitch.get()) return false;
        double yaw = switch (direction) {
            case East -> 90;
            case South -> 180;
            case West -> -90;
            default -> 0;
        };
        Rotations.rotate(yaw, Rotations.getPitch(pos), () -> {
            BlockUtils.place(pos, bed, false, 0, swing.get(), true);
            breakPos = pos;
        });
        return true;
    }

    private void breakBed(BlockPos pos) {
        if (pos == null) return;
        breakPos = null;
        //if (damageMode.get() == DamageCalc.Strong) placePos = null;
        if (!(mc.world.getBlockState(pos).getBlock() instanceof BedBlock)) return;
        boolean wasSneaking = mc.player.isSneaking();
        if (wasSneaking) mc.player.setSneaking(false);
        Hand bHand;
        if (breakHand.get() == BreakHand.Mainhand) { bHand = Hand.MAIN_HAND;
        } else { bHand = Hand.OFF_HAND; }
        if (bedRotation.get() == BedRotate.Both) {
            Rotations.rotate(Rotations.getYaw(pos), Rotations.getPitch(pos), () -> mc.interactionManager.interactBlock(mc.player, mc.world, bHand, new BlockHitResult(mc.player.getPos(), Direction.UP, pos, false)));
        } else {
            mc.interactionManager.interactBlock(mc.player, mc.world, bHand, new BlockHitResult(mc.player.getPos(), Direction.UP, pos, false));
        }
        mc.player.setSneaking(wasSneaking);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (render.get() && placePos != null && breakPos == null) {
            int x = placePos.getX();
            int y = placePos.getY();
            int z = placePos.getZ();

            switch (direction) {
                case North -> event.renderer.box(x, y, z, x + 1, y + 0.6, z + 2, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                case South -> event.renderer.box(x, y, z - 1, x + 1, y + 0.6, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                case East -> event.renderer.box(x - 1, y, z, x + 1, y + 0.6, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                case West -> event.renderer.box(x, y, z, x + 2, y + 0.6, z + 1, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        }
        if (renderAutomation.get() && target != null) {
            if (stb != null) event.renderer.box(stb, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (sentBurrowMine) event.renderer.box(target.getBlockPos(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (AutomationUtils.isWeb(target.getBlockPos())) event.renderer.box(target.getBlockPos(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            if (AutomationUtils.isWeb(target.getBlockPos().up())) event.renderer.box(target.getBlockPos().up(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (renderDamage.get() && placePos != null) {
            // store xyz for current break pos
            int x = placePos.getX();
            int y = placePos.getY();
            int z = placePos.getZ();
            // bedVec is for damage calc, bedVec2 is for render
            Vec3d bedVec = Utils.vec3d(placePos);
            Vec3 bedVec2 = new Vec3();
            bedVec2.set(x, y, z);
            if (NametagUtils.to2D(bedVec2, damageScale.get())) {
                NametagUtils.begin(bedVec2);
                TextRenderer.get().begin(1.0, false, true);
                // get the current place pos damage (calculated and stored from BedUtils find place methods)
                double damage = nextDamage;
                String damageText = "0";
                // handle damage rounding
                switch (roundDamage.get()) {
                    case 0 -> { damageText = String.valueOf(Math.round(damage)); }
                    case 1 -> { damageText = String.valueOf(Math.round(damage * 10.0) / 10.0); }
                    case 2 -> { damageText = String.valueOf(Math.round(damage * 100.0) / 100.0); }
                    case 3 -> { damageText = String.valueOf(Math.round(damage * 1000.0) / 1000.0); }
                }
                // render the damage
                final double w = TextRenderer.get().getWidth(damageText) / 2.0;
                TextRenderer.get().render(damageText, -w, 0.0, damageColor.get());
                TextRenderer.get().end();
                NametagUtils.end();
            }
        }
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }

    public enum BreakHand {
        Mainhand,
        Offhand
    }

    public enum DamageCalc {
        Fast,
        Strong
    }

    public enum BedRotate {
        Place,
        Both
    }
}
