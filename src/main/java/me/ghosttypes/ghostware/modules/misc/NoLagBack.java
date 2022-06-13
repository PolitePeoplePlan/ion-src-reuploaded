package me.ghosttypes.ghostware.modules.misc;

import me.ghosttypes.ghostware.modules.hud.visual.NotificationsHUD;
import me.ghosttypes.ghostware.utils.Categories;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.speed.Speed;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;


public class NoLagBack extends Module {


    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Notifications.mode> notifyMode = sgGeneral.add(new EnumSetting.Builder<Notifications.mode>().name("notification-mode").description("How notifications are displayed.").defaultValue(Notifications.mode.Chat).build());
    private final Setting<Integer> reEnableDelay = sgGeneral.add(new IntSetting.Builder().name("re-enable-delay").description("Delay between re-enabling movement modules after lagback.").defaultValue(10).min(0).sliderMax(100).build());

    private boolean isWaiting;
    private boolean toggledSpeed;
    private int enableTimer;


    public NoLagBack() {
        super(Categories.Misc, "no-lag-back", "Detect and mitigate lagback caused by movement modules.");
    }

    @Override
    public void onActivate() {
        toggledSpeed = false;
        isWaiting = false;
        enableTimer = reEnableDelay.get();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (isWaiting) {
            enableTimer--;
            if (enableTimer <= 0) {
                enableTimer = reEnableDelay.get();
                isWaiting = false;
                if (toggledSpeed) {
                    Speed speed = Modules.get().get(Speed.class);
                    if (!speed.isActive()) speed.toggle();
                }
            }
        }
    }


    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket && !isWaiting) {
            notify("Lagback detected");
            //info("Rubberband detected!");
            isWaiting = true;
            Speed speed = Modules.get().get(Speed.class);
            if (speed.isActive()) {
                toggledSpeed = true;
                speed.toggle();
            }
        }
    }


    private void notify(String msg) {
        String title = "[" + this.name + "] ";
        switch (notifyMode.get()) {
            case Chat -> info(msg);
            case Hud -> NotificationsHUD.addNotification(title + msg);
        }
    }
}
