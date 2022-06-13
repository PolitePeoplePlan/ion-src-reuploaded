package me.ghosttypes.ghostware.modules.combat;

import me.ghosttypes.ghostware.modules.hud.visual.NotificationsHUD;
import me.ghosttypes.ghostware.modules.misc.Notifications;
import me.ghosttypes.ghostware.utils.Categories;
import me.ghosttypes.ghostware.utils.Wrapper;
import me.ghosttypes.ghostware.utils.player.AutomationUtils;
import me.ghosttypes.ghostware.utils.world.BlockHelper;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.Timer;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;


public class BurrowPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<BurrowMode> burrowMode = sgGeneral.add(new EnumSetting.Builder<BurrowMode>().name("mode").description("Which burrow mode to use.").defaultValue(BurrowMode.Single).build());
    private final Setting<Integer> persistDelay = sgGeneral.add(new IntSetting.Builder().name("persist-delay").description("Delay between re-burrowing.").defaultValue(3).min(1).sliderMax(10).visible(() -> burrowMode.get() == BurrowMode.Persist).build());
    private final Setting<Block> block = sgGeneral.add(new EnumSetting.Builder<Block>().name("block-to-use").description("The block to use for Burrow.").defaultValue(Block.EChest).build());
    private final Setting<Double> rubberbandHeight = sgGeneral.add(new DoubleSetting.Builder().name("rubberband-height").description("How far to attempt to cause rubberband.").defaultValue(12).sliderMin(-30).sliderMax(30).build());
    private final Setting<Double> timer = sgGeneral.add(new DoubleSetting.Builder().name("timer").description("Timer override.").defaultValue(1.00).min(0.01).sliderMax(10).build());
    private final Setting<Boolean> surroundAssist = sgGeneral.add(new BoolSetting.Builder().name("auto-surround").description("Turn on surround before burrowing.").defaultValue(false).build());
    private final Setting<Boolean> surroundAssistToggle = sgGeneral.add(new BoolSetting.Builder().name("toggle-after").description("Turn surround off after you are surrounded.").defaultValue(false).visible(surroundAssist::get).build());
    private final Setting<Boolean> onlyInHole = sgGeneral.add(new BoolSetting.Builder().name("only-in-holes").description("Stops you from burrowing when not in a hole.").defaultValue(false).build());
    private final Setting<Boolean> center = sgGeneral.add(new BoolSetting.Builder().name("center").description("Centers you to the middle of the block before burrowing.").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Faces the block you place server-side.").defaultValue(true).build());
    public final Setting<Notifications.mode> notifyMode = sgGeneral.add(new EnumSetting.Builder<Notifications.mode>().name("notification-mode").description("How notifications are displayed.").defaultValue(Notifications.mode.Chat).build());

    private final BlockPos.Mutable blockPos = new BlockPos.Mutable();
    private int persistTimer;

    public BurrowPlus() {
        super(Categories.Combat, "burrow-plus", "Easily.");
    }

    @Override
    public void onActivate() {
        persistTimer = 0;
        if (!mc.world.getBlockState(mc.player.getBlockPos()).getMaterial().isReplaceable()) {
            if (burrowMode.get() == BurrowMode.Persist) {
                persistTimer = persistDelay.get();
            } else {
                error("You are already burrowed.");
                toggle();
                return;
            }
        }
        if (!Wrapper.isInHole(mc.player) && onlyInHole.get()) {
            error("You are not in a hole.");
            toggle();
            return;
        }
        if (!checkHead()) {
            error("You have blocks too close to your head, cannot burrow.");
            toggle();
            return;
        }
        FindItemResult result = getItem();
        if (!result.isHotbar() && !result.isOffhand()) {
            error("No burrow block found.");
            toggle();
            return;
        }
        blockPos.set(mc.player.getBlockPos());
        Modules.get().get(Timer.class).setOverride(this.timer.get());
    }

    @Override
    public void onDeactivate() {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        SurroundPlus surround = Modules.get().get(SurroundPlus.class);
        if (shouldBurrow()) {
            if (!mc.player.isOnGround()) {
                toggle();
                return;
            }
            if (surroundAssist.get()) {
                if (BlockHelper.isArrayComplete(AutomationUtils.getSurroundBlocks(mc.player))) {
                    if (surround.isActive() && surroundAssistToggle.get()) surround.toggle();
                } else {
                    if (!surround.isActive()) surround.toggle();
                    return;
                }
            }
            if (burrowMode.get() == BurrowMode.Persist) {
                if (persistTimer <= 0) {
                    doBurrow();
                    persistTimer = persistDelay.get();
                } else {
                    persistTimer--;
                }
            } else {
                doBurrow();
                toggle();
            }
        } else {
            blockPos.set(mc.player.getBlockPos());
        }
    }

    private boolean shouldBurrow() {
        return mc.world.getBlockState(mc.player.getBlockPos()).getMaterial().isReplaceable();
    }

    private void doBurrow() {
        if (rotate.get()) {
            Rotations.rotate(Rotations.getYaw(mc.player.getBlockPos()), Rotations.getPitch(mc.player.getBlockPos()), 50, this::burrow);
        } else {
            burrow();
        }
    }

    private void burrow() {
        if (center.get()) PlayerUtils.centerPlayer();
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.4, mc.player.getZ(), true));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.75, mc.player.getZ(), true));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.01, mc.player.getZ(), true));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.15, mc.player.getZ(), true));
        FindItemResult block = getItem();
        if (!(mc.player.getInventory().getStack(block.getSlot()).getItem() instanceof BlockItem)) return;
        InvUtils.swap(block.getSlot(), true);
        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, new BlockHitResult(Utils.vec3d(blockPos), Direction.UP, blockPos, false));
        mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        InvUtils.swapBack();
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + rubberbandHeight.get(), mc.player.getZ(), false));
    }

    private FindItemResult getItem() {
        return switch (block.get()) {
            case EChest -> InvUtils.findInHotbar(Items.ENDER_CHEST);
            case Anvil -> InvUtils.findInHotbar(itemStack -> net.minecraft.block.Block.getBlockFromItem(itemStack.getItem()) instanceof AnvilBlock);
            case Held -> InvUtils.findInHotbar(itemStack -> true);
            default -> InvUtils.findInHotbar(Items.OBSIDIAN, Items.CRYING_OBSIDIAN);
        };
    }

    private boolean checkHead() {
        BlockState blockState1 = mc.world.getBlockState(blockPos.set(mc.player.getX() + .3, mc.player.getY() + 2.3, mc.player.getZ() + .3));
        BlockState blockState2 = mc.world.getBlockState(blockPos.set(mc.player.getX() + .3, mc.player.getY() + 2.3, mc.player.getZ() - .3));
        BlockState blockState3 = mc.world.getBlockState(blockPos.set(mc.player.getX() - .3, mc.player.getY() + 2.3, mc.player.getZ() - .3));
        BlockState blockState4 = mc.world.getBlockState(blockPos.set(mc.player.getX() - .3, mc.player.getY() + 2.3, mc.player.getZ() + .3));
        boolean air1 = blockState1.getMaterial().isReplaceable();
        boolean air2 = blockState2.getMaterial().isReplaceable();
        boolean air3 = blockState3.getMaterial().isReplaceable();
        boolean air4 = blockState4.getMaterial().isReplaceable();
        return air1 & air2 & air3 & air4;
    }

    public enum Block {
        EChest,
        Obsidian,
        Anvil,
        Held
    }

    public enum BurrowMode {
        Single,
        Persist
    }


    private void notify(String msg) {
        String title = "[" + this.name + "] ";
        switch (notifyMode.get()) {
            case Chat -> info(msg);
            case Hud -> NotificationsHUD.addNotification(title + msg);
        }
    }
}
