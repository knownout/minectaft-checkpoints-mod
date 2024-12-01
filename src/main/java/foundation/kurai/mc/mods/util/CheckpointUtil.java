package foundation.kurai.mc.mods.util;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ErrorScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public class CheckpointUtil {
    public static final Screen SAVING_LEVEL_SCREEN = new GenericMessageScreen(Component.translatable("menu.savingLevel"));

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
        lockWorld(worldPath);

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

        minecraft.setScreen(SAVING_LEVEL_SCREEN);

        File metadataFile = new File(checkpointDir, "checkpoint.json");

        CompletableFuture.runAsync(() -> {
            try {
                saveCurrentWorld();
                // Save screenshot

                // Save JSON metadata
                saveMetadata(metadataFile, checkpointName);

                WorldBackupUtil.backupWorld(new File(checkpointDir, "world").toPath(), System.out::println);
                unlockWorld();

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

        if (!minecraft.isLocalServer()) return;

        assert minecraft.level != null;
        minecraft.level.disconnect();
        minecraft.disconnect(SAVING_LEVEL_SCREEN);

        TitleScreen titlescreen = new TitleScreen();
        minecraft.setScreen(titlescreen);
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
        lockWorld(worldDirectory);

        File checkpointDir = new File(worldDirectory, "checkpoints/" + checkpointFileName + "/world");

        if (!checkpointDir.exists() || !checkpointDir.isDirectory()) {
            System.out.println("Checkpoint directory not found: " + checkpointDir.getAbsolutePath());
            return;
        }

        File worldDir = minecraft.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toFile();

        // Exit the world safely
        minecraft.execute(() -> {
            minecraft.getReportingContext().draftReportHandled(minecraft, SAVING_LEVEL_SCREEN, CheckpointUtil::onDisconnect, true);

            try {
                // Clear the current world directory
                FileSystemUtil.deleteDirectory(worldDir, false);

                // Copy files from the checkpoint directory
                FileSystemUtil.copyDirectory(checkpointDir.toPath(), worldDir.toPath(), true);

                unlockWorld();

                // Rejoin the world on the main thread
                minecraft.execute(() -> minecraft.createWorldOpenFlows().openWorld(worldName, () -> minecraft.setScreen(SAVING_LEVEL_SCREEN)));
            } catch (IOException e) {
                e.printStackTrace();
                minecraft.execute(() -> minecraft.setScreen(new ErrorScreen(Component.translatable("menu.savemod.error"), Component.translatable("menu.savemod.error.text"))));
            }
        });
    }

    private static FileLock lock;
    private static FileChannel channel;

    public static void lockWorld(File worldDirectory) {
        try {
            // Create a lock file in the world directory
            File lockFile = new File(worldDirectory, "world.lock");
            if (!lockFile.exists()) {
                lockFile.createNewFile();
            }

            // Acquire a lock on the file
            channel = new FileOutputStream(lockFile).getChannel();
            lock = channel.tryLock();

            if (lock != null) {
                System.out.println("World successfully locked.");
            } else {
                System.out.println("Failed to lock the world.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void unlockWorld() {
        try {
            if (lock != null) {
                lock.release();
                channel.close();
                System.out.println("World successfully unlocked.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}