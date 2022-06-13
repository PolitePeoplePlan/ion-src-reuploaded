package me.ghosttypes.ghostware.utils.combat;

import me.ghosttypes.ghostware.modules.combat.AnvilAura;
import me.ghosttypes.ghostware.utils.world.BlockHelper;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

public class AnvilUtil {


    public static boolean isValidFloorBlock(BlockPos p) {
        return BlockHelper.getBlock(p) instanceof AbstractButtonBlock || BlockHelper.getBlock(p) instanceof AbstractPressurePlateBlock;
    }


    public static ArrayList<Vec3d> getDesign(PlayerEntity p) {
        AnvilAura aa = Modules.get().get(AnvilAura.class);
        AnvilAura.LegacyMode mode = aa.legacyMode.get();
        ArrayList<Vec3d> tDesign = new ArrayList<>();
        switch (mode) {
            case Speed -> tDesign.addAll(fastTrap);
            case Damage -> tDesign.addAll(damageTrap);
        }
        return tDesign;
    }

    public static int getLegacyAnvilPos(AnvilAura.LegacyMode mode) {
        switch (mode) {
            case Speed -> { return 3; }
            case Damage -> { return 4; }
        }
        return 0;
    }

    public static final ArrayList<Vec3d> antiStepTrap = new ArrayList<Vec3d>() {{
        add(new Vec3d(1, 1, 0));
        add(new Vec3d(-1, 1, 0));
        add(new Vec3d(0, 1, 1));
        add(new Vec3d(0, 1, -1));
    }};

    public static final ArrayList<Vec3d> fastTrap = new ArrayList<Vec3d>() {{
        add(new Vec3d(1, 1, 0));
        add(new Vec3d(-1, 1, 0));
        add(new Vec3d(0, 1, 1));
        add(new Vec3d(0, 1, -1));

        add(new Vec3d(1, 2, 0));
        add(new Vec3d(-1, 2, 0));
        add(new Vec3d(0, 2, 1));
        add(new Vec3d(0, 2, -1));

        add(new Vec3d(1, 3, 0));
        add(new Vec3d(-1, 3, 0));
        add(new Vec3d(0, 3, 1));
        add(new Vec3d(0, 3, -1));
    }};

    public static final ArrayList<Vec3d> damageTrap = new ArrayList<Vec3d>() {{
        add(new Vec3d(1, 1, 0));
        add(new Vec3d(-1, 1, 0));
        add(new Vec3d(0, 1, 1));
        add(new Vec3d(0, 1, -1));

        add(new Vec3d(1, 2, 0));
        add(new Vec3d(-1, 2, 0));
        add(new Vec3d(0, 2, 1));
        add(new Vec3d(0, 2, -1));

        add(new Vec3d(1, 3, 0));
        add(new Vec3d(-1, 3, 0));
        add(new Vec3d(0, 3, 1));
        add(new Vec3d(0, 3, -1));

        add(new Vec3d(1, 4, 0));
        add(new Vec3d(-1, 4, 0));
        add(new Vec3d(0, 4, 1));
        add(new Vec3d(0, 4, -1));
    }};
}
