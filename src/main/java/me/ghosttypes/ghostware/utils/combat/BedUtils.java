package me.ghosttypes.ghostware.utils.combat;

import me.ghosttypes.ghostware.modules.combat.BedAuraRewrite;
import me.ghosttypes.ghostware.utils.Wrapper;
import me.ghosttypes.ghostware.utils.player.AutomationUtils;
import me.ghosttypes.ghostware.utils.world.BlockHelper;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BedItem;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BedUtils {
    // Globals
    public static BlockPos placePos = null;
    public static BlockPos currentTrapBlockClient = null;
    public static BlockPos currentTrapBlockPacket = null;

    public static Executor placeCalculator = Executors.newCachedThreadPool();
    //public static Executor placeCalculator = Executors.newFixedThreadPool(3);

    public static boolean isValidTrapBlock(BlockPos trapBlock) {
        if (trapBlock == null) return false;
        return AutomationUtils.isTrapBlock(trapBlock);
    }

    public static ArrayList<Vec3d> selfTrapPositions = new ArrayList<>() {{
        add(new Vec3d(1, 1, 0));
        add(new Vec3d(-1, 1, 0));
        add(new Vec3d(0, 1, 1));
        add(new Vec3d(0, 1, -1));
    }};

    public static boolean canPlace(BlockPos centerPos, CardinalDirection dir) {
        BedAuraRewrite ba = Modules.get().get(BedAuraRewrite.class);
        BlockPos offsetPos = centerPos.offset(dir.toDirection());
        if (ba.debug.get()) ChatUtils.info("offsetPos is " + offsetPos.getX() + "," + offsetPos.getY() + "," + offsetPos.getZ());
        boolean canPlace = mc.world.getBlockState(centerPos).getMaterial().isReplaceable() && BlockUtils.canPlace(centerPos.offset(dir.toDirection()));
        if (centerPos.getY() <= 1) {
            if (ba.debug.get()) ChatUtils.info("canPlace Y check failed");
            canPlace = false; // ignore below the void
        }
        return canPlace;
    }

    public static Boolean shouldBypassSelfDmg (double currentHP, double headSelfDamage, double offsetSelfDamage, double targetDmg, double targetHP, boolean antiSuicide, boolean selfDamageBypass) {
        if (!selfDamageBypass) return false;
        BedAuraRewrite ba = Modules.get().get(BedAuraRewrite.class);
        // store vars for pre/post health
        double minPreHealth = ba.selfDamageBypassHPbefore.get();
        double minPostHealth = ba.selfDamageBypassHPafter.get();
        boolean should = false;
        if (targetHP - targetDmg <= 0) { // check that the target will pop from the damage
            // check that our current health is >= minPreHealth, and that our health after is >= minPostHealth
            if (currentHP >= minPreHealth && currentHP - headSelfDamage >= minPostHealth && currentHP - offsetSelfDamage >= minPostHealth) {
                // check anti suicide
                should = !antiSuicide || PlayerUtils.getTotalHealth() - offsetSelfDamage > 0;
            }
        }
        return should;
    }

    public static Boolean damageCheck(PlayerEntity target, double minDamage, double maxSelfDamage, double headSelfDamage, double offsetSelfDamage, double targetDmg, boolean antiSuicide, boolean selfDamageBypass) {
        BedAuraRewrite ba = Modules.get().get(BedAuraRewrite.class);
        boolean valid = false;
        boolean bypassSelfDmg = shouldBypassSelfDmg(PlayerUtils.getTotalHealth(), headSelfDamage, offsetSelfDamage, targetDmg, target.getHealth() + target.getAbsorptionAmount(), antiSuicide, selfDamageBypass);
        boolean bypassMinDmg = (target.getHealth() + target.getAbsorptionAmount()) - targetDmg <= 0; // bypass minDamage if the bed will kill/pop the target
        if (targetDmg >= minDamage || bypassMinDmg) { // check minDmg
            //if (!selfDamageBypass) { // check if we are bypassing self dmg
            //    bypassSelfDmg = false;
            //} else {
            //    bypassSelfDmg = (target.getHealth() + target.getAbsorptionAmount()) - targetDmg <= 0;
            //}
            if (!bypassSelfDmg) { // if we aren't bypassing minSelfDmg, check selfDmg + anti suicide
                if (ba.debug.get()) ChatUtils.info("we are not bypassing self damage");
                if (offsetSelfDamage <= maxSelfDamage && headSelfDamage <= maxSelfDamage) {
                    valid = !antiSuicide || PlayerUtils.getTotalHealth() - offsetSelfDamage > 0;
                }
            } else { // if we are bypassing minDmg, just check anti suicide
                if (ba.debug.get()) ChatUtils.info("bypassing self damage, target will pop after taking " + targetDmg + " damage");
                valid = !antiSuicide || PlayerUtils.getTotalHealth() - offsetSelfDamage > 0;
            }
        }
        return valid;
    }


    public static BlockPos findPlaceWrapper(PlayerEntity target) {
        BedAuraRewrite ba = Modules.get().get(BedAuraRewrite.class);
        if (ba.debug.get()) ChatUtils.info("findPlaceWrapper running");
        // vars for damage calc modes
        BedAuraRewrite.DamageCalc damageCalc;
        BedAuraRewrite.DamageCalc movingCalcMode = ba.movingDamgeMode.get();
        BedAuraRewrite.DamageCalc holeCalcMode = ba.holeDamageMode.get();
        if (Wrapper.isPlayerMoving(target)) { // set damage calc mode to whatever the user has for movingCalcMode
            if (ba.debug.get()) ChatUtils.info("using movingCalcAlgo");
            damageCalc = movingCalcMode;
        } else {
            if (ba.debug.get()) ChatUtils.info("using holeCalcAlgo");
            damageCalc = holeCalcMode; // set damage calc mode to whatever the user has for holeCalcMode
        }
        if (AutomationUtils.isWebbed(target)) {
            if (ba.debug.get()) ChatUtils.info("overriding algo to strong because target is webbed");
            damageCalc = BedAuraRewrite.DamageCalc.Strong; // override to strong mode to help bypass webs
        }
        switch (damageCalc) { // find a place pos based on damage calc mode
            case Fast -> {
                return findPlace(target);
            }
            case Strong -> {
                return findPlaceStrong(target);
            }
        }
        return null;
    }

    public static boolean shouldPredict(PlayerEntity target) {
        BedAuraRewrite ba = Modules.get().get(BedAuraRewrite.class);
        if (ba.predictIgnoreElytra.get() && target.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) return false;
        return !Wrapper.isInHole(target);
    }

    public static BlockPos findPlace(PlayerEntity target) {
        // make sure we have a bed lmao
        if (!InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem).found()) return null;
        BedAuraRewrite ba = Modules.get().get(BedAuraRewrite.class);
        if (ba.debug.get()) ChatUtils.info("findPlace debug");
        // store the vars we need from BedAuraRewrite
        double minDamage = ba.minDamage.get();
        double maxSelfDamage = ba.maxSelfDamage.get();
        boolean antiSuicide = ba.antiSuicide.get();
        boolean selfDamageBypass = ba.selfDamageBypass.get();

        // loop through foot -> body -> head block pos on target
        for (int index = 0; index < 3; index++) {
            int i = index == 0 ? 1 : index == 1 ? 0 : 2;

            // loop through directions (bed orientation)
            for (CardinalDirection dir : CardinalDirection.values()) {
                if (ba.strictDirection.get() // strict direction check
                    && dir.toDirection() != mc.player.getHorizontalFacing()
                    && dir.toDirection().getOpposite() != mc.player.getHorizontalFacing()) continue;

                BlockPos centerPos = target.getBlockPos().up(i); // store which part of the player we're targeting
                if (ba.debug.get()) ChatUtils.info("checking place pos at " + centerPos.getX() + "," + centerPos.getY() + "," + centerPos.getZ());

                // predict
                if (ba.debug.get()) ChatUtils.info("checking if we should predict movement");
                if (ba.predictMovement.get() && shouldPredict(target)) {
                    if (ba.debug.get()) ChatUtils.info("we should, calculating predict pos");
                    // to predict, we add their current xyz velocity to their current blockpos xyz
                    double plusX = Math.round(target.getVelocity().x);
                    double plusY = Math.round(target.getVelocity().y);
                    double plusZ = Math.round(target.getVelocity().z);
                    centerPos = centerPos.add(plusX, plusY, plusZ);
                    if (ba.debug.get()) ChatUtils.info("predicting next position to " + centerPos.getX() + "," + centerPos.getY() + "," + centerPos.getZ());
                }

                if (ba.debug.get()) ChatUtils.info("checking if we can place");
                if (!canPlace(centerPos, dir)) return null; // check if we can place before damage calcs
                if (ba.debug.get()) ChatUtils.info("we can place");

                // damage calcs
                double headSelfDamage = DamageUtils.bedDamage(mc.player, Utils.vec3d(centerPos));
                double offsetSelfDamage = DamageUtils.bedDamage(mc.player, Utils.vec3d(centerPos.offset(dir.toDirection())));
                double targetDamage = DamageUtils.bedDamage(target, Utils.vec3d(centerPos));
                if (ba.debug.get()) ChatUtils.info("Self damage: " + headSelfDamage + " | Target damage: " + targetDamage);
                if (ba.debug.get()) ChatUtils.info("damageCheck running");
                if (damageCheck(target, minDamage, maxSelfDamage, headSelfDamage, offsetSelfDamage, targetDamage, antiSuicide, selfDamageBypass)) {
                    if (ba.debug.get()) ChatUtils.info("position is valid, setting nextDamage and returning centerPos offset");
                    ba.nextDamage = targetDamage;
                    return centerPos.offset((ba.direction = dir).toDirection());
                }
            }
        }
        return null;
    }

    public static BlockPos findPlaceStrong(PlayerEntity target) {
        // loop through foot -> body -> head block pos on target
        for(int i = 0; i < 3; ++i) {
            int finalI = i; // need to store this as a separate var so we don't mess up thread shit
            placeCalculator.execute(() -> calcPlaceForY(target , finalI)); // calculate the placement for the Y pos on a separate thread
            BlockPos pp = placePos;
            if (pp != null) { // return the first valid place pos
                placePos = null;
                return pp;
            }
        }
        return null; // return null if there is no valid place pos
    }

    public static void calcPlaceForY(PlayerEntity target, int y) {
        if (placePos != null) return;
        BedAuraRewrite ba = Modules.get().get(BedAuraRewrite.class);
        if (ba.debug.get()) ChatUtils.info("calcPlaceForY " + y + " running");
        // store the vars we need from BedAuraRewrite
        double minDamage = ba.minDamage.get();
        double maxSelfDamage = ba.maxSelfDamage.get();
        boolean antiSuicide = ba.antiSuicide.get();
        boolean selfDamageBypass = ba.selfDamageBypass.get();

        if (!InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem).found()) return;
        for (CardinalDirection dir : CardinalDirection.values()) {
            if (ba.strictDirection.get()
                && dir.toDirection() != mc.player.getHorizontalFacing()
                && dir.toDirection().getOpposite() != mc.player.getHorizontalFacing()) continue;

            BlockPos centerPos = target.getBlockPos().up(y);
            if (ba.debug.get()) ChatUtils.info("centerPos is " + centerPos.getX() + "," + centerPos.getY() + "," + centerPos.getZ());


            // predict
            if (ba.predictMovement.get() && shouldPredict(target)) {
                if (ba.debug.get()) ChatUtils.info("predicting target movement");
                double plusX = Math.round(target.getVelocity().x);
                double plusY = Math.round(target.getVelocity().y);
                double plusZ = Math.round(target.getVelocity().z);
                centerPos = centerPos.add(plusX, plusY, plusZ);
            }

            if (ba.debug.get()) ChatUtils.info("checking canPlace");
            if (!canPlace(centerPos, dir)) return; // check if we can place before damage calcs
            if (ba.debug.get()) ChatUtils.info("we can place");

            double headSelfDamage = DamageUtils.bedDamage(mc.player, Utils.vec3d(centerPos));
            double offsetSelfDamage = DamageUtils.bedDamage(mc.player, Utils.vec3d(centerPos.offset(dir.toDirection())));
            double targetDamage = DamageUtils.bedDamage(target, Utils.vec3d(centerPos));

            if (ba.debug.get()) ChatUtils.info("Self Damage: " + headSelfDamage + " | Target Damage: " + targetDamage);

            if (ba.debug.get()) ChatUtils.info("damageCheck running");
            if (damageCheck(target, minDamage, maxSelfDamage, headSelfDamage, offsetSelfDamage, targetDamage, antiSuicide, selfDamageBypass)) {
                if (ba.debug.get()) ChatUtils.info("position is valid, setting placePos and nextDamage");
                placePos = centerPos.offset((ba.direction = dir).toDirection());
                ba.nextDamage = targetDamage;
            }
        }
    }


    public static boolean isSelfTrapped(PlayerEntity p) {
        BlockPos tpos = p.getBlockPos();
        for (Vec3d stp : selfTrapPositions) { // loop through all self trap positions
            BlockPos stb = tpos.add(stp.x, stp.y, stp.z);
            if (BlockHelper.getBlock(stb) == Blocks.AIR && BlockUtils.canPlace(stb)) return false; // check if we can place at any of the positions
        }
        return true; // return true if all the positions are blocked or unplacable
    }

    public static BlockPos getSelfTrapBlock(PlayerEntity p, boolean autoTrap, boolean randomize) {
        BlockPos tpos = p.getBlockPos();
        List<BlockPos> selfTrapBlocks = new ArrayList<>(); // used to store all the blocks around the player that are a self trap block (obby etc)
        if (!autoTrap && AutomationUtils.isTrapBlock(tpos.up(2))) return tpos.up(2); // if we didn't trap them and the block above their head is obby, mine that first
        for (Vec3d stp : selfTrapPositions) { // if not we loop through all the self trap positions
            BlockPos stb = tpos.add(stp.x, stp.y, stp.z);
            if (AutomationUtils.isTrapBlock(stb) && PlayerUtils.distanceTo(stb) <= mc.interactionManager.getReachDistance()) selfTrapBlocks.add(stb); // if the block is a self trap block and we can reach it , store it to the list
        }
        selfTrapBlocks.removeIf(BlockHelper::isOurSurroundBlock); // remove any block from the list that is part of our surround
        selfTrapBlocks.removeIf(stb -> !shouldMineTrapBlock(p, stb)); // remove any block that wont help us place

        if (selfTrapBlocks.isEmpty()) return null; // return nothing if the list is empty
        BlockPos selfTrapBlock = null;
        if (randomize) { // return a random blockpos if the user has randomize enabled in BedAuraRewrite
            selfTrapBlock = selfTrapBlocks.get(new Random().nextInt(selfTrapBlocks.size()));
        } else {
            selfTrapBlock = selfTrapBlocks.get(0);
        }
        return selfTrapBlock;
    }

    public static boolean shouldMineTrapBlock(PlayerEntity p, BlockPos trapBlock) {
        BedAuraRewrite ba = Modules.get().get(BedAuraRewrite.class);
        for (int index = 0; index < 3; index++) {
            int i = index == 0 ? 1 : index == 1 ? 0 : 2;
            for (CardinalDirection dir : CardinalDirection.values()) {
                if (ba.strictDirection.get() // strict direction check
                    && dir.toDirection() != mc.player.getHorizontalFacing()
                    && dir.toDirection().getOpposite() != mc.player.getHorizontalFacing()) continue;
                BlockPos centerPos = p.getBlockPos().up(i);
                BlockPos offsetPos = centerPos.offset((dir).toDirection());
                if (trapBlock.equals(offsetPos)) return true;
            }
        }
        return false;
    }
}
