package foundation.kurai.util;

import com.google.gson.JsonObject;
import com.mojang.realmsclient.RealmsMainScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ErrorScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.io.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public class CheckpointUtil {
    public static void saveCurrentWorld() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.getSingleplayerServer() != null) {
            MinecraftServer server = minecraft.getSingleplayerServer();

            for (ServerLevel level : server.getAllLevels()) {
                try {
                    // Force-save the current level
                    level.save(null, true, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("No singleplayer server found. Cannot save the world.");
        }
    }

    public static void createCheckpoint(String checkpointName, String fileName, java.util.function.Consumer<String> callback) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.getSingleplayerServer() == null || minecraft.level == null) {
            if (callback != null) callback.accept("No singleplayer world is currently loaded.");
            return;
        }

        File worldPath = MetadataUtil.getWorldPath().toFile();
        File checkpointDir = new File(worldPath, "checkpoints/" + fileName);
        File screenshotFile = new File(checkpointDir, "screenshot.png");


        if (!checkpointDir.exists() && !checkpointDir.mkdirs()) {
            if (callback != null) callback.accept("Failed to create /checkpoints directory.");
            return;
        }

        if (!checkpointDir.exists() && !checkpointDir.mkdirs()) {
            if (callback != null) callback.accept("Failed to create checkpoint directory: " + checkpointDir.getAbsolutePath());
            return;
        }

        minecraft.setScreen(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")));

        File metadataFile = new File(checkpointDir, "checkpoint.json");

        CompletableFuture.runAsync(() -> {
            try {
                saveCurrentWorld();
                // Save screenshot

                // Save JSON metadata
                saveMetadata(metadataFile, checkpointName);

                WorldBackupUtil.backupWorld(new File(checkpointDir, "world/").toPath(), System.out::println);

                if (callback != null) callback.accept("Checkpoint created successfully: " + checkpointDir.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                if (callback != null) callback.accept("Failed to create checkpoint: " + e.getMessage());
            } finally {
                minecraft.execute(() -> {
                    minecraft.setScreen(null);

                    ScreenshotUtil.takeGameplayScreenshot(nativeImage -> {
                        try {
                            nativeImage.writeToFile(screenshotFile);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        nativeImage.close();
                    });
                });
            }
        });
    }

    private static void saveMetadata(File metadataFile, String originalName) throws IOException {
        JsonObject metadata = new JsonObject();
        metadata.addProperty("createdAt", ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        metadata.addProperty("originalCheckpointName", originalName);
        metadata.addProperty("originalWorldName", MetadataUtil.getWorldName());
        metadata.addProperty("modVersion", MetadataUtil.getModVersion());
        metadata.addProperty("biomeName", MetadataUtil.getCurrentBiomeName());

        try (Writer writer = new FileWriter(metadataFile)) {
            writer.write(metadata.toString());
        }
    }

    private static void onDisconnect() {
        Minecraft minecraft = Minecraft.getInstance();

        boolean flag = minecraft.isLocalServer();
        boolean flag1 = minecraft.isConnectedToRealms();
        assert minecraft.level != null;
        minecraft.level.disconnect();
        if (flag) {
            minecraft.clearLevel(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")));
        } else {
            minecraft.clearLevel();
        }

        TitleScreen titlescreen = new TitleScreen();
        if (flag) {
            minecraft.setScreen(titlescreen);
        } else if (flag1) {
            minecraft.setScreen(new RealmsMainScreen(titlescreen));
        } else {
            minecraft.setScreen(new JoinMultiplayerScreen(titlescreen));
        }

    }

    public static void loadCheckpoint(String checkpointFileName) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.getSingleplayerServer() == null) {
            System.out.println("No world currently loaded.");
            return;
        }

        // Get the current world and checkpoint paths
        String worldName = MetadataUtil.getWorldName();
        File worldDirectory = MetadataUtil.getWorldPath().toFile();
        File checkpointDir = new File(worldDirectory, "checkpoints/" + checkpointFileName + "/world");

        if (!checkpointDir.exists() || !checkpointDir.isDirectory()) {
            System.out.println("Checkpoint directory not found: " + checkpointDir.getAbsolutePath());
            return;
        }

        File worldDir = minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toFile();

        // Exit the world safely
        minecraft.execute(() -> {
            minecraft.getReportingContext().draftReportHandled(minecraft, null, CheckpointUtil::onDisconnect, true);

            try {
                // Clear the current world directory
                FileSystemUtil.deleteDirectory(worldDir, false);

                // Copy files from the checkpoint directory
                System.out.println(checkpointDir.toPath() + " - " + worldDir.toPath());
                FileSystemUtil.copyDirectory(checkpointDir.toPath(), worldDir.toPath(), true);

                // Rejoin the world on the main thread
                minecraft.execute(() -> minecraft.createWorldOpenFlows().loadLevel(new GenericDirtMessageScreen(Component.translatable("menu.savingLevel")), worldName));
            } catch (IOException e) {
                e.printStackTrace();
                minecraft.execute(() -> minecraft.setScreen(new ErrorScreen(Component.translatable("menu.savemod.error"), Component.translatable("menu.savemod.error.text"))));
            }
        });
    }
}