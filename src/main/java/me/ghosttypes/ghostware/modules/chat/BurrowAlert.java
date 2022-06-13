package me.ghosttypes.ghostware.modules.chat;

import me.ghosttypes.ghostware.modules.hud.visual.NotificationsHUD;
import me.ghosttypes.ghostware.modules.misc.Notifications;
import me.ghosttypes.ghostware.utils.Categories;
import me.ghosttypes.ghostware.utils.Wrapper;
import me.ghosttypes.ghostware.utils.player.AutomationUtils;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;

public class BurrowAlert extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder().name("range").description("How far away from you to check for burrowed players.").defaultValue(2).min(0).sliderMax(10).build());
    private final Setting<Notifications.mode> notifyMode = sgGeneral.add(new EnumSetting.Builder<Notifications.mode>().name("notification-mode").description("How notifications are displayed.").defaultValue(Notifications.mode.Chat).build());

    public BurrowAlert() {
        super(Categories.Chat, "burrow-alert", "Alerts you when players are burrowed.");
    }

    private int burrowMsgWait;
    public static List<PlayerEntity> burrowedPlayers = new ArrayList<>();

    @Override
    public void onActivate() {
        burrowMsgWait = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (isValid(player)) {
                burrowedPlayers.add(player);
                notify(player.getEntityName() + " is burrowed!");
            }
            if (burrowedPlayers.contains(player) && !AutomationUtils.isBurrowed(player, true)) {
                burrowedPlayers.remove(player);
                notify(player.getEntityName() + " is no longer burrowed.");
            }
        }
    }

    private boolean isValid(PlayerEntity p) {
        if (p == mc.player) return false;
        return mc.player.distanceTo(p) <= range.get() && !burrowedPlayers.contains(p) && AutomationUtils.isBurrowed(p, true) && !Wrapper.isPlayerMoving(p);
    }

    private void notify(String msg) {
        String title = "[" + this.name + "] ";
        switch (notifyMode.get()) {
            case Chat -> info(msg);
            case Hud -> NotificationsHUD.addNotification(title + msg);
        }
    }



}
