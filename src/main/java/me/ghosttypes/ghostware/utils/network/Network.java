package me.ghosttypes.ghostware.utils.network;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Network {

    public static ExecutorService networkExecutor = Executors.newFixedThreadPool(5);

    public static boolean downloadFile(String inUrl, File outFile) {
        //TODO: thread this
        try (BufferedInputStream in = new BufferedInputStream(new URL(inUrl).openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(outFile)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) fileOutputStream.write(dataBuffer, 0, bytesRead);
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }
}
