package me.ghosttypes.ghostware.utils.services;

import club.minnced.discord.rpc.DiscordEventHandlers;
import club.minnced.discord.rpc.DiscordRPC;
import club.minnced.discord.rpc.DiscordRichPresence;
import me.ghosttypes.ghostware.Ghostware;
import me.ghosttypes.ghostware.modules.misc.RPC;
import me.ghosttypes.ghostware.utils.Wrapper;
import me.ghosttypes.ghostware.utils.misc.Placeholders;
import me.ghosttypes.ghostware.utils.misc.Stats;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.screen.AddServerScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class DiscordHelper {

    //TODO: Update for meteors RPC changes, this won't be needed anymore since minegay took my idea (and made it better)

    public static ScheduledExecutorService rpcUpdater = Executors.newScheduledThreadPool(1);
    public static int messageI = 0;
    public static int messageI2 = 0;
    public static int updateDelay;
    public static boolean initialised = false;

    public static final DiscordRichPresence rpc1 = new DiscordRichPresence();
    public static final DiscordRPC instance = DiscordRPC.INSTANCE;

    public static void init() {
        RPC rpc = Modules.get().get(RPC.class);
        updateDelay = rpc.delay.get();
        DiscordEventHandlers handlers = new DiscordEventHandlers();
        instance.Discord_Initialize("913522705079337030", handlers, true, null);
        rpc1.startTimestamp = Stats.rpcStart;
        rpc1.largeImageKey = "gw";
        rpc1.largeImageText = "Ghostware " + Ghostware.VERSION;
        updateRPC();
        if (!initialised) {
            initService();
            initialised = true;
        }
        instance.Discord_UpdatePresence(rpc1);
        instance.Discord_RunCallbacks();
    }

    public static void initService() {
        Ghostware.log("Discord RPC started.");
        rpcUpdater.scheduleAtFixedRate(DiscordHelper::updateRPC, 5, updateDelay, TimeUnit.SECONDS);
    }

    public static void shutdown() {
        Ghostware.log("Discord RPC shutdown");
        instance.Discord_ClearPresence();
        instance.Discord_Shutdown();
    }

    public static void updateRPC() {
        RPC rpc = Modules.get().get(RPC.class);
        if (!rpc.isActive()) return;
        List<String> messages = rpc.messages.get();
        List<String> messages2 = rpc.messages2.get();
        if (messages.isEmpty() || messages2.isEmpty()) {
            messages.add("{username} | {hp}");
            messages2.add("Winning with the power of Ghostware!");
        }
        String message;
        String message2;
        if (!isInGame()) {
            message = "Ghostware " + Ghostware.VERSION;
            String screen = getCurrentScreen();
            if (!screen.equals("none")) {
                message2 = "Browsing the " + screen;
            } else {
                message2 = "Minecraft " + SharedConstants.getGameVersion().getName();
            }
        } else {
            if (messageI >= messages.size()) messageI = 0;
            if (messageI2 >= messages2.size()) messageI2 = 0;
            int i = messageI++;
            int i2 = messageI2++;
            message = Placeholders.apply(messages.get(i));
            message2 = Placeholders.apply(messages2.get(i2));
        }
        // Spotify
        int selector = Wrapper.randomNum(1, 3);
        if (selector == 1) {
            rpc1.details = Placeholders.apply("Listening to {songtitle}");
            rpc1.state = Placeholders.apply("By {songartist}");
        } else {
            rpc1.details = Placeholders.apply(message);
            rpc1.state = Placeholders.apply(message2);
        }
        instance.Discord_UpdatePresence(rpc1);
    }

    public static boolean isInGame() {
        return mc.world != null && mc.player != null;
    }

    public static String getCurrentScreen() {
        if (mc.currentScreen instanceof TitleScreen) return "Title Screen";
        if (mc.currentScreen instanceof MultiplayerScreen) return "Multiplayer Menu";
        if (mc.currentScreen instanceof AddServerScreen) return "Add Server Screen";
        return "none";
    }

}
