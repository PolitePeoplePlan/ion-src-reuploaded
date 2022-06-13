package me.ghosttypes.ghostware.modules.misc;

import me.ghosttypes.ghostware.mixins.FirstPersonRendererAccessor;
import me.ghosttypes.ghostware.utils.Categories;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Quaternion;

public class Viewmodel extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSpin = settings.createGroup("Spin");

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder().name("scale").description("The scale of your hands.").defaultValue(1).sliderMax(5).build());
    private final Setting<Double> mainHeight = sgGeneral.add((new DoubleSetting.Builder()).name("mainhand-height").description("How heigh to have the mainhand appear").defaultValue(1.0D).sliderMin(0.01D).sliderMax(2.0D).build());
    private final Setting<Double> offHeight = sgGeneral.add((new DoubleSetting.Builder()).name("offhand-height").description("How heigh to have the offhand appear").defaultValue(1.0D).sliderMin(0.01D).sliderMax(2.0D).build());

    private final Setting<Integer> speedX = sgSpin.add(new IntSetting.Builder().name("x").description("The speed at which you rotate.").defaultValue(0).sliderMin(-100).sliderMax(100).build());
    private final Setting<Integer> speedY = sgSpin.add(new IntSetting.Builder().name("y").description("The speed at which you rotate.").defaultValue(0).sliderMin(-100).sliderMax(100).build());
    private final Setting<Integer> speedZ = sgSpin.add(new IntSetting.Builder().name("z").description("The speed at which you rotate.").defaultValue(0).sliderMin(-100).sliderMax(100).build());

    private float nextRotationX = 0, nextRotationY = 0, nextRotationZ = 0;

    public Viewmodel() {
        super(Categories.Misc, "viewmodel", "description sleeping rn.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        // hand height
        FirstPersonRendererAccessor accessor = (FirstPersonRendererAccessor) mc.gameRenderer.firstPersonRenderer;
        if (!mc.player.getMainHandStack().isEmpty()) {
            accessor.setItemStackMainHand(mc.player.getMainHandStack());
            accessor.setEquippedProgressMainHand((mainHeight.get()).floatValue());
        }
        if (!mc.player.getOffHandStack().isEmpty()) {
            accessor.setItemStackOffHand(mc.player.getOffHandStack());
            accessor.setEquippedProgressOffHand((offHeight.get()).floatValue());
        }
    }

    public void transform(MatrixStack matrices) {
        if (!isActive()) return;
        float defRotation = 0;

        matrices.scale(scale.get().floatValue(), scale.get().floatValue(), scale.get().floatValue());

        if (!speedX.get().equals(0)) {
            float finalRotationX = (nextRotationX++ / speedX.get());
            matrices.multiply(Quaternion.method_35821(finalRotationX, defRotation, defRotation));
        }
        if (!speedY.get().equals(0)) {
            float finalRotationY = (nextRotationY++ / speedY.get());
            matrices.multiply(Quaternion.method_35821(defRotation, finalRotationY, defRotation));
        }
        if (!speedZ.get().equals(0)) {
            float finalRotationZ = (nextRotationZ++ / speedZ.get());
            matrices.multiply(Quaternion.method_35821(defRotation, defRotation, finalRotationZ));
        }
    }
}
