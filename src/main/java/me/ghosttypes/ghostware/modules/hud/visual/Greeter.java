package me.ghosttypes.ghostware.modules.hud.visual;

import me.ghosttypes.ghostware.utils.misc.GreetingHelper;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.render.hud.modules.DoubleTextHudElement;

public class Greeter extends DoubleTextHudElement {
    public Greeter(HUD hud) {
        super(hud, "gw-greeter", "", "");
    }

    @Override
    protected String getRight() {
        if (mc.player == null) return GreetingHelper.getGreeting();
        return GreetingHelper.getGreeting() + mc.player.getEntityName();
    }
}
