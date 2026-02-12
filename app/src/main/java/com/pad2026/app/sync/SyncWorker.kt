package com.pad2026.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pad2026.app.core.AppContainer
import com.pad2026.app.data.remote.DeviceStatusRequest
import com.pad2026.app.util.DeviceStatusUtils

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val container = AppContainer.from(applicationContext)
        val device = container.deviceRepository.getDevice() ?: return Result.success()

        return try {
            container.playLogRepository.uploadPending(device.deviceId)

            val hasDownloadError = container.adRepository.runPendingDownloads()
            val freeStorage = DeviceStatusUtils.freeStorageBytes()

            container.adRepository.syncPlaylist(device.deviceId, container.adRepository.currentPlaylistVersion())
            container.adRepository.processPendingDelete { adId ->
                container.playLogRepository.countPendingByAd(adId) == 0
            }

            com.pad2026.app.data.remote.ApiClient.apiService.uploadDeviceStatus(
                DeviceStatusRequest(
                    deviceId = device.deviceId,
                    appVersion = device.appVersion,
                    networkType = DeviceStatusUtils.networkType(applicationContext),
                    freeStorageBytes = freeStorage,
                    latestPlayAt = container.playLogRepository.latestPlayAt(),
                    hasDownloadError = hasDownloadError,
                    hasStorageError = freeStorage < 512L * 1024L * 1024L
                )
            )
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
