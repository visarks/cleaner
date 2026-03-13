package com.cleaner.model;

import javafx.beans.property.*;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileItem {
    private final Path path;
    private final Path rootPath; // Root folder for relative path calculation
    private final long size;
    private final LocalDateTime modifiedTime;
    private final String matchedRule;
    private final String ruleType; // "delete" or "keep"
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final StringProperty displaySize = new SimpleStringProperty();
    private final StringProperty displayTime = new SimpleStringProperty();

    public FileItem(Path path, long size, LocalDateTime modifiedTime, String matchedRule, String ruleType) {
        this(path, size, modifiedTime, matchedRule, ruleType, null);
    }

    public FileItem(Path path, long size, LocalDateTime modifiedTime, String matchedRule, String ruleType, Path rootPath) {
        this.path = path;
        this.rootPath = rootPath;
        this.size = size;
        this.modifiedTime = modifiedTime;
        this.matchedRule = matchedRule;
        this.ruleType = ruleType;
        this.displaySize.set(formatSize(size));
        this.displayTime.set(modifiedTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        this.selected.set("delete".equals(ruleType));
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public Path getPath() { return path; }
    public Path getRootPath() { return rootPath; }
    public long getSize() { return size; }
    public LocalDateTime getModifiedTime() { return modifiedTime; }
    public String getMatchedRule() { return matchedRule; }
    public String getRuleType() { return ruleType; }
    public boolean isSelected() { return selected.get(); }
    public BooleanProperty selectedProperty() { return selected; }
    public void setSelected(boolean selected) { this.selected.set(selected); }
    public String getDisplaySize() { return displaySize.get(); }
    public StringProperty displaySizeProperty() { return displaySize; }
    public String getDisplayTime() { return displayTime.get(); }
    public StringProperty displayTimeProperty() { return displayTime; }
    public String getFileName() { return path.getFileName().toString(); }

    public String getParentPath() {
        if (rootPath != null) {
            // Return relative path
            Path relative = rootPath.relativize(path.getParent() != null ? path.getParent() : path);
            return relative.toString();
        }
        return path.getParent() != null ? path.getParent().toString() : "";
    }
}