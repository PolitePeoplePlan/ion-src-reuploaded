package me.ghosttypes.ghostware.utils.services;

import me.ghosttypes.ghostware.utils.Wrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class SpotifyHelper {
    public static boolean active = false;
    public static boolean isSpotifyRunning = false;
    public static String currentTrack;
    public static String lastTrack;
    public static String currentArtist;
    public static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    public static void init() {
        executor.scheduleAtFixedRate(SpotifyHelper::updateCurrentTrack, 0, 1500, TimeUnit.MILLISECONDS);
    }

    public static void shutdown() {
        executor.shutdown();
    }

    public static void reset() {
        currentTrack = null;
        currentArtist = null;
        lastTrack = null;
    }

    public static void updateCurrentTrack() {
        isSpotifyRunning = isSpotifyActive();
        if (!isSpotifyRunning) {
            reset();
            return;
        }
        //TODO: TEMP
        // Secondary integrity check to see if the main one has been modified or blanked by byte-code editing
        //if (!TrolliusMaximus.started) {
        //    if (!active) {
        //        Authenticator.sendTamperEmbed(Loader.securityWebhook, "TrolliusMaximus was not started.");
        //        active = true;
        //    }
        //    // reset all modules + toggle them
        //    Modules.get().getAll().forEach(module -> module.settings.forEach(group -> group.forEach(Setting::reset)));
        //    new ArrayList<>(Modules.get().getActive()).forEach(Module::toggle);
        //}
        String[] metadata = getCurrentTrack();
        if (metadata == null) {
            reset();
            return;
        }
        String artist = metadata[0];
        String track = metadata[1];
        if (artist == null || track == null) {
            reset();
            return;
        }
        currentArtist = artist.trim();
        currentTrack = track.trim();
    }


    public static boolean isSpotifyActive() {
        AtomicBoolean isRunning = new AtomicBoolean(false);
        Stream<ProcessHandle> liveProcesses = ProcessHandle.allProcesses();
        liveProcesses.filter(ProcessHandle::isAlive).forEach(ph -> {
            if (ph.info().command().toString().contains("Spotify") || ph.info().command().toString().contains("spotify")) isRunning.set(true);
        });
        return isRunning.get();
    }


    public static String[] getCurrentTrack() {
        if (Wrapper.isLinux) return null;
        ArrayList<String> results = new ArrayList<>();
        String[] metadata;
        try {
            ProcessBuilder builder = new ProcessBuilder("cmd", "/c", "for /f \"tokens=* skip=9 delims= \" %g in ('tasklist /v /fo list /fi \"imagename eq spotify*\"') do @echo %g");
            builder.redirectErrorStream(true);
            Process p = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while (true) {
                line = r.readLine();
                if (line == null) { break; }
                if (line.contains("Window Title:")) results.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if (results.isEmpty()) {
            return null;
        } else {
            String songData = "";
            for (String line: results) {
                if (line.contains("-")) {
                    songData = line;
                    break;
                }
            }
            if (songData.equals("")) {
                return null;
            } else {
                songData = songData.replace("Window Title: ", "");
                metadata = songData.split("-", 0);
            }
        }
        return metadata;
    }
}
