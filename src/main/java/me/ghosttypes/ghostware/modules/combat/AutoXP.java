package me.ghosttypes.ghostware.modules.combat;

import me.ghosttypes.ghostware.modules.hud.visual.NotificationsHUD;
import me.ghosttypes.ghostware.modules.misc.Notifications;
import me.ghosttypes.ghostware.utils.Categories;
import me.ghosttypes.ghostware.utils.Wrapper;
import me.ghosttypes.ghostware.utils.player.ArmorUtil;
import me.ghosttypes.ghostware.utils.player.InvUtil;
import me.ghosttypes.ghostware.utils.world.Loader;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.systems.modules.combat.Offhand;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoXP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPause = settings.createGroup("Pause");

    private final Setting<Double> enableAt = sgGeneral.add(new DoubleSetting.Builder().name("threshold").description("What durability to enable at.").defaultValue(20).min(1).sliderMin(1).sliderMax(100).max(100).build());
    private final Setting<Double> minHealth = sgGeneral.add(new DoubleSetting.Builder().name("min-health").description("Min health for repairing.").defaultValue(10).min(0).sliderMax(36).max(36).build());
    private final Setting<Boolean> passive = sgGeneral.add(new BoolSetting.Builder().name("passive").description("Keep AutoXP on and repair automatically.").defaultValue(false).build());
    private final Setting<Boolean> moduleControl = sgGeneral.add(new BoolSetting.Builder().name("module-control").description("Disable combat modules while repairing armor.").defaultValue(true).build());
    private final Setting<Boolean> onlyInHole = sgGeneral.add(new BoolSetting.Builder().name("only-in-hole").description("Only throw XP while in a hole.").defaultValue(false).build());
    private final Setting<Boolean> silent = sgGeneral.add(new BoolSetting.Builder().name("silent").description("Allows you to use other hotbar slots while throwing XP.").defaultValue(false).build());
    private final Setting<Boolean> refill = sgGeneral.add(new BoolSetting.Builder().name("refill").description("Moves XP from your inventory to your hotbar when you run out.").defaultValue(false).build());
    private final Setting<Boolean> refillOffhand = sgGeneral.add(new BoolSetting.Builder().name("use-offhand").description("Uses your offhand for XP.").defaultValue(false).build());
    private final Setting<Integer> refillSlot = sgGeneral.add(new IntSetting.Builder().name("refill-slot").description("Which slot to refill.").defaultValue(1).min(1).sliderMin(1).max(9).sliderMax(9).visible(refill::get).build());
    private final Setting<Boolean> lookDown = sgGeneral.add(new BoolSetting.Builder().name("look-down").description("Throws the XP at your feet.").defaultValue(true).build());
    private final Setting<Notifications.mode> notifyMode = sgGeneral.add(new EnumSetting.Builder<Notifications.mode>().name("notification-mode").description("How notifications are displayed.").defaultValue(Notifications.mode.Chat).build());

    private final Setting<Boolean> pauseOnEat = sgPause.add(new BoolSetting.Builder().name("pause-on-eat").description("Pauses while eating.").defaultValue(true).build());
    private final Setting<Boolean> pauseOnDrink = sgPause.add(new BoolSetting.Builder().name("pause-on-drink").description("Pauses while drinking.").defaultValue(true).build());
    private final Setting<Boolean> pauseOnMine = sgPause.add(new BoolSetting.Builder().name("pause-on-mine").description("Pauses while mining.").defaultValue(true).build());

    private boolean alerted, toggledOffhand, passiveRepairing;
    private int slotRefill, passiveTimer;

    public AutoXP() {
        super(Categories.Combat, "auto-xp", "Automatically repair your armor.");
    }

    @Override
    public void onActivate() {
        Loader.moduleAuth();
        if (moduleControl.get()) {
            CrystalAura ca = Modules.get().get(CrystalAura.class);
            KillAura ka = Modules.get().get(KillAura.class);
            Offhand offhand = Modules.get().get(Offhand.class);
            if (ca.isActive()) ca.toggle();
            if (ka.isActive()) ka.toggle();
            if (offhand.isActive() && refillOffhand.get()) {
                toggledOffhand = true;
                offhand.toggle();
            }
        }
        passiveRepairing = false;
        passiveTimer = 25;
        alerted = false;
        slotRefill = refillSlot.get() - 1;
    }

    @Override
    public void onDeactivate() {
        Offhand offhand = Modules.get().get(Offhand.class);
        if (moduleControl.get() && toggledOffhand && !offhand.isActive()) offhand.toggle();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        assert mc.player != null;
        if (PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) return;
        // passive mode
        if (passive.get()) {
            // hole check
            if (onlyInHole.get() && !Wrapper.isInHole(mc.player)) {
                if (passiveRepairing) {
                    notify("Auto Repair cancelled.");
                    passiveRepairing = false;
                    // reset le timer
                    passiveTimer = 25;
                }
                return;
            }
            // check if we should repair + timer (timer prevents rotation downwards before fully entering the hole)
            if (shouldRepair() && passiveTimer <= 0) {
                if (!passiveRepairing) {
                    notify("Auto Repairing to " + enableAt.get() + "%%");
                    passiveRepairing = true;
                }
                moduleCheck(); // check if combat modules were re-enabled
                refill(); // refill xp slot
                repair(); // repair
            } else {
                // reset + return
                passiveTimer--;
                passiveRepairing = false;
                return;
            }
        }
        if (Wrapper.getTotalHealth(mc.player) <= minHealth.get()) {
            notify("Your health is too low!");
            toggle();
            return;
        }
        if (onlyInHole.get() && !Wrapper.isInHole(mc.player)) {
            notify("You're not in a hole!");
            toggle();
            return;
        }
        if (!shouldRepair()) {
            if (alerted) {
                notify("Finished repair.");
            } else {
                notify("Your armor is above the threshold.");
            }
            toggle();
            return;
        }
        moduleCheck(); // check if combat modules were re-enabled
        refill(); // refill xp slot
        if (!alerted) {
            notify("Repairing armor to " + enableAt.get() + "%%");
            alerted = true;
        }
        repair(); // repair
    }

    private void moduleCheck() {
        CrystalAura ca = Modules.get().get(CrystalAura.class);
        KillAura ka = Modules.get().get(KillAura.class);
        if (ca.isActive() || ka.isActive()) toggle();
    }

    private void repair() {
        if (lookDown.get()) {
            Rotations.rotate(mc.player.getYaw(), 90, 50, this::throwXP);
        } else {
            throwXP();
        }
    }

    private void refill() {
        if (refillSlotEmpty(false) && !refillOffhand.get()) {
            if (refill.get()) {
                FindItemResult invXP = InvUtil.findXPinAll();
                if (invXP.found()) {
                    InvUtils.move().from(invXP.getSlot()).toHotbar(slotRefill);
                } else {
                    notify("You're out of XP!");
                    toggle();
                    return;
                }
            } else {
                notify("No XP in hotbar!");
                toggle();
                return;
            }
        }
        if (refillSlotEmpty(true) && refillOffhand.get()) {
            FindItemResult invXP = InvUtil.findXPinAll();
            if (invXP.found()) {
                InvUtils.move().from(invXP.getSlot()).toOffhand();
            } else {
                notify("You're out of XP!");
                toggle();
            }
        }
    }


    private void notify(String msg) {
        String title = "[" + this.name + "] ";
        switch (notifyMode.get()) {
            case Chat -> info(msg);
            case Hud -> NotificationsHUD.addNotification(title + msg);
        }
    }

    private void throwXP() {
        int lastSlot = mc.player.getInventory().selectedSlot;
        if (mc.player.getInventory().getStack(lastSlot).getItem() == Items.ENCHANTED_GOLDEN_APPLE && pauseOnEat.get()) return;
        if (refillOffhand.get()) {
            mc.interactionManager.interactItem(mc.player, mc.world, Hand.OFF_HAND);
        } else {
            Wrapper.updateSlot(slotRefill);
            mc.interactionManager.interactItem(mc.player, mc.world, Hand.MAIN_HAND);
            if (silent.get() && lastSlot != -1) Wrapper.updateSlot(lastSlot);
        }
    }

    private boolean shouldRepair() {
        for (int i = 0; i < 4; i++) if (ArmorUtil.checkThreshold(ArmorUtil.getArmor(i), enableAt.get())) return true;
        return false;
    }

    private boolean refillSlotEmpty(boolean offhand) {
        if (offhand) return Wrapper.getItemFromSlot(SlotUtils.OFFHAND) != Items.EXPERIENCE_BOTTLE;
        return Wrapper.getItemFromSlot(slotRefill) != Items.EXPERIENCE_BOTTLE;
    }
}
