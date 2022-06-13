package me.ghosttypes.ghostware.modules.chat;

import me.ghosttypes.ghostware.modules.hud.visual.NotificationsHUD;
import me.ghosttypes.ghostware.modules.misc.Notifications;
import me.ghosttypes.ghostware.utils.Categories;
import me.ghosttypes.ghostware.utils.player.ArmorUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ArmorAlert extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    private final Setting<Notifications.mode> notifyMode = sgGeneral.add(new EnumSetting.Builder<Notifications.mode>().name("notification-mode").description("How notifications are displayed.").defaultValue(Notifications.mode.Chat).build());
    private final Setting<Double> threshold = sgGeneral.add(new DoubleSetting.Builder().name("durability").description("How low an armor piece needs to be to alert you.").defaultValue(2).min(1).sliderMin(1).sliderMax(100).max(100).build());


    public ArmorAlert() {
        super(Categories.Chat, "armor-alert", "Alerts you when your armor pieces are low.");
    }

    private boolean alertedHelm;
    private boolean alertedChest;
    private boolean alertedLegs;
    private boolean alertedBoots;

    @Override
    public void onActivate() {
        alertedHelm = false;
        alertedChest = false;
        alertedLegs = false;
        alertedBoots = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        Iterable<ItemStack> armorPieces = mc.player.getArmorItems();
        for (ItemStack armorPiece : armorPieces) {

            if (ArmorUtil.checkThreshold(armorPiece, threshold.get())) {
                if (ArmorUtil.isHelm(armorPiece) && !alertedHelm) {
                    notif(armorPiece.getItem(), "helmet");
                    alertedHelm = true;
                }
                if (ArmorUtil.isChest(armorPiece) && !alertedChest) {
                    notif(armorPiece.getItem(), "chestplate");
                    alertedChest = true;
                }
                if (ArmorUtil.isLegs(armorPiece) && !alertedLegs) {
                    notif(armorPiece.getItem(), "leggings");
                    alertedLegs = true;
                }
                if (ArmorUtil.isBoots(armorPiece) && !alertedBoots) {
                    notif(armorPiece.getItem(), "boots");
                    alertedBoots = true;
                }
            }
            if (!ArmorUtil.checkThreshold(armorPiece, threshold.get())) {
                if (ArmorUtil.isHelm(armorPiece) && alertedHelm) alertedHelm = false;
                if (ArmorUtil.isChest(armorPiece) && alertedChest) alertedChest = false;
                if (ArmorUtil.isLegs(armorPiece) && alertedLegs) alertedLegs = false;
                if (ArmorUtil.isBoots(armorPiece) && alertedBoots) alertedBoots = false;
            }
        }
    }

    private void notif(Item armor, String name) {
        String end = "";
        //grammar moment
        if (name.endsWith("s")) { end = " are low!";
        } else { end = " is low!";}
        switch (notifyMode.get()) {
            case Chat -> warning("Your " + name + end);
            case Toast -> NotificationsHUD.lowArmor(armor, "Your " + name + end);
            case Hud -> NotificationsHUD.addNotification("Your " + name + end);
        }
    }
}
