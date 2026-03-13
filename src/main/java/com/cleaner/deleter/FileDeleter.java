package com.cleaner.deleter;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class FileDeleter {

    private volatile boolean cancelled = false;

    public static record DeleteResult(int successCount, int failCount, List<Path> failedPaths) {}

    public static record DeleteProgress(int current, int total, Path currentPath) {}

    /**
     * Moves files to the system trash/recycle bin.
     */
    public CompletableFuture<DeleteResult> moveToTrashAsync(List<Path> paths, Consumer<DeleteProgress> progressCallback) {
        cancelled = false;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Path> failedPaths = new java.util.concurrent.CopyOnWriteArrayList<>();

        return CompletableFuture.supplyAsync(() -> {
            int total = paths.size();

            for (int i = 0; i < paths.size(); i++) {
                if (cancelled) break;

                Path path = paths.get(i);

                if (progressCallback != null) {
                    progressCallback.accept(new DeleteProgress(i + 1, total, path));
                }

                if (moveToTrash(path)) {
                    successCount.incrementAndGet();
                } else {
                    failCount.incrementAndGet();
                    failedPaths.add(path);
                }
            }

            return new DeleteResult(successCount.get(), failCount.get(), failedPaths);
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * Moves a single file or directory to trash.
     * Uses Desktop.moveToTrash if available, otherwise uses platform-specific approach.
     */
    private boolean moveToTrash(Path path) {
        try {
            File file = path.toFile();

            // Try Java 9+ Desktop.moveToTrash
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.MOVE_TO_TRASH)) {
                    return desktop.moveToTrash(file);
                }
            }

            // Fallback: move to system trash folder
            return moveToSystemTrash(path);

        } catch (Exception e) {
            System.err.println("Failed to move to trash: " + path + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Fallback method to move files to system-specific trash location.
     */
    private boolean moveToSystemTrash(Path path) {
        String os = System.getProperty("os.name").toLowerCase();

        try {
            if (os.contains("mac")) {
                // macOS: Use AppleScript or trash location
                Path trashPath = Path.of(System.getProperty("user.home"), ".Trash");
                if (Files.exists(trashPath)) {
                    Path targetPath = trashPath.resolve(path.getFileName());
                    targetPath = resolveUniquePath(trashPath, path.getFileName().toString());
                    Files.move(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    return true;
                }
            } else if (os.contains("win")) {
                // Windows: Use recycle bin via PowerShell
                return windowsMoveToRecycleBin(path);
            } else if (os.contains("nix") || os.contains("nux")) {
                // Linux: Use trash-cli or ~/.local/share/Trash
                Path trashPath = Path.of(System.getProperty("user.home"), ".local", "share", "Trash", "files");
                if (Files.exists(trashPath)) {
                    Path targetPath = resolveUniquePath(trashPath, path.getFileName().toString());
                    Files.move(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Fallback trash move failed: " + e.getMessage());
        }

        return false;
    }

    /**
     * On Windows, use PowerShell to move to Recycle Bin.
     */
    private boolean windowsMoveToRecycleBin(Path path) {
        try {
            String command = String.format(
                "powershell -command \"Add-Type -AssemblyName Microsoft.VisualBasic; " +
                "[Microsoft.VisualBasic.FileIO.FileSystem]::DeleteDirectory('%s', 'OnlyForWindows', 'RecycleOption.SendToRecycleBin')\"",
                path.toString().replace("\\", "\\\\")
            );
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            System.err.println("Windows recycle bin failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Resolves a unique path to avoid name conflicts in trash.
     */
    private Path resolveUniquePath(Path parent, String name) {
        Path target = parent.resolve(name);
        if (!Files.exists(target)) {
            return target;
        }

        String baseName;
        String extension;
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = name.substring(0, dotIndex);
            extension = name.substring(dotIndex);
        } else {
            baseName = name;
            extension = "";
        }

        int counter = 1;
        while (Files.exists(target)) {
            target = parent.resolve(baseName + " (" + counter + ")" + extension);
            counter++;
        }

        return target;
    }

    public void cancel() {
        cancelled = true;
    }
}