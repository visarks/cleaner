package com.cleaner.scanner;

import com.cleaner.matcher.RuleMatcher;
import com.cleaner.model.FileItem;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class FileScanner {

    private final RuleMatcher ruleMatcher;
    private volatile boolean cancelled = false;
    private final AtomicInteger fileCount = new AtomicInteger(0);
    private final AtomicLong totalSize = new AtomicLong(0);

    public FileScanner(RuleMatcher ruleMatcher) {
        this.ruleMatcher = ruleMatcher;
    }

    public static record ScanResult(List<FileItem> items, int totalCount, long totalSize) {}

    public static record ScanProgress(int currentCount, long currentSize, Path currentFolder, Path rootPath, FileItem newItem) {}

    public CompletableFuture<ScanResult> scanAsync(List<Path> rootPaths, Consumer<ScanProgress> progressCallback) {
        cancelled = false;
        fileCount.set(0);
        totalSize.set(0);

        return CompletableFuture.supplyAsync(() -> {
            List<FileItem> items = new ArrayList<>();

            for (Path rootPath : rootPaths) {
                if (cancelled) break;

                // Report current scanning folder
                if (progressCallback != null) {
                    progressCallback.accept(new ScanProgress(fileCount.get(), totalSize.get(), rootPath, rootPath, null));
                }

                scanDirectory(rootPath, items, progressCallback, rootPath);
            }

            return new ScanResult(items, fileCount.get(), totalSize.get());
        }, Executors.newVirtualThreadPerTaskExecutor());
    }

    private void scanDirectory(Path rootPath, List<FileItem> items, Consumer<ScanProgress> progressCallback, Path originalRoot) {
        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                private Path lastReportedFolder = null;

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (cancelled) return FileVisitResult.TERMINATE;

                    // Report current scanning folder (immediate child of root only)
                    Path relativeToRoot = originalRoot.relativize(dir);
                    if (relativeToRoot.getNameCount() == 1) {
                        // This is immediate child folder only
                        if (progressCallback != null && !dir.equals(lastReportedFolder)) {
                            lastReportedFolder = dir;
                            progressCallback.accept(new ScanProgress(fileCount.get(), totalSize.get(), dir, originalRoot, null));
                        }
                    }

                    // Check if directory matches any rule
                    RuleMatcher.MatchResult result = ruleMatcher.match(dir);
                    if (result.type() != null) {
                        // If matched, skip directory contents
                        if ("delete".equals(result.type())) {
                            // Only add to list if it's a delete rule
                            LocalDateTime modifiedTime = toLocalDateTime(attrs.lastModifiedTime());
                            FileItem item = new FileItem(dir, attrs.size(), modifiedTime, result.rule(), result.type(), originalRoot);
                            synchronized (items) {
                                items.add(item);
                            }
                            fileCount.incrementAndGet();
                            totalSize.addAndGet(attrs.size());

                            // Report new item
                            if (progressCallback != null) {
                                progressCallback.accept(new ScanProgress(fileCount.get(), totalSize.get(), null, originalRoot, item));
                            }
                        }
                        // Skip directory contents if matched (both keep and delete rules)
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (cancelled) return FileVisitResult.TERMINATE;

                    RuleMatcher.MatchResult result = ruleMatcher.match(file);
                    if (result.type() != null && "delete".equals(result.type())) {
                        // Only add to list if it's a delete rule
                        LocalDateTime modifiedTime = toLocalDateTime(attrs.lastModifiedTime());
                        FileItem item = new FileItem(file, attrs.size(), modifiedTime, result.rule(), result.type(), originalRoot);
                        synchronized (items) {
                            items.add(item);
                        }
                        fileCount.incrementAndGet();
                        totalSize.addAndGet(attrs.size());

                        // Report new item
                        if (progressCallback != null) {
                            progressCallback.accept(new ScanProgress(fileCount.get(), totalSize.get(), file.getParent(), originalRoot, item));
                        }
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    // Skip files that can't be accessed
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // Log error and continue
            System.err.println("Error scanning directory: " + rootPath + " - " + e.getMessage());
        }
    }

    private LocalDateTime toLocalDateTime(FileTime fileTime) {
        Instant instant = fileTime.toInstant();
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public void cancel() {
        cancelled = true;
    }

    public int getFileCount() {
        return fileCount.get();
    }

    public long getTotalSize() {
        return totalSize.get();
    }
}