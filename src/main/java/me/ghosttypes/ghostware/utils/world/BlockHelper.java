package me.ghosttypes.ghostware.utils.world;

import me.ghosttypes.ghostware.utils.player.AutomationUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BlockHelper {

    //TODO: merge these into one method
    public static boolean isVecComplete(ArrayList<Vec3d> vlist) {
        if (vlist == null) return false;
        if (vlist.isEmpty()) return false;
        BlockPos ppos = mc.player.getBlockPos();
        for (Vec3d b: vlist) {
            BlockPos bb = ppos.add(b.x, b.y, b.z);
            if (getBlock(bb) == Blocks.AIR) return false;
        }
        return true;
    }

    public static boolean isTargetVecComplete(PlayerEntity target, ArrayList<Vec3d> vlist) {
        if (vlist == null) return false;
        if (vlist.isEmpty()) return false;
        BlockPos ppos = target.getBlockPos();
        for (Vec3d b: vlist) {
            BlockPos bb = ppos.add(b.x, b.y, b.z);
            if (getBlock(bb) == Blocks.AIR) return false;
        }
        return true;
    }

    public static boolean isArrayComplete(ArrayList<BlockPos> blist) {
        if (blist == null) return false;
        if (blist.isEmpty()) return false;
        for (BlockPos b: blist) if (getBlock(b) == Blocks.AIR) return false;
        return true;
    }

    public static List<BlockPos> getSphere(BlockPos centerPos, int radius, int height) {
        ArrayList<BlockPos> blocks = new ArrayList<>();
        for (int i = centerPos.getX() - radius; i < centerPos.getX() + radius; i++) {
            for (int j = centerPos.getY() - height; j < centerPos.getY() + height; j++) {
                for (int k = centerPos.getZ() - radius; k < centerPos.getZ() + radius; k++) {
                    BlockPos pos = new BlockPos(i, j, k);
                    if (distanceBetween(centerPos, pos) <= radius && !blocks.contains(pos)) blocks.add(pos);
                }
            }
        }
        return blocks;
    }

    public static double distanceBetween(BlockPos pos1, BlockPos pos2) {
        double d = pos1.getX() - pos2.getX();
        double e = pos1.getY() - pos2.getY();
        double f = pos1.getZ() - pos2.getZ();
        return MathHelper.sqrt((float) (d * d + e * e + f * f));
    }

    public static BlockPos getBlockPosFromDirection(Direction direction, BlockPos orginalPos) {
        return switch (direction) {
            case UP -> orginalPos.up();
            case DOWN -> orginalPos.down();
            case EAST -> orginalPos.east();
            case WEST -> orginalPos.west();
            case NORTH -> orginalPos.north();
            case SOUTH -> orginalPos.south();
        };
    }

    public static Block getBlock(BlockPos p) {
        if (p == null) return null;
        return mc.world.getBlockState(p).getBlock();
    }

    public static BlockState getState(BlockPos p) {
        if (p == null) return null;
        return mc.world.getBlockState(p);
    }
    public static double getHardness(BlockPos p) {
        if (p == null || mc.world == null) return 420;
        return getState(p).getHardness(mc.world, p);
    }

    public static boolean isAir(BlockPos p) {
        return getBlock(p) == Blocks.AIR;
    }

    public static BlockPos getCityBlock(PlayerEntity target, Boolean randomize) {
        if (target == null || mc.player.distanceTo(target) > 4.8) return null;
        BlockPos targetBlock;
        // Get surround positions
        List<BlockPos> cityBlocks = AutomationUtils.getSurroundBlocks(target);
        if (cityBlocks == null) return null;
        if (cityBlocks.isEmpty()) return null;
        // Sort them by distance
        cityBlocks.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));
        // Remove any blocks that are part of our surround, or out of range
        cityBlocks.removeIf(cityBlock -> isOurSurroundBlock(cityBlock) || outOfRange(cityBlock));
        if (!cityBlocks.isEmpty()) return cityBlocks.get(0);
        return null;
    }

    public static boolean isOurSurroundBlock(BlockPos bp) {
        BlockPos ppos = mc.player.getBlockPos();
        for (Direction direction : Direction.values()) {
            if (direction == Direction.UP || direction == Direction.DOWN) continue;
            BlockPos pos = ppos.offset(direction);
            if (pos.equals(bp)) return true;
        }
        return false;
    }

    public static boolean outOfRange(BlockPos cityBlock) {
        return MathHelper.sqrt((float) mc.player.squaredDistanceTo(cityBlock.getX(), cityBlock.getY(), cityBlock.getZ())) > mc.interactionManager.getReachDistance();
    }
}
