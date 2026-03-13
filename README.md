# 🧹 Cleaner - 桌面清理工具

<p align="center">
  <img src="javafx/mac/Cleaner.icns" alt="Cleaner Logo" width="128" height="128">
</p>

<p align="center">
  <strong>一款简洁高效的 JavaFX 桌面清理软件</strong>
</p>

<p align="center">
  扫描并删除匹配规则的文件/目录，快速释放磁盘空间
</p>

---

## ✨ 功能特性

- 📁 **多文件夹管理** - 支持添加多个文件夹，每个文件夹独立配置规则
- 🎯 **灵活的规则系统** - 支持删除规则和保留规则，保留规则优先级更高
- 🔍 **通配符匹配** - 支持 `*.tmp`、`node_modules`、`**/dist/**`、`{*.log,*.tmp}` 等模式
- ♻️ **安全删除** - 文件移动到回收站，非永久删除
- 💾 **配置持久化** - 自动保存文件夹和规则配置
- 🎨 **美观界面** - 使用 AtlantaFX 主题，支持深色/浅色模式

---

## 📸 界面预览

| 主界面 | 规则配置 |
|:---:|:---:|
| 文件列表展示 | 删除/保留规则管理 |

---

## 🚀 快速开始

### 环境要求

- **JDK 21+** (推荐使用 [OpenJDK 25](https://jdk.java.net/25/))
- **Maven 3.6+**

### 运行项目

```bash
# 克隆项目
git clone https://github.com/visarks/cleaner.git
cd cleaner

# 编译运行
mvn javafx:run
```

### 打包应用

```bash
# 设置 JDK 环境（推荐 OpenJDK 25）
export JAVA_HOME=/path/to/openjdk-25

# 打包 DMG（macOS）
mvn clean javafx:jlink jfx:package

# 输出文件位于: target/javafx/Cleaner-1.0.0.dmg
```

---

## 📖 使用指南

### 1️⃣ 添加文件夹

点击左侧「📁+ 添加文件夹」按钮，选择需要扫描的目录。

### 2️⃣ 配置规则

切换到「删除规则」标签页，添加匹配规则：

| 规则示例 | 说明 |
|:---|:---|
| `*.tmp` | 匹配所有 .tmp 后缀文件 |
| `node_modules` | 匹配名为 node_modules 的目录 |
| `**/dist/**` | 匹配任意路径下的 dist 目录 |
| `{*.log,*.tmp}` | 同时匹配 .log 和 .tmp 文件 |
| `target` | 匹配 Maven 构建目录 |
| `.DS_Store` | 匹配 macOS 系统文件 |

### 3️⃣ 保留规则（可选）

切换到「保留规则」标签页，添加不需要删除的文件规则：

- 保留规则优先级 **高于** 删除规则
- 匹配保留规则的文件不会被删除
- 适用于保护重要文件

### 4️⃣ 开始扫描

1. 勾选需要扫描的文件夹
2. 点击「开始扫描」按钮
3. 查看扫描进度和结果

### 5️⃣ 删除文件

1. 在文件列表中勾选需要删除的文件
2. 点击「🗑 删除选中」按钮
3. 确认后文件将移动到回收站

---

## ⚙️ 规则语法

### 基本通配符

| 符号 | 含义 | 示例 |
|:---:|:---|:---|
| `*` | 匹配任意字符（不含路径分隔符） | `*.log` 匹配所有 .log 文件 |
| `**` | 匹配任意路径 | `**/test/**` 匹配任意路径下的 test 目录 |
| `?` | 匹配单个字符 | `file?.txt` 匹配 file1.txt, fileA.txt |
| `{}` | 分组匹配 | `{*.log,*.tmp}` 匹配 .log 或 .tmp 文件 |

### 常用规则示例

```
# 缓存文件
*.cache
.cache
__pycache__

# 构建产物
target
build
dist
out
*.class

# 依赖目录
node_modules
vendor
Pods

# 日志文件
*.log
logs

# 系统文件
.DS_Store
Thumbs.db
*.swp

# 临时文件
*.tmp
*.temp
*.bak
```

---

## 🛠️ 技术栈

| 技术 | 版本 | 说明 |
|:---|:---:|:---|
| Java | 21+ | 编程语言 |
| JavaFX | 21 | UI 框架 |
| Maven | 3.6+ | 构建工具 |
| AtlantaFX | 2.0.1 | UI 主题 |
| Ikonli | 12.3.1 | 图标库 |
| Jackson | 2.16.1 | JSON 处理 |

---

## 📁 项目结构

```
src/main/java/com/cleaner/
├── CleanerApplication.java    # 应用入口
├── config/
│   └── ConfigManager.java     # 配置管理
├── controller/
│   ├── MainController.java    # 主控制器
│   └── RuleController.java    # 规则控制器
├── deleter/
│   └── FileDeleter.java      # 文件删除器
├── matcher/
│   └── RuleMatcher.java      # 规则匹配器
├── model/
│   ├── DeleteRule.java       # 删除规则
│   ├── FileItem.java         # 文件项
│   ├── FolderConfig.java     # 文件夹配置
│   ├── KeepRule.java         # 保留规则
│   └── Rule.java             # 规则基类
├── scanner/
│   └── FileScanner.java      # 文件扫描器
└── util/
    ├── LogoGenerator.java    # Logo 生成器
    └── PathUtils.java        # 路径工具

src/main/resources/
├── css/style.css             # 样式文件
├── fxml/main.fxml            # UI 布局
└── icon.icns                 # 应用图标
```

---

## 📝 开发说明

### 编译

```bash
mvn compile
```

### 运行

```bash
mvn javafx:run
```

### 打包

```bash
# 创建自定义 JRE
mvn javafx:jlink

# 打包安装程序
mvn jfx:package
```

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

---

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/visarks">visarks</a>
</p>