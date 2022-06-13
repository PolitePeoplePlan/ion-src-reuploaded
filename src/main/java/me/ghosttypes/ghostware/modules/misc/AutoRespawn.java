package me.ghosttypes.ghostware.modules.misc;

import me.ghosttypes.ghostware.modules.chat.BurrowAlert;
import me.ghosttypes.ghostware.modules.hud.visual.NotificationsHUD;
import me.ghosttypes.ghostware.utils.Categories;
import me.ghosttypes.ghostware.utils.Wrapper;
import me.ghosttypes.ghostware.utils.misc.Stats;
import me.ghosttypes.ghostware.utils.world.Loader;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DeathScreen;

import java.util.Collections;
import java.util.List;
import java.util.Random;

public class AutoRespawn extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRekit = settings.createGroup("Rekit");
    private final SettingGroup sgExcuse = settings.createGroup("AutoExcuse");
    private final SettingGroup sgHS = settings.createGroup("HighScore");

    private final Setting<Boolean> rekit = sgRekit.add(new BoolSetting.Builder().name("rekit").description("Rekit after dying on pvp servers.").defaultValue(false).build());
    private final Setting<String> kitName = sgRekit.add(new StringSetting.Builder().name("kit-name").description("The name of your kit.").defaultValue("default").build());
    private final Setting<Notifications.mode> notifyMode = sgGeneral.add(new EnumSetting.Builder<Notifications.mode>().name("notification-mode").description("How notifications are displayed.").defaultValue(Notifications.mode.Chat).build());


    private final Setting<Boolean> excuse = sgExcuse.add(new BoolSetting.Builder().name("excuse").description("Send an excuse to global chat after death.").defaultValue(false).build());
    private final Setting<Boolean> randomize = sgExcuse.add(new BoolSetting.Builder().name("randomize").description("Randomizes the excuse message.").defaultValue(false).build());
    private final Setting<List<String>> messages = sgExcuse.add(new StringListSetting.Builder().name("excuse-messages").description("Messages to use for AutoExcuse").defaultValue(Collections.emptyList()).build());

    private final Setting<Boolean> alertHS = sgHS.add(new BoolSetting.Builder().name("alert").description("Alerts you client side when you reach a new highscore.").defaultValue(false).build());
    private final Setting<Boolean> announceHS = sgHS.add(new BoolSetting.Builder().name("announce").description("Announce when you reach a new highscore.").defaultValue(false).build());



    private boolean shouldRekit = false;
    private boolean shouldExcuse = false;
    private boolean shouldHS = false;
    private int excuseWait = 50;
    private int rekitWait = 50;
    private int messageI = 0;

    public AutoRespawn() {
        super(Categories.Misc, "auto-respawn", "Automatically respawns after death.");
    }

    @Override
    public void onActivate() {
        Loader.moduleAuth();
    }

    @EventHandler
    private void onOpenScreenEvent(OpenScreenEvent event) {
        if (!(event.screen instanceof DeathScreen)) return;
        mc.player.requestRespawn();
        if (rekit.get()) shouldRekit = true;
        if (excuse.get()) shouldExcuse = true;
        Stats.deaths++;
        //clear these when we die
        BurrowAlert.burrowedPlayers.clear();
        if (Stats.killStreak > Stats.highscore) {
            shouldHS = true;
            Stats.highscore = Stats.killStreak;
        }
        Stats.killStreak = 0;
        Stats.killfeed.clear();
        event.cancel();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (Wrapper.isLagging()) return;
        if (shouldRekit && rekitWait <= 1) {
            if (shouldHS) {
                if (alertHS.get()) notify("You reached a new highscore of " + Stats.highscore + "!");
                if (announceHS.get()) Wrapper.sendMessage("I reached a new highscore of " + Stats.highscore + " thanks to Ghostware!");
                shouldHS = false;
            }
            notify("Rekitting with kit " + kitName.get());
            Wrapper.sendMessage("/kit " + kitName.get());
            shouldRekit = false;
            shouldHS = false;
            rekitWait = 50;
            return;
        } else { rekitWait--; }
        if (shouldExcuse && excuseWait <= 1) {
            String excuseMessage = getExcuseMessage();
            Wrapper.sendMessage(excuseMessage);
            shouldExcuse = false;
            excuseWait = 50;
        } else { excuseWait--; }
    }


    private void notify(String msg) {
        String title = "[" + this.name + "] ";
        switch (notifyMode.get()) {
            case Chat -> info(msg);
            case Hud -> NotificationsHUD.addNotification(title + msg);
        }
    }

    private String getExcuseMessage() {
        String excuseMessage;
        if (messages.get().isEmpty()) {
            notify("Your excuse message list is empty!");
            return "Lag";
        } else {
            if (randomize.get()) {
                excuseMessage = messages.get().get(new Random().nextInt(messages.get().size()));
            } else {
                if (messageI >= messages.get().size()) messageI = 0;
                int i = messageI++;
                excuseMessage = messages.get().get(i);
            }
        }
        return excuseMessage;
    }
}

