package me.ghosttypes.ghostware.utils.combat;

import me.ghosttypes.ghostware.utils.player.AutomationUtils;
import me.ghosttypes.ghostware.utils.world.BlockHelper;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class HoleUtil {


    public static List<BlockPos> getHoles(BlockPos startingPos, int rangeH, int rangeV) {
        ArrayList<BlockPos> holes = new ArrayList<>();
        List<BlockPos> blocks = BlockHelper.getSphere(startingPos, rangeH, rangeV);
        blocks.removeIf(b -> BlockHelper.getBlock(b) != Blocks.AIR); // only want air blocks
        blocks.removeIf(block -> BlockHelper.getBlock(block.down()) == Blocks.AIR); // make sure there is a block below it
        blocks.removeIf(block -> !isHole(block)); // remove any non-hole position
        blocks.removeIf(block -> mc.player.getBlockPos().equals(block)); // remove our own position
        if (!blocks.isEmpty()) holes.addAll(blocks);
        return holes;
    }


    public static BlockPos getHoleNearPlayer(PlayerEntity player, int rangeH, int rangeV) {
        if (player == null) return null;
        List<BlockPos> holes = getHoles(player.getBlockPos(), rangeH, rangeV);
        holes.removeIf(HoleUtil::isHoleObstructed); // remove obstructed holes
        holes.removeIf(hole -> mc.player.getBlockPos().equals(hole)); // remove our own hole
        if (holes.isEmpty()) return null;
        return holes.get(0);
    }


    public static boolean isHole(BlockPos pos) {
        for (Vec3d sb : AutomationUtils.surroundPositions) if (BlockHelper.getBlock(pos.add(sb.x, sb.y, sb.z)) == Blocks.AIR) return false;
        return true;
    }

    public static boolean isHoleObstructed(BlockPos pos) {
        return BlockHelper.getBlock(pos.up()) != Blocks.AIR || BlockHelper.getBlock(pos.up(2)) != Blocks.AIR;
    }

}
