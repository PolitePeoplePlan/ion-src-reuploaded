package me.ghosttypes.ghostware;

import me.ghosttypes.ghostware.utils.Categories;
import me.ghosttypes.ghostware.utils.Wrapper;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class Ghostware extends MeteorAddon {
	public static final Logger LOG = LogManager.getLogger();
	public static final String VERSION = "1.0";
    public static final File FOLDER = new File(System.getProperty("user.home"), "Ghostware");
    public static final File MODFOLDER = new File(FabricLoader.getInstance().getGameDir().toString(), "ghostware-addon");

	public static void log(String msg) {
        LOG.info("[Ghostware] " + msg);
    }

	@Override
	public void onInitialize() {
		log("Loading...");
        log("Thank you to everyone who donated to Ion. It was a fun but my time has come - GhostTypes");
        log("I'm coping - Ajaj");

		if (!MODFOLDER.exists()) MODFOLDER.mkdirs();
        if (!FOLDER.exists()) FOLDER.mkdirs();
        long startTime = System.currentTimeMillis();

		Wrapper.init(startTime);
	}

	@Override
	public void onRegisterCategories() {
	    Modules.registerCategory(Categories.Combat);
	    Modules.registerCategory(Categories.Chat);
	    Modules.registerCategory(Categories.Misc);
	}
}
