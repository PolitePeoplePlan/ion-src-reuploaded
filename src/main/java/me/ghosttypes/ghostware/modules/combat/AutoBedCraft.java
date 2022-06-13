package me.ghosttypes.ghostware.modules.combat;

import me.ghosttypes.ghostware.modules.hud.visual.NotificationsHUD;
import me.ghosttypes.ghostware.modules.misc.Notifications;
import me.ghosttypes.ghostware.utils.Categories;
import me.ghosttypes.ghostware.utils.Wrapper;
import me.ghosttypes.ghostware.utils.player.InvUtil;
import me.ghosttypes.ghostware.utils.player.PlayerUtil;
import me.ghosttypes.ghostware.utils.world.BlockHelper;
import me.ghosttypes.ghostware.utils.world.Loader;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.client.recipebook.RecipeBookGroup;
import net.minecraft.item.BedItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.Recipe;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class AutoBedCraft extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAuto = settings.createGroup("Auto (Buggy)");

    private final Setting<Boolean> disableAfter = sgGeneral.add(new BoolSetting.Builder().name("disable-after").description("Toggle off after filling your inv with beds.").defaultValue(false).build());
    private final Setting<Boolean> disableNoMats = sgGeneral.add(new BoolSetting.Builder().name("disable-on-no-mats").description("Toggle off if you run out of material.").defaultValue(false).build());
    private final Setting<Boolean> closeAfter = sgGeneral.add(new BoolSetting.Builder().name("close-after").description("Close the crafting GUI after filling.").defaultValue(true).build());
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder().name("craft-delay").description("Delay between crafting beds.").defaultValue(0).min(0).sliderMax(10).build());
    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder().name("table-place-delay").description("Delay between placing crafting tables.").defaultValue(3).min(1).sliderMax(10).build());
    private final Setting<Integer> openDelay = sgGeneral.add(new IntSetting.Builder().name("table-open-delay").description("Delay between opening crafting tables.").defaultValue(3).min(1).sliderMax(10).build());
    private final Setting<Notifications.mode> notifyMode = sgGeneral.add(new EnumSetting.Builder<Notifications.mode>().name("notification-mode").description("How notifications are displayed.").defaultValue(Notifications.mode.Chat).build());

    private final Setting<Boolean> automatic = sgAuto.add(new BoolSetting.Builder().name("automatic").description("Automatically place/search for and open crafting tables when you're out of beds.").defaultValue(false).build());
    private final Setting<Boolean> antiTotemFail = sgAuto.add(new BoolSetting.Builder().name("anti-totem-fail").description("Will not open / close current crafting table if you don't have a totem.").defaultValue(false).build());
    private final Setting<Boolean> antiDesync = sgAuto.add(new BoolSetting.Builder().name("anti-desync").description("Try to prevent inventory desync.").defaultValue(false).build());
    private final Setting<Boolean> debug = sgAuto.add(new BoolSetting.Builder().name("debug").description("Don't enable").defaultValue(false).build());
    private final Setting<Boolean> chatInfo = sgAuto.add(new BoolSetting.Builder().name("chat-info").description("Alerts you in chat when auto refill is starting.").defaultValue(false).build());
    private final Setting<Boolean> autoOnlyHole = sgAuto.add(new BoolSetting.Builder().name("in-hole-only").description("Only auto refill while in a hole.").defaultValue(false).build());
    private final Setting<Boolean> autoOnlyGround = sgAuto.add(new BoolSetting.Builder().name("on-ground-only").description("Only auto refill while on the ground.").defaultValue(false).build());
    private final Setting<Boolean> autoWhileMoving = sgAuto.add(new BoolSetting.Builder().name("while-moving").description("Allow auto refill while in motion").defaultValue(false).build());
    private final Setting<Integer> refillAt = sgAuto.add(new IntSetting.Builder().name("refill-at").description("How many beds are left in your inventory to start filling.").defaultValue(3).min(1).build());
    private final Setting<Integer> emptySlotsNeeded = sgAuto.add(new IntSetting.Builder().name("required-empty-slots").description("How many empty slots are required for activation.").defaultValue(5).min(1).build());
    private final Setting<Integer> radius = sgAuto.add(new IntSetting.Builder().name("radius").description("How far to search for crafting tables near you.").defaultValue(3).min(1).build());
    private final Setting<Double> minHealth = sgAuto.add(new DoubleSetting.Builder().name("min-health").description("Min health require to activate.").defaultValue(10).min(1).max(36).sliderMax(36).build());

    public AutoBedCraft() {
        super(Categories.Combat, "auto-bed-craft", "Automatically craft beds.");
    }


    private boolean didRefill = false;
    private boolean startedRefill = false;
    private boolean alertedNoMats = false;

    private int delayTimer, placeTimer, openTimer;

    @Override
    public void onActivate() {
        Loader.moduleAuth();
        delayTimer = delay.get();
        placeTimer = placeDelay.get();
        openTimer = openDelay.get();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (PlayerUtils.getTotalHealth() <= minHealth.get()) {
            closeCraftingTable();
            return;
        }
        if (PlayerUtil.isHoldingGap()) {
            closeCraftingTable();
            return; // *should* fix causing eating problems?
        }
        if (willTotemFail()) {
            closeCraftingTable();
            return;
        }

        if (automatic.get() && isOutOfMaterial() && !alertedNoMats) {
            notify("Cannot activate auto mode, no material left.");
            alertedNoMats = true;
        }
        if (automatic.get() && needsRefill() && canRefill(true) && !isOutOfMaterial() && !(mc.player.currentScreenHandler instanceof CraftingScreenHandler)) {
            FindItemResult craftTable = InvUtil.findCraftTable();
            if (!craftTable.found()) {
                toggle();
                notify("No crafting tables in hotbar!");
                return;
            }
            if (debug.get()) notify("Searching for nearby crafting tables");
            BlockPos tablePos;
            tablePos = findCraftingTable();
            if (tablePos == null) {
                if (debug.get()) notify("None nearby, placing table and returning.");
                if (placeTimer <= 0) {
                    placeTimer = placeDelay.get();
                } else {
                    placeTimer--;
                    return;
                }
                placeCraftingTable(craftTable);
                return;
            }
            if (debug.get()) notify("Located usable crafting table, opening and refilling");
            if (openTimer <= 0) {
                openCraftingTable(tablePos);
                openTimer = openDelay.get();
            } else {
                openTimer--;
                return;
            }
            if (chatInfo.get() && !startedRefill) {
                notify("Refilling...");
                startedRefill = true;
            }
            didRefill = true;
            return;
        }
        if (didRefill && !needsRefill()) {
            if (chatInfo.get()) notify("Refill complete.");
            didRefill = false;
            startedRefill = false;
            if (debug.get()) notify("Automatic finished.");
        }

        if (mc.player.currentScreenHandler instanceof CraftingScreenHandler currentScreenHandler) {
            if (PlayerUtils.getTotalHealth() <= minHealth.get() || willTotemFail()) {
                closeCraftingTable();
                return;
            }
            if (!canRefill(false)) {
                if (debug.get()) notify("Cancelling current refill because canRefill is false");
                closeCraftingTable();
                if (antiDesync.get()) mc.player.getInventory().updateItems();
                return;
            }
            if (isOutOfMaterial()) {
                if (chatInfo.get()) notify("You are out of material!");
                if (disableNoMats.get()) toggle();
                closeCraftingTable();
                if (antiDesync.get()) mc.player.getInventory().updateItems();
                return;
            }
            if (InvUtil.isInventoryFull()) {
                if (disableAfter.get()) toggle();
                if (closeAfter.get()) {
                    closeCraftingTable();
                    if (antiDesync.get()) mc.player.getInventory().updateItems();
                }
                if (chatInfo.get() && !automatic.get()) notify("Your inventory is full.");
                return;
            }
            List<RecipeResultCollection> recipeResultCollectionList = mc.player.getRecipeBook().getResultsForGroup(RecipeBookGroup.CRAFTING_MISC);
            if (delayTimer <= 0) {
                delayTimer = delay.get();
            } else {
                delayTimer--;
                return;
            }
            for (RecipeResultCollection recipeResultCollection : recipeResultCollectionList) {
                for (Recipe<?> recipe : recipeResultCollection.getRecipes(true)) {
                    if (recipe.getOutput().getItem() instanceof BedItem) {
                        assert mc.interactionManager != null;
                        mc.interactionManager.clickRecipe(currentScreenHandler.syncId, recipe, false);
                        windowClick(currentScreenHandler, 0, SlotActionType.QUICK_MOVE, 1);
                    }
                }
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

    private void placeCraftingTable(FindItemResult craftTable) {
        List<BlockPos> nearbyBlocks = BlockHelper.getSphere(mc.player.getBlockPos(), radius.get(), radius.get());
        for (BlockPos block : nearbyBlocks) {
            if (BlockHelper.getBlock(block) == Blocks.AIR && BlockUtils.canPlace(block, true)) {
                BlockUtils.place(block, craftTable, 0, true);
                break;
            }
        }
    }

    private BlockPos findCraftingTable() {
        List<BlockPos> nearbyBlocks = BlockHelper.getSphere(mc.player.getBlockPos(), radius.get(), radius.get());
        for (BlockPos block : nearbyBlocks) if (BlockHelper.getBlock(block) == Blocks.CRAFTING_TABLE) return block;
        return null;
    }

    private void openCraftingTable(BlockPos tablePos) {
        Vec3d tableVec = new Vec3d(tablePos.getX(), tablePos.getY(), tablePos.getZ());
        BlockHitResult table = new BlockHitResult(tableVec, Direction.UP, tablePos, false);
        mc.interactionManager.interactBlock(mc.player, mc.world, Hand.MAIN_HAND, table);
    }

    private void closeCraftingTable() {
        if (mc.player.currentScreenHandler instanceof CraftingScreenHandler) mc.player.closeHandledScreen();
    }

    private boolean needsRefill() {
        FindItemResult bed = InvUtils.find(itemStack -> itemStack.getItem() instanceof BedItem);
        if (!bed.found()) return true;
        if (bed.getCount() <= refillAt.get()) return true;
        return !InvUtil.isInventoryFull();
    }

    private boolean canRefill(boolean checkSlots) {
        if (!autoWhileMoving.get() && Wrapper.isPlayerMoving(mc.player)) return false;
        if (autoOnlyHole.get() && !Wrapper.isInHole(mc.player)) return false;
        if (autoOnlyGround.get() && !mc.player.isOnGround()) return false;
        if (InvUtil.isInventoryFull()) return false;
        if (checkSlots) if (InvUtil.getEmptySlots() < emptySlotsNeeded.get()) return false;
        return !(PlayerUtils.getTotalHealth() <= minHealth.get());
    }

    private boolean isOutOfMaterial() {
        FindItemResult wool = InvUtils.find(itemStack -> InvUtil.wools.contains(itemStack.getItem()));
        FindItemResult plank = InvUtils.find(itemStack -> InvUtil.planks.contains(itemStack.getItem()));
        FindItemResult craftTable = InvUtil.findCraftTable();
        if (!craftTable.found()) return true;
        if (!wool.found() || !plank.found()) return true;
        return wool.getCount() < 3 || plank.getCount() < 3;
    }

    private boolean willTotemFail() {
        if (!antiTotemFail.get()) return false;
        Item offhand = mc.player.getOffHandStack().getItem();
        if (offhand == null) return true;
        return offhand != Items.TOTEM_OF_UNDYING;
    }

    private void windowClick(ScreenHandler container, int slot, SlotActionType action, int clickData) {
        assert mc.interactionManager != null;
        mc.interactionManager.clickSlot(container.syncId, slot, clickData, action, mc.player);
    }
}
