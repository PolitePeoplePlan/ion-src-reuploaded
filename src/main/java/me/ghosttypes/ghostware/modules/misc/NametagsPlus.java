package me.ghosttypes.ghostware.modules.misc;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import me.ghosttypes.ghostware.utils.Categories;
import me.ghosttypes.ghostware.utils.misc.Colors;
import me.ghosttypes.ghostware.utils.misc.Potions;
import me.ghosttypes.ghostware.utils.player.PlayerUtil;
import me.ghosttypes.ghostware.utils.world.Loader;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.NameProtect;
import meteordevelopment.meteorclient.systems.modules.render.Freecam;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.misc.Vec3;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.*;

public class NametagsPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlayers = settings.createGroup("Players");
    private final SettingGroup sgItems = settings.createGroup("Items");
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    private final Setting<Object2BooleanMap<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder().name("entities").description("Select entities to draw nametags on.").defaultValue(Utils.asO2BMap(EntityType.PLAYER, EntityType.ITEM)).build());
    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder().name("scale").description("The scale of the nametag.").defaultValue(1.5).min(0.1).build());
    private final Setting<Boolean> yourself = sgGeneral.add(new BoolSetting.Builder().name("self").description("Displays a nametag on your player if you're in Freecam.").defaultValue(true).build());
    private final Setting<SettingColor> background = sgGeneral.add(new ColorSetting.Builder().name("background-color").description("The color of the nametag background.").defaultValue(new SettingColor(0, 0, 0, 75)).build());
    private final Setting<SettingColor> names = sgGeneral.add(new ColorSetting.Builder().name("primary-color").description("The color of the nametag names.").defaultValue(new SettingColor()).build());
    private final Setting<Boolean> culling = sgGeneral.add(new BoolSetting.Builder().name("culling").description("Only render a certain number of nametags at a certain distance.").defaultValue(false).build());
    private final Setting<Double> maxCullRange = sgGeneral.add(new DoubleSetting.Builder().name("culling-range").description("Only render nametags within this distance of your player.").defaultValue(20).min(0).sliderMax(200).visible(culling::get).build());
    private final Setting<Integer> maxCullCount = sgGeneral.add(new IntSetting.Builder().name("culling-count").description("Only render this many nametags.").defaultValue(50).min(1).sliderMin(1).sliderMax(100).visible(culling::get).build());

    //Players
    private final Setting<Boolean> displayIonUsers = sgPlayers.add(new BoolSetting.Builder().name("show-ghostware").description("Shows which players are using Ghostware.").defaultValue(true).build());
    private final Setting<Boolean> displayPots = sgPlayers.add(new BoolSetting.Builder().name("show-pots").description("Show if the player has speed/strength.").defaultValue(false).build());
    private final Setting<Boolean> displayPops = sgPlayers.add(new BoolSetting.Builder().name("show-pops").description("Show the players pops.").defaultValue(true).build());
    private final Setting<Boolean> displayItems = sgPlayers.add(new BoolSetting.Builder().name("show-items").description("Displays armor and hand items above the name tags.").defaultValue(true).build());
    private final Setting<Double> itemSpacing = sgPlayers.add(new DoubleSetting.Builder().name("item-spacing").description("The spacing between items.").defaultValue(2).min(0).max(10).sliderMax(5).visible(displayItems::get).build());
    private final Setting<Boolean> ignoreEmpty = sgPlayers.add(new BoolSetting.Builder().name("ignore-empty-slots").description("Doesn't add spacing where an empty item stack would be.").defaultValue(true).visible(displayItems::get).build());
    private final Setting<Boolean> displayItemEnchants = sgPlayers.add(new BoolSetting.Builder().name("display-enchants").description("Displays item enchantments on the items.").defaultValue(true).visible(displayItems::get).build());
    private final Setting<Position> enchantPos = sgPlayers.add(new EnumSetting.Builder<Position>().name("enchantment-position").description("Where the enchantments are rendered.").defaultValue(Position.Above).visible(displayItemEnchants::get).build());
    private final Setting<Integer> enchantLength = sgPlayers.add(new IntSetting.Builder().name("enchant-name-length").description("The length enchantment names are trimmed to.").defaultValue(3).min(1).max(5).sliderMin(0).sliderMax(5).visible(displayItemEnchants::get).build());
    private final Setting<List<Enchantment>> ignoredEnchantments = sgPlayers.add(new EnchantmentListSetting.Builder().name("ignored-enchantments").description("The enchantments that aren't shown on nametags.").defaultValue(new ArrayList<>()).visible(displayItemEnchants::get).build());
    private final Setting<Double> enchantTextScale = sgPlayers.add(new DoubleSetting.Builder().name("enchant-text-scale").description("The scale of the enchantment text.").defaultValue(1).min(0.1).max(2).sliderMin(0.1).sliderMax(2).visible(displayItemEnchants::get).build());
    private final Setting<Boolean> displayGameMode = sgPlayers.add(new BoolSetting.Builder().name("gamemode").description("Shows the player's game mode.").defaultValue(true).build());
    private final Setting<Boolean> displayPing = sgPlayers.add(new BoolSetting.Builder().name("ping").description("Shows the player's ping.").defaultValue(true).build());
    private final Setting<Boolean> displayDistance = sgPlayers.add(new BoolSetting.Builder().name("distance").description("Shows the distance between you and the player.").defaultValue(true).build());

    // Colors
    private final Setting<Colors.ColorType> strColor = sgColors.add(new EnumSetting.Builder<Colors.ColorType>().name("strength-color").defaultValue(Colors.ColorType.DarkRed).build());
    private final Setting<Colors.ColorType> speedColor = sgColors.add(new EnumSetting.Builder<Colors.ColorType>().name("speed-color").defaultValue(Colors.ColorType.LightBlue).build());
    private final Setting<Colors.ColorType> popsColor = sgColors.add(new EnumSetting.Builder<Colors.ColorType>().name("pops-color").defaultValue(Colors.ColorType.Orange).build());
    private final Setting<Colors.ColorType> gmColor = sgColors.add(new EnumSetting.Builder<Colors.ColorType>().name("game-mode-color").defaultValue(Colors.ColorType.LightOrange).build());
    private final Setting<Colors.ColorType> pingColor = sgColors.add(new EnumSetting.Builder<Colors.ColorType>().name("ping-color").defaultValue(Colors.ColorType.LightBlue).build());
    private final Setting<Colors.ColorType> distColor = sgColors.add(new EnumSetting.Builder<Colors.ColorType>().name("distance-color").defaultValue(Colors.ColorType.SlateGray).build());



    //Items
    private final Setting<Boolean> itemCount = sgItems.add(new BoolSetting.Builder().name("show-count").description("Displays the number of items in the stack.").defaultValue(true).build());

    private final Vec3 pos = new Vec3();
    private final double[] itemWidths = new double[6];
    private final String[] devNames = {"EurekaEffect", "osshe5", "Npho"};

    private final Map<Enchantment, Integer> enchantmentsToShowScale = new HashMap<>();
    private final List<Entity> entityList = new ArrayList<>();

    public NametagsPlus() {
        super(Categories.Misc, "nametags-plus", "Cooler nametags.");
    }

    @Override
    public void onActivate() {
        Loader.moduleAuth();
    }

    private static String ticksToTime(int ticks) {
        if (ticks > 20 * 3600) {
            int h = ticks / 20 / 3600;
            return h + " h";
        } else if (ticks > 20 * 60) {
            int m = ticks / 20 / 60;
            return m + " m";
        } else {
            int s = ticks / 20;
            int ms = (ticks % 20) / 2;
            return s + "." + ms + " s";
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        entityList.clear();

        boolean freecamNotActive = !Modules.get().isActive(Freecam.class);
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();

        for (Entity entity : mc.world.getEntities()) {
            EntityType<?> type = entity.getType();
            if (!entities.get().containsKey(type)) continue;

            if (type == EntityType.PLAYER) {
                if ((!yourself.get() || freecamNotActive) && entity == mc.player) continue;
            }

            if (!culling.get() || entity.getPos().distanceTo(cameraPos) < maxCullRange.get()) {
                entityList.add(entity);
            }
        }

        entityList.sort(Comparator.comparing(e -> e.squaredDistanceTo(cameraPos)));
    }

    @EventHandler
    private void onAdded(EntityAddedEvent event) {
        LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
        boolean once = false;

        if (event.entity instanceof PlayerEntity) {
            for (String n : devNames) {
                if (event.entity.getName().equals(n)) {
                    BlockPos devPos = event.entity.getBlockPos();

                    lightning.updatePosition(devPos.getX(), devPos.getY(), devPos.getZ());
                    lightning.refreshPositionAfterTeleport(devPos.getX(), devPos.getY(), devPos.getZ());

                    mc.world.addEntity(lightning.getId(), lightning);

                    if(!once) {
                        mc.world.playSound(mc.player, devPos.getX(), devPos.getY(), devPos.getZ(), SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 10000.0F, 0.8F * 0.2F);
                        mc.world.playSound(mc.player, devPos.getX(), devPos.getY(), devPos.getZ(), SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.WEATHER, 2.0F, 0.5F * 0.2F);
                        once = true;
                    }
                }
            }
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        int count = getRenderCount();

        for (int i = count - 1; i > -1; i--) {
            Entity entity = entityList.get(i);

            pos.set(entity, event.tickDelta);
            pos.add(0, getHeight(entity), 0);

            EntityType<?> type = entity.getType();

            if (NametagUtils.to2D(pos, scale.get())) {
                if (type == EntityType.PLAYER) renderNametagPlayer((PlayerEntity) entity);
                else if (type == EntityType.ITEM) renderNametagItem(((ItemEntity) entity).getStack());
                else if (type == EntityType.ITEM_FRAME)
                    renderNametagItem(((ItemFrameEntity) entity).getHeldItemStack());
                else if (type == EntityType.TNT) renderTntNametag((TntEntity) entity);
                else if (entity instanceof LivingEntity) renderGenericNametag((LivingEntity) entity);
            }
        }
    }

    private int getRenderCount() {
        int count = culling.get() ? maxCullCount.get() : entityList.size();
        count = MathHelper.clamp(count, 0, entityList.size());

        return count;
    }

    @Override
    public String getInfoString() {
        return Integer.toString(getRenderCount());
    }

    private double getHeight(Entity entity) {
        double height = entity.getEyeHeight(entity.getPose());

        if (entity.getType() == EntityType.ITEM || entity.getType() == EntityType.ITEM_FRAME) height += 0.2;
        else height += 0.5;

        return height;
    }

    private void renderNametagPlayer(PlayerEntity player) {
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        // Speed / Strength

        boolean showSpeed = false;
        boolean showStr = false;
        String speedText = " [Sp]";
        String strText = " [St]";

        if (player.hasStatusEffect(Potions.SPEED) && displayPots.get()) {
            showSpeed = true;
            if (Potions.getAmplifier(player, Potions.SPEED) > 1) speedText = "[Sp II]";
        }
        if (player.hasStatusEffect(Potions.STRENGTH) && displayPots.get()) {
            showStr = true;
            if (Potions.getAmplifier(player, Potions.STRENGTH) > 1) strText = "[St II]";
        }

        // Dev Tag
        boolean showDev = false;
        String gwText = "[Dev]";
        for (String n : devNames) {
            if (player.getEntityName().equals(n)) showDev = true;
        }
        // Pops
        String popText = "[" + PlayerUtil.getPops(player) + "]";

        // Gamemode
        GameMode gm = EntityUtils.getGameMode(player);
        String gmText = "BOT";
        if (gm != null) {
            gmText = switch (gm) {
                case SPECTATOR -> "Sp";
                case SURVIVAL -> "S";
                case CREATIVE -> "C";
                case ADVENTURE -> "A";
            };
        }

        gmText = "[" + gmText + "] ";

        // Name
        String name;
        Color nameColor = PlayerUtils.getPlayerColor(player, names.get());

        if (player == mc.player) name = Modules.get().get(NameProtect.class).getName(player.getEntityName());
        else name = player.getEntityName();

        name = name + " ";

        // Health
        float absorption = player.getAbsorptionAmount();
        int health = Math.round(player.getHealth() + absorption);
        double healthPercentage = health / (player.getMaxHealth() + absorption);

        String healthText = String.valueOf(health);
        Color healthColor;

        if (healthPercentage <= 0.333) healthColor = Colors.RED;
        else if (healthPercentage <= 0.666) healthColor = Colors.LIGHT_ORANGE;
        else healthColor = Colors.LIGHT_GREEN;

        // Ping
        int ping = EntityUtils.getPing(player);
        String pingText = " [" + ping + "ms]";

        // Distance
        double dist = Math.round(PlayerUtils.distanceToCamera(player) * 10.0) / 10.0;
        String distText = " " + dist + "m";

        // Calc widths
        double devWidth = text.getWidth(gwText, true);
        double gmWidth = text.getWidth(gmText, true);
        double nameWidth = text.getWidth(name, true);
        double healthWidth = text.getWidth(healthText, true);
        double pingWidth = text.getWidth(pingText, true);
        double distWidth = text.getWidth(distText, true);
        double popWidth = text.getWidth(popText, true);
        double speedWidth = text.getWidth(speedText, true);
        double strWidth = text.getWidth(strText, true);
        double width = nameWidth + healthWidth;

        if (showDev) width += devWidth;
        if (showSpeed) width += speedWidth;
        if (showStr) width += strWidth;
        if (displayGameMode.get()) width += gmWidth;
        if (displayPing.get()) width += pingWidth;
        if (displayDistance.get()) width += distWidth;
        if (displayPops.get()) width += popWidth;

        double widthHalf = width / 2;
        double heightDown = text.getHeight(true);

        drawBg(-widthHalf, -heightDown, width, heightDown);

        // Render texts
        text.beginBig();
        double hX = -widthHalf;
        double hY = -heightDown;

        // Ghostware / Dev Tag
        if (showDev) hX = text.render(gwText, hX, hY, Colors.RED, true);

        // Game mode
        if (displayGameMode.get()) hX = text.render(gmText, hX, hY, Colors.getColor(gmColor.get()), true);
        hX = text.render(name, hX, hY, nameColor, true);

        // Ping / Distance
        hX = text.render(healthText, hX, hY, healthColor, true);
        if (displayPing.get()) hX = text.render(pingText, hX, hY, Colors.getColor(pingColor.get()), true);
        if (displayDistance.get()) hX = text.render(distText, hX, hY, Colors.getColor(distColor.get()), true);

        // Pops
        if (displayPops.get()) hX = text.render(popText, hX, hY, Colors.getColor(popsColor.get()), true);

        // Strength / Speed
        if (showSpeed) hX = text.render(speedText, hX, hY, Colors.getColor(speedColor.get()), true);
        if (showStr) hX = text.render(strText, hX, hY, Colors.getColor(strColor.get()), true);

        text.end();

        if (displayItems.get()) {
            // Item calc
            Arrays.fill(itemWidths, 0);
            boolean hasItems = false;
            int maxEnchantCount = 0;

            for (int i = 0; i < 6; i++) {
                ItemStack itemStack = getItem(player, i);

                // Setting up widths
                if (itemWidths[i] == 0 && (!ignoreEmpty.get() || !itemStack.isEmpty()))
                    itemWidths[i] = 32 + itemSpacing.get();

                if (!itemStack.isEmpty()) hasItems = true;

                if (displayItemEnchants.get()) {
                    Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(itemStack);
                    enchantmentsToShowScale.clear();

                    for (Enchantment enchantment : enchantments.keySet()) {
                        if (!ignoredEnchantments.get().contains(enchantment)) enchantmentsToShowScale.put(enchantment, enchantments.get(enchantment));
                    }

                    for (Enchantment enchantment : enchantmentsToShowScale.keySet()) {
                        String enchantName = Utils.getEnchantSimpleName(enchantment, enchantLength.get()) + " " + enchantmentsToShowScale.get(enchantment);
                        itemWidths[i] = Math.max(itemWidths[i], (text.getWidth(enchantName, true) / 2));
                    }

                    maxEnchantCount = Math.max(maxEnchantCount, enchantmentsToShowScale.size());
                }
            }

            double itemsHeight = (hasItems ? 32 : 0);
            double itemWidthTotal = 0;
            for (double w : itemWidths) itemWidthTotal += w;
            double itemWidthHalf = itemWidthTotal / 2;

            double y = -heightDown - 7 - itemsHeight;
            double x = -itemWidthHalf;

            // Rendering items and enchants
            for (int i = 0; i < 6; i++) {
                ItemStack stack = getItem(player, i);

                RenderUtils.drawItem(stack, (int) x, (int) y, 2, true);

                if (maxEnchantCount > 0 && displayItemEnchants.get()) {
                    text.begin(0.5 * enchantTextScale.get(), false, true);

                    Map<Enchantment, Integer> enchantments = EnchantmentHelper.get(stack);
                    Map<Enchantment, Integer> enchantmentsToShow = new HashMap<>();

                    for (Enchantment enchantment : enchantments.keySet()) {
                        if (!ignoredEnchantments.get().contains(enchantment)) enchantmentsToShow.put(enchantment, enchantments.get(enchantment));
                    }

                    double aW = itemWidths[i];
                    double enchantY = 0;

                    double addY = switch (enchantPos.get()) {
                        case Above -> -((enchantmentsToShow.size() + 1) * text.getHeight(true));
                        case OnTop -> (itemsHeight - enchantmentsToShow.size() * text.getHeight(true)) / 2;
                    };

                    double enchantX;

                    for (Enchantment enchantment : enchantmentsToShow.keySet()) {
                        String enchantName = Utils.getEnchantSimpleName(enchantment, enchantLength.get()) + " " + enchantmentsToShow.get(enchantment);

                        Color enchantColor = Colors.WHITE;
                        if (enchantment.isCursed()) enchantColor = Colors.RED;

                        enchantX = switch (enchantPos.get()) {
                            case Above -> x + (aW / 2) - (text.getWidth(enchantName, true) / 2);
                            case OnTop -> x + (aW - text.getWidth(enchantName, true)) / 2;
                        };

                        text.render(enchantName, enchantX, y + addY + enchantY, enchantColor, true);

                        enchantY += text.getHeight(true);
                    }

                    text.end();
                }

                x += itemWidths[i];
            }
        } else if (displayItemEnchants.get()) displayItemEnchants.set(false);

        NametagUtils.end();
    }

    private void renderNametagItem(ItemStack stack) {
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        String name = stack.getName().getString();
        String count = " x" + stack.getCount();

        double nameWidth = text.getWidth(name, true);
        double countWidth = text.getWidth(count, true);
        double heightDown = text.getHeight(true);

        double width = nameWidth;
        if (itemCount.get()) width += countWidth;
        double widthHalf = width / 2;

        drawBg(-widthHalf, -heightDown, width, heightDown);

        text.beginBig();
        double hX = -widthHalf;
        double hY = -heightDown;

        hX = text.render(name, hX, hY, names.get(), true);
        if (itemCount.get()) text.render(count, hX, hY, Colors.LIGHT_ORANGE, true);
        text.end();

        NametagUtils.end();
    }

    private void renderGenericNametag(LivingEntity entity) {
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        //Name
        String nameText = entity.getType().getName().getString();
        nameText += " ";

        //Health
        float absorption = entity.getAbsorptionAmount();
        int health = Math.round(entity.getHealth() + absorption);
        double healthPercentage = health / (entity.getMaxHealth() + absorption);

        String healthText = String.valueOf(health);
        Color healthColor;

        if (healthPercentage <= 0.333) healthColor = Colors.RED;
        else if (healthPercentage <= 0.666) healthColor = Colors.LIGHT_ORANGE;
        else healthColor = Colors.GREEN;

        double nameWidth = text.getWidth(nameText, true);
        double healthWidth = text.getWidth(healthText, true);
        double heightDown = text.getHeight(true);

        double width = nameWidth + healthWidth;
        double widthHalf = width / 2;

        drawBg(-widthHalf, -heightDown, width, heightDown);

        text.beginBig();
        double hX = -widthHalf;
        double hY = -heightDown;

        hX = text.render(nameText, hX, hY, names.get(), true);
        text.render(healthText, hX, hY, healthColor, true);
        text.end();

        NametagUtils.end();
    }

    private void renderTntNametag(TntEntity entity) {
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);

        String fuseText = ticksToTime(entity.getFuse());

        double width = text.getWidth(fuseText, true);
        double heightDown = text.getHeight(true);

        double widthHalf = width / 2;

        drawBg(-widthHalf, -heightDown, width, heightDown);

        text.beginBig();
        double hX = -widthHalf;
        double hY = -heightDown;

        text.render(fuseText, hX, hY, names.get(), true);
        text.end();

        NametagUtils.end();
    }

    private ItemStack getItem(PlayerEntity entity, int index) {
        return switch (index) {
            case 0 -> entity.getMainHandStack();
            case 1 -> entity.getInventory().armor.get(3);
            case 2 -> entity.getInventory().armor.get(2);
            case 3 -> entity.getInventory().armor.get(1);
            case 4 -> entity.getInventory().armor.get(0);
            case 5 -> entity.getOffHandStack();
            default -> ItemStack.EMPTY;
        };
    }

    private void drawBg(double x, double y, double width, double height) {
        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(x - 1, y - 1, width + 2, height + 2, background.get());
        Renderer2D.COLOR.render(null);
    }

    public enum Position {
        Above,
        OnTop
    }
}
