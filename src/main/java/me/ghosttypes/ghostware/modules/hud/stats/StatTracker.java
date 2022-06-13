package me.ghosttypes.ghostware.modules.hud.stats;

import me.ghosttypes.ghostware.utils.misc.Stats;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.render.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.render.hud.modules.HudElement;

import java.util.ArrayList;
import java.util.Comparator;

public class StatTracker extends HudElement {
    public StatTracker(HUD hud) {
        super(hud, "stats", "Display various stats about your current session", false);
    }


    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>().name("sort-mode").description("How to sort the stats list.").defaultValue(SortMode.Shortest).build());
    private final Setting<Boolean> deaths = sgGeneral.add(new BoolSetting.Builder().name("deaths").description("Display your total deaths.").defaultValue(true).build());
    private final Setting<Boolean> highscore = sgGeneral.add(new BoolSetting.Builder().name("highscore").description("Display your highest killstreak.").defaultValue(true).build());
    private final Setting<Boolean> kd = sgGeneral.add(new BoolSetting.Builder().name("kd").description("Display your kills to death ratio.").defaultValue(true).build());
    private final Setting<Boolean> kills = sgGeneral.add(new BoolSetting.Builder().name("deaths").description("Display your total kills.").defaultValue(true).build());
    private final Setting<Boolean> killstreak = sgGeneral.add(new BoolSetting.Builder().name("deaths").description("Display your current killstreak.").defaultValue(true).build());

    @Override
    public void update(HudRenderer renderer) {
        ArrayList<String> stats = getStats();
        double width = 0;
        double height = 0;
        int i = 0;
        if (stats.isEmpty()) {
            String t = "You have no stat modules enabled.";
            width = Math.max(width, renderer.textWidth(t));
            height += renderer.textHeight();
        } else {
            for (String stat : stats) {
                width = Math.max(width, renderer.textWidth(stat));
                height += renderer.textHeight();
                if (i > 0) height += 2;
                i++;
            }
        }
        box.setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        ArrayList<String> stats = getStats();
        double x = box.getX();
        double y = box.getY();
        if (isInEditor()) {
            renderer.text("Stat trackers", x, y, hud.secondaryColor.get());
            return;
        }
        int i = 0;
        if (stats.isEmpty()) {
            String t = "You have no stat modules enabled.";
            renderer.text(t, x + box.alignX(renderer.textWidth(t)), y, hud.secondaryColor.get());
        } else {
            for (String stat : stats) {
                renderer.text(stat, x + box.alignX(renderer.textWidth(stat)), y, hud.secondaryColor.get());
                y += renderer.textHeight();
                if (i > 0) y += 2;
                i++;
            }
        }
    }

    private ArrayList<String> getStats() {
        ArrayList<String> stats = new ArrayList<>();
        if (deaths.get()) stats.add("Deaths: " + Stats.deaths);
        if (highscore.get()) stats.add("Highscore: " + Stats.highscore);
        if (kd.get()) stats.add("KD: " + Stats.getKD());
        if (kills.get()) stats.add("Kills: " + Stats.kills);
        if (killstreak.get()) stats.add("Killstreak: " + Stats.killStreak);
        if (sortMode.get().equals(SortMode.Shortest)) {
            stats.sort(Comparator.comparing(String::length));
        } else {
            stats.sort(Comparator.comparing(String::length).reversed());
        }
        return stats;
    }


    public enum SortMode {
        Longest,
        Shortest
    }


}
