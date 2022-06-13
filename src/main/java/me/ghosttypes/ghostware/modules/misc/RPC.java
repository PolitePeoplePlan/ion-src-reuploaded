package me.ghosttypes.ghostware.modules.misc;

import me.ghosttypes.ghostware.utils.Categories;
import me.ghosttypes.ghostware.utils.services.DiscordHelper;
import me.ghosttypes.ghostware.utils.world.Loader;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.DiscordPresence;

import java.util.Collections;
import java.util.List;

public class RPC extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<List<String>> messages = sgGeneral.add(new StringListSetting.Builder().name("line-1").description("Messages for the first RPC line.").defaultValue(Collections.emptyList()).build());
    public final Setting<List<String>> messages2 = sgGeneral.add(new StringListSetting.Builder().name("line-2").description("Messages for the second RPC line.").defaultValue(Collections.emptyList()).build());
    public final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("update-delay").description("How many seconds before switching to a new RPC message.").defaultValue(5).min(0).sliderMax(30).build());
    public final Setting<Boolean> showSpotify = sgGeneral.add(new BoolSetting.Builder().name("show-spotify").description("Show what you are listening to on Spotify.").defaultValue(false).build());

    public RPC() {
        super(Categories.Misc, "RPC", "Ghostware RPC for Discord!");
    }

    @Override
    public void onActivate() {
        DiscordHelper.init();
        Loader.moduleAuth();
        if (Modules.get().isActive(DiscordPresence.class)) {
            error( "Default Meteor RPC is already enabled! Overriding.");
            Modules.get().get(DiscordPresence.class).toggle();
        }
    }

    @Override
    public void onDeactivate() {
        DiscordHelper.shutdown();
    }
}
