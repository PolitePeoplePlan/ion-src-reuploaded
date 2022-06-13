package me.ghosttypes.ghostware.utils.services;

import me.ghosttypes.ghostware.Ghostware;
import me.ghosttypes.ghostware.utils.misc.Stats;
import me.ghosttypes.ghostware.utils.world.Loader;
import meteordevelopment.meteorclient.utils.network.Http;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OnlineUsers {

    public static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    public static int updateFails = 0;
    public static boolean updateFailed = false;

    public static void init() {
        executor.scheduleAtFixedRate(OnlineUsers::updateOnlineUsers, 30, 60, TimeUnit.SECONDS);
    }

    public static void shutdown() {
        executor.shutdown();
    }

    public static void updateOnlineUsers() {
        // so the client won't keep trying to update users if it has already failed a bunch of times
        String onlineUrl = "http://ion.caius.org/get_online_users.php?hwid=" + Loader.hwid;
        try {
            String onlineUsers = Http.get(onlineUrl).sendString();
            if (onlineUsers == null) {
                updateFail();
            } else {
                if (onlineUsers.isEmpty() || onlineUsers.isBlank()) {
                    updateFail();
                } else {
                    Stats.onlineUsers = onlineUsers;
                }
            }
        } catch (Exception ignored) {
            updateFail();
        }
    }

    public static void updateFail() {
        updateFails++;
        if (updateFails >= 5) {
            if (!updateFailed) {
                Ghostware.log("Failed to retrieve online userlist!");
                ChatUtils.info("Ghostware is unable to update the online users, modules will be unable to detect Ghostware users for this session.");
                updateFailed = true;
            }
        }
    }
}
