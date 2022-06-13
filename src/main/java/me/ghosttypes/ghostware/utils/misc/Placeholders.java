package me.ghosttypes.ghostware.utils.misc;


import me.ghosttypes.ghostware.Ghostware;
import me.ghosttypes.ghostware.utils.Wrapper;
import me.ghosttypes.ghostware.utils.services.SpotifyHelper;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.SharedConstants;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Placeholders {
    public static String apply(String m) {
        if (m.contains("{highscore}")) m = m.replace("{highscore}", String.valueOf(Stats.highscore));
        if (m.contains("{killstreak}")) m = m.replace("{killstreak}", String.valueOf(Stats.killStreak));
        if (m.contains("{kills}")) m = m.replace("{kills}", String.valueOf(Stats.kills));
        if (m.contains("{deaths}")) m = m.replace("{deaths}", String.valueOf(Stats.deaths));
        if (m.contains("{server}")) m = m.replace("{server}", Utils.getWorldName());
        if (m.contains("{version}")) m = m.replace("{version}", SharedConstants.getGameVersion().getName());
        if (m.contains("{oversion}")) m = m.replace("{oversion}", Ghostware.VERSION);
        if (m.contains("{random}")) m = m.replace("{random}", String.valueOf(Wrapper.randomNum(1, 9)));
        if (m.contains("{username}")) m = m.replace("{username}", mc.getSession().getUsername());
        if (m.contains("{hp}")) m = m.replace("{hp}", String.valueOf(Math.rint(PlayerUtils.getTotalHealth())));
        if (m.contains("{songtitle}")) m = m.replace("{songtitle}", getCurrentTrack());
        if (m.contains("{songartist}")) m = m.replace("{songartist}", getCurrentArtist());
        return m;
    }

    public static String getCurrentTrack() {
        String currentTrack = SpotifyHelper.currentTrack;
        if (currentTrack == null) return "No song playing";
        if (currentTrack.isBlank() || currentTrack.isEmpty()) return "No song playing";
        return currentTrack;
    }

    public static String getCurrentArtist() {
        String currentArtist = SpotifyHelper.currentArtist;
        if (currentArtist == null) return "No song playing";
        if (currentArtist.isBlank() || currentArtist.isEmpty()) return "No song playing";
        return currentArtist;
    }
}
