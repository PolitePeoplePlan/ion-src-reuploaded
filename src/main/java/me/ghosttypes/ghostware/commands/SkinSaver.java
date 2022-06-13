package me.ghosttypes.ghostware.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.ghosttypes.ghostware.Ghostware;
import meteordevelopment.meteorclient.systems.commands.Command;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.function.Consumer;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class SkinSaver extends Command {

    public SkinSaver() {
        super("dumpskins", "Save the skins for all online players");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            ArrayList<PlayerEntity> players = new ArrayList<>();
            for (Entity entity : mc.world.getEntities()) if (entity instanceof PlayerEntity) players.add((PlayerEntity) entity);
            ChatUtils.info("Downloading " + players.size() + " skins.");
            for (PlayerEntity player : players) MeteorExecutor.execute(() -> downloadSkin().accept(player));
            return SINGLE_SUCCESS;
        });
    }

    public static Consumer<PlayerEntity> downloadSkin() {
        return playerEntity -> {
            String uuid = playerEntity.getUuidAsString();
            String name = playerEntity.getEntityName();
            String skinUrl = "https://crafatar.com/skins/" + uuid;
            File skinFolder = new File(Ghostware.MODFOLDER, "saved-skins");
            if (!skinFolder.exists()) skinFolder.mkdirs();
            File skinFile = new File(skinFolder, name + ".png");
            ChatUtils.info("Downloading " + name + "'s skin.");
            try (BufferedInputStream in = new BufferedInputStream(new URL(skinUrl).openStream());
                FileOutputStream fileOutputStream = new FileOutputStream(skinFile)) {
                byte[] dataBuffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) fileOutputStream.write(dataBuffer, 0, bytesRead);
                ChatUtils.info("Skin saved.");
            } catch (IOException e) {
                ChatUtils.error("Error while downloading " + name + "'s skin");
            }
        };
    }
}
