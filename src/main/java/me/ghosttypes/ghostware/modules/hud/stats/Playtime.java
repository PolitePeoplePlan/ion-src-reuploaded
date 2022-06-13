package me.ghosttypes.ghostware.modules.hud.stats;

import me.ghosttypes.ghostware.utils.misc.Stats;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.render.hud.modules.DoubleTextHudElement;

public class Playtime extends DoubleTextHudElement {
    public Playtime(HUD hud) {
        super(hud, "playtime", "Displays how long you've been playing for", "Playtime: ");
    }
    @Override
    protected String getRight() {
        return Stats.getPlayTime();
    }
}
