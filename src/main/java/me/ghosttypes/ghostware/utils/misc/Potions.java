package me.ghosttypes.ghostware.utils.misc;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;


public class Potions {

    public static StatusEffect SPEED = StatusEffect.byRawId(1);
    public static StatusEffect STRENGTH = StatusEffect.byRawId(5);


    public static int getAmplifier(PlayerEntity p, StatusEffect statusEffect) {
        return p.getStatusEffect(statusEffect).getAmplifier() + 1;
    }
}
