package me.ghosttypes.ghostware.utils.stats;

import me.ghosttypes.ghostware.modules.misc.LegacyMode;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Globals {

    public static boolean isLegacyModeActive() {
        return Modules.get().get(LegacyMode.class).isActive();
    }

    public static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    public static ArrayList<DeathInstance> deaths = new ArrayList<>();


    public static void init() {
        executor.scheduleAtFixedRate(Globals::trackDeaths, 10, 5, TimeUnit.SECONDS);
    }

    public static void shutdown() {
        executor.shutdown();
    }

    // TODO: Maybe find another way to do this but a thread might be best so it's not per-module reliant
    // Depends on weather or not it causes a ConcurrentModification exception
    public static void trackDeaths() {
        if (!deaths.isEmpty()) {
            ArrayList<DeathInstance> removalQueue = new ArrayList<>();
            for (DeathInstance death : deaths) {
                death.validUntil--;
                if (death.validUntil <= 0) removalQueue.add(death);
            }
            removalQueue.forEach(deathInstance -> deaths.remove(deathInstance));
        }
    }

    public static DeathInstance getDeathInstance(PlayerEntity p) {
        for (DeathInstance dI : deaths) if (dI.player.equals(p)) return dI;
        return null;
    }

    public static DeathInstance getDeathInstanceByName(String pname) {
        for (DeathInstance dI : deaths) if (dI.player.getEntityName().equals(pname)) return dI;
        return null;
    }

    public static class DeathInstance {
        public final PlayerEntity player;
        public final String playerName;
        public final int pops;
        public int validUntil = 5;

        public DeathInstance(PlayerEntity p, int p1) {
            player = p;
            playerName = p.getEntityName();
            pops = p1;
        }

        public void update() {
            validUntil--;
        }
    }
}
