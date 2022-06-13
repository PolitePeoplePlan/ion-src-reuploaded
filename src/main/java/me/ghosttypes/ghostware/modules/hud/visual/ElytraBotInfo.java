package me.ghosttypes.ghostware.modules.hud.visual;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.render.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.render.hud.modules.HudElement;
import me.ghosttypes.ghostware.modules.misc.elytrabot.ElytraBotThreaded;

import java.util.ArrayList;

public class ElytraBotInfo extends HudElement {

    public ElytraBotInfo(HUD hud) {
        super(hud, "elytra-bot-info", "Displays information about the elytra bot instance.");
    }

    private final ArrayList<String> infoStrings = new ArrayList<>();

    @Override
    public void update(HudRenderer renderer) {
        updateInfo();
        double width = 0;
        double height = 0;
        int i = 0;
        if (isInEditor()) {
            String t = "Elytra Bot Info";
            width = Math.max(width, renderer.textWidth(t));
            height += renderer.textHeight();
        } else {
            for (String info : infoStrings) {
                width = Math.max(width, renderer.textWidth(info));
                height += renderer.textHeight();
                if (i > 0) height += 2;
                i++;
            }
        }
        box.setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        updateInfo();


        double x = box.getX();
        double y = box.getY();
        int i = 0;

        if (isInEditor()) {
            String text = "Elytra Bot Info:";
            renderer.text(text, x + box.alignX(renderer.textWidth(text)), y, hud.primaryColor.get());
            return;
        }
        for (String info : infoStrings) {
            renderer.text(info, x + box.alignX(renderer.textWidth(info)), y, hud.primaryColor.get());
            y += renderer.textHeight();
            if (i > 0) y += 2;
            i++;
        }
    }

    private void updateInfo() {
        ElytraBotThreaded elytraBotThreaded = Modules.get().get(ElytraBotThreaded.class);
        String status = elytraBotThreaded.Status;
        String goal = elytraBotThreaded.Goal;
        String eta = elytraBotThreaded.Time;
        String fireworks = elytraBotThreaded.Fireworks;
        infoStrings.clear();
        infoStrings.add("Elytra Bot Info");
        if (status != null) { infoStrings.add("Status: " + status); } else { infoStrings.add("Status: None"); }
        if (goal != null) { infoStrings.add("Goal: " + goal); } else { infoStrings.add("Goal: None"); }
        if (eta != null) { infoStrings.add("Estimated Time: " + eta); } else { infoStrings.add("Estimated Time: None"); }
        if (fireworks != null) { infoStrings.add("Fireworks Required: " + fireworks); } else {infoStrings.add("Fireworks Required: None");}
    }




    //status, goal/facing, time, fireworks
}
