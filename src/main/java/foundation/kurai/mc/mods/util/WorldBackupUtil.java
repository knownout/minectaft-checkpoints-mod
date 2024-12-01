package foundation.kurai.mc.mods.util;

import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class WorldBackupUtil {
    public static void backupWorld(Path savePath, Consumer<String> callback) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || !minecraft.isSingleplayer()) {
            if (callback != null) callback.accept("No world is currently loaded.");
            return;
        }

        File worldDir = MetadataUtil.getWorldPath().toFile();
        File backupDir = savePath.toFile();

        if (!backupDir.exists() && !backupDir.mkdirs()) {
            if (callback != null) callback.accept("Failed to create backup directory: " + backupDir.getAbsolutePath());
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                FileSystemUtil.copyDirectory(worldDir.toPath(), backupDir.toPath(), false);
                if (callback != null) callback.accept("World backup completed successfully: " + backupDir.getAbsolutePath());
            } catch (IOException e) {
                if (callback != null) callback.accept("Failed to backup world: " + e.getMessage());
            }
        });
    }
}