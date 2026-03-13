package com.cleaner.config;

import com.cleaner.model.DeleteRule;
import com.cleaner.model.FolderConfig;
import com.cleaner.model.KeepRule;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ConfigManager {

    private static final String CONFIG_FILE = "cleaner-config.json";
    private final ObjectMapper objectMapper;
    private final Path configPath;

    public ConfigManager() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.configPath = Path.of(System.getProperty("user.home"), "." + CONFIG_FILE);
    }

    public static class AppConfig {
        @JsonProperty("folders")
        private List<FolderConfig> folders = new ArrayList<>();

        public List<FolderConfig> getFolders() { return folders; }
        public void setFolders(List<FolderConfig> folders) { this.folders = folders; }
    }

    public AppConfig load() {
        try {
            if (Files.exists(configPath)) {
                String content = Files.readString(configPath);
                return objectMapper.readValue(content, AppConfig.class);
            }
        } catch (IOException e) {
            System.err.println("Failed to load config: " + e.getMessage());
        }
        return new AppConfig();
    }

    public void save(AppConfig config) {
        try {
            String content = objectMapper.writeValueAsString(config);
            Files.writeString(configPath, content);
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }

    public Optional<FolderConfig> findFolderById(String id) {
        AppConfig config = load();
        return config.getFolders().stream()
            .filter(f -> f.getId().equals(id))
            .findFirst();
    }

    public void saveFolder(FolderConfig folder) {
        AppConfig config = load();
        Optional<FolderConfig> existing = config.getFolders().stream()
            .filter(f -> f.getId().equals(folder.getId()))
            .findFirst();

        if (existing.isPresent()) {
            config.getFolders().remove(existing.get());
        }
        config.getFolders().add(folder);
        save(config);
    }

    public void deleteFolder(String folderId) {
        AppConfig config = load();
        config.getFolders().removeIf(f -> f.getId().equals(folderId));
        save(config);
    }
}