package me.ghosttypes.ghostware.modules.misc;

import me.ghosttypes.ghostware.utils.Categories;
import meteordevelopment.meteorclient.events.entity.player.BreakBlockEvent;
import meteordevelopment.meteorclient.events.entity.player.InteractBlockEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AntiDesync extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> breakBlock = sgGeneral.add(new BoolSetting.Builder().name("break").description("Anti-desync for block breaking.").defaultValue(true).build());
    private final Setting<Boolean> placeBlock = sgGeneral.add(new BoolSetting.Builder().name("place").description("Anti-desync for block placing.").defaultValue(true).build());

    public AntiDesync() {
        super(Categories.Misc, "anti-desync", "Prevent ghost block placements.");
    }


    @EventHandler
    private void onBlockPlace(InteractBlockEvent event) {
        if (!placeBlock.get()) return;
        BlockPos placePos = event.result.getBlockPos();
        if (placePos != null) mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, placePos, Direction.UP));
    }

    @EventHandler
    private void onBlockBreak(BreakBlockEvent event) {
        if (!breakBlock.get()) return;
        BlockPos breakPos = event.blockPos;
        if (breakPos != null) mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, breakPos, Direction.UP));
    }
}
