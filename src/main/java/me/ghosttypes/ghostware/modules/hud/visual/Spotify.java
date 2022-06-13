package me.ghosttypes.ghostware.modules.hud.visual;

import me.ghosttypes.ghostware.utils.services.SpotifyHelper;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.render.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.render.hud.modules.HudElement;


public class Spotify extends HudElement {

    public Spotify(HUD hud) {
        super(hud, "spotify-hud", "Display the current song playing in spotify.", false);
    }


    @Override
    public void update(HudRenderer renderer) {
        double width = 0;
        double height = 0;

        String t;

        if (!SpotifyHelper.isSpotifyRunning) {
            t = "Spotify is not running";
        } else if (SpotifyHelper.currentTrack == null || SpotifyHelper.currentArtist == null) {
            t = "Idle";
        } else {
            t = "Playing " + SpotifyHelper.currentTrack + " - " + SpotifyHelper.currentArtist;
        }

        width = Math.max(width, renderer.textWidth(t));

        // old code cope
        //if (SpotifyHelper.currentTrack == null || SpotifyHelper.currentArtist == null) {
        //String t = "No song is currently playing.";
        //    width = Math.max(width, renderer.textWidth(t));
        //} else {
        //    width = Math.max(width, renderer.textWidth("Playing: " + SpotifyHelper.currentTrack + " - " + SpotifyHelper.currentArtist));
        //}
        height += renderer.textHeight();
        box.setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.getX();
        double y = box.getY();
        if (isInEditor()) {
            renderer.text("Spotify HUD", x, y, hud.secondaryColor.get());
            return;
        }

        String t;

        if (!SpotifyHelper.isSpotifyRunning) {
            t = "Spotify is not running";
        } else if (SpotifyHelper.currentTrack == null || SpotifyHelper.currentArtist == null) {
            t = "Idle";
        } else {
            t = "Playing " + SpotifyHelper.currentTrack + " - " + SpotifyHelper.currentArtist;
        }

        // old code cope
        //if (SpotifyHelper.currentTrack == null || SpotifyHelper.currentArtist == null) {
        //    renderer.text("No song is currently playing.", x, y, hud.secondaryColor.get());
        //    return;
        //}
        //String song = "Playing: " + SpotifyHelper.currentTrack + " - " + SpotifyHelper.currentArtist;
        renderer.text(t, x + box.alignX(renderer.textWidth(t)), y, hud.secondaryColor.get());
    }
}
