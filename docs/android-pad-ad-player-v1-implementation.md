# 车载 PAD Android 广告播放系统（V1）开发实现方案

> 基于你提供的功能设计文档，整理为可直接落地的 Android 开发实现稿（偏工程实现与接口约束）。

## 1. 目标与边界

### 1.1 目标
- 在 Android 9+ 设备上实现**离线优先**的视频广告播放 App。
- 确保播放不中断、统计不丢失、同步不影响当前播放。
- 以 `playlistVersion` 驱动增量更新，避免重复下载。

### 1.2 非目标
- 不实现互动广告（点击、跳转、曝光热区）。
- 不实现在线播放（只播本地完整文件）。
- 不依赖蓝牙、摄像头能力。

---

## 2. 推荐技术栈

- 语言：Kotlin
- 架构：Clean Architecture + MVVM
- 本地数据库：Room
- 后台任务：WorkManager（周期同步 + 网络约束）
- 前台保活：Foreground Service（播放与下载状态）
- 播放器：ExoPlayer（AndroidX Media3）
- 网络：Retrofit + OkHttp
- JSON：Kotlinx Serialization 或 Moshi
- 依赖注入：Hilt（可选但推荐）

---

## 3. 模块划分

- `app`：Application、Activity、Service、DI、权限与系统集成
- `core-network`：API 定义、DTO、拦截器、重试策略
- `core-db`：Room entities/dao/migrations
- `feature-bind`：绑定页（accountId / 绑定码）
- `feature-player`：播放控制、可播列表构建、失败跳过
- `feature-sync`：同步编排（上报 -> 拉清单 -> 比对）
- `feature-download`：断点续传下载与校验
- `feature-maintenance`：维护页（手动同步、状态查看）

---

## 4. 生命周期与启动流程

### 4.1 开机自启
- 通过 `BOOT_COMPLETED` 广播接收器拉起 App 主流程。
- 冷启动后立即检查本地是否存在已绑定 `deviceId`。
- 强约束：App 必须跟随系统启动自动进入主流程，不依赖人工点击图标启动。

### 4.2 启动状态机
1. `Unbound`：进入绑定界面。
2. `Bound-NoPlaylist`：显示占位页并立即触发首次同步。
3. `Bound-Playable`：进入全屏循环播放。

### 4.3 启动后霸屏（Kiosk）
- 进入播放页后立即切换为沉浸式全屏，隐藏状态栏与导航栏。
- 建议启用 **Lock Task Mode**（设备所有者场景）或厂商提供的 kiosk 能力，禁止切到其他应用。
- 屏蔽 Home/Recent（在设备策略允许前提下）与返回误触退出。
- 仅允许通过维护入口（口令/后台指令）临时退出霸屏。

### 4.4 绑定成功行为
- 持久化 `deviceId/accountId/bindAt`。
- 触发一次 `OneTimeWorkRequest` 同步任务。
- 同步成功后刷新播放队列。

---

## 5. 本地数据模型（Room）

> 以下字段为建议最小集合，可直接用于建表。

### 5.1 DeviceTable
- `deviceId` (PK)
- `accountId`
- `bindCode`
- `isBound`
- `lastSyncAt`
- `serverTimeOffsetMs`（serverTime 与本地时间差）
- `appVersion`

### 5.2 AdTable
- `adId` (PK)
- `playlistId`
- `playlistVersion`
- `url`
- `size`
- `checksum`
- `order`
- `validFrom`
- `validTo`
- `status`（ENABLED/DISABLED/TO_DELETE）
- `localFilePath`
- `downloadState`（NONE/DOWNLOADING/COMPLETED/FAILED）
- `verifyState`（UNKNOWN/PASS/FAIL）
- `updatedAt`

### 5.3 DownloadTaskTable
- `taskId` (PK)
- `adId`
- `url`
- `tempPath`
- `targetPath`
- `downloadedBytes`
- `totalBytes`
- `etag`（可选）
- `state`（PENDING/RUNNING/RETRY/FAILED/DONE）
- `retryCount`
- `lastError`

### 5.4 PlayLogTable
- `logId` (PK, 自增)
- `adId`
- `playStartAt`
- `playEndAt`
- `playedDurationSec`
- `completed`
- `failReason`
- `uploadState`（PENDING/UPLOADING/ACKED）
- `uploadBatchId`（可选）

### 5.5 索引建议
- `AdTable(playlistVersion)`
- `AdTable(order, validFrom, validTo, downloadState, verifyState, status)`
- `PlayLogTable(uploadState, playEndAt)`

---

## 6. 播放清单协议与处理规则

### 6.1 协议落库原则
- 拉到 Playlist 后，先校验字段完整性（`adId/url/size/checksum/order/validFrom/validTo`）。
- 使用事务写入：`playlistVersion` 新才覆盖。
- `serverTime` 用于计算并更新 `serverTimeOffsetMs`。

### 6.2 强约束实现
1. 仅当远端 `playlistVersion > localVersion` 才进入比对流程。
2. 同 `playlistId` 下版本必须递增，异常版本记录错误并拒绝覆盖。
3. 版本不变时禁止重复创建下载任务。
4. 播放排序只看 `order`，不要使用插入时间。
5. 不在新 `items` 的本地广告标记 `TO_DELETE`，走延迟删除。

