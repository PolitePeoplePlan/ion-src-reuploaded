package me.ghosttypes.ghostware.modules.hud.visual;

import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.render.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.render.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.render.hud.modules.HudElement;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.RainbowColor;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.util.Identifier;

public class Logo extends HudElement {
    private static final Identifier LOGO = new Identifier("ghostware", "icon.png");
    private static final Identifier LOGO_FLAT = new Identifier("ghostware", "logo_flat.png");
    private static final Identifier LOGO_TRANSPARENT = new Identifier("ghostware", "logo_transparent.png");

    private static final RainbowColor RAINBOW = new RainbowColor();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder().name("scale").description("The scale.").defaultValue(2).min(1).sliderMin(1).sliderMax(5).build());
    public final Setting<Boolean> transparent = sgGeneral.add(new BoolSetting.Builder().name("transparent").description("Remove the background from the logo.").defaultValue(false).build());
    public final Setting<Boolean> chroma = sgGeneral.add(new BoolSetting.Builder().name("chroma").description("Chroma logo animation.").defaultValue(false).build());
    private final Setting<Double> chromaSpeed = sgGeneral.add(new DoubleSetting.Builder().name("chroma-speed").description("Speed of the chroma animation.").defaultValue(0.09).min(0.01).sliderMax(5).decimalPlaces(2).build());
    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder().name("background-color").description("Color of the background.").defaultValue(new SettingColor(255, 255, 255)).build());

    public Logo(HUD hud) {
        super(hud, "gw-logo", "Displays the Ghostware logo.");
    }

    @Override
    public void update(HudRenderer renderer) {
        if (getLogo() == LOGO_TRANSPARENT || getLogo() == LOGO_FLAT) box.setSize(78 * scale.get(), 165 * scale.get());
        box.setSize(78 * scale.get(), 96 * scale.get());
    }

    @Override
    public void render(HudRenderer renderer) {

        if (!Utils.canUpdate()) return;
        double x = box.getX();
        double y = box.getY();
        int w = (int) box.width;
        int h = (int) box.height;
        GL.bindTexture(getLogo());
        Renderer2D.TEXTURE.begin();
        if (chroma.get()) {
            RAINBOW.setSpeed(chromaSpeed.get() / 100);
            Renderer2D.TEXTURE.texQuad(x, y, w, h, RAINBOW.getNext());
        } else {
            Renderer2D.TEXTURE.texQuad(x, y, w, h, color.get());
        }
        Renderer2D.TEXTURE.render(null);
    }

    private Identifier getLogo() {
        if (chroma.get() && transparent.get()) {
            ChatUtils.error("Chroma animation is not supported with transparent style, switching to flat logo");
            transparent.set(false);
        }
        if (transparent.get()) return LOGO_TRANSPARENT;
        if (chroma.get()) return LOGO_FLAT;
        return LOGO;
    }
}
