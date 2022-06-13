package me.ghosttypes.ghostware.modules.misc;

import me.ghosttypes.ghostware.utils.Categories;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class FunnyCrossbow extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").defaultValue(10).sliderRange(0, 20).build());
    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder().name("auto-switch").defaultValue(true).build());
    private final Setting<Integer> moveSlot = sgGeneral.add(new IntSetting.Builder().name("slot").defaultValue(9).min(1).sliderMin(1).max(9).sliderMax(9).build());

    public FunnyCrossbow() {
        super(Categories.Misc, "funny-crossbow", "description is sleeping rn.");
    }

    private int wait = delay.get();

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (wait > 0) {
            wait--;
        } else {
            for (int i = 0; i < mc.player.getInventory().size(); i++) {
                ItemStack crossbow = mc.player.getInventory().getStack(i);
                if (CrossbowItem.isCharged(crossbow)) {
                    InvUtils.move().from(i).to(moveSlot.get() - 1);
                    break;
                }
            }

            doSwitch();

            if (mc.player.getInventory().getMainHandStack().getItem() instanceof CrossbowItem &&
                CrossbowItem.isCharged(mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot))) {
                mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
            }
            wait = delay.get();
        }
    }

    public void doSwitch() {
        if (autoSwitch.get()) mc.player.getInventory().selectedSlot = moveSlot.get() - 1;
    }
}
