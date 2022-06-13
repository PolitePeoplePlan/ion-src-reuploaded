package me.ghosttypes.ghostware.utils.misc;

import me.ghosttypes.ghostware.Ghostware;
import meteordevelopment.meteorclient.MeteorClient;
import org.apache.commons.io.FileUtils;

import java.io.File;


public class ConfigHelper {

    public static File backupFolder = new File(Ghostware.MODFOLDER, "backup");

    public static void backup() {
        try {
            FileUtils.copyDirectory(MeteorClient.FOLDER, backupFolder);
        } catch (Exception e) {
            Ghostware.log("Error backing up current settings: " + e);
        }
    }

}
