package com.pad2026.app.core

import android.content.Context
import com.pad2026.app.data.local.AppDatabase
import com.pad2026.app.data.remote.ApiClient
import com.pad2026.app.data.repo.AdRepository
import com.pad2026.app.data.repo.DeviceRepository
import com.pad2026.app.data.repo.PlayLogRepository
import com.pad2026.app.download.VideoDownloadManager

class AppContainer private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val db = AppDatabase.getInstance(appContext)
    private val api = ApiClient.apiService

    val videoDownloadManager = VideoDownloadManager(appContext)
    val deviceRepository = DeviceRepository(db.deviceDao(), api)
    val adRepository = AdRepository(db.adDao(), db.downloadTaskDao(), api, videoDownloadManager)
    val playLogRepository = PlayLogRepository(db.playLogDao(), api)

    companion object {
        @Volatile
        private var INSTANCE: AppContainer? = null

        fun from(context: Context): AppContainer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppContainer(context).also { INSTANCE = it }
            }
        }
    }
}
