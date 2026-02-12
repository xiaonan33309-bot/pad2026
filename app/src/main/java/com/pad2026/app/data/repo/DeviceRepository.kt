package com.pad2026.app.data.repo

import com.pad2026.app.data.local.DeviceDao
import com.pad2026.app.data.local.DeviceEntity
import com.pad2026.app.data.remote.ApiService
import com.pad2026.app.data.remote.BindRequest

class DeviceRepository(
    private val deviceDao: DeviceDao,
    private val api: ApiService
) {
    suspend fun bind(accountId: String, bindCode: String, appVersion: String): String {
        val response = api.bind(BindRequest(accountId, bindCode))
        deviceDao.upsert(
            DeviceEntity(
                deviceId = response.deviceId,
                accountId = accountId,
                bindCode = bindCode,
                boundAt = System.currentTimeMillis(),
                appVersion = appVersion
            )
        )
        return response.deviceId
    }

    suspend fun getDevice() = deviceDao.getDevice()
}
