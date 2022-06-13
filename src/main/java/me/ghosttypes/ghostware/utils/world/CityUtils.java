package me.ghosttypes.ghostware.utils.world;

import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CityUtils {

    public static List<BlockPos> getSmartBlocks(PlayerEntity player, double mineRange) {
        if (player == null) return null;

        List<BlockPos> positions = new ArrayList<>();

        BlockPos pos = player.getBlockPos();

        if (validBlock(pos.west(), mineRange) && (BlockUtils.canPlace(pos.west(2)) && canCrystal(pos.west(2)) || BlockUtils.canPlace(pos.west().south()) && canCrystal(pos.west().south()) || BlockUtils.canPlace(pos.west().north()) && canCrystal(pos.west().north())))
            positions.add(pos.west());
        if (validBlock(pos.east(), mineRange) && (BlockUtils.canPlace(pos.east(2)) && canCrystal(pos.east(2)) || BlockUtils.canPlace(pos.east().south()) && canCrystal(pos.east().south()) || BlockUtils.canPlace(pos.east().north()) && canCrystal(pos.east().north())))
            positions.add(pos.east());
        if (validBlock(pos.south(), mineRange) && (BlockUtils.canPlace(pos.south(2)) && canCrystal(pos.south(2)) || BlockUtils.canPlace(pos.west().south()) && canCrystal(pos.west().south()) || BlockUtils.canPlace(pos.south().east()) && canCrystal(pos.south().east())))
            positions.add(pos.south());
        if (validBlock(pos.north(), mineRange) && (BlockUtils.canPlace(pos.north(2)) && canCrystal(pos.north(2)) || BlockUtils.canPlace(pos.north().east()) && canCrystal(pos.north().east()) || BlockUtils.canPlace(pos.west().north()) && canCrystal(pos.west().north())))
            positions.add(pos.north());

        if (positions.isEmpty()) {
            for (Direction direction : Direction.values()) {
                if (direction == Direction.UP || direction == Direction.DOWN) continue;
                BlockPos off = player.getBlockPos().offset(direction);
                if (validBlock(off, mineRange)) {
                    positions.add(off);
                }
            }
        }
        return positions;
    }

    public static BlockPos getSmartCityBlock(PlayerEntity player, double mineRange) {
        List<BlockPos> posList = getSmartBlocks(player, mineRange);
        posList.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));
        return posList.isEmpty() ? null : posList.get(0);
    }

    public static boolean canCrystal(BlockPos pos) {
        if (mc.world.getBlockState(pos.down()).getBlock() == Blocks.OBSIDIAN || mc.world.getBlockState(pos.down()).getBlock() == Blocks.BEDROCK)
            return true;
        return false;
    }

    public static boolean validBlock(BlockPos pos, double mineRange) {
        if (mc.world.getBlockState(pos).getBlock().getBlastResistance() >= 600 && meteordevelopment.meteorclient.utils.world.BlockUtils.canBreak(pos) && PlayerUtils.distanceTo(pos) <= mineRange && !EntityUtils.getSurroundBlocks(mc.player).contains(pos))
            return true;
        return false;
    }

}
