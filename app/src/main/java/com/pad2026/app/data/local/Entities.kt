package com.pad2026.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device")
data class DeviceEntity(
    @PrimaryKey val id: Int = 1,
    val deviceId: String,
    val accountId: String,
    val bindCode: String,
    val boundAt: Long,
    val appVersion: String
)

@Entity(tableName = "ads")
data class AdEntity(
    @PrimaryKey val adId: String,
    val playlistId: String,
    val playlistVersion: Long,
    val url: String,
    val size: Long,
    val checksum: String,
    val playOrder: Int,
    val validFromEpochSec: Long,
    val validToEpochSec: Long,
    val localVideoPath: String?,
    val downloaded: Boolean,
    val verified: Boolean,
    val enabled: Boolean = true,
    val pendingDelete: Boolean = false
)

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey val adId: String,
    val url: String,
    val tmpPath: String,
    val targetPath: String,
    val expectedSize: Long,
    val checksum: String,
    val downloadedBytes: Long,
    val retryCount: Int,
    val status: String,
    val updatedAt: Long
)

@Entity(tableName = "play_logs")
data class PlayLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val adId: String,
    val playStartAt: Long,
    val playEndAt: Long,
    val playedDurationSec: Long,
    val completed: Boolean,
    val failReason: String?,
    val uploaded: Boolean = false
)
