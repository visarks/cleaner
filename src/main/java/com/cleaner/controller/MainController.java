package com.cleaner.controller;

import com.cleaner.config.ConfigManager;
import com.cleaner.config.ConfigManager.AppConfig;
import com.cleaner.deleter.FileDeleter;
import com.cleaner.matcher.RuleMatcher;
import com.cleaner.model.DeleteRule;
import com.cleaner.model.FileItem;
import com.cleaner.model.FolderConfig;
import com.cleaner.model.KeepRule;
import com.cleaner.scanner.FileScanner;
import com.cleaner.util.LogoGenerator;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MainController {

    // Left panel
    @FXML private ImageView logoImageView;
    @FXML private ListView<FolderConfig> folderListView;
    @FXML private Button addFolderButton;
    @FXML private Button scanButton;
    @FXML private Button stopButton;

    // Right panel - Tabs
    @FXML private TabPane tabPane;
    @FXML private Tab fileListTab;

    // File list tab
    @FXML private TableView<FileItem> fileTableView;
    @FXML private TableColumn<FileItem, Boolean> selectColumn;
    @FXML private TableColumn<FileItem, String> fileNameColumn;
    @FXML private TableColumn<FileItem, String> pathColumn;
    @FXML private TableColumn<FileItem, String> sizeColumn;
    @FXML private TableColumn<FileItem, String> modifiedColumn;
    @FXML private TableColumn<FileItem, String> ruleColumn;
    @FXML private Button deleteSelectedButton;
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Label fileCountLabel;

    // Delete rules tab
    @FXML private TextField deleteRuleField;
    @FXML private ListView<DeleteRule> deleteRulesListView;
    @FXML private Button addDeleteRuleButton;

    // Keep rules tab
    @FXML private TextField keepRuleField;
    @FXML private ListView<KeepRule> keepRulesListView;
    @FXML private Button addKeepRuleButton;

    // Core components
    private final RuleMatcher ruleMatcher = new RuleMatcher();
    private final ConfigManager configManager = new ConfigManager();
    private FileScanner fileScanner;
    private final FileDeleter fileDeleter = new FileDeleter();
    private final ObservableList<FileItem> fileItems = FXCollections.observableArrayList();
    private final List<FileItem> allScannedItems = new ArrayList<>(); // Store all scanned items
    private final Object scanLock = new Object(); // Lock for thread safety
    private volatile boolean filteringFiles = false; // Flag to prevent race condition
    private final ObservableList<FolderConfig> folders = FXCollections.observableArrayList();
    private final ObservableList<DeleteRule> currentDeleteRules = FXCollections.observableArrayList();
    private final ObservableList<KeepRule> currentKeepRules = FXCollections.observableArrayList();
    private final Set<String> selectedFolderIds = new HashSet<>();
    private volatile boolean isScanning = false;

    @FXML
    public void initialize() {
        setupLogo();
        setupButtonIcons();
        setupFolderListView();
        setupFileTableView();
        setupRulesListViews();
        setupRuleFields();
        loadConfig();
        updateScanButtonState();
        updateDeleteButtonState();
    }

    private void setupButtonIcons() {
        // 使用 Unicode 符号替代图标库
        addFolderButton.setText("📁+ 添加文件夹");
        deleteSelectedButton.setText("🗑 删除选中");
        addDeleteRuleButton.setText("+ 添加规则");
        addKeepRuleButton.setText("+ 添加规则");
    }

    private void setupLogo() {
        Image logo = LogoGenerator.generateLogo(64);
        logoImageView.setImage(logo);
    }

    private void setupFolderListView() {
        folderListView.setItems(folders);
        folderListView.setCellFactory(listView -> new ListCell<>() {
            private final CheckBox checkBox = new CheckBox();
            private final Label nameLabel = new Label();
            private final Label pathLabel = new Label();
            private final VBox vbox = new VBox(0, nameLabel, pathLabel);
            private final Button removeBtn = new Button("×");
            private final Region spacer = new Region();
            private final HBox hbox = new HBox(6, checkBox, vbox, spacer, removeBtn);

            {
                setPadding(new javafx.geometry.Insets(4, 4, 4, 4));
                hbox.setAlignment(Pos.CENTER_LEFT);
                pathLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #8b949e;");
                nameLabel.setStyle("-fx-font-size: 12px;");
                vbox.setPadding(new javafx.geometry.Insets(0, 0, 0, 0));
                vbox.setSpacing(0);

                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #cf222e; " +
                    "-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 0 4 0 4; " +
                    "-fx-cursor: hand; -fx-min-width: 20px; -fx-min-height: 20px;");
                removeBtn.setOnMouseClicked(e -> {
                    FolderConfig folder = getItem();
                    if (folder != null) {
                        removeFolder(folder);
                        e.consume();
                    }
                });

                checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    FolderConfig folder = getItem();
                    if (folder != null) {
                        if (newVal) {
                            selectedFolderIds.add(folder.getId());
                        } else {
                            selectedFolderIds.remove(folder.getId());
                        }
                        updateScanButtonState();
                        filterFileList(); // Update file list when folder selection changes
                    }
                });
            }

            @Override
            protected void updateItem(FolderConfig folder, boolean empty) {
                super.updateItem(folder, empty);
                if (empty || folder == null) {
                    setGraphic(null);
                } else {
                    nameLabel.setText(folder.getName() != null ? folder.getName() : "未命名");
                    pathLabel.setText(shortenPath(folder.getPath()));
                    checkBox.setSelected(selectedFolderIds.contains(folder.getId()));
                    setGraphic(hbox);
                }
            }

            private String shortenPath(String path) {
                if (path == null) return "";
                if (path.length() > 26) {
                    return "..." + path.substring(path.length() - 23);
                }
                return path;
            }
        });

        folderListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadFolderRules(newVal);
            }
            // Update file list when selection changes
            filterFileList();
        });
    }

    private void removeFolder(FolderConfig folder) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认移除");
        confirm.setHeaderText(null);
        confirm.setGraphic(null);
        confirm.setContentText("确定要移除这个文件夹吗？");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                folders.remove(folder);
                selectedFolderIds.remove(folder.getId());
                configManager.deleteFolder(folder.getId());
                updateScanButtonState();
            }
        });
    }

    private void loadFolderRules(FolderConfig folder) {
        currentDeleteRules.clear();
        currentKeepRules.clear();
        currentDeleteRules.addAll(folder.getDeleteRules());
        currentKeepRules.addAll(folder.getKeepRules());
        syncRulesToMatcher();
    }

    private void setupFileTableView() {
        fileTableView.setItems(fileItems);
        fileTableView.setEditable(true);

        // Header checkbox for select all
        CheckBox selectAllHeaderCheckBox = new CheckBox();
        selectColumn.setGraphic(selectAllHeaderCheckBox);
        selectColumn.setResizable(false);
        selectColumn.setSortable(false);

        selectAllHeaderCheckBox.setOnAction(e -> {
            boolean selectAll = selectAllHeaderCheckBox.isSelected();
            fileItems.forEach(item -> item.setSelected(selectAll));
            fileTableView.refresh();
            updateDeleteButtonState();
        });

        // Custom checkbox cell
        selectColumn.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();

            {
                checkBox.setOnAction(e -> {
                    int index = getIndex();
                    if (index >= 0 && index < getTableView().getItems().size()) {
                        FileItem item = getTableView().getItems().get(index);
                        if (item != null) {
                            item.setSelected(checkBox.isSelected());
                            updateDeleteButtonState();
                            // Update header checkbox state
                            boolean allSelected = fileItems.stream().allMatch(FileItem::isSelected);
                            selectAllHeaderCheckBox.setSelected(allSelected);
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    int index = getIndex();
                    if (index >= 0 && index < getTableView().getItems().size()) {
                        FileItem fileItem = getTableView().getItems().get(index);
                        if (fileItem != null) {
                            checkBox.setSelected(fileItem.isSelected());
                        }
                    }
                    setGraphic(checkBox);
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });

        fileNameColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getFileName()));
        pathColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getParentPath()));
        sizeColumn.setCellValueFactory(cellData ->
            cellData.getValue().displaySizeProperty());
        modifiedColumn.setCellValueFactory(cellData ->
            cellData.getValue().displayTimeProperty());
        ruleColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getMatchedRule()));

        ruleColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #f57c00;");
                }
            }
        });

        fileItems.addListener((javafx.collections.ListChangeListener<FileItem>) change -> {
            updateFileCountLabel();
            updateDeleteButtonState();
        });
    }

    private void updateFileCountLabel() {
        long totalSize = fileItems.stream().mapToLong(FileItem::getSize).sum();
        fileCountLabel.setText(String.format("%d 个文件, %s", fileItems.size(), formatSize(totalSize)));
    }

    private void setupRulesListViews() {
        deleteRulesListView.setItems(currentDeleteRules);
        keepRulesListView.setItems(currentKeepRules);

        // Delete rules cell - button on the right
        deleteRulesListView.setCellFactory(listView -> new ListCell<>() {
            private final Label label = new Label();
            private final Button removeBtn = new Button("×");
            private final Region spacer = new Region();
            private final HBox hbox = new HBox(10, label, spacer, removeBtn);

            {
                hbox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #cf222e; " +
                    "-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 0 4 0 4; " +
                    "-fx-cursor: hand; -fx-min-width: 24px; -fx-min-height: 24px;");
                removeBtn.setOnMouseClicked(e -> {
                    DeleteRule rule = getItem();
                    if (rule != null) {
                        currentDeleteRules.remove(rule);
                        saveCurrentFolderRules();
                        updateScanButtonState();
                        e.consume();
                    }
                });
            }

            @Override
            protected void updateItem(DeleteRule rule, boolean empty) {
                super.updateItem(rule, empty);
                if (empty || rule == null) {
                    setGraphic(null);
                } else {
                    label.setText(rule.getPattern());
                    setGraphic(hbox);
                }
            }
        });

        // Keep rules cell - button on the right
        keepRulesListView.setCellFactory(listView -> new ListCell<>() {
            private final Label label = new Label();
            private final Button removeBtn = new Button("×");
            private final Region spacer = new Region();
            private final HBox hbox = new HBox(10, label, spacer, removeBtn);

            {
                hbox.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #cf222e; " +
                    "-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 0 4 0 4; " +
                    "-fx-cursor: hand; -fx-min-width: 24px; -fx-min-height: 24px;");
                removeBtn.setOnMouseClicked(e -> {
                    KeepRule rule = getItem();
                    if (rule != null) {
                        currentKeepRules.remove(rule);
                        saveCurrentFolderRules();
                        e.consume();
                    }
                });
            }

            @Override
            protected void updateItem(KeepRule rule, boolean empty) {
                super.updateItem(rule, empty);
                if (empty || rule == null) {
                    setGraphic(null);
                } else {
                    label.setText(rule.getPattern());
                    setGraphic(hbox);
                }
            }
        });

        currentDeleteRules.addListener((javafx.collections.ListChangeListener<DeleteRule>) change -> {
            syncRulesToMatcher();
            updateScanButtonState();
        });

        currentKeepRules.addListener((javafx.collections.ListChangeListener<KeepRule>) change -> {
            syncRulesToMatcher();
        });
    }

    private void setupRuleFields() {
        deleteRuleField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                onAddDeleteRule();
            }
        });

        keepRuleField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                onAddKeepRule();
            }
        });
    }

    private void updateScanButtonState() {
        boolean hasSelectedFolders = !selectedFolderIds.isEmpty();
        boolean hasDeleteRules = folders.stream()
            .anyMatch(f -> selectedFolderIds.contains(f.getId()) && !f.getDeleteRules().isEmpty());

        scanButton.setDisable(!hasSelectedFolders || !hasDeleteRules || isScanning);

        if (isScanning) {
            scanButton.setText("扫描中...");
        } else if (!hasSelectedFolders) {
            scanButton.setText("请选择文件夹");
        } else if (!hasDeleteRules) {
            scanButton.setText("请添加删除规则");
        } else {
            scanButton.setText("开始扫描");
        }
    }

    private void updateDeleteButtonState() {
        boolean hasSelectedDeleteFiles = fileItems.stream()
            .anyMatch(item -> item.isSelected() && "delete".equals(item.getRuleType()));

        deleteSelectedButton.setDisable(isScanning || !hasSelectedDeleteFiles);
    }

    private void loadConfig() {
        AppConfig config = configManager.load();
        folders.clear();
        folders.addAll(config.getFolders());

        for (FolderConfig folder : folders) {
            selectedFolderIds.add(folder.getId());
        }

        if (!folders.isEmpty()) {
            folderListView.getSelectionModel().selectFirst();
        }
        folderListView.refresh();
        updateScanButtonState();
    }

    private void saveCurrentFolderRules() {
        FolderConfig selected = folderListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.getDeleteRules().clear();
            selected.getDeleteRules().addAll(currentDeleteRules);
            selected.getKeepRules().clear();
            selected.getKeepRules().addAll(currentKeepRules);
            configManager.saveFolder(selected);
        }
    }

    private void syncRulesToMatcher() {
        ruleMatcher.clearRules();
        currentDeleteRules.forEach(ruleMatcher::addRule);
        currentKeepRules.forEach(ruleMatcher::addRule);
    }

    @FXML
    private void onAddFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择文件夹");

        Stage stage = (Stage) addFolderButton.getScene().getWindow();
        java.io.File dir = chooser.showDialog(stage);

        if (dir != null) {
            String path = dir.getAbsolutePath();
            boolean exists = folders.stream().anyMatch(f -> path.equals(f.getPath()));
            if (exists) {
                showWarning("文件夹已存在", "该文件夹已在列表中。");
                return;
            }

            FolderConfig folder = new FolderConfig(dir.getName(), dir.getAbsolutePath());
            folders.add(folder);
            selectedFolderIds.add(folder.getId());
            configManager.saveFolder(folder);
            folderListView.getSelectionModel().selectLast();
            folderListView.refresh();
            updateScanButtonState();
        }
    }

    @FXML
    private void onScan() {
        if (selectedFolderIds.isEmpty()) {
            showError("请先选择要扫描的文件夹");
            return;
        }

        List<Path> pathsToScan = folders.stream()
            .filter(f -> selectedFolderIds.contains(f.getId()) && f.getPath() != null)
            .map(f -> Path.of(f.getPath()))
            .collect(Collectors.toList());

        if (pathsToScan.isEmpty()) {
            showError("请先选择要扫描的文件夹");
            return;
        }

        ruleMatcher.clearRules();
        for (FolderConfig folder : folders) {
            if (selectedFolderIds.contains(folder.getId())) {
                folder.getKeepRules().forEach(ruleMatcher::addRule);
                folder.getDeleteRules().forEach(ruleMatcher::addRule);
            }
        }

        if (ruleMatcher.getDeleteRules().isEmpty()) {
            showError("请先添加删除规则");
            return;
        }

        fileItems.clear();
        synchronized (scanLock) {
            allScannedItems.clear();
        }
        isScanning = true;
        progressBar.setProgress(0);
        statusLabel.setText("正在扫描...");
        progressBar.setVisible(true);
        statusLabel.setVisible(true);
        tabPane.getSelectionModel().select(fileListTab);

        // Show stop button, hide scan button
        scanButton.setVisible(false);
        scanButton.setManaged(false);
        stopButton.setVisible(true);
        stopButton.setManaged(true);

        updateScanButtonState();
        updateDeleteButtonState();

        fileScanner = new FileScanner(ruleMatcher);

        // Count immediate subfolders for progress
        int totalSubfolders = 0;
        for (Path p : pathsToScan) {
            try (var stream = Files.list(p)) {
                totalSubfolders += (int) stream.filter(Files::isDirectory).count();
            } catch (Exception e) {
                // Ignore
            }
        }

        final int finalTotalSubfolders = totalSubfolders;
        final AtomicInteger currentFolderCount = new AtomicInteger(0);

        Task<FileScanner.ScanResult> scanTask = new Task<>() {
            @Override
            protected FileScanner.ScanResult call() {
                return fileScanner.scanAsync(pathsToScan, progress -> {
                    Platform.runLater(() -> {
                        // Add file to list immediately
                        if (progress.newItem() != null) {
                            synchronized (scanLock) {
                                allScannedItems.add(progress.newItem());
                            }
                            // Refresh file display based on current selection
                            refreshFileList(progress.newItem());
                        }

                        // Update progress with subfolder count
                        if (progress.currentFolder() != null) {
                            int current = currentFolderCount.incrementAndGet();
                            String folderName = progress.currentFolder().getFileName() != null
                                ? progress.currentFolder().getFileName().toString()
                                : progress.currentFolder().toString();

                            statusLabel.setText(String.format("(%d/%d) 正在扫描: %s", current, finalTotalSubfolders, folderName));
                            if (finalTotalSubfolders > 0) {
                                progressBar.setProgress((double) current / finalTotalSubfolders);
                            }
                        }
                    });
                }).join();
            }
        };

        scanTask.setOnSucceeded(event -> {
            Platform.runLater(() -> {
                progressBar.setProgress(1.0);
                statusLabel.setText(String.format("(%d/%d) 扫描完成", finalTotalSubfolders, finalTotalSubfolders));
                finishScanning();
            });
        });

        scanTask.setOnFailed(event -> {
            Platform.runLater(() -> {
                statusLabel.setText("扫描失败: " + scanTask.getException().getMessage());
                progressBar.setProgress(0);
                finishScanning();
            });
        });

        Thread.ofVirtual().start(scanTask);
    }

    /**
     * Refresh file list for a single new item during scanning.
     */
    private void refreshFileList(FileItem newItem) {
        // Skip if currently filtering
        if (filteringFiles) {
            return;
        }

        FolderConfig selectedFolder = folderListView.getSelectionModel().getSelectedItem();
        boolean shouldShow = false;

        if (selectedFolder != null && selectedFolder.getPath() != null) {
            shouldShow = newItem.getPath().toString().startsWith(selectedFolder.getPath());
        } else {
            // Show all files from selected folders
            shouldShow = selectedFolderIds.stream()
                .map(id -> folders.stream().filter(f -> f.getId().equals(id)).findFirst())
                .filter(opt -> opt.isPresent())
                .anyMatch(opt -> newItem.getPath().toString().startsWith(opt.get().getPath()));
        }

        if (shouldShow) {
            fileItems.add(newItem);
        }
    }

    /**
     * Filter file list based on selected folder.
     * If a folder is selected in the list view, show only files from that folder.
     * If no folder is selected, show all files from selected folders.
     */
    private void filterFileList() {
        // Don't filter if no data yet
        if (allScannedItems.isEmpty()) {
            return;
        }

        // Set flag to prevent concurrent modification
        filteringFiles = true;

        try {
            FolderConfig selectedFolder = folderListView.getSelectionModel().getSelectedItem();

            List<FileItem> itemsToShow;
            synchronized (scanLock) {
                if (selectedFolder != null && selectedFolder.getPath() != null) {
                    // Show only files from the selected folder
                    String folderPath = selectedFolder.getPath();
                    itemsToShow = allScannedItems.stream()
                        .filter(item -> item.getPath().toString().startsWith(folderPath))
                        .collect(Collectors.toList());
                } else {
                    // Show all files from selected folders
                    Set<String> selectedPaths = folders.stream()
                        .filter(f -> selectedFolderIds.contains(f.getId()) && f.getPath() != null)
                        .map(FolderConfig::getPath)
                        .collect(Collectors.toSet());

                    itemsToShow = allScannedItems.stream()
                        .filter(item -> {
                            String itemPath = item.getRootPath() != null
                                ? item.getRootPath().toString()
                                : item.getPath().toString();
                            return selectedPaths.stream().anyMatch(itemPath::startsWith);
                        })
                        .collect(Collectors.toList());
                }
            }

            fileItems.clear();
            fileItems.addAll(itemsToShow);
        } finally {
            filteringFiles = false;
        }
    }

    @FXML
    private void onStopScan() {
        if (fileScanner != null) {
            fileScanner.cancel();
        }
        isScanning = false;
        progressBar.setVisible(false);
        statusLabel.setVisible(false);
        finishScanning();
    }

    private void finishScanning() {
        isScanning = false;
        // Hide stop button, show scan button
        scanButton.setVisible(true);
        scanButton.setManaged(true);
        stopButton.setVisible(false);
        stopButton.setManaged(false);
        updateScanButtonState();
        updateDeleteButtonState();
    }

    @FXML
    private void onDeleteSelected() {
        List<Path> toDelete = fileItems.stream()
            .filter(FileItem::isSelected)
            .filter(item -> "delete".equals(item.getRuleType()))
            .map(FileItem::getPath)
            .collect(Collectors.toList());

        if (toDelete.isEmpty()) {
            showInfo("没有选中的文件", "请选中要删除的文件。\n保留规则的文件不会被删除。");
            return;
        }

        long totalSize = fileItems.stream()
            .filter(FileItem::isSelected)
            .filter(item -> "delete".equals(item.getRuleType()))
            .mapToLong(FileItem::getSize)
            .sum();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认删除");
        confirm.setHeaderText(null);
        confirm.setGraphic(null);
        confirm.setContentText(String.format("确定删除 %d 个文件/目录？\n总大小: %s", toDelete.size(), formatSize(totalSize)));

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                performDelete(toDelete);
            }
        });
    }

    private void performDelete(List<Path> paths) {
        progressBar.setProgress(0);
        statusLabel.setText("正在删除...");

        fileDeleter.moveToTrashAsync(paths, progress -> {
            Platform.runLater(() -> {
                statusLabel.setText(String.format("删除中... %d/%d", progress.current(), progress.total()));
                progressBar.setProgress((double) progress.current() / progress.total());
            });
        }).thenAccept(result -> {
            Platform.runLater(() -> {
                progressBar.setProgress(1);

                if (result.failCount() > 0) {
                    statusLabel.setText(String.format("删除完成: 成功 %d, 失败 %d",
                        result.successCount(), result.failCount()));
                    showWarning("部分文件删除失败",
                        String.format("成功: %d, 失败: %d", result.successCount(), result.failCount()));
                } else {
                    statusLabel.setText(String.format("删除完成: 成功 %d", result.successCount()));
                    showInfo("删除完成", String.format("成功删除 %d 个文件/目录", result.successCount()));
                }

                fileItems.removeIf(item ->
                    item.isSelected() && "delete".equals(item.getRuleType()) &&
                    !result.failedPaths().contains(item.getPath())
                );
            });
        });
    }

    @FXML
    private void onAddDeleteRule() {
        String pattern = deleteRuleField.getText();
        if (pattern != null && !pattern.isBlank()) {
            currentDeleteRules.add(new DeleteRule(pattern.trim()));
            deleteRuleField.clear();
            saveCurrentFolderRules();
            updateScanButtonState();
        }
    }

    @FXML
    private void onAddKeepRule() {
        String pattern = keepRuleField.getText();
        if (pattern != null && !pattern.isBlank()) {
            currentKeepRules.add(new KeepRule(pattern.trim()));
            keepRuleField.clear();
            saveCurrentFolderRules();
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}