package me.ghosttypes.ghostware.modules.misc;

import me.ghosttypes.ghostware.modules.hud.visual.NotificationsHUD;
import me.ghosttypes.ghostware.utils.Categories;
import me.ghosttypes.ghostware.utils.services.SpotifyHelper;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;



public class SpotifyAlerts extends Module {


    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    private final Setting<Notifications.mode> notifyMode = sgGeneral.add(new EnumSetting.Builder<Notifications.mode>().name("notification-mode").description("How notifications are displayed.").defaultValue(Notifications.mode.Chat).build());


    public SpotifyAlerts() { super(Categories.Misc, "spotify-alerts", ""); }

    String lastTrack = null;

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (SpotifyHelper.currentTrack == null) return;
        if (!SpotifyHelper.currentTrack.equals(lastTrack)) {
            lastTrack = SpotifyHelper.currentTrack;
            switch (notifyMode.get()) {
                case Chat -> sendChatNotif(SpotifyHelper.currentArtist, SpotifyHelper.currentTrack);
                case Toast -> NotificationsHUD.spotify(SpotifyHelper.currentArtist, SpotifyHelper.currentTrack);
            }
        }
    }

    private void sendChatNotif(String artist, String track) {
        ChatUtils.info("Now Playing: " + artist + " - " + track);
    }

}
