package me.ghosttypes.ghostware.modules.chat;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.ghosttypes.ghostware.Ghostware;
import me.ghosttypes.ghostware.modules.hud.visual.NotificationsHUD;
import me.ghosttypes.ghostware.modules.misc.Notifications;
import me.ghosttypes.ghostware.utils.Categories;
import me.ghosttypes.ghostware.utils.OutgoingMessages;
import me.ghosttypes.ghostware.utils.Wrapper;
import me.ghosttypes.ghostware.utils.misc.*;
import me.ghosttypes.ghostware.utils.stats.Globals;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.Formatting;

import java.util.*;

public class PopCounter extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAutoEz = settings.createGroup("AutoEz");
    private final SettingGroup sgMessages = settings.createGroup("Messages");

    // General
    private final Setting<Boolean> own = sgGeneral.add(new BoolSetting.Builder().name("own").description("Notifies you of your own totem pops.").defaultValue(false).build());
    private final Setting<Boolean> friends = sgGeneral.add(new BoolSetting.Builder().name("friends").description("Notifies you of your friends totem pops.").defaultValue(true).build());
    private final Setting<Boolean> others = sgGeneral.add(new BoolSetting.Builder().name("others").description("Notifies you of other players totem pops.").defaultValue(true).build());
    private final Setting<Notifications.mode> notifyMode = sgGeneral.add(new EnumSetting.Builder<Notifications.mode>().name("notification-mode").description("How notifications are displayed.").defaultValue(Notifications.mode.Chat).build());
    private final Setting<Boolean> announceOthers = sgGeneral.add(new BoolSetting.Builder().name("announce").description("Announces when other players pop totems in global chat.").defaultValue(false).visible(others::get).build());
    public final Setting<Boolean> pmOthers = sgGeneral.add(new BoolSetting.Builder().name("pm").description("Message players when they pop a totem.").defaultValue(false).visible(announceOthers::get).build());
    private final Setting<Integer> announceDelay = sgGeneral.add(new IntSetting.Builder().name("announce-delay").description("How many seconds between announcing totem pops.").defaultValue(5).min(1).sliderMax(100).visible(announceOthers::get).build());
    private final Setting<Double> announceRange = sgGeneral.add(new DoubleSetting.Builder().name("announce-range").description("How close players need to be to announce pops or AutoEz.").defaultValue(3).min(0).sliderMax(10).visible(announceOthers::get).build());
    private final Setting<Boolean> dontAnnounceFriends = sgGeneral.add(new BoolSetting.Builder().name("dont-announce-friends").description("Don't annnounce when your friends pop.").defaultValue(true).build());
    public final Setting<Boolean> doPlaceholders = sgGeneral.add(new BoolSetting.Builder().name("placeholders").description("Enable global placeholders for pop messages.").defaultValue(false).build());

    // AutoEz
    public final Setting<Boolean> autoEz = sgAutoEz.add(new BoolSetting.Builder().name("auto-ez").description("Send a message when you kill a player.").defaultValue(false).build());
    private final Setting<Integer> ezDelay = sgGeneral.add(new IntSetting.Builder().name("ez-delay").description("How many seconds before sending an ez message.").defaultValue(5).min(1).sliderMax(100).visible(announceOthers::get).build());
    public final Setting<Boolean> pmEz = sgAutoEz.add(new BoolSetting.Builder().name("pm-ez").description("Send the AutoEz message to the player's dm.").defaultValue(false).build());
    public final Setting<Boolean> killStr = sgAutoEz.add(new BoolSetting.Builder().name("killstreak").description("Add your killstreak to the end of AutoEz messages.").defaultValue(false).build());
    public final Setting<Boolean> suffix = sgAutoEz.add(new BoolSetting.Builder().name("suffix").description("Add Ghostware suffix to the end of pop messages.").defaultValue(false).build());

    // Messages
    private final Setting<List<String>> ezMessages = sgMessages.add(new StringListSetting.Builder().name("ez-messages").description("Messages to use for AutoEz.").defaultValue(Collections.emptyList()).build());
    private final Setting<List<String>> popMessages = sgMessages.add(new StringListSetting.Builder().name("pop-messages").description("Messages to use when announcing pops.").defaultValue(Collections.emptyList()).build());

    public final Object2IntMap<UUID> totemPops = new Object2IntOpenHashMap<>();
    private final Object2IntMap<UUID> chatIds = new Object2IntOpenHashMap<>();

    private final Random random = new Random();
    private int updateWait = 0;

    public PopCounter() {
        super(Categories.Chat, "pop-counter", "Count player's totem pops.");
    }
    private int announceWait;


    @Override
    public void onActivate() {
        totemPops.clear();
        chatIds.clear();
        announceWait = announceDelay.get() * 20;
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        Stats.reset();
        totemPops.clear();
        chatIds.clear();
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {

        // Auto EZ
        if ((event.packet instanceof GameMessageS2CPacket packet)) {
            if (packet.getSender().toString().contains("000000000")) { //make sure message is from the server
                String msg = packet.getMessage().getString();
                if (msg.contains("by " + mc.player.getEntityName())) { //make sure it contains "by PlayerName"
                    String firstName = msg.substring(0, msg.indexOf(" ")); //get the first str in the message (who died)
                    if (firstName.contains(mc.player.getEntityName())) return; //make sure it wasn't us
                    for (Friend friend : Friends.get()) if (firstName.contains(friend.name)) return; //make sure it wasn't a friend
                    queueEz(firstName);
                }
            }
        }

        // Totem Pops
        if (!(event.packet instanceof EntityStatusS2CPacket p)) return;
        if (p.getStatus() != 35) return;
        Entity entity = p.getEntity(mc.world);
        if (entity != null && ! (entity instanceof PlayerEntity)) return;
        if (entity == null
                || (entity.equals(mc.player) && !own.get())
                || (Friends.get().isFriend(((PlayerEntity) entity)) && !others.get())
                || (!Friends.get().isFriend(((PlayerEntity) entity)) && !friends.get())
        ) return;

        synchronized (totemPops) {
            int pops = totemPops.getOrDefault(entity.getUuid(), 0);
            totemPops.put(entity.getUuid(), ++pops);
            switch (notifyMode.get()) {
                case Chat -> ChatUtils.sendMsg(getChatId(entity), Formatting.GRAY, "(highlight)%s (default)popped (highlight)%d (default)%s.", entity.getEntityName(), pops, pops == 1 ? "totem" : "totems");
                case Hud -> popAlertHUD((PlayerEntity) entity, pops, false);
                case Toast -> popAlertToast((PlayerEntity) entity, pops, false);
            }
        }
        if (announceOthers.get() && mc.player.distanceTo(entity) <= announceRange.get() && announceWait <= 0) {
            if (dontAnnounceFriends.get() && Friends.get().isFriend((PlayerEntity) entity)) return;
            String popMessage = getPopMessage((PlayerEntity) entity);
            if (doPlaceholders.get()) popMessage = Placeholders.apply(popMessage);
            String name = entity.getEntityName();
            if (suffix.get()) { popMessage = popMessage + " | Ghostware " + Ghostware.VERSION; }
            Wrapper.sendMessage(popMessage);
            if (pmOthers.get()) Wrapper.messagePlayer(name, StringHelper.stripName(name, popMessage));
            announceWait = announceDelay.get() * 20;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        OutgoingMessages.updateQueue();
        announceWait--;
        synchronized (totemPops) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (!totemPops.containsKey(player.getUuid())) continue;

                if (player.deathTime > 0 || player.getHealth() <= 0) {
                    int pops = totemPops.removeInt(player.getUuid());
                    Globals.deaths.add(new Globals.DeathInstance(player, pops));
                    switch (notifyMode.get()) {
                        case Chat -> ChatUtils.sendMsg(getChatId(player), Formatting.GRAY, "(highlight)%s (default)popped (highlight)%d (default)%s.", player.getEntityName(), pops, pops == 1 ? "totem" : "totems");
                        case Hud -> popAlertHUD(player, pops, true);
                        case Toast -> popAlertToast(player, pops, true);
                    }
                    chatIds.removeInt(player.getUuid());
                }
            }
        }
    }

    public void queueEz(String target) {
        Stats.kills++;
        Stats.killStreak++;
        Stats.addKill(target);
        if (!autoEz.get()) return; // still track stats without sending messages
        if (ezMessages.get().isEmpty()) {
            ChatUtils.warning("Your auto ez message list is empty!");
            return;
        }
        String ezMessage;
        // prevent duplicates
        OutgoingMessages.ezMessages.removeIf(e -> e.playerName.contains(target));
        // placeholder
        ArrayList<String> msgs = new ArrayList<>();
        // try and retrieve a death instance for the target
        Globals.DeathInstance deathInstance = Globals.getDeathInstanceByName(target);
        int i = 0;
        if (deathInstance == null) { // if we don't have one we can't use messages that have the {pops} placeholder
            for (String ezMsg : ezMessages.get()) { // add any ez message without it to the placeholder list
                if (!ezMsg.contains("{pops}")) {
                    i++;
                    msgs.add(ezMsg);
                }
            }
            if (i > 0) { // get a random ez message from the placeholder list
                ezMessage = msgs.get(new Random().nextInt(msgs.size()));
            } else { // if all the messages contain {pops} use the default message
                ezMessage = "GG {player}, Ghostware owns me and all";
            }
        } else { // if we have one just pick an ez message randomly like normal
            ezMessage = ezMessages.get().get(new Random().nextInt(ezMessages.get().size()));
            if (ezMessage.contains("{pops}")) ezMessage = ezMessage.replace("{pops}", String.valueOf(deathInstance.pops));
        }
        //String ezMessage = ezMessages.get().get(new Random().nextInt(ezMessages.get().size()));
        // placeholder parsing
        if (ezMessage.contains("{player}")) ezMessage = ezMessage.replace("{player}", target);
        if (doPlaceholders.get()) ezMessage = Placeholders.apply(ezMessage);
        if (killStr.get()) ezMessage = ezMessage + " | Killstreak: " + Stats.killStreak;
        if (suffix.get()) ezMessage = ezMessage + " | Ghostware " + Ghostware.VERSION;
        // queue the ez message
        OutgoingMessages.ezMessages.add(new OutgoingMessages.EzMessage(target, ezMessage, ezDelay.get(), pmEz.get()));
    }

    private String getPopAlert(PlayerEntity p, int pops, boolean died) {
        String popAlert = "";
        if (died) {
            popAlert = p.getEntityName() + " died after popping " + pops + getPopGrammar(pops);
        } else {
            popAlert = p.getEntityName() + " popped " + pops + getPopGrammar(pops);
        }
        return popAlert;
    }

    private void popAlertToast(PlayerEntity p, int pops, boolean died) {
        NotificationsHUD.popAlert(getPopAlert(p, pops, died));
    }

    private void popAlertHUD(PlayerEntity p, int pops, boolean died) {
        NotificationsHUD.addNotification(getPopAlert(p, pops, died));
    }

    private String getPopGrammar(int pops) {
        if (pops == 1) return " totem";
        return " totems";
    }

    private int getChatId(Entity entity) {
        return chatIds.computeIntIfAbsent(entity.getUuid(), value -> random.nextInt());
    }

    private String getPopMessage(PlayerEntity p) {
        if (popMessages.get().isEmpty()) {
            ChatUtils.warning("Your pop message list is empty!");
            return "Ez pop";
        }
        String playerName = p.getEntityName();
        String popMessage = popMessages.get().get(new Random().nextInt(popMessages.get().size()));
        if (popMessage.contains("{pops}")) {
            if (totemPops.containsKey(p.getUuid())) {
                int pops = totemPops.getOrDefault(p.getUuid(), 0);
                popMessage = popMessage.replace("{pops}", pops + " " + getPopGrammar(pops));
            } else { popMessage = "Ezz pop"; }
        }
        if (popMessage.contains("{player}")) popMessage = popMessage.replace("{player}", playerName);
        return popMessage;
    }

}
