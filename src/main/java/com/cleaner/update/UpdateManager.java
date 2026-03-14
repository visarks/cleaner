package com.cleaner.update;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * 管理更新下载和安装
 */
public class UpdateManager {

    private final HttpClient httpClient;
    private final DoubleProperty downloadProgress = new SimpleDoubleProperty(0);
    private final StringProperty downloadStatus = new SimpleStringProperty("");
    private volatile boolean cancelled = false;

    public UpdateManager() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    public DoubleProperty downloadProgressProperty() {
        return downloadProgress;
    }

    public StringProperty downloadStatusProperty() {
        return downloadStatus;
    }

    public void cancel() {
        cancelled = true;
    }

    /**
     * 下载更新文件
     */
    public Path downloadUpdate(UpdateChecker.UpdateInfo updateInfo, Consumer<Double> progressCallback) throws Exception {
        if (updateInfo.downloadUrl() == null || updateInfo.downloadUrl().isEmpty()) {
            throw new RuntimeException("没有找到适合当前平台的下载链接");
        }

        String userHome = System.getProperty("user.home");
        Path downloadDir = Paths.get(userHome, ".cleaner", "updates");
        Files.createDirectories(downloadDir);

        Path downloadFile = downloadDir.resolve(updateInfo.assetName());

        // 删除旧文件
        if (Files.exists(downloadFile)) {
            Files.delete(downloadFile);
        }

        updateStatus("正在连接服务器...");
        downloadProgress.set(0);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(updateInfo.downloadUrl()))
            .timeout(Duration.ofMinutes(30))
            .header("User-Agent", "Cleaner-App")
            .GET()
            .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new RuntimeException("下载失败: HTTP " + response.statusCode());
        }

        long contentLength = response.headers().firstValue("Content-Length")
            .map(Long::parseLong)
            .orElse(-1L);

        try (InputStream inputStream = response.body();
             OutputStream outputStream = Files.newOutputStream(downloadFile)) {

            byte[] buffer = new byte[8192];
            long totalRead = 0;
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1 && !cancelled) {
                outputStream.write(buffer, 0, bytesRead);
                totalRead += bytesRead;

                if (contentLength > 0) {
                    double progress = (double) totalRead / contentLength;
                    final long finalTotalRead = totalRead;
                    final long finalContentLength = contentLength;
                    Platform.runLater(() -> {
                        downloadProgress.set(progress);
                        updateStatus(String.format("已下载: %.1f MB / %.1f MB",
                            finalTotalRead / (1024.0 * 1024.0),
                            finalContentLength / (1024.0 * 1024.0)));
                    });
                    if (progressCallback != null) {
                        progressCallback.accept(progress);
                    }
                }
            }

            if (cancelled) {
                Files.deleteIfExists(downloadFile);
                throw new RuntimeException("下载已取消");
            }
        }

        updateStatus("下载完成");
        downloadProgress.set(1.0);
        return downloadFile;
    }

    /**
     * 安装更新
     */
    public void installUpdate(Path updateFile) throws Exception {
        String os = System.getProperty("os.name", "").toLowerCase();
        String fileName = updateFile.getFileName().toString().toLowerCase();

        if (os.contains("mac") || fileName.endsWith(".dmg")) {
            installMac(updateFile);
        } else if (os.contains("win") || fileName.endsWith(".exe")) {
            installWindows(updateFile);
        } else if (os.contains("nix") || os.contains("nux") || fileName.endsWith(".deb")) {
            installLinux(updateFile);
        } else {
            // 默认打开文件所在目录
            openFileLocation(updateFile);
        }
    }

    /**
     * macOS 安装 - 打开 DMG 文件
     */
    private void installMac(Path updateFile) throws Exception {
        // 使用 open 命令打开 DMG
        ProcessBuilder pb = new ProcessBuilder("open", updateFile.toString());
        pb.start();

        // 退出当前应用
        Platform.runLater(() -> {
            try {
                Thread.sleep(1000);
                System.exit(0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * Windows 安装 - 运行 EXE 安装程序
     */
    private void installWindows(Path updateFile) throws Exception {
        // 直接运行安装程序
        ProcessBuilder pb = new ProcessBuilder(updateFile.toString());
        pb.start();

        // 退出当前应用
        Platform.runLater(() -> {
            try {
                Thread.sleep(1000);
                System.exit(0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * Linux 安装 - 使用 dpkg 安装 DEB 包
     */
    private void installLinux(Path updateFile) throws Exception {
        // 需要 sudo 权限
        ProcessBuilder pb = new ProcessBuilder("pkexec", "dpkg", "-i", updateFile.toString());
        pb.start();

        Platform.runLater(() -> {
            try {
                Thread.sleep(1000);
                System.exit(0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * 打开文件所在位置
     */
    private void openFileLocation(Path updateFile) throws Exception {
        String os = System.getProperty("os.name", "").toLowerCase();

        ProcessBuilder pb;
        if (os.contains("mac")) {
            pb = new ProcessBuilder("open", "-R", updateFile.toString());
        } else if (os.contains("win")) {
            pb = new ProcessBuilder("explorer", "/select," + updateFile.toString());
        } else {
            pb = new ProcessBuilder("xdg-open", updateFile.getParent().toString());
        }
        pb.start();
    }

    private void updateStatus(String status) {
        Platform.runLater(() -> downloadStatus.set(status));
    }
}