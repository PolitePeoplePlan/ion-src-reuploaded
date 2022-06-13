package me.ghosttypes.ghostware.utils.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.ghosttypes.ghostware.Ghostware;
import meteordevelopment.meteorclient.mixin.MinecraftClientAccessor;
import net.minecraft.client.util.Session;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class AuthToken {

    public static boolean setSession(String username, String token) {
        String UUID = getUUID(username);
        if (UUID == null) return false;
        try {
            Session session = new Session(username, UUID, token, "mojang");
            ((MinecraftClientAccessor) mc).setSession(session);
            mc.getSessionProperties().clear();
            return true;
        } catch (Exception e) {
            Ghostware.log("[TokenLogin] " + e);}
        return false;
    }


    public static boolean isTokenValid(String accessToken) {
        try {
            JsonObject json = new JsonObject();
            json.addProperty( "accessToken", accessToken );

            HttpURLConnection connection = (HttpURLConnection)new URL("https://authserver.mojang.com/validate").openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            OutputStream os = connection.getOutputStream();
            os.write(json.toString().getBytes( StandardCharsets.UTF_8 ) );
            int code = connection.getResponseCode();
            connection.disconnect();
            return code == 204;
        } catch (Exception e) {
            Ghostware.log("[TokenLogin] " + e);}
        return false;
    }

    public static String getUUID(String username) {
        try {
            StringBuilder sb = new StringBuilder();
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = "";
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            JsonObject json = new JsonParser().parse(sb.toString()).getAsJsonObject();
            return json.get("id").getAsString();
        } catch (Exception e) {
            Ghostware.log("[TokenLogin] " + e);
        }
        return null;
    }
}
