package me.ghosttypes.ghostware.utils.world;

import me.ghosttypes.ghostware.Ghostware;
import me.ghosttypes.ghostware.utils.Wrapper;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static me.ghosttypes.ghostware.utils.world.Loader.exit;

public class ExternalWrapper {


    public static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    public static Process authProcess = null;
    public static boolean isLinux = false;
    public static File authFile = new File(Ghostware.FOLDER, "GhostWareExternal.jar");
    public static File lockFile = new File(Ghostware.FOLDER, "gw.lock");
    public static File modsFolder;
    public static File ionFile;

    // Init External Auth
    public static void init() {
        Authenticator.OSType osType = Authenticator.getOS();
        if (osType == Authenticator.OSType.Linux || osType == Authenticator.OSType.Mac) isLinux = true;
        Wrapper.isLinux = isLinux;
        modsFolder = new File(FabricLoader.getInstance().getGameDir().toString(), "mods");
        if (!modsFolder.exists() || !modsFolder.canRead() || !modsFolder.canWrite()) Loader.exit("Could not locate your minecraft folder! Report this bug in the Ghostware Discord.");
        Ghostware.log("Mods folder found at " + modsFolder);
        ionFile = findIon(modsFolder.getPath());
        if (ionFile == null) Loader.exit("Could not locate Ghostware inside your minecraft folder! Report this bug in the Ghostware Discord.");
        Ghostware.log("Ghostware found at " + ionFile);
        setup();

    }

    // Setup External Auth

    // Error Codes
    // 1 = External auth not found in Ion
    // 2 = Unable to extract External Auth
    // 3 = External Auth is not loaded
    // 4 = External Auth doesn't exist or can't be executed
    // 5 = Couldn't mark External Auth executable (Linux/Mac)

    public static void setup() {
        if (Authenticator.isBeingDebugged()) exit("An external debugging tool was detected, please close them and re-launch Ghostware. If you believe this was a bug report it in the Ghostware discord.");
        if (Authenticator.isOnVM()) exit("Virtual Machines are not supported.");
        // Create auth folder if it doesn't exist
        // Extract the authenticator to the auth folder
        try {
            InputStream in = ExternalWrapper.class.getResourceAsStream("/assets/ghostware/GhostWareExternal.jar");
            if (in == null) {
                exit("Could not verify the integrity of Ghostware (1). Please report this bug in the Ghostware Discord.");
            } else {
                OutputStream out = new FileOutputStream(authFile);
                IOUtils.copy(in, out);
            }
        } catch (Exception ignored) {
            exit("Could not verify the integrity of Ghostware (2). Please report this bug in the Ghostware Discord");
        }
        // create lock file so the external auth knows we are running
        lockFile.mkdirs();
        // mark the file as executable on Linux/macOS
        if (isLinux) {
            //Ion.log("Linux/Mac system detected, marking external auth as executable");
            String[] command = new String[]{"chmod", "+x", getFormattedPath(authFile)};
            //Ion.log("Command is " + Arrays.toString(command));
            ProcessBuilder builder = new ProcessBuilder(command);
            try {
                //Process p = builder.start();
                builder.start();
                    //try {
                    //    String line;
                    //    BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    //    while ((line = input.readLine()) != null) Ion.log("[MarkExternal] " + line);
                    //    input.close();
                    //    Ion.log("[MarkExternal] end of output");
                    //} catch (Exception e) {
                    //    Ion.log("MarkExternal log error: " + e);
                    //}
                //Ion.log("Command executed successfully");
            } catch (Exception ignored) {
                //Ion.log("Could not mark auth as executable: " + e);
                exit("Could not verify the integrity of Ghostware (5). Please report this bug in the Ghostware Discord.");
            }
        }
        // start external auth
        startExternal();
        // schedule integrity checks every minute
        executor.scheduleAtFixedRate(ExternalWrapper::check, 1, 1, TimeUnit.MINUTES);
        // add shutdown hook to stop external auth after Ghostware is closed
        Runtime.getRuntime().addShutdownHook(new Thread(ExternalWrapper::shutdown));
    }

