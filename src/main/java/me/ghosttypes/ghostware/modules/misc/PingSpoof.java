package me.ghosttypes.ghostware.modules.misc;

import me.ghosttypes.ghostware.utils.Categories;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.KeepAliveC2SPacket;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class PingSpoof extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    private final Setting<Integer> lagMS = sgGeneral.add(new IntSetting.Builder().name("latency").description("How many MS of latency to add to your ping.").defaultValue(500).min(1).max(2000).build());

    public static Executor packetManager = Executors.newSingleThreadExecutor();
    private final ArrayList<Packet<?>> packets = new ArrayList<>();
    private boolean sendingPackets, sendPackets;

    public PingSpoof() {super(Categories.Misc, "ping-spoof", "Make your ping higher.");}

    @Override
    public void onActivate() {
        sendingPackets = false;
        sendPackets = false;
        packetManager.execute(this::managePackets);
    }

    private void managePackets() {
        while (this.isActive()) {
            try {Thread.sleep(lagMS.get());} catch (Exception ignored) {}
            sendPackets = true;
        }
    }

    private void sendPacket(Packet<?> packet) {
        try { mc.getNetworkHandler().sendPacket(packet);} catch (Exception ignored) {}
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (sendingPackets) return;
        if (event.packet instanceof KeepAliveC2SPacket) {
            packets.add(event.packet);
            event.cancel();
        }
        if (sendPackets) {
            sendingPackets = true;
            packets.forEach(this::sendPacket);
            sendingPackets = false;
            packets.clear();
            sendPackets = false;
        }
    }
}
