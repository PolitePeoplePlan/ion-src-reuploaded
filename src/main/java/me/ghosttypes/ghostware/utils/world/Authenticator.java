package me.ghosttypes.ghostware.utils.world;

import com.sun.jna.Platform;
import me.ghosttypes.ghostware.Ghostware;
import me.ghosttypes.ghostware.utils.network.DiscordWebhook;
import meteordevelopment.meteorclient.utils.network.Http;
import net.minecraft.client.MinecraftClient;
import org.apache.commons.codec.digest.DigestUtils;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.util.FileUtil;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static me.ghosttypes.ghostware.utils.world.Loader.exit;

public class Authenticator {

    // Thread Pool for running any auth checks on (startup, method, module)
    public static ExecutorService authChecker = Executors.newFixedThreadPool(5);
    public static boolean checked = false;

    // VM Detection Variables
    // https://github.com/oshi/oshi/blob/master/oshi-demo/src/main/java/oshi/demo/DetectVM.java
    private static final String OSHI_VM_MAC_ADDR_PROPERTIES = "oshi.vmmacaddr.properties";
    private static final Properties vmMacAddressProps = FileUtil.readPropertiesFromFilename(OSHI_VM_MAC_ADDR_PROPERTIES);
    private static final Map<String, String> vmVendor = new HashMap<>();
    static {
        vmVendor.put("bhyve bhyve", "bhyve");
        vmVendor.put("KVMKVMKVM", "KVM");
        vmVendor.put("TCGTCGTCGTCG", "QEMU");
        vmVendor.put("Microsoft Hv", "Microsoft Hyper-V or Windows Virtual PC");
        vmVendor.put("lrpepyh vr", "Parallels");
        vmVendor.put("VMwareVMware", "VMware");
        vmVendor.put("XenVMMXenVMM", "Xen HVM");
        vmVendor.put("ACRNACRNACRN", "Project ACRN");
        vmVendor.put("QNXQVMBSQG", "QNX Hypervisor");
    }
    private static final String[] vmModelArray = new String[] { "Linux KVM", "Linux lguest", "OpenVZ", "Qemu",
        "Microsoft Virtual PC", "VMWare", "linux-vserver", "Xen", "FreeBSD Jail", "VirtualBox", "Parallels",
        "Linux Containers", "LXC" };


    // HWID Generation
    public static String getHwid() {
        OSType os = getOS();
        if (os.equals(OSType.Unsupported)) exit("Your operating system is not supported! Please open a bug report in the Ghostware discord.");
        switch (os) {
            case Windows -> { return getWindowsHWID(); }
            case Linux, Mac -> { return getLinuxOrMacHWID(); }
        }
        exit("There was an error generating a HWID for your system. Please open a bug report in the Ghostware discord.");
        return null;
    }

    public static String getWindowsHWID() {
        try {
            String raw = System.getProperty("user.name") + java.net.InetAddress.getLocalHost().getHostName() + System.getenv("APPDATA") + "copium";
            return DigestUtils.sha256Hex(raw);
        } catch (Exception ignored) {
            exit("Unable to generate a HWID for your system!");
            return null;
        }
    }

    public static String getLinuxOrMacHWID() {
        try {
            String raw = System.getProperty("user.name") + java.net.InetAddress.getLocalHost().getHostName() + "alternatecopium";
            return DigestUtils.sha256Hex(raw);
        } catch (Exception ignored) {
            exit("Unable to generate a HWID for your system!");
            return null;
        }
    }



    public static String getIgn() {
        return MinecraftClient.getInstance().getSession().getUsername();
    }

    // Online Player / Playtime Tracking
    // Still broken server side and only used in Nametags atm so it's not really an issue
    public static void ping(boolean online) {
//        if (online) {
//            String onlineUrl = "http://ion.caius.org/online.php?hwid=" + Loader.hwid + "&name=" + getIgn();
//            try {
//                String isOnline = Http.get(onlineUrl).sendString();
//                if (isOnline == null) {
//                    exit("Received a null pong from the authentication server.");
//                } else {
//                    if (isOnline.isEmpty() || isOnline.isBlank()) {
//                        exit("Received an empty pong from the authentication server.");
//                    } else {
//                        if (!isOnline.contains("authed")) {
//                            exit("Received an invalid pong from the authentication server.");
//                        }
//                    }
//                }
//            } catch (Exception ignored) {
//                exit("Failed to ping the authentication server.");
//            }
//        } else {
//            String offlineUrl = "http://ion.caius.org/offline.php?hwid=" + Loader.hwid + "&name=" + getIgn() + "&pt=" + Stats.getPlayTime();
//            try {
//               Http.get(offlineUrl).sendString();
//            } catch (Exception ignored) {}
//        }
    }


    // Auth Checking

    // Build the auth url
    public static String getAuthUrl(boolean notify) {
        // TODO: Auth hotfix, replace after
        //StringBuilder authUrl = new StringBuilder("http://ion.caius.org/auth.php?hwid=");
        //authUrl.append(Loader.hwid);
        //if (notify) {
        //    authUrl.append("&notify=true");
        //} else {
        //    authUrl.append("&notify=False");
        //}
        //authUrl.append("&mc_name=").append(getIgn());
        //authUrl.append("&version=" + Ion.VERSION);
        //return authUrl.toString();
        return "https://pastebin.com/raw/fui1qZta";
    }

