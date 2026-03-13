# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JavaFX 桌面清理软件，用于扫描和删除匹配规则的文件/目录以释放存储空间。

## Build Tool

**Maven** (NOT Gradle)

## Technical Stack

- **Java**: JDK 21 LTS
- **UI Framework**: JavaFX 21 + FXML
- **Concurrency**: Virtual Threads + CompletableFuture
- **Config**: JSON format for rule persistence

## Architecture

```
src/main/java/com/cleaner/
├── CleanerApplication.java      # JavaFX entry point
├── controller/
│   ├── MainController.java      # Main UI controller
│   └── RuleController.java     # Rule management
├── model/
│   ├── FileItem.java           # File item (path, size, modified time, matched rule)
│   ├── Rule.java                # Base rule class
│   ├── DeleteRule.java         # Delete rule
│   └── KeepRule.java           # Keep rule (higher priority than delete)
├── scanner/
│   └── FileScanner.java        # Parallel directory scanner
├── matcher/
│   └── RuleMatcher.java        # Wildcard matching (keep rules > delete rules)
├── deleter/
│   └── FileDeleter.java        # Move files to recycle bin
├── config/
│   └── ConfigManager.java      # Rule persistence (JSON)
└── util/
    └── PathUtils.java          # Utilities
```

## Key Design Decisions

1. **Rule Priority**: Keep rules always override delete rules for safety
2. **Delete Method**: Move to recycle bin (not permanent delete)
3. **UI Layout**: Left panel for folder tree, right panel with tabs (file list, delete rules, keep rules)
4. **Wildcard Support**: `*.tmp`, `node_modules`, `**/dist/**`, `{*.log,*.tmp}`

## Maven Commands

```bash
# Compile
mvn compile

# Run application
mvn javafx:run

# Package
mvn package

# Clean
mvn clean
```