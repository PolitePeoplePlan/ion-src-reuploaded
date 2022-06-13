package me.ghosttypes.ghostware.modules.combat;

import me.ghosttypes.ghostware.utils.Categories;
import me.ghosttypes.ghostware.utils.misc.ItemCounter;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.DamageUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;

import java.text.DecimalFormat;

public class TacticalLog extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<TickMode> tickMode = sgGeneral.add(new EnumSetting.Builder<TickMode>().name("tick-mode").description("When to check each tick.").defaultValue(TickMode.Post).build());
    public final Setting<Integer> totems = sgGeneral.add(new IntSetting.Builder().name("min-totems").description("How many totems you must have left before logging.").defaultValue(2).min(1).sliderMax(20).max(20).build());
    public final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder().name("health").description("What health to log out at.").defaultValue(15).min(1).sliderMax(20).max(20).build());
    public final Setting<Boolean> requireTotems = sgGeneral.add(new BoolSetting.Builder().name("strict-totem").description("Ignore health check until your totems are below min-totems.").defaultValue(true).build());
    public final Setting<Boolean> zeroTickBypass = sgGeneral.add(new BoolSetting.Builder().name("zero-tick-bypass").description("Log out if you get zero ticked by a sword").defaultValue(true).build());
    public final Setting<Boolean> beforeDeath = sgGeneral.add(new BoolSetting.Builder().name("death-bypass").description("Log out if you will take enough damage to kill you.").defaultValue(true).build());
    public final Setting<Boolean> beforeDeathStrict = sgGeneral.add(new BoolSetting.Builder().name("death-bypass-strict").description("Only log out if you will die, ignore if you will pop.").defaultValue(true).visible(beforeDeath::get).build());
    public final Setting<Boolean> beforeDeathStrictTotem = sgGeneral.add(new BoolSetting.Builder().name("death-bypass-totem-check").description("Log out if the next pop will be below min-totems.").defaultValue(true).visible(beforeDeath::get).build());

    public TacticalLog() {super(Categories.Combat, "tactical-log", "Auto log v2");}


    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (tickMode.get() == TickMode.Pre) check();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (tickMode.get() == TickMode.Post) check();
    }

    private void check() {
        int totemCount = ItemCounter.totem();
        if (totemCount <= totems.get()) logOut("Totem Count below min totems");
        if (mc.player.getHealth() <= health.get()) {
            if (requireTotems.get() && totemCount <= totems.get()) {
                logOut("Health below min health : " + getHealthString());
            }
        }
        if (beforeDeath.get()) {
            double possibleDamage = PlayerUtils.possibleHealthReductions();
            if (PlayerUtils.getTotalHealth() - PlayerUtils.possibleHealthReductions() < health.get()) {
                String firstReason = "You were going to take " + getDamageString(possibleDamage) + "dmg, ";
                if (beforeDeathStrict.get() && !(mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING)) {
                    logOut(firstReason + "and had no totem equipped");
                }
                if (beforeDeathStrictTotem.get() && (totemCount - 1) <= totems.get()) {
                    logOut(firstReason + "and would be below " + totems.get() + " totems");
                }
            }
        }
        if (zeroTickBypass.get()) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof PlayerEntity && entity.getUuid() != mc.player.getUuid()) {
                    PlayerEntity player = (PlayerEntity) entity;
                    if (!Friends.get().isFriend(player)) {
                        if (mc.player.distanceTo(entity) < 8 && DamageUtils.getSwordDamage(player, true) > PlayerUtils.getTotalHealth()) {
                            logOut("You were about to be 0ticked by " + player.getEntityName());
                        }
                    }
                }
            }
        }
    }


    private String getHealthString() {
        DecimalFormat df = new DecimalFormat("0.00");
        float health = mc.player.getHealth();
        return df.format(health);
    }

    private String getDamageString(Double damage) {
        DecimalFormat df = new DecimalFormat("0.00");
        return df.format(damage);
    }

    private void logOut(String reason) {
        BlockPos currentPos = mc.player.getBlockPos();
        String pos = "X: " + currentPos.getX() + " Y: " + currentPos.getY() + " Z: " + currentPos.getZ();
        String logMessage = "[TaticalLog] " + reason + "." + " Pos: " + pos;
        mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(new LiteralText(logMessage)));
    }

    public enum TickMode {
        Pre,
        Post
    }
}