### 6.3 可播筛选 SQL 语义
可播条件：
- `downloadState = COMPLETED`
- `verifyState = PASS`
- `status = ENABLED`
- `now between validFrom and validTo`
按 `order asc` 返回。

---

## 7. 同步机制（不打断播放）

### 7.1 触发器
- 网络从无到有（`ConnectivityManager` 回调）
- 每 6 小时周期任务（WorkManager PeriodicWork）
- 维护页手动触发（OneTimeWork）

### 7.2 编排顺序
1. 上传播放日志（增量，`PENDING` -> `ACKED`）
2. 上传设备状态
3. 拉取最新 Playlist
4. 比对并生成下载/替换/待删除计划
5. 下载队列按并发 1~2 执行

### 7.3 幂等要求
- 日志上传接口建议支持 `batchId` 幂等。
- Playlist 拉取可带 `If-None-Match` 或 `currentVersion`。
- 同步 worker 采用唯一任务名，避免并发重复跑。

---

## 8. 下载管理与校验

### 8.1 断点续传
- 临时文件写入 `/tmp/`。
- 请求头 `Range: bytes=<downloadedBytes>-`。
- 服务端返回 `206` 则续传，`200` 则全量重下并重置偏移。

### 8.2 完成后校验
1. 比较文件 `size`。
2. 计算 `sha256` 对比 `checksum`。
3. 通过后原子移动到 `/videos/` 并更新 `verifyState = PASS`。
4. 失败则删临时文件，`retryCount +1`，指数退避重试。

### 8.3 并发与资源控制
- 同时下载数建议默认 1，最大 2。
- 低存储阈值（如 <1GB）先执行可删除广告清理，再继续下载。

---

## 9. 播放器行为细节

### 9.1 全屏与防误触
- `WindowInsetsController` 隐藏状态栏/导航栏。
- 沉浸式 + 锁定屏幕方向为横屏。
- 屏蔽返回键退出（仅维护口令页可退出）。

### 9.2 播放循环
- 从可播列表构建 `ConcatenatingMediaSource`（或逐条 setMediaItem）。
- 每条播放开始写 `playStartAt`。
- 正常结束写 `completed=true`；异常写 `completed=false + failReason` 并跳过下一条。

### 9.3 无可播素材兜底
- 展示“等待素材同步”占位页。
- 后台保持同步与下载任务，恢复后自动切回播放。

---

## 10. 统计上报可靠性

### 10.1 本地先落库
- 任何播放事件先写 `PlayLogTable`，再异步上传。
- 上传中应用重启后可继续从 `PENDING`/`UPLOADING` 恢复。

### 10.2 ACK 机制
- 服务端返回已接收 `logId` 或 `batchId`。
- 本地只在收到 ACK 后标记 `ACKED`。
- 未 ACK 数据禁止物理删除对应广告文件。

---

## 11. 删除策略落地

1. 新清单不存在的广告设为 `TO_DELETE`。
2. 查询该广告是否存在 `uploadState != ACKED` 播放记录。
3. 若有未回传记录：仅保留文件，不删。
4. 若全部 ACK：删除 `/videos/<adId>.mp4`，并清理数据库记录。

---

## 12. API 建议（示意）

### 12.1 绑定
`POST /api/pad/bind`

请求：
```json
{ "accountId": "a001", "bindCode": "123456" }
```
响应：
```json
{ "deviceId": "dev_xxx", "bindAt": "2026-01-10T12:00:00+09:00" }
```

### 12.2 拉清单
`GET /api/pad/{deviceId}/playlist?currentVersion=12`

- 无更新可返回 `304` 或 `{ "notModified": true }`。

### 12.3 上报播放日志
`POST /api/pad/{deviceId}/playlogs:batch`

请求包含数组：`adId/playStartAt/playEndAt/playedDurationSec/completed/failReason`。

### 12.4 上报设备状态
`POST /api/pad/{deviceId}/status`

字段：`appVersion/networkType/freeStorage/lastPlayAt/errorFlags`。

---

## 13. 异常场景与恢复

- **断网**：继续本地循环播放；同步任务等待网络。
- **下载失败**：记录错误码与重试次数；超过阈值后延迟重试。
- **播放失败**：当前条目记日志并跳过，不阻塞队列。
- **存储不足**：先清理可删广告，再尝试下载；仍不足则上报错误标记。
- **时间漂移**：使用 `serverTimeOffsetMs` 计算“业务当前时间”，避免本地时钟不准导致错播。

---

## 14. 验收测试清单（建议 QA 用例）

1. **离线 24h 回归**：断网下连续播放 24h 不崩溃。
2. **续传验证**：下载中断后重连，确认从断点继续。
3. **版本不变**：多次同步不重复下载同一文件。
4. **同步无感**：同步期间当前视频不中断。
5. **日志可靠**：断网期间累计日志，联网后全部 ACK。
6. **延迟删除**：有未 ACK 日志的广告不得删除文件。
7. **有效期控制**：过期素材自动下架，不再进入可播列表。

---

## 15. V1 里程碑建议

- **M1（1 周）**：绑定 + 本地数据库 + 基础播放器
- **M2（1 周）**：Playlist 同步 + 比对 + 下载器（含校验）
- **M3（1 周）**：播放日志上报 + 设备状态上报 + 删除策略
- **M4（1 周）**：稳定性优化 + 长稳测试 + 线上灰度
