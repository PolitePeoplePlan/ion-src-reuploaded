package me.ghosttypes.ghostware.modules.hud.visual;

import me.ghosttypes.ghostware.modules.misc.Notifications;
import me.ghosttypes.ghostware.utils.misc.MathUtil;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.render.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.render.hud.modules.HudElement;
import meteordevelopment.meteorclient.utils.render.MeteorToast;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.ArrayList;

public class NotificationsHUD extends HudElement {

    public static ArrayList<Notif> notifs = new ArrayList<>();
    public static ArrayList<Notif> notifications = new ArrayList<>();

    public NotificationsHUD(HUD hud) {
        super(hud, "notifications", "Display notifications", false);
    }


    public static void addNotification(String notification) {
        notifs.add(new Notif(notification));
    }

    public static void updateNotifs() {
        if (notifs.size() > 6) notifs.remove(0);
        notifs.removeIf(notif -> notif.ticksLeft <= 0);
        notifications.clear();
        notifications.addAll(notifs);
    }

    public static void spotify(String artist, String track) {
        MeteorClient.mc.getToastManager().add(new MeteorToast(Items.NOTE_BLOCK, artist, track, 2000));
    }

    public static void lowArmor(Item armorPiece, String text) {
        MeteorClient.mc.getToastManager().add(new MeteorToast(armorPiece, "Armor Alert", text, 2000));
    }

    public static void popAlert(String p) {
        MeteorClient.mc.getToastManager().add(new MeteorToast(Items.TOTEM_OF_UNDYING, "PopCounter", p, 1000));
    }


    @Override
    public void update(HudRenderer renderer) {
        updateNotifs();
        double width = 0;
        double height = 0;
        int i = 0;
        if (notifications.isEmpty()) {
            String t = "Notifications";
            width = Math.max(width, renderer.textWidth(t));
            height += renderer.textHeight();
            box.setSize(width, height);
            return;
        }
        if (!isActive()) return;
        for (Notif notif : notifications) {
            String noti = notif.text;
            width = Math.max(width, renderer.textWidth(noti));
            height += renderer.textHeight();
            if (i > 0) height += 2;
            i++;
        }
        box.setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        updateNotifs();
        double x = box.getX();
        double y = box.getY();
        double w = box.width;
        double h = box.width;
        int i = 0;
        Notifications n = Modules.get().get(Notifications.class);
        if (isInEditor()) {
            if (n.drawBackground.get()) {
                Renderer2D.COLOR.begin();
                Renderer2D.COLOR.quad(x, y, w, h, n.backgroundColor.get());
                Renderer2D.COLOR.render(null);
            }
            renderer.text("Notifications", x, y, hud.secondaryColor.get());
            return;
        }
        if (!isActive() || notifications.isEmpty()) return;

        for (Notif notif : notifications) {
            String noti = notif.text;
            renderer.text(noti, x + box.alignX(renderer.textWidth(noti)), y, hud.secondaryColor.get());
            y += renderer.textHeight();
            if (i > 0) y += 2;
            i++;
        }

        //if (notifications.isEmpty()) {
            //return;
            //if (n.drawBackground.get()) {
            //    Renderer2D.COLOR.begin();
            //    Renderer2D.COLOR.quad(x, y, w, h, n.backgroundColor.get());
            //    Renderer2D.COLOR.render(null);
            //}
            //String t = "Notifications";
            //renderer.text(t, x + box.alignX(renderer.textWidth(t)), y, hud.secondaryColor.get());
        //} else {
        //    for (Notif notif : notifications) {
        //        String noti = notif.text;
        //        renderer.text(noti, x + box.alignX(renderer.textWidth(noti)), y, hud.secondaryColor.get());
        //        y += renderer.textHeight();
        //        if (i > 0) y += 2;
        //        i++;
        //    }
        //}
    }

    public static boolean isActive() {
        return Modules.get().get(Notifications.class).isActive();
    }

    public static int getNotificationTime() {
        return Modules.get().get(Notifications.class).displayTime.get();
    }

    public static class Notif {
        public final String text;
        public int ticksLeft = MathUtil.intToTicks(getNotificationTime());

        public Notif(String notifText) {
            text = notifText;
        }

        public void tick() {
            ticksLeft--;
        }
    }

}
