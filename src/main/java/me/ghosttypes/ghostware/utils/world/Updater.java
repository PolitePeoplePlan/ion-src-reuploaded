package me.ghosttypes.ghostware.utils.world;


public class Updater {

    // check the latest version and see if we need to update
    public static void checkUpdate() {
//        Ion.log("Checking for an update...");
//        String updateUrl = "https://pastebin.com/raw/iULANkuw"; // idc cope, github wouldn't refresh fast enough so a perm paste will do
//        String latestVersion = Http.get(updateUrl).sendString();
//        if (latestVersion == null) {
//            Ion.log("Failed to check the latest version.");
//        } else {
//            if (latestVersion.isBlank() || latestVersion.isEmpty()) {
//                Ion.log("Failed to check the latest version");
//            } else {
//                // remove any spaces or version comparing will cope immensely
//                latestVersion = latestVersion.replaceAll("\\s", "");
//                if (needsUpdate(latestVersion)) {
//                    Ion.log("Update v" + latestVersion + " is available! Preparing to update...");
//                    ExternalWrapper.startUpdate(latestVersion);
//                    //downloadUpdate(latestVersion);
//                } else {
//                    Ion.log("You're on the latest version.");
//                }
//            }
//        }
    }

    // compare current version to the latest version on the server
    public static boolean needsUpdate(String latestVersion) {
//        Version current = new Version(Ion.VERSION);
//        Version latest = new Version(latestVersion);
//        int compare = current.compareTo(latest);
//        return compare < 0; // -1 means we are on an old version, 0 means we are on the latest version
        return false;
    }
}
