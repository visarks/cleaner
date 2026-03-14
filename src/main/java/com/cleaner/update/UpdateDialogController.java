package com.cleaner.update;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Path;

/**
 * 更新对话框控制器
 */
public class UpdateDialogController {

    @FXML
    private Label currentVersionLabel;

    @FXML
    private Label latestVersionLabel;

    @FXML
    private TextArea releaseNotesArea;

    @FXML
    private VBox downloadBox;

    @FXML
    private Label downloadStatusLabel;

    @FXML
    private Label downloadPercentLabel;

    @FXML
    private ProgressBar downloadProgressBar;

    @FXML
    private Button laterButton;

    @FXML
    private Button downloadButton;

    @FXML
    private Button viewButton;

    @FXML
    private Button cancelButton;

    private UpdateChecker.UpdateInfo updateInfo;
    private UpdateManager updateManager;
    private Task<Path> downloadTask;
    private Stage dialogStage;

    public void setUpdateInfo(UpdateChecker.UpdateInfo updateInfo) {
        this.updateInfo = updateInfo;
        this.updateManager = new UpdateManager();

        currentVersionLabel.setText(updateInfo.currentVersion());
        latestVersionLabel.setText(updateInfo.versionTag());

        String notes = updateInfo.releaseNotes();
        if (notes == null || notes.isEmpty()) {
            notes = "暂无更新说明";
        }
        // 限制长度
        if (notes.length() > 1000) {
            notes = notes.substring(0, 1000) + "...";
        }
        releaseNotesArea.setText(notes);

        // 绑定进度属性
        downloadProgressBar.progressProperty().bind(updateManager.downloadProgressProperty());
        downloadStatusLabel.textProperty().bind(updateManager.downloadStatusProperty());

        updateManager.downloadProgressProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                downloadPercentLabel.setText(String.format("%.0f%%", newVal.doubleValue() * 100));
            });
        });
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    @FXML
    private void onDownloadAndInstall() {
        showDownloadProgress();

        downloadTask = new Task<>() {
            @Override
            protected Path call() throws Exception {
                return updateManager.downloadUpdate(updateInfo, null);
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    try {
                        updateManager.installUpdate(getValue());
                    } catch (Exception e) {
                        showError("安装失败", e.getMessage());
                        resetToInitialState();
                    }
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    Throwable e = getException();
                    if (e != null && !e.getMessage().contains("取消")) {
                        showError("下载失败", e.getMessage());
                    }
                    resetToInitialState();
                });
            }

            @Override
            protected void cancelled() {
                Platform.runLater(() -> resetToInitialState());
            }
        };

        Thread thread = Thread.ofVirtual().start(downloadTask);
    }

    @FXML
    private void onCancelDownload() {
        if (downloadTask != null) {
            downloadTask.cancel();
            updateManager.cancel();
        }
    }

    @FXML
    private void onLater() {
        closeDialog();
    }

    @FXML
    private void onViewRelease() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(updateInfo.htmlUrl()));
            }
        } catch (Exception e) {
            showError("无法打开链接", e.getMessage());
        }
    }

    private void showDownloadProgress() {
        downloadBox.setVisible(true);
        downloadBox.setManaged(true);
        downloadButton.setVisible(false);
        downloadButton.setManaged(false);
        laterButton.setText("后台下载");
        viewButton.setVisible(false);
        viewButton.setManaged(false);
        cancelButton.setVisible(true);
        cancelButton.setManaged(true);
    }

    private void resetToInitialState() {
        downloadBox.setVisible(false);
        downloadBox.setManaged(false);
        downloadButton.setVisible(true);
        downloadButton.setManaged(true);
        laterButton.setText("稍后提醒");
        viewButton.setVisible(true);
        viewButton.setManaged(true);
        cancelButton.setVisible(false);
        cancelButton.setManaged(false);
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}