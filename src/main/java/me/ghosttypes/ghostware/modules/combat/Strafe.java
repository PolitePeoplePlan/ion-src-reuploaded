package me.ghosttypes.ghostware.modules.combat;

import me.ghosttypes.ghostware.utils.Categories;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class Strafe extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Double> speedNormal = sgGeneral.add(new DoubleSetting.Builder().name("speed").description("how many speed you need!!!&&&").defaultValue(0.4).min(0).sliderMax(3).build());
    public final Setting<Double> speedEating = sgGeneral.add(new DoubleSetting.Builder().name("speed-eating").description("eating speed cuz without eating you can move faster").defaultValue(0.4).min(0).sliderMax(3).build());

    public Strafe() {
        super(Categories.Combat, "Strafe", "how many speed you want!!!!");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        assert mc.player != null;
        if (mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0) {

            double sp;
            if (mc.player.isUsingItem()) sp = speedEating.get();
            else sp = speedNormal.get();

            double yaw = mc.player.getYaw();
            float incres = 1;
            if (mc.player.forwardSpeed < 0) {
                yaw += 180;
                incres = -0.5f;
            } else if (mc.player.forwardSpeed > 0) incres = 0.5f;
            if (mc.player.sidewaysSpeed > 0) yaw -= 90 * incres;
            if (mc.player.sidewaysSpeed < 0) yaw += 90 * incres;

            yaw = Math.toRadians(yaw);
            mc.player.setVelocity(-Math.sin(yaw) * sp, mc.player.getVelocity().y, Math.cos(yaw) * sp);
        }
    }
}
