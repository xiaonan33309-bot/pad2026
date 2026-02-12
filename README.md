# PAD 2026 - 安卓可直接导入运行示例

这是一个可直接导入 Android Studio（或 PAD 的 Android 工程能力）运行的完整 Kotlin 项目，包含：

- Jetpack Compose UI
- Room 本地数据库持久化
- MVVM 架构（ViewModel + Repository + DAO）
- 任务新增、搜索、完成切换、删除

## 目录结构

- `app/src/main/java/com/example/pad/MainActivity.kt`：应用入口
- `app/src/main/java/com/example/pad/ui/`：界面和 ViewModel
- `app/src/main/java/com/example/pad/data/`：Room 数据层

## 运行方式

1. 用 Android Studio 打开仓库根目录。
2. 等待 Gradle 同步完成。
3. 运行 `app` 模块到模拟器或真机（Android 7.0+）。

## 功能说明

- 新增任务：输入“任务标题”和“任务描述”后点击“新增任务”。
- 搜索任务：在“搜索任务”输入关键词，按标题或描述模糊匹配。
- 勾选任务：切换任务完成状态。
- 删除任务：点击垃圾桶图标删除。

## 技术栈

- Kotlin 1.9.24
- Android Gradle Plugin 8.5.2
- Compose BOM 2024.06.00
- Room 2.6.1
