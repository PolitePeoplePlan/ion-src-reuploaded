package me.ghosttypes.ghostware.modules.misc;

import me.ghosttypes.ghostware.utils.Categories;
import me.ghosttypes.ghostware.utils.network.AuthToken;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.text.LiteralText;

public class TokenLogin extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();


    private final Setting<String> username = sgGeneral.add(new StringSetting.Builder().name("username").description("The username the access token belongs to").defaultValue("Steve").build());
    private final Setting<String> accessToken = sgGeneral.add(new StringSetting.Builder().name("token").description("The access token").defaultValue("12345").build());

    public TokenLogin() {super(Categories.Misc, "token-login", "Log in with an access token.");}

    @Override
    public void onActivate() {
        if (AuthToken.isTokenValid(accessToken.get())) {
            ChatUtils.info("Token validated! Logging in...");
            if (AuthToken.setSession(username.get(), accessToken.get())) {
                mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(new LiteralText("Logged into " + username.get() + " successfully!")));
            } else {
                ChatUtils.error("An error occurred, please check minecraft logs.");
            }
        } else {
            ChatUtils.error("Invalid token!");
        }
        this.toggle();
    }
}
