package com.pad2026.app.data.remote

data class BindRequest(val accountId: String, val bindCode: String)
data class BindResponse(val deviceId: String)

data class PlaylistResponse(
    val playlistId: String,
    val playlistVersion: Long,
    val serverTime: String,
    val items: List<AdItemResponse>
)

data class AdItemResponse(
    val adId: String,
    val url: String,
    val size: Long,
    val checksum: String,
    val order: Int,
    val validFrom: String,
    val validTo: String
)

data class PlayLogPayload(
    val id: Long,
    val adId: String,
    val playStartAt: Long,
    val playEndAt: Long,
    val playedDurationSec: Long,
    val completed: Boolean,
    val failReason: String?
)

data class PlayLogUploadRequest(val deviceId: String, val records: List<PlayLogPayload>)
data class PlayLogUploadResponse(val ackIds: List<Long>)

data class DeviceStatusRequest(
    val deviceId: String,
    val appVersion: String,
    val networkType: String,
    val freeStorageBytes: Long,
    val latestPlayAt: Long?,
    val hasDownloadError: Boolean,
    val hasStorageError: Boolean
)
