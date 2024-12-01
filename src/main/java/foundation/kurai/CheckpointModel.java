package foundation.kurai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import foundation.kurai.util.MetadataUtil;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class CheckpointModel {
    public final String name;
    public final String createdAt;
    public final String fileName;
    public final String modVersion;
    public final String biomeName;

    public final Path screenshotPath;
    public final Path metadataPath;

    public CheckpointModel(String name, String fileName, String createdAt, String modVersion, String biomeName) {
        this.name = name;
        this.createdAt = createdAt;
        this.fileName = fileName;
        this.modVersion = modVersion;
        this.biomeName = biomeName;

        this.screenshotPath = new File(MetadataUtil.getWorldPath().toFile(), "/checkpoints/" + fileName + "/screenshot.png").toPath();
        this.metadataPath = new File(MetadataUtil.getWorldPath().toFile(), "/checkpoints/" + fileName + "/checkpoint.json").toPath();
    }

    public static List<CheckpointModel> getCheckpoints() {
        List<CheckpointModel> checkpoints = new ArrayList<>();

        File worldDirectory = MetadataUtil.getWorldPath().toFile();
        File checkpointsDir = new File(worldDirectory, "checkpoints");

        if (checkpointsDir.exists() && checkpointsDir.isDirectory()) {
            for (File checkpointDir : Objects.requireNonNull(checkpointsDir.listFiles())) {
                if (checkpointDir.isDirectory()) {
                    File metadataFile = new File(checkpointDir, "checkpoint.json");
                    if (metadataFile.exists()) {
                        try (FileReader reader = new FileReader(metadataFile)) {
                            JsonObject metadata = JsonParser.parseReader(reader).getAsJsonObject();
                            String name = metadata.get("originalCheckpointName").getAsString();
                            String createdAt = metadata.get("createdAt").getAsString();
                            String modVersion = metadata.get("modVersion").getAsString();
                            String biomeName = metadata.get("biomeName").getAsString();

                            checkpoints.add(new CheckpointModel(name, checkpointDir.getName(), createdAt, modVersion, biomeName));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        checkpoints.sort(Comparator.comparing((CheckpointModel entry) -> MetadataUtil.parseDate(entry.createdAt)).reversed());

        return checkpoints;
    }
}