    public static void check() {
        //  if (!isLoaded()) exit("Could not verify the integrity of Ghostware (3). Please report this bug in the Ghostware Discord");
    }

    // Auth shutdown hook
    public static void shutdown() {
        lockFile.delete(); // remove the lock file (external auth will close itself if it's gone, if the below method fails)
        if (authProcess != null) { // try closing external auth
            try {authProcess.destroy();} catch (Exception ignored) {}
        }
    }

    // Check if the auth process was started / is active
    public static boolean isLoaded() {
        if (authProcess == null) return false;
        return authProcess.isAlive();
    }

    // Start the external authenticator
    public static void startExternal() {
        if (!authFile.exists()) exit("Could not verify the integrity of Ghostware (4). Please report this bug in the Ghostware Discord");
        // java -jar "path\to\auth.jar" -start "path\to\ion.jar" -name GhostTypes -version 0.1.1
        try {
            String[] command = new String[]{"java", "-jar", getFormattedPath(authFile), "-start", getFormattedPath(ionFile), "-name", Authenticator.getIgn(), "-version", Ghostware.VERSION};
            //Ion.log("External command: " + Arrays.toString(command));
            ProcessBuilder builder = new ProcessBuilder(command);
            authProcess = builder.start();
            //ThreadHelper.fixedExecutor.execute(() -> logExternal(authProcess));
            //Ion.log("External Auth started! | PID: " + authProcess.pid());
        } catch (Exception e) {
            //Ion.log("startExternal exception: " + e);
            exit("Could not verify the integrity of Ghostware (4) . Please report this bug in the Ghostware Discord");
        }
        if (!isLoaded()) exit("Could not verify the integrity of Ghostware (3). Please report this bug in the Ghostware Discord");
    }

    public static void logExternal(Process external) {
        try {
            String line;
            BufferedReader input = new BufferedReader(new InputStreamReader(external.getInputStream()));
            while ((line = input.readLine()) != null) Ghostware.log("[ExternalAuth] " + line);
            input.close();
            Ghostware.log("[ExternalAuth] Shutdown");
        } catch (Exception e) {
            Ghostware.log("logExternal error: " + e);
        }
    }

    // Queue an update via the external authenticator (Ion will close after)
    public static void startUpdate(String updateVersion) {
        if (!authFile.exists() || !authFile.canExecute()) exit("Could not verify the integrity of Ghostware (4). Please report this bug in the Ghostware Discord");
        //java -jar path\to\auth.jar -update path\to\ion.jar -version 0.1.2
        try {
            String[] command = new String[]{"java", "-jar", getFormattedPath(authFile), "-update", getFormattedPath(ionFile), "-version", updateVersion};
            //Ion.log("External command" + Arrays.toString(command));
            ProcessBuilder builder = new ProcessBuilder(command);
            authProcess = builder.start();
            //Ion.log("External update started! | PID: " + authProcess.pid());
        } catch (Exception e) {
            //Ion.log("startUpdate exception: " + e);
            exit("Failed to start the update. Please report this bug in the Ghostware Discord");
        }
    }

    // Find Ghostware in the mods folder
    public static File findIon(String modsPath) {
        String ionPath = Ghostware.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        try {
            return new File(URLDecoder.decode(ionPath, StandardCharsets.UTF_8));
        } catch (Exception e) {
            Ghostware.log("Failed to locate Ghostware, falling back to folder scanning.");
            Ghostware.log("Exception: " + e);
            List<File> result = new ArrayList<>(); // results
            try {
                Files.walk(Paths.get(modsPath), FileVisitOption.FOLLOW_LINKS).filter(t -> t.toString().contains("ghostware") && t.toString().contains(".jar")).forEach(path -> result.add(path.getFileName().toFile()));
                if (result.isEmpty()) return null;
                return new File(modsFolder, result.get(0).getPath());
            } catch (Exception ignored2) {return null;}
        }
    }

    public static String getFormattedPath(File f) {
        return "\"" + f.getPath() + "\"";
    }

}
