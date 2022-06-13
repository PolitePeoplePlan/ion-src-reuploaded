package me.ghosttypes.ghostware.modules.hud.visual;

import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.render.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.render.hud.modules.HudElement;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FakeModules extends HudElement {
    public FakeModules(HUD hud) {
        super(hud, "fake-modules", "Display a customizable list of fake modules.", false);
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>().name("sort-mode").description("How to sort the binds list.").defaultValue(SortMode.Shortest).build());
    private final Setting<List<String>> modules = sgGeneral.add(new StringListSetting.Builder().name("pop-messages").description("Messages to use when announcing pops.").defaultValue(Collections.emptyList()).build());

    private void sort() {
        if (sortMode.get().equals(SortMode.Shortest)) {
            modules.get().sort(Comparator.comparing(String::length));
        } else {
            modules.get().sort(Comparator.comparing(String::length).reversed());
        }
    }


    @Override
    public void update(HudRenderer renderer) {
        sort();
        double width = 0;
        double height = 0;
        int i = 0;
        if (modules.get().isEmpty()) {
            String t = "FakeModules";
            width = Math.max(width, renderer.textWidth(t));
            height += renderer.textHeight();
        } else {
            for (String m : modules.get()) {
                width = Math.max(width, renderer.textWidth(m));
                height += renderer.textHeight();
                if (i > 0) height += 2;
                i++;
            }
        }
        box.setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();
        if (isInEditor()) {
            renderer.text("FakeModules", x, y, hud.primaryColor.get());
            return;
        }
        int i = 0;
        if (modules.get().isEmpty()) {
            String t = "FakeModules";
            renderer.text(t, x + box.alignX(renderer.textWidth(t)), y, hud.primaryColor.get());
        } else {
            for (String m : modules.get()) {
                renderer.text(m, x + box.alignX(renderer.textWidth(m)), y, hud.primaryColor.get());
                y += renderer.textHeight();
                if (i > 0) y += 2;
                i++;
            }
        }
    }

    public enum SortMode {
        Longest,
        Shortest
    }
}
