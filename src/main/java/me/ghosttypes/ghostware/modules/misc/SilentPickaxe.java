package me.ghosttypes.ghostware.modules.misc;

import me.ghosttypes.ghostware.utils.Categories;
import me.ghosttypes.ghostware.utils.world.BlockHelper;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class SilentPickaxe extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> onClick = sgGeneral.add(new BoolSetting.Builder().name("on-click").description("Works only while clicking on block.").defaultValue(true).build());
//    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder().name("debug").defaultValue(true).build());
//    private final Setting<Boolean> whitelisted = sgGeneral.add(new BoolSetting.Builder().name("whitelisted").defaultValue(true).build());
//    private final Setting<Double> eff5 = sgGeneral.add((new DoubleSetting.Builder()).name("eff5").defaultValue(1.0D).sliderMin(0.1D).sliderMax(5.0D).build());
//    private final Setting<Double> eff4 = sgGeneral.add((new DoubleSetting.Builder()).name("eff4").defaultValue(1.0D).sliderMin(0.1D).sliderMax(5.0D).build());
//    private final Setting<Double> eff3 = sgGeneral.add((new DoubleSetting.Builder()).name("eff3").defaultValue(1.0D).sliderMin(0.1D).sliderMax(5.0D).build());
//    private final Setting<Double> eff2 = sgGeneral.add((new DoubleSetting.Builder()).name("eff2").defaultValue(1.0D).sliderMin(0.1D).sliderMax(5.0D).build());
//    private final Setting<Double> eff1 = sgGeneral.add((new DoubleSetting.Builder()).name("eff1").defaultValue(1.0D).sliderMin(0.1D).sliderMax(5.0D).build());
//    private final Setting<Double> eff0 = sgGeneral.add((new DoubleSetting.Builder()).name("eff0").defaultValue(1.0D).sliderMin(0.1D).sliderMax(5.0D).build());

    public SilentPickaxe() {
        super(Categories.Misc, "silent-pickaxe", "");
    }

    public int preSlot;
    public boolean doMine;
    public BlockPos currentPos, prevPos;

    @Override
    public void onActivate() {
        currentPos = new BlockPos(1, 1, 1);
        prevPos = new BlockPos(2, 2, 2);

        doMine = false;
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (doMine) {
            gotoPreSlot();
            if (onClick.get()) mc.options.keyAttack.setPressed(false);
            doMine = false;
        }
    }

    @EventHandler
    public void onBreak(StartBreakingBlockEvent event) {
        BlockPos pos = event.blockPos;
        if (prevPos != currentPos) {
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, prevPos, Direction.UP));
            prevPos = currentPos;
        }

        currentPos = pos;
        preSlot = mc.player.getInventory().selectedSlot;

        if (/*whitelisted.get() && */!canBeBroken(findPickaxe().getSlot(), currentPos)) return;
        /*if (debug.get()) info(BlockHelper.getHardness(currentPos) + " hardness of the block.");*/

        doBreak(findPickaxe().getSlot(), currentPos);
        doMine = true;
    }

    public FindItemResult findPickaxe() {
        return InvUtils.findInHotbar(
            itemStack -> itemStack.getItem() == Items.NETHERITE_PICKAXE
        );
    }

    public boolean hasPickaxe() {
        /*if (debug.get()) info(findPickaxe().found() ? "found." : "not found.");*/
        return findPickaxe().found();
    }

    public boolean canBeBroken(int slot, BlockPos blockPos) {
        if (!hasPickaxe()) return false;

        ItemStack stack = mc.player.getInventory().getStack(slot);
        int efficiency = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, stack);

        return switch (efficiency) {
            case 5 -> BlockHelper.getHardness(blockPos) <= 1.5;
            case 4 -> BlockHelper.getHardness(blockPos) <= 1.2;
            case 3 -> BlockHelper.getHardness(blockPos) <= 0.81;
            case 2 -> BlockHelper.getHardness(blockPos) <= 0.65;
            case 1 -> BlockHelper.getHardness(blockPos) <= 0.5;
            default -> BlockHelper.getHardness(blockPos) <= 0.41;
        };
    }

    public void doBreak(int slot, BlockPos pos) {
        /*if (debug.get()) info("breaking...");*/

        if (hasPickaxe()) {
            InvUtils.swap(slot, true);
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
        }
    }

    public void gotoPreSlot() {
        if (hasPickaxe() && mc.player.getInventory().getMainHandStack().getItem() == Items.NETHERITE_PICKAXE && mc.player.getInventory().selectedSlot != preSlot) InvUtils.swap(preSlot, false);
        InvUtils.swap(preSlot, false);
    }
}
