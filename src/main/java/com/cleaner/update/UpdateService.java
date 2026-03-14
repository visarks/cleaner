package com.cleaner.update;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 更新服务 - 统一管理更新检查和对话框显示
 */
public class UpdateService {

    private static UpdateService instance;
    private final UpdateChecker updateChecker;
    private Stage primaryStage;

    private UpdateService() {
        // 从 manifest 或默认值获取当前版本
        String version = getVersionFromManifest();
        this.updateChecker = new UpdateChecker(version);
    }

    public static synchronized UpdateService getInstance() {
        if (instance == null) {
            instance = new UpdateService();
        }
        return instance;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    /**
     * 获取当前版本号
     */
    private String getVersionFromManifest() {
        try {
            // 尝试从 MANIFEST.MF 读取版本
            String version = UpdateService.class.getPackage().getImplementationVersion();
            if (version != null && !version.isEmpty()) {
                return version;
            }
        } catch (Exception ignored) {
        }
        // 默认版本
        return "1.0.0";
    }

    /**
     * 异步检查更新并显示对话框
     */
    public CompletableFuture<Optional<UpdateChecker.UpdateInfo>> checkForUpdateAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return updateChecker.checkForUpdate();
            } catch (Exception e) {
                return Optional.empty();
            }
        });
    }

    /**
     * 检查更新并在有更新时显示对话框
     */
    public void checkAndShowDialog() {
        checkForUpdateAsync().thenAccept(optionalUpdate -> {
            if (optionalUpdate.isPresent()) {
                Platform.runLater(() -> showUpdateDialog(optionalUpdate.get()));
            }
        });
    }

    /**
     * 检查更新并回调
     */
    public void checkForUpdate(java.util.function.Consumer<Optional<UpdateChecker.UpdateInfo>> callback) {
        checkForUpdateAsync().thenAccept(update -> {
            Platform.runLater(() -> callback.accept(update));
        });
    }

    /**
     * 显示更新对话框
     */
    public void showUpdateDialog(UpdateChecker.UpdateInfo updateInfo) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/update_dialog.fxml"));
            Parent root = loader.load();

            UpdateDialogController controller = loader.getController();
            controller.setUpdateInfo(updateInfo);

            Stage dialogStage = new Stage();
            dialogStage.initStyle(StageStyle.UTILITY);
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            if (primaryStage != null) {
                dialogStage.initOwner(primaryStage);
            }
            dialogStage.setTitle("软件更新");
            dialogStage.setResizable(false);

            Scene scene = new Scene(root);
            // 应用主题
            scene.getStylesheets().add(getClass().getResource("/css/main.css").toExternalForm());

            dialogStage.setScene(scene);
            controller.setDialogStage(dialogStage);
            dialogStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}