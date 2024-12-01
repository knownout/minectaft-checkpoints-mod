package foundation.kurai.util;

import foundation.kurai.CheckpointsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.ModList;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class MetadataUtil {
    public static String getModVersion() {
        return ModList.get().getModContainerById(CheckpointsMod.MODID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("Unknown");
    }

    public static String getCurrentBiomeName() {
        Minecraft minecraft = Minecraft.getInstance();

        // Ensure the player and level are valid
        if (minecraft.player != null && minecraft.level != null) {
            // Get the player's current position
            BlockPos playerPos = minecraft.player.blockPosition();

            // Get the biome at the player's position
            Biome biome = minecraft.level.getBiome(playerPos).value();

            // Get the biome's name
            ResourceLocation biomeKey = minecraft.level.registryAccess()
                    .registryOrThrow(Registries.BIOME)
                    .getKey(biome);

            return biomeKey != null ? biomeKey.getPath() : "Unknown Biome";
        }

        return "Unknown Biome";
    }

    public static Path getWorldPath() {
        Minecraft minecraft = Minecraft.getInstance();

        return Objects.requireNonNull(minecraft.getSingleplayerServer()).getWorldPath(LevelResource.ROOT);
    }

    public static String getWorldName() {
        return getWorldPath().toFile().getParentFile().getName();
    }

    public static String getReadableDatetimeString(String isoString) {
        DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss");

        try {
            LocalDateTime dateTime = LocalDateTime.parse(isoString);
            return dateTime.format(DATE_FORMATTER);
        } catch (Exception e) {
            e.printStackTrace();
            return "Invalid Date"; // Fallback in case of error
        }
    }

    public static LocalDateTime parseDate(String dateString) {
        try {
            return LocalDateTime.parse(dateString); // Parses ISO 8601 date strings directly
        } catch (Exception e) {
            e.printStackTrace();
            return LocalDateTime.MIN; // Default to the earliest possible date on error
        }
    }

    public static String generateNextName(String name) {
        // Check if the name ends with a "-N" pattern
        if (name.matches(".+-\\d+$")) {
            int lastDashIndex = name.lastIndexOf("-");
            String baseName = name.substring(0, lastDashIndex); // Extract base name
            String numberPart = name.substring(lastDashIndex + 1); // Extract the number part

            try {
                int number = Integer.parseInt(numberPart); // Parse the number
                return baseName + "-" + (number + 1); // Increment the number
            } catch (NumberFormatException e) {
                // If parsing fails, fallback to appending "-1"
                return name + "-1";
            }
        } else {
            // If no "-N" at the end, append "-1"
            return name + "-1";
        }
    }
}
