package me.ghosttypes.ghostware.modules.misc;

import me.ghosttypes.ghostware.modules.hud.visual.NotificationsHUD;
import me.ghosttypes.ghostware.utils.Categories;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

public class Notifications extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Integer> displayTime = sgGeneral.add(new IntSetting.Builder().name("display-time").description("How long each notification displays for.").defaultValue(2).min(1).build());
    public final Setting<Boolean> drawBackground = sgGeneral.add(new BoolSetting.Builder().name("render-background").defaultValue(true).build());
    public final Setting<SettingColor> backgroundColor = sgGeneral.add(new ColorSetting.Builder().name("background-color").defaultValue(new SettingColor(156, 56, 56,75)).build());

    public Notifications() { super(Categories.Misc, "notifications", "Show notifications on the hud."); }


    @EventHandler
    private void onTick(TickEvent.Post event) {
        for (NotificationsHUD.Notif notif : NotificationsHUD.notifs) notif.tick();
    }

    public enum mode {
        Chat,
        Toast,
        Hud
    }
}
