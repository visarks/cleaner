package com.cleaner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a folder configuration with its own set of rules.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FolderConfig {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("path")
    private String path;

    @JsonProperty("deleteRules")
    private List<DeleteRule> deleteRules = new ArrayList<>();

    @JsonProperty("keepRules")
    private List<KeepRule> keepRules = new ArrayList<>();

    public FolderConfig() {
        this.id = UUID.randomUUID().toString();
    }

    public FolderConfig(String name, String path) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.path = path;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public List<DeleteRule> getDeleteRules() { return deleteRules; }
    public void setDeleteRules(List<DeleteRule> deleteRules) { this.deleteRules = deleteRules; }

    public List<KeepRule> getKeepRules() { return keepRules; }
    public void setKeepRules(List<KeepRule> keepRules) { this.keepRules = keepRules; }

    @Override
    public String toString() {
        return name != null ? name : (path != null ? path : "未命名文件夹");
    }
}