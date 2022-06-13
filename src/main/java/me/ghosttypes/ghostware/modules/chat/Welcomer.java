package me.ghosttypes.ghostware.modules.chat;

import me.ghosttypes.ghostware.Ghostware;
import me.ghosttypes.ghostware.utils.Categories;
import me.ghosttypes.ghostware.utils.Wrapper;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Welcomer extends Module {


    private final Executor messageSender = Executors.newSingleThreadExecutor();
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").description("The delay between sending messages.").defaultValue(1).min(1).sliderMax(10).max(10).build());
    private final Setting<List<String>> joinMessages = sgGeneral.add(new StringListSetting.Builder().name("join-messages").description("Messages to send when a player joins.").defaultValue(Collections.emptyList()).build());
    private final Setting<List<String>> leaveMessages = sgGeneral.add(new StringListSetting.Builder().name("leave-messages").description("Messages to send when a player leaves.").defaultValue(Collections.emptyList()).build());


    public Welcomer() {
        super(Categories.Chat, "welcomer", "Sends a message when somebody joins or leaves the server.");
    }


    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        assert mc.player != null;
        if (mc.player.age < 30) return;
        if ((event.packet instanceof GameMessageS2CPacket packet)) {
            if (packet.getSender().toString().contains("000000000")) {
                String msg = packet.getMessage().getString();
                if (msg.contains("left")) {
                    boolean valid = false;
                    String name = msg.substring(0, msg.indexOf(" "));
                    for (PlayerEntity player : mc.world.getPlayers()) {
                        if (player.getEntityName().equals(name) || player.getDisplayName().asString().contains(name)) {
                            valid = true;
                            break;
                        }
                    }
                    if (valid) sendLeaveMsg(name);
                }
            }
        }

        if (event.packet instanceof PlayerListS2CPacket playerListPacket) {
            if (playerListPacket.getAction() == PlayerListS2CPacket.Action.ADD_PLAYER) {
                PlayerListS2CPacket.Entry entry = playerListPacket.getEntries().get(0);
                if (entry != null) {
                    String name = entry.getProfile().getName();
                    if (name != null) sendJoinMsg(name);
                }
            }
        }
    }

    private void sendJoinMsg(String name) {
        if (joinMessages.get().isEmpty()) return;
        String msg = joinMessages.get().get(new Random().nextInt(joinMessages.get().size()));
        msg = msg.replace("{player}", name);
        String finalMsg = msg;
        messageSender.execute(() -> queueMsg(finalMsg));
    }

    private void sendLeaveMsg(String name) {
        if (leaveMessages.get().isEmpty()) return;
        String msg = leaveMessages.get().get(new Random().nextInt(leaveMessages.get().size()));
        msg = msg.replace("{player}", name);
        String finalMsg = msg;
        messageSender.execute(() -> queueMsg(finalMsg));
    }

    private void queueMsg(String msg) {
        try { Thread.sleep(delay.get() * 1000); } catch (Exception e) { Ghostware.log("Welcomer error: " + e);}
        Wrapper.sendMessage(msg);
    }
}
