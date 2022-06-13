package me.ghosttypes.ghostware.modules.combat;

import me.ghosttypes.ghostware.modules.hud.visual.NotificationsHUD;
import me.ghosttypes.ghostware.modules.misc.Notifications;
import me.ghosttypes.ghostware.utils.Categories;
import me.ghosttypes.ghostware.utils.combat.MineUtil;
import me.ghosttypes.ghostware.utils.player.AutomationUtils;
import me.ghosttypes.ghostware.utils.world.BlockHelper;
import me.ghosttypes.ghostware.utils.world.Loader;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

public class SurroundBuster extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> toggleAfter = sgGeneral.add(new BoolSetting.Builder().name("toggle-off").description("Toggle off after placement.").defaultValue(true).build());
    private final Setting<Integer> toggleDelay = sgGeneral.add(new IntSetting.Builder().name("toggle-delay").description("How long to wait before toggling.").defaultValue(10).min(1).sliderMax(50).build());
    private final Setting<Boolean> breakAfter = sgGeneral.add(new BoolSetting.Builder().name("break-after").description("Break the string after placement.").defaultValue(true).build());
    private final Setting<Integer> breakAfterDelay = sgGeneral.add(new IntSetting.Builder().name("break-delay").description("The delay between breaking the string.").defaultValue(4).min(1).sliderMax(20).build());
    private final Setting<MineMode> mineMode = sgGeneral.add(new EnumSetting.Builder<MineMode>().name("break-mode").description("How to break the string.").defaultValue(MineMode.Client).build());
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder().name("range").description("The max targeting range.").defaultValue(5).min(1).sliderMax(5).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("delay").description("The delay between placing string.").defaultValue(1).min(0).sliderMax(10).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Rotate on block interactions.").defaultValue(true).build());
    private final Setting<Integer> rotatePrio = sgGeneral.add(new IntSetting.Builder().name("rotate-priority").description("Rotation priority.").defaultValue(50).min(1).max(100).build());
    private final Setting<Notifications.mode> notifyMode = sgGeneral.add(new EnumSetting.Builder<Notifications.mode>().name("notification-mode").description("How notifications are displayed.").defaultValue(Notifications.mode.Chat).build());

    PlayerEntity target;
    BlockPos stringPos;
    int timer, breakTimer, toggleTimer;
    boolean sentPacketMine, sentRegularMine;

    public SurroundBuster() {
        super(Categories.Combat, "surround-buster", "Try to prevent a target from re-surrounding.");
    }

    @Override
    public void onActivate() {
        Loader.moduleAuth();
        timer = 0;
        breakTimer = breakAfterDelay.get();
        toggleTimer = toggleDelay.get();
        sentPacketMine = false;
        stringPos = null;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (MineUtil.override) return;
        FindItemResult string = InvUtils.findInHotbar(Items.STRING);
        if (!string.found()) {
            notify("No string in hotbar!");
            toggle();
            return;
        }
        target = TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestDistance);
        if (target == null) {
            notify("No target in range.");
            toggle();
            return;
        }
        if (timer <= 0) {
            timer = delay.get();
        } else {
            timer--;
            return;
        }
        BlockPos placePos = AutomationUtils.getOpenPos(target);
        if (placePos == null) {
            BlockPos sP = AutomationUtils.getStringPos(target);
            if (sP != null && breakAfter.get()) {
                stringPos = sP;
                switch (mineMode.get()) {
                    case Client -> doRegularMine(sP);
                    case Server -> doPacketMine(sP);
                }
            }
            return;
        }
        if (shouldToggle() && toggleAfter.get()) {
            if (toggleTimer <= 0) {
                notify("Finished.");
                toggle();
            } else { toggleTimer--; }
            return;
        }
        if (sentPacketMine) sentPacketMine = false;
        if (sentRegularMine) sentRegularMine = false;
        BlockUtils.place(placePos, string, rotate.get(), rotatePrio.get(), true, true, true);
    }

    private boolean shouldToggle() {
        boolean sentMine = sentRegularMine || sentPacketMine;
        return BlockHelper.getBlock(stringPos) != Block.getBlockFromItem(Items.STRING) && sentMine;
    }

    private void doRegularMine(BlockPos stringPos) {
        if (!sentRegularMine) sentRegularMine = true;
        AutomationUtils.doRegularMine(stringPos, rotate.get(), rotatePrio.get());
    }

    private void doPacketMine(BlockPos stringPos) {
        if (sentPacketMine) return;
        sentPacketMine = true;
        AutomationUtils.doPacketMine(stringPos, rotate.get(), rotatePrio.get());
    }

    private void notify(String msg) {
        String title = "[" + this.name + "] ";
        switch (notifyMode.get()) {
            case Chat -> info(msg);
            case Hud -> NotificationsHUD.addNotification(title + msg);
        }
    }

    @Override
    public String getInfoString() {
        if (target != null) return target.getEntityName();
        return null;
    }

    public enum MineMode {
        Client,
        Server
    }



}
