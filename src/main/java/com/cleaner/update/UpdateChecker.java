package com.cleaner.update;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 检查 GitHub Releases 更新
 */
public class UpdateChecker {

    private static final String GITHUB_API_URL = "https://api.github.com/repos/visarks/cleaner/releases/latest";
    private static final String GITHUB_RELEASES_URL = "https://github.com/visarks/cleaner/releases/latest";
    private static final Pattern VERSION_PATTERN = Pattern.compile("v?(\\d+)\\.(\\d+)\\.(\\d+)");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String currentVersion;

    public UpdateChecker(String currentVersion) {
        this.currentVersion = normalizeVersion(currentVersion);
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 检查是否有新版本
     */
    public Optional<UpdateInfo> checkForUpdate() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(GITHUB_API_URL))
            .timeout(Duration.ofSeconds(30))
            .header("Accept", "application/vnd.github.v3+json")
            .header("User-Agent", "Cleaner-App")
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to check for updates: HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());

        String latestVersionTag = root.path("tag_name").asText();
        String latestVersion = normalizeVersion(latestVersionTag);
        String releaseNotes = root.path("body").asText("");
        String htmlUrl = root.path("html_url").asText(GITHUB_RELEASES_URL);

        // 找到适合当前平台的下载 URL
        String downloadUrl = null;
        String assetName = null;
        JsonNode assets = root.path("assets");

        if (assets.isArray()) {
            String platform = getPlatformSuffix();
            for (JsonNode asset : assets) {
                String name = asset.path("name").asText();
                if (name.endsWith(platform)) {
                    downloadUrl = asset.path("browser_download_url").asText();
                    assetName = name;
                    break;
                }
            }
        }

        // 比较版本号
        if (compareVersions(latestVersion, currentVersion) > 0) {
            return Optional.of(new UpdateInfo(
                latestVersionTag,
                latestVersion,
                currentVersion,
                releaseNotes,
                htmlUrl,
                downloadUrl,
                assetName
            ));
        }

        return Optional.empty();
    }

    /**
     * 获取当前平台的文件后缀
     */
    private String getPlatformSuffix() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            return ".dmg";
        } else if (os.contains("win")) {
            return ".exe";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return ".deb";
        }
        return ".dmg"; // 默认 macOS
    }

    /**
     * 标准化版本号
     */
    private String normalizeVersion(String version) {
        if (version == null || version.isEmpty()) {
            return "0.0.0";
        }
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (matcher.find()) {
            return matcher.group(1) + "." + matcher.group(2) + "." + matcher.group(3);
        }
        return "0.0.0";
    }

    /**
     * 比较版本号，返回 >0 表示 v1 > v2
     */
    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }

    /**
     * 更新信息
     */
    public record UpdateInfo(
        String versionTag,      // v1.0.1
        String version,         // 1.0.1
        String currentVersion,  // 1.0.0
        String releaseNotes,    // 发布说明
        String htmlUrl,         // GitHub Release 页面
        String downloadUrl,     // 下载链接
        String assetName        // 文件名
    ) {}
}