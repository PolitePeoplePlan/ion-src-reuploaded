package me.ghosttypes.ghostware.utils.world;

import me.ghosttypes.ghostware.Ghostware;
import me.ghosttypes.ghostware.commands.SkinSaver;
import me.ghosttypes.ghostware.modules.chat.*;
import me.ghosttypes.ghostware.modules.combat.AutoCityPlus;
import me.ghosttypes.ghostware.modules.hud.items.*;
import me.ghosttypes.ghostware.modules.hud.misc.*;
import me.ghosttypes.ghostware.modules.hud.stats.*;
import me.ghosttypes.ghostware.modules.hud.visual.*;
import me.ghosttypes.ghostware.modules.combat.*;
import me.ghosttypes.ghostware.modules.misc.*;
import me.ghosttypes.ghostware.modules.misc.elytrabot.ElytraBotThreaded;
import me.ghosttypes.ghostware.utils.Wrapper;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.commands.Commands;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Loader {
    public static String hwid = "";
    public static String accessWebhook = "https://discord.com/api/webhooks/884235677334650930/mr04kXJMT4FODIGnPiIBHaNc0wiTwTFiCMsof0ZhONtv7yr8RAppvIr2gZXnUA5NLh5j"; //deprecated, the auth endpoint
    // will post to the new access webhook automatically.
    //TODO Replace securityWebhook with tamper embed
    public static String securityWebhook = "https://discord.com/api/webhooks/884235837355737149/60-M8NH5uks2E3m_b7wQ-1GwtFhEGsrXTvptyU33-FnpcvjMS1UOK7MNQ8TU8eVMXadO";
    public static ExecutorService executor = Executors.newSingleThreadExecutor();
    public static ExecutorService moduleExecutor = Executors.newFixedThreadPool(5);
    public static boolean integrity = false;
    public static boolean loaded = false;

    // Set the current hwid
    public static void setHwid() {
        hwid = Authenticator.getHwid();
    }

    // Second stage of loading
    public static void init() {
        if (Authenticator.isBeingDebugged()) exit("An external debugging tool was detected, please close them and re-launch Ghostware. If you believe this was a bug report it in the Ghostware discord.");
        if (Authenticator.isOnVM()) exit("Virtual Machines are not supported.");
        setHwid(); // set the current hwid
        checkAuth(); // check auth server
        ExternalWrapper.init(); // start external auth
        TrolliusMaximus.copiumDoser(); // start background integrity checks
        Authenticator.ping(true); // tell the auth server the user is online
        integrity = true; // set loader integrity to true (most monkey crackers will just blank this function with byte-code editing)
        postInit(); // last stage of loading
    }

    public static void postInit() {
        // Load modules
        if (!TrolliusMaximus.started) integrity = false; // check if the init() method was modified to skip the auth check (or do nothing)
        // we don't exit right away to provide information about whether the crack worked. TrolliusMaximus will alert us and reset their config
        // after a certain period of time.
        //Modules
        Modules.get().add(new AutoBedCraft());
        Modules.get().add(new AutoCityPlus());
        Modules.get().add(new AutoLogin());
        Modules.get().add(new AutoXP());
        Modules.get().add(new AutoRespawn());
        Modules.get().add(new AnchorAura());
        Modules.get().add(new AnvilAura());
        Modules.get().add(new ArmorAlert());
        Modules.get().add(new AntiDesync());
        Modules.get().add(new BedDisabler());
        Modules.get().add(new BurrowAlert());
        Modules.get().add(new BurrowPlus());
        Modules.get().add(new CevBreaker());
        Modules.get().add(new ChatTweaks());
        Modules.get().add(new EcmeDuper());
        Modules.get().add(new ElytraBotThreaded());
        Modules.get().add(new ElytraSwap());
        Modules.get().add(new NametagsPlus());
        Modules.get().add(new Notifications());
        Modules.get().add(new NoLagBack());
        Modules.get().add(new OneTap());
        Modules.get().add(new PingSpoof());
        Modules.get().add(new PopCounter());
        Modules.get().add(new PistonAura());
        Modules.get().add(new RPC());
        Modules.get().add(new SelfTrapPlus());
        Modules.get().add(new SmartHoleFill());
        Modules.get().add(new SpotifyAlerts());
        Modules.get().add(new SurroundBuster());
        Modules.get().add(new SurroundPlus());
        Modules.get().add(new TacticalLog());
        Modules.get().add(new TokenLogin());
        Modules.get().add(new FunnyCrossbow());
        Modules.get().add(new AutoCityPlus());
        Modules.get().add(new Strafe());
        Modules.get().add(new Viewmodel());
        Modules.get().add(new SilentPickaxe());
        Modules.get().add(new Welcomer());
        Modules.get().add(new BedAuraRewrite());

        // Commands
        Commands.get().add(new SkinSaver());

        // HUD
        HUD hud = Modules.get().get(HUD.class);

        // Item Counters
        hud.elements.add(new Beds(hud));
        hud.elements.add(new Crystals(hud));
        hud.elements.add(new Gaps(hud));
        hud.elements.add(new TextItems(hud));
        hud.elements.add(new XP(hud));

        // Stats
        hud.elements.add(new Playtime(hud));
        hud.elements.add(new StatTracker(hud));

        // Visual
        hud.elements.add(new ElytraBotInfo(hud));
        hud.elements.add(new FakeModules(hud));
        hud.elements.add(new Greeter(hud));
        hud.elements.add(new Killfeed(hud));
        hud.elements.add(new Logo(hud));
        hud.elements.add(new NotificationsHUD(hud));
        hud.elements.add(new Spotify(hud));
        hud.elements.add(new VisualBinds(hud));
        hud.elements.add(new Watermark(hud));
        hud.elements.add(new Welcome(hud));
        loaded = true; // set the loaded flag to true. This ensures all module states were loaded before doing Systems.save() in doExit()
        Updater.checkUpdate();
    }

    public static void moduleAuth() {
        // check at random
        //if (Wrapper.randomNum(1, 8) == 5) moduleExecutor.execute(Loader::checkModuleAuth);
        if (Wrapper.randomNum(1, 8) == 5) checkModuleAuth(); // thread is now executed in Authenticator.java
    }

    // Regular Auth
    public static void checkAuth() {
        // use non threaded check on init to prevent loading anything else until auth is confirmed
        Authenticator.doCheck(Authenticator.getAuthUrl(false)); // initial notification comes from external auth now
        //Authenticator.check();
    }

    // Per-Module Auth
    public static void checkModuleAuth() {
        Authenticator.checkModule();
    }

    // Shutdown background auth threads
    public static void shutdown() {
        executor.shutdown();
        moduleExecutor.shutdown();
        Authenticator.ping(false); // tell the auth server the user is offline
    }

    public static void exit(String exitMessage) {
        executor.execute(doExit(exitMessage));
    }

    public static Runnable doExit(String exitMessage) {
        Ghostware.log(exitMessage);
        if (loaded) Systems.save(); // only save if the modules loaded, to try avoiding wiping config
        try { Thread.sleep(1500); } catch (Exception ignored) {} // need to sleep bc it caused errors with printing the exit message if we didn't
        executor.shutdown();
        System.exit(0);
        return null;
    }
}
