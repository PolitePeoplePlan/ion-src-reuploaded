package me.ghosttypes.ghostware.utils.player;

import me.ghosttypes.ghostware.modules.chat.PopCounter;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class PlayerUtil {

    public static int getPops(PlayerEntity p) {
        PopCounter popCounter = Modules.get().get(PopCounter.class);
        if (!popCounter.isActive()) return 0;
        if (!popCounter.totemPops.containsKey(p.getUuid())) return 0;
        return popCounter.totemPops.getOrDefault(p.getUuid(), 0);
    }

    public static PlayerEntity getPlayerByName(String name) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity player) if (player.getEntityName().equalsIgnoreCase(name)) return player;
        }
        return null;
    }


    public static boolean isHoldingGap() {
        //TODO make offhand boolean
        return mc.player.getMainHandStack().getItem() == Items.ENCHANTED_GOLDEN_APPLE;//|| mc.player.getOffHandStack().getItem() == Items.ENCHANTED_GOLDEN_APPLE;
    }

    public static boolean isDead(PlayerEntity player) {
        return !player.isAlive() || player.isDead() || player.getHealth() <= 0;
    }

}