    public static void checkModule() {
        //if (isBeingDebugged()) exit("An external debugging tool was detected, please close them and re-launch Ion. If you believe this was a bug report it in the Ghostware discord.");
        if (!checked) checked = true;
        //authChecker.execute(() -> doCheck(getAuthUrl(false))); // don't spam the webhook
    }

    public static void doCheck(String authUrl) {
        if (isBeingDebugged()) exit("An external debugging tool was detected, please close them and re-launch Ghostware. If you believe this was a bug report it in the Ghostware discord.");
        if (!checked) checked = true;
        //String auth = Http.get(authUrl).sendString();
        //TODO : Auth hotfix, replace after
        String auth = Http.get("https://pastebin.com/raw/fui1qZta").sendString();
        if (auth == null) { // handle server response / client connection error
            exit("Failed to read response from authentication servers.");
        } else { // handle unauthorized launches (webhook will be submitted server-side)
            // empty hwid
            if (auth.isEmpty() || auth.isBlank()) exit("You are not authorized to use this addon. Purchase Ghostware at https://discord.com/invite/9vGTkfA6H4");
        //   // unauthed hwid
            if (!auth.contains(Loader.hwid)) exit("You are not authorized to use this addon. Purchase Ghostware at https://discord.com/invite/9vGTkfA6H4");
                } // don't need to do anything else, the addon will only continue if the auth returned their hwid
    }


    // Anti Debug
    public static boolean isBeingDebugged() {
        //TODO: better / more process checks
        AtomicBoolean detected = new AtomicBoolean(false);
        Stream<ProcessHandle> liveProcesses = ProcessHandle.allProcesses();
        List<String> badProcesses = Arrays.asList("wireshark", "recaf", "dump");
        liveProcesses.filter(ProcessHandle::isAlive).forEach(ph -> {
            for (String badProcess : badProcesses) {
                if (ph.info().command().toString().contains(badProcess)) {
                    detected.set(true);
                    try { ph.destroy(); } catch (Exception ignored) {}
                }
            }
        });
        return detected.get();
    }

    public static boolean isOnVM() {
        //TODO: Uncomment after linux compat is fixed
        return false;
        //String vm = identifyVM();
        //if (vm.isEmpty()) {
        //    return false;
        //} else {
        //    sendTamperEmbed(Loader.securityWebhook, "A VM was detected: " + vm);
        //    return true;
        //}
    }

    // Tampering alerts
    public static void sendTamperEmbed(String webhookUrl, String reason) {
        String playerName = MinecraftClient.getInstance().getSession().getUsername();
        String user = System.getProperty("user.name");
        String os = System.getProperty("os.name");
        String dip = dip();
        String hwid = Loader.hwid;
        if (hwid == null) {
            hwid = "None";
        } else {
            if (hwid.isEmpty() || hwid.isBlank()) hwid = "None";
        }
        DiscordWebhook webhook = new DiscordWebhook(webhookUrl);
        webhook.addEmbed(new DiscordWebhook.EmbedObject()
            .setTitle("Jar Tampering Detected")
            .setColor(Color.RED)
            .addField("Username", playerName, false)
            .addField("Desktop Username", user, false)
            .addField("OS", os, false)
            .addField("IP", dip, false)
            .addField("Ghostware Version", Ghostware.VERSION, false)
            .addField("HWID", hwid, false)
            //.addField("Suspected Leaker", Loader.jarOwner, false)
            .addField("Reason", reason, false)
        );
        try {webhook.execute();} catch (IOException ignored) {}
    }

    private static String dip() {
        try {return new BufferedReader(new InputStreamReader(new URL("http://checkip.amazonaws.com").openStream())).readLine();} catch (Exception ignored) {return "Failed to log.";}
    }

    // OS Checking

    public static OSType getOS() {
        if (Platform.isWindows()) return OSType.Windows;
        if (Platform.isLinux()) return OSType.Linux;
        if (Platform.isMac()) return OSType.Mac;
        return OSType.Unsupported;

    }

    public enum OSType {
        Windows,
        Linux,
        Mac,
        Unsupported
    }

    // VM Detection
    public static String identifyVM() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hw = si.getHardware();
        // Check CPU Vendor
        String vendor = hw.getProcessor().getProcessorIdentifier().getVendor().trim();
        if (vmVendor.containsKey(vendor)) return vmVendor.get(vendor);
        // Check known MAC addresses
        List<NetworkIF> nifs = hw.getNetworkIFs();
        for (NetworkIF nif : nifs) {
            String mac = nif.getMacaddr().toUpperCase();
            String oui = mac.length() > 7 ? mac.substring(0, 8) : mac;
            if (vmMacAddressProps.containsKey(oui)) return vmMacAddressProps.getProperty(oui);
        }
        // Check known models
        String model = hw.getComputerSystem().getModel();
        for (String vm : vmModelArray) if (model.contains(vm)) return vm;
        String manufacturer = hw.getComputerSystem().getManufacturer();
        if ("Microsoft Corporation".equals(manufacturer) && "Virtual Machine".equals(model)) return "Microsoft Hyper-V";
        return "";
    }
}
