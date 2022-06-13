package me.ghosttypes.ghostware.modules.hud.visual;


import me.ghosttypes.ghostware.Ghostware;
import me.ghosttypes.ghostware.modules.chat.ChatTweaks;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.render.hud.modules.DoubleTextHudElement;

public class Watermark extends DoubleTextHudElement {
    public Watermark(HUD hud) {
        super(hud, "ghostware-watermark", "", "");
    }

    @Override
    protected String getRight() {
        ChatTweaks chatTweaks = Modules.get().get(ChatTweaks.class);
        if (chatTweaks.isActive() && chatTweaks.customPrefix.get()) {
            return chatTweaks.prefixText.get() + " " + Ghostware.VERSION;
        }
        return "Ghostware " + Ghostware.VERSION; }
}
