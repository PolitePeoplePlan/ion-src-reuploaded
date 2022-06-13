package me.ghosttypes.ghostware.utils;

import me.ghosttypes.ghostware.Ghostware;
import me.ghosttypes.ghostware.utils.misc.ConfigHelper;
import me.ghosttypes.ghostware.utils.player.AutomationUtils;
import me.ghosttypes.ghostware.utils.services.*;
import me.ghosttypes.ghostware.utils.world.BlockHelper;
import me.ghosttypes.ghostware.utils.world.Loader;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.world.TickRate;
import net.minecraft.block.Blocks;
import net.minecraft.client.tutorial.TutorialStep;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.lang.invoke.MethodHandles;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Wrapper {

    public static boolean isLinux = false;
    // first stage of loading
    public static void init(long startTime) {
        ConfigHelper.backup(); // backup config
        try { Loader.init(); } catch (Exception ignored) { Loader.exit("Failed to authenticate!"); } // start second stage of loading
        Wrapper.setTitle("Ghostware " + Ghostware.VERSION); // override window title
        Config.get().customWindowTitle = true;
        Config.get().customWindowTitleText = "Ghostware " + Ghostware.VERSION;
        Services.startServices(); // start background services
        disableTutorial();
        Runtime.getRuntime().addShutdownHook(new Thread(Wrapper::shutdown)); // shutdown hook
        MeteorClient.EVENT_BUS.registerLambdaFactory("me.ghosttypes.ghostware", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup())); // event handler
        long loadTime = System.currentTimeMillis() - startTime;
        Ghostware.log("Finished loading Ghostware in " + loadTime + "ms!");
    }

    // shutdown hook
    public static void shutdown() {
        long startTime = System.currentTimeMillis();
        Ghostware.log("Shutting down.");
        Loader.shutdown(); // stop auth services
        Services.stopServices(); // stop background services
        long loadTime = System.currentTimeMillis() - startTime;
        Ghostware.log("Shutdown Ghostware in " + loadTime + "ms!");
    }


    public static void disableTutorial() { mc.getTutorialManager().setStep(TutorialStep.NONE);}

    public static boolean isLagging() {
        return TickRate.INSTANCE.getTimeSinceLastTick() >= 0.8;
    }

    public static float getTotalHealth(PlayerEntity p) {
        return p.getHealth() + p.getAbsorptionAmount();
    }

    public static Item getItemFromSlot(Integer slot) {
        if (slot == -1) return null;
        if (slot == 45) return mc.player.getOffHandStack().getItem();
        return mc.player.getInventory().getStack(slot).getItem();
    }

    public static void updateSlot(int newSlot) {
        //mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(newSlot));
        mc.player.getInventory().selectedSlot = newSlot;
    }


    public static void swingHand(boolean offhand) {
        if (offhand) {
            mc.player.swingHand(Hand.OFF_HAND);
        } else {
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    public static boolean isPlayerMoving(PlayerEntity p) {
        return p.forwardSpeed != 0 || p.sidewaysSpeed != 0;
    }

    public static void setTitle(String titleText) {
        mc.getWindow().setTitle(titleText);
    }

    public static boolean isInHole(PlayerEntity p) {
        BlockPos pos = p.getBlockPos();
        for (Vec3d sb : AutomationUtils.surroundPositions) if (BlockHelper.getBlock(pos.add(sb.x, sb.y, sb.z)) == Blocks.AIR) return false;
        //TODO compare blocks to trap blocks in AutomationUtils or something
        //return !mc.world.getBlockState(pos.add(1, 0, 0)).isAir()
        //        && !mc.world.getBlockState(pos.add(-1, 0, 0)).isAir()
        //        && !mc.world.getBlockState(pos.add(0, 0, 1)).isAir()
        //        && !mc.world.getBlockState(pos.add(0, 0, -1)).isAir()
        //        && !mc.world.getBlockState(pos.add(0, -1, 0)).isAir();
        return true;
    }

    public static int randomNum(int min, int max) {
        return min + (int) (Math.random() * ((max - min) + 1));
    }

    public static void sendMessage(String msg) {
        if (mc.player == null || msg == null) return;
        mc.player.networkHandler.sendPacket(new ChatMessageC2SPacket(msg));
    }

    public static void messagePlayer(String playerName, String m) {
        assert mc.player != null;
        sendMessage("/msg " + playerName + " " +  m);
    }

}
