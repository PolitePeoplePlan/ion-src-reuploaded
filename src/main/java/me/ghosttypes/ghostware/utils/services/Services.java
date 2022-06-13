package me.ghosttypes.ghostware.utils.services;

import me.ghosttypes.ghostware.Ghostware;
import me.ghosttypes.ghostware.utils.OutgoingMessages;
import me.ghosttypes.ghostware.utils.stats.Globals;

public class Services {

    public static void startServices() {
        Ghostware.log("Starting services.");
        DigitalWellbeing.init();
        DiscordHelper.init();
        //OnlineUsers.init(); TODO: Temp auth fix, restore after
        SpotifyHelper.init();
        OutgoingMessages.init();
        Globals.init();
    }

    public static void stopServices() {
        Ghostware.log("Stopping services.");
        DigitalWellbeing.shutdown();
        DiscordHelper.shutdown();
        //OnlineUsers.shutdown();
        SpotifyHelper.shutdown();
        Globals.shutdown();
    }
}
