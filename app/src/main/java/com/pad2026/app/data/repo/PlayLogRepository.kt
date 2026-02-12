package com.pad2026.app.data.repo

import com.pad2026.app.data.local.PlayLogDao
import com.pad2026.app.data.local.PlayLogEntity
import com.pad2026.app.data.remote.ApiService
import com.pad2026.app.data.remote.PlayLogPayload
import com.pad2026.app.data.remote.PlayLogUploadRequest

class PlayLogRepository(
    private val playLogDao: PlayLogDao,
    private val api: ApiService
) {
    suspend fun insert(log: PlayLogEntity) = playLogDao.insert(log)

    suspend fun uploadPending(deviceId: String) {
        val pending = playLogDao.getPending()
        if (pending.isEmpty()) return
        val payload = pending.map {
            PlayLogPayload(
                id = it.id,
                adId = it.adId,
                playStartAt = it.playStartAt,
                playEndAt = it.playEndAt,
                playedDurationSec = it.playedDurationSec,
                completed = it.completed,
                failReason = it.failReason
            )
        }
        val ack = api.uploadPlayLogs(PlayLogUploadRequest(deviceId, payload)).ackIds
        if (ack.isNotEmpty()) playLogDao.markUploaded(ack)
    }

    suspend fun countPendingByAd(adId: String): Int = playLogDao.countPendingByAd(adId)
    suspend fun latestPlayAt(): Long? = playLogDao.latestPlayEndAt()
}
