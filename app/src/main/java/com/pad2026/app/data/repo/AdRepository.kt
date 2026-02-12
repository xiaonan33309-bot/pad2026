package com.pad2026.app.data.repo

import com.pad2026.app.data.local.AdDao
import com.pad2026.app.data.local.AdEntity
import com.pad2026.app.data.local.DownloadTaskDao
import com.pad2026.app.data.local.DownloadTaskEntity
import com.pad2026.app.data.remote.ApiService
import com.pad2026.app.download.VideoDownloadManager
import com.pad2026.app.util.HashUtils
import com.pad2026.app.util.TimeUtils
import java.io.File

class AdRepository(
    private val adDao: AdDao,
    private val downloadTaskDao: DownloadTaskDao,
    private val api: ApiService,
    private val downloader: VideoDownloadManager
) {
    suspend fun getPlayable(nowEpochSec: Long): List<AdEntity> = adDao.getPlayable(nowEpochSec)

    suspend fun syncPlaylist(deviceId: String, currentVersion: Long?) {
        val remote = api.getPlaylist(deviceId, currentVersion)
        if (currentVersion != null && remote.playlistVersion <= currentVersion) return

        val localAds = adDao.getAll().associateBy { it.adId }
        val remoteIds = remote.items.map { it.adId }.toSet()

        val entities = remote.items.map { item ->
            val existing = localAds[item.adId]
            AdEntity(
                adId = item.adId,
                playlistId = remote.playlistId,
                playlistVersion = remote.playlistVersion,
                url = item.url,
                size = item.size,
                checksum = item.checksum,
                playOrder = item.order,
                validFromEpochSec = TimeUtils.parseIsoToEpochSec(item.validFrom),
                validToEpochSec = TimeUtils.parseIsoToEpochSec(item.validTo),
                localVideoPath = existing?.localVideoPath,
                downloaded = existing?.downloaded == true,
                verified = existing?.verified == true,
                enabled = true,
                pendingDelete = false
            )
        }
        adDao.upsertAll(entities)

        val staleIds = localAds.keys.filter { it !in remoteIds }
        if (staleIds.isNotEmpty()) adDao.markPendingDelete(staleIds)

        queueDownloadsForMissing(entities)
    }

    private suspend fun queueDownloadsForMissing(ads: List<AdEntity>) {
        ads.filter { !it.downloaded || !it.verified || it.localVideoPath == null }.forEach { ad ->
            val tmpPath = downloader.tmpPath(ad.adId)
            val targetPath = downloader.videoPath(ad.adId)
            downloadTaskDao.upsert(
                DownloadTaskEntity(
                    adId = ad.adId,
                    url = ad.url,
                    tmpPath = tmpPath,
                    targetPath = targetPath,
                    expectedSize = ad.size,
                    checksum = ad.checksum,
                    downloadedBytes = 0,
                    retryCount = 0,
                    status = "QUEUED",
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun runPendingDownloads(maxTasks: Int = 2): Boolean {
        val tasks = downloadTaskDao.getPendingTasks().take(maxTasks)
        var hasDownloadError = false
        tasks.forEach { task ->
            downloadTaskDao.upsert(task.copy(status = "DOWNLOADING", updatedAt = System.currentTimeMillis()))
            val result = downloader.downloadWithResume(task.url, task.tmpPath, task.targetPath)
            if (!result.success) {
                hasDownloadError = true
                downloadTaskDao.upsert(
                    task.copy(
                        downloadedBytes = result.downloadedBytes,
                        retryCount = task.retryCount + 1,
                        status = "FAILED",
                        updatedAt = System.currentTimeMillis()
                    )
                )
                return@forEach
            }

            val file = File(task.targetPath)
            val checksum = "sha256:${HashUtils.sha256(file)}"
            if (file.length() != task.expectedSize || checksum != task.checksum) {
                hasDownloadError = true
                file.delete()
                downloadTaskDao.upsert(
                    task.copy(
                        retryCount = task.retryCount + 1,
                        status = "FAILED",
                        updatedAt = System.currentTimeMillis()
                    )
                )
                return@forEach
            }

            val ad = adDao.getAll().firstOrNull { it.adId == task.adId } ?: return@forEach
            adDao.upsertAll(listOf(ad.copy(localVideoPath = task.targetPath, downloaded = true, verified = true)))
            downloadTaskDao.deleteByAdId(task.adId)
        }
        return hasDownloadError
    }

    suspend fun currentPlaylistVersion(): Long? = adDao.getAll().maxOfOrNull { it.playlistVersion }

    suspend fun processPendingDelete(removable: suspend (String) -> Boolean) {
        val candidates = adDao.getAll().filter { it.pendingDelete }
        candidates.forEach { ad ->
            if (removable(ad.adId)) {
                ad.localVideoPath?.let { File(it).delete() }
                adDao.deleteByAdId(ad.adId)
            }
        }
    }
}
