# pad2026 车载 PAD 广告播放 App（Android）

基于你提供的 V1 设计文档实现的开发版代码，目标：
- 本地全屏循环播放广告视频
- 支持绑定、同步、断点续传下载、播放日志回传
- 弱网/断网可持续运行

## 已实现模块（代码级）

1. **启动与绑定流程**
   - `MainActivity` 启动后检查是否已绑定 `deviceId`
   - 未绑定进入 `BindingActivity` 输入 `accountId / bindCode`
   - 绑定成功后持久化保存并触发一次手动同步

2. **播放模块（霸屏）**
   - `PlayerActivity` 使用 Media3 ExoPlayer 全屏播放
   - 隐藏系统 UI（状态栏/导航栏）
   - 播放列表按 `order` 升序循环
   - 单条播放失败自动跳过并记录失败日志

3. **播放清单与同步机制**
   - `SyncWorker`（WorkManager）每 6 小时触发一次
   - 支持手动同步触发（绑定成功/播放后触发）
   - 同步流程：回传播放日志 → 下载任务推进 → 拉取清单 → 标记删除 → 上报设备状态
   - 清单版本按 `playlistVersion` 判断更新

4. **下载管理**
   - `VideoDownloadManager` 使用 HTTP Range 进行断点续传
   - 下载临时目录：`files/tmp`
   - 可播目录：`files/videos`
   - 下载后校验 `size + checksum(sha256)`，不通过则失败重试

5. **本地数据库（Room）**
   - `device`：绑定信息
   - `ads`：广告清单与本地状态
   - `download_tasks`：下载队列与重试状态
   - `play_logs`：播放记录与上传 ACK 状态

6. **删除策略**
   - 不在最新清单中的广告标记 `pendingDelete`
   - 若该广告仍存在未上传播放记录，则暂不删除
   - 全部 ACK 后删除文件和 DB 记录

7. **设备状态上报**
   - 上报 `deviceId/appVersion/networkType/freeStorage/latestPlayAt/错误标记`

8. **开机自启与保活**
   - `BOOT_COMPLETED` 后自动注册周期同步并拉起前台服务
   - 前台服务 `PlaybackForegroundService` 提升存活能力

## 关键目录

- `app/src/main/java/com/pad2026/app/data/local`：Room 表结构与 DAO
- `app/src/main/java/com/pad2026/app/data/remote`：接口与数据模型
- `app/src/main/java/com/pad2026/app/data/repo`：业务仓库层
- `app/src/main/java/com/pad2026/app/download`：断点下载
- `app/src/main/java/com/pad2026/app/sync`：同步调度与 Worker
- `app/src/main/java/com/pad2026/app/ui`：绑定与播放器页面

## 运行说明

1. Android Studio 打开项目。
2. 配置服务端地址：`ApiClient.BASE_URL`。
3. 运行 `app` 到车载 PAD 设备（Android 9+）。
4. 首次输入 `accountId / bindCode` 完成绑定。

## 说明

当前环境对 Maven 仓库访问返回 403，无法在此容器完成依赖下载与构建验证；代码结构已按 V1 文档实现。
