package com.pad2026.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface DeviceDao {
    @Query("SELECT * FROM device WHERE id = 1")
    suspend fun getDevice(): DeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: DeviceEntity)
}

@Dao
interface AdDao {
    @Query("SELECT * FROM ads")
    suspend fun getAll(): List<AdEntity>

    @Query("SELECT * FROM ads WHERE downloaded = 1 AND verified = 1 AND enabled = 1 AND validFromEpochSec <= :now AND validToEpochSec >= :now ORDER BY playOrder ASC")
    suspend fun getPlayable(now: Long): List<AdEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<AdEntity>)

    @Query("UPDATE ads SET pendingDelete = 1 WHERE adId IN (:adIds)")
    suspend fun markPendingDelete(adIds: List<String>)

    @Query("DELETE FROM ads WHERE adId = :adId")
    suspend fun deleteByAdId(adId: String)
}

@Dao
interface DownloadTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: DownloadTaskEntity)

    @Query("SELECT * FROM download_tasks WHERE status IN ('QUEUED','DOWNLOADING','FAILED') ORDER BY updatedAt ASC")
    suspend fun getPendingTasks(): List<DownloadTaskEntity>

    @Query("DELETE FROM download_tasks WHERE adId = :adId")
    suspend fun deleteByAdId(adId: String)
}

@Dao
interface PlayLogDao {
    @Insert
    suspend fun insert(log: PlayLogEntity)

    @Query("SELECT * FROM play_logs WHERE uploaded = 0 ORDER BY id ASC LIMIT :limit")
    suspend fun getPending(limit: Int = 200): List<PlayLogEntity>

    @Query("UPDATE play_logs SET uploaded = 1 WHERE id IN (:ids)")
    suspend fun markUploaded(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM play_logs WHERE adId = :adId AND uploaded = 0")
    suspend fun countPendingByAd(adId: String): Int

    @Query("SELECT MAX(playEndAt) FROM play_logs")
    suspend fun latestPlayEndAt(): Long?
}
