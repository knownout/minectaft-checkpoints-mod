package foundation.kurai.mc.mods.util;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

public class FileSystemUtil {
    public static void copyDirectory(Path source, Path target, boolean restore) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult preVisitDirectory(Path dir, @NotNull BasicFileAttributes attrs) throws IOException {
                // Skip any directory whose path includes "/checkpoints"
                if (dir.toAbsolutePath().toString().contains("checkpoints") && !restore) {
                    return FileVisitResult.SKIP_SUBTREE; // Skip this directory and its contents
                }

                // Resolve the target path and create the directory
                Path targetDir = target.resolve(source.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public @NotNull FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                // Skip files in "/checkpoints" directory
                if (file.toAbsolutePath().toString().contains("checkpoints") && !restore) {
                    return FileVisitResult.CONTINUE; // Skip this file
                }

                // Resolve the target path and copy the file
                Path targetFile = target.resolve(source.relativize(file));
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void deleteDirectory(File directory, boolean force) {
        if (directory.isDirectory()) {
            for (File file : Objects.requireNonNull(directory.listFiles())) {
                // Skip any directory or file inside a "checkpoints" folder
                if (file.toPath().toAbsolutePath().toString().contains("checkpoints") && !force) {
                    continue; // Skip this file or directory
                }
                deleteDirectory(file, force); // Recursively delete other files and directories
            }
        }
        // Skip deleting the "checkpoints" folder itself
        if (!directory.toPath().toAbsolutePath().toString().contains("checkpoints") || force) {
            directory.delete();
        }
    }
}
