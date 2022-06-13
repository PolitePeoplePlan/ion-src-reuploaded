package me.ghosttypes.ghostware.utils.services;

import me.ghosttypes.ghostware.utils.Wrapper;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DigitalWellbeing {

    public static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    public static void init() {
        executor.scheduleAtFixedRate(DigitalWellbeing::alert, 0, 30, TimeUnit.MINUTES);
    }

    public static void shutdown() {
        executor.shutdown();
    }

    public static void alert() {
        String msg = "Take a break and get some water!";
        int selector = Wrapper.randomNum(1, 5);
        if (selector == 1) msg = "Don't play for too long!";
        if (selector == 2) msg = "Take a break and get some water!";
        if (selector == 3) msg = "Get up and stretch!";
        if (selector == 4) msg = "Don't spend all your time on block game!";
        if (selector == 5) msg = "Hope you're having a good time!";
        ChatUtils.info(msg);
    }

}
