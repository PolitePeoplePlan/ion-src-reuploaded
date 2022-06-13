package me.ghosttypes.ghostware.modules.misc;

import me.ghosttypes.ghostware.utils.Categories;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

// credit to Murphy for porting the base module


public class OneTap extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAimbot = settings.createGroup("Aimbot");

    // General
    private final Setting<Boolean> bows = sgGeneral.add(new BoolSetting.Builder().name("bows").defaultValue(true).build());
    private final Setting<Boolean> pearls = sgGeneral.add(new BoolSetting.Builder().name("pearl").defaultValue(true).build());
    private final Setting<Boolean> eggs = sgGeneral.add(new BoolSetting.Builder().name("eggs").defaultValue(true).build());
    private final Setting<Boolean> snowballs = sgGeneral.add(new BoolSetting.Builder().name("snowballs").defaultValue(true).build());
    //private final Setting<Boolean> hungerCheck = sgGeneral.add(new BoolSetting.Builder().name("hunger-check").defaultValue(true).build());
    //private final Setting<Integer> minHunger = sgGeneral.add(new IntSetting.Builder().name("min-food").min(1).max(10).sliderMin(1).sliderMax(10).defaultValue(5).build());
    private final Setting<Boolean> sfx = sgGeneral.add(new BoolSetting.Builder().name("railgun-sfx").defaultValue(true).build());
    public final Setting<Double> sfxVolume = sgGeneral.add(new DoubleSetting.Builder().name("sfx-volume").description("How loud the sound is.").defaultValue(1).min(1).sliderMax(5).build());
    private final Setting<Integer> timeout = sgGeneral.add(new IntSetting.Builder().name("timeout").min(0).max(20000).sliderMin(100).sliderMax(20000).defaultValue(1).build());
    private final Setting<Integer> spoofs = sgGeneral.add(new IntSetting.Builder().name("spoofs").min(0).max(300).sliderMin(1).sliderMax(300).defaultValue(100).build());
    private final Setting<Boolean> bypass = sgGeneral.add(new BoolSetting.Builder().name("bypass").defaultValue(false).build());
    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder().name("debug").defaultValue(false).build());




    public OneTap() {
        super(Categories.Misc, "one-tap", "one tap bow exploit.");
    }

    private boolean shooting;
    private long lastShootTime;


    @Override
    public void onActivate() {
        shooting = false;
        lastShootTime = System.currentTimeMillis();
    }


    // Exploit
    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerActionC2SPacket packet) {
            if (packet.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM) {
                //if (!hungerCheck()) return;
                ItemStack handStack = mc.player.getStackInHand(Hand.MAIN_HAND);

                if (!handStack.isEmpty() && handStack.getItem() != null && handStack.getItem() instanceof BowItem && bows.get()) {
                    if (sfx.get()) {
                        float volume = sfxVolume.get().floatValue();
                        mc.player.playSound(SoundEvents.BLOCK_CONDUIT_ACTIVATE, volume, 1.0f);
                    }
                    doSpoofs();
                    if (debug.get()) ChatUtils.info(name, "trying to spoof");
                }
            }

        } else if (event.packet instanceof PlayerInteractItemC2SPacket packet2) {
            if (packet2.getHand() == Hand.MAIN_HAND) {
                //if (!hungerCheck()) return;
                ItemStack handStack = mc.player.getStackInHand(Hand.MAIN_HAND);

                if (!handStack.isEmpty() && handStack.getItem() != null) {
                    if (handStack.getItem() instanceof EggItem && eggs.get()) {
                        doSpoofs();
                    } else if (handStack.getItem() instanceof EnderPearlItem && pearls.get()) {
                        doSpoofs();
                    } else if (handStack.getItem() instanceof SnowballItem && snowballs.get()) {
                        doSpoofs();
                    }
                }
            }
        }
    }

    //private boolean hungerCheck() {
        //if (!hungerCheck.get()) return false;
        //return mc.player.getHungerManager().getFoodLevel() < (minHunger.get() * 2);
    //}


    private void doSpoofs() {
        if (System.currentTimeMillis() - lastShootTime >= timeout.get()) {
            shooting = true;
            lastShootTime = System.currentTimeMillis();

            mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));

            for (int index = 0; index < spoofs.get(); ++index) {
                if (bypass.get()) {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1e-10, mc.player.getZ(), false));
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 1e-10, mc.player.getZ(), true));
                } else {
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() - 1e-10, mc.player.getZ(), true));
                    mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1e-10, mc.player.getZ(), false));
                }

            }

            if (debug.get()) ChatUtils.info(name, "spoofed");
            shooting = false;
        }
    }
}
