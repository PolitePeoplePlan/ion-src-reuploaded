package me.ghosttypes.ghostware.modules.misc;

import me.ghosttypes.ghostware.utils.Categories;
import me.ghosttypes.ghostware.utils.Wrapper;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.player.ChestSwap;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;

public class ElytraSwap extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgChestSwap = settings.createGroup("ChestSwap");
    private final SettingGroup sgFirework = settings.createGroup("Fireworks");
    private final SettingGroup sgTakeoff = settings.createGroup("Takeoff");

    // General
    private final Setting<Boolean> debug = sgFirework.add(new BoolSetting.Builder().name("debug").defaultValue(false).build());
    public final Setting<Integer> fireworkMoveDelay = sgGeneral.add(new IntSetting.Builder().name("firework-move-delay").description("How long to wait before moving fireworks to your hotbar.").defaultValue(3).min(0).sliderMax(20).build());

    // Chest Swap
    public final Setting<ChestSwapMode> chestSwap = sgChestSwap.add(new EnumSetting.Builder<ChestSwapMode>().name("chest-swap").description("When to swap back to your chestplate.").defaultValue(ChestSwapMode.WaitForGround).build());

    // Fireworks
    private final Setting<Boolean> moveFireworks = sgFirework.add(new BoolSetting.Builder().name("move-to-hotbar").description("Moves fireworks into your hotbar.").defaultValue(false).build());
    private final Setting<Integer> fireworkSlot = sgFirework.add(new IntSetting.Builder().name("firework-slot").description("The slot to move fireworks to.").defaultValue(9).min(1).max(9).sliderMin(1).sliderMax(9).visible(moveFireworks::get).build());
    private final Setting<Boolean> swapToFireworks = sgFirework.add(new BoolSetting.Builder().name("swap-to-fireworks").description("Swap to fireworks after moving them to your hotbar.").defaultValue(false).visible(moveFireworks::get).build());
    private final Setting<Boolean> swapBack = sgFirework.add(new BoolSetting.Builder().name("swap-back").description("Swap the original item back to the firework slot.").defaultValue(false).visible(moveFireworks::get).build());

    // Takeoff
    private final Setting<Boolean> autoTakeoff = sgTakeoff.add(new BoolSetting.Builder().name("auto-takeoff").description("Start flying automatically.").defaultValue(false).build());
    private final Setting<Integer> takeoffPitch = sgTakeoff.add(new IntSetting.Builder().name("takeoff-pitch").description("What to set your pitch to before takeoff.").defaultValue(0).visible(autoTakeoff::get).build());
    private final Setting<Integer> takeoffFireworks = sgTakeoff.add(new IntSetting.Builder().name("takeoff-fireworks").description("How many fireworks to use for takeoff.").defaultValue(2).visible(autoTakeoff::get).build());


    private boolean moved, doTakeoff;
    private int moveTimer;
    private Item ogItem;

    public ElytraSwap() {
        super(Categories.Misc, "elytra-swap", "Swap to an elytra to quickly start flying");
    }


    @Override
    public void onActivate() {
        moved = false;
        doTakeoff = false;
        if (swapBack.get()) {
            ogItem = mc.player.getInventory().getStack(fireworkSlot.get()).getItem();
        } else {
            ogItem = null;
        }
        moveTimer = fireworkMoveDelay.get();
        if ((chestSwap.get() == ChestSwapMode.Always || chestSwap.get() == ChestSwapMode.WaitForGround)
            && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) {
            Modules.get().get(ChestSwap.class).swap();
        }
    }

    @Override
    public void onDeactivate() {
        if (chestSwap.get() == ChestSwapMode.Always && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
            Modules.get().get(ChestSwap.class).swap();
        } else if (chestSwap.get() == ChestSwapMode.WaitForGround) {
            enableGroundListener();
        }
        if (swapBack.get() && ogItem != null) {
            FindItemResult ogI = InvUtils.find(ogItem);
            InvUtils.move().from(ogI.getSlot()).to(fireworkSlot.get());
        }
    }


    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (moveFireworks.get()) {
            if (moveTimer <= 0 && !moved) {
                if (debug.get()) info("Moving fireworks");
                moved = true;
                doTakeoff = true;
                FindItemResult fireworks = InvUtils.find(Items.FIREWORK_ROCKET);
                if (fireworks.found()) {
                    InvUtils.move().from(fireworks.getSlot()).to(fireworkSlot.get());
                    if (swapToFireworks.get()) Wrapper.updateSlot(fireworkSlot.get());
                }
            } else {
                moveTimer--;
            }
        }
        if (doTakeoff && autoTakeoff.get()) {
            if (debug.get()) info("Taking off");
            FindItemResult firework = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
            if (firework.found()) {
                if (debug.get()) info("Found fireworks, rotating");
                Rotations.rotate(mc.player.getYaw(), takeoffPitch.get());
                if (debug.get()) info("Jumping and setting velocity");
                mc.player.jump();
                mc.player.setVelocity(0, -0.04, 0);
                if (debug.get()) info("Sending START_FLY_FALLING packet");
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                if (mc.player.getMainHandStack().getItem() != Items.FIREWORK_ROCKET) Wrapper.updateSlot(firework.getSlot());
                int used = 0;
                if (debug.get()) info("Using " + takeoffFireworks.get() + " fireworks for takeoff");
                while (used < takeoffFireworks.get()) {
                    useFirework();
                    used++;
                }
                if (debug.get()) info("Takeoff complete");
                doTakeoff = false;
            }
        }
    }



    private void useFirework() {
        mc.options.keyUse.setPressed(true);
        if (!mc.player.isUsingItem()) Utils.rightClick();
        mc.options.keyUse.setPressed(false);
    }

    private class StaticGroundListener {
        @EventHandler
        private void chestSwapGroundListener(PlayerMoveEvent event) {
            if (mc.player != null && mc.player.isOnGround()) {
                if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
                    Modules.get().get(ChestSwap.class).swap();
                    disableGroundListener();
                }
            }
        }
    }

    private final StaticGroundListener staticGroundListener = new StaticGroundListener();

    protected void enableGroundListener() {
        MeteorClient.EVENT_BUS.subscribe(staticGroundListener);
    }

    protected void disableGroundListener() {
        MeteorClient.EVENT_BUS.unsubscribe(staticGroundListener);
    }


    public enum ChestSwapMode {
        Always,
        Never,
        WaitForGround
    }
}
