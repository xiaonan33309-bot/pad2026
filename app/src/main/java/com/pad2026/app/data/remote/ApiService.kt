package com.pad2026.app.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("/api/pad/bind")
    suspend fun bind(@Body request: BindRequest): BindResponse

    @GET("/api/pad/playlist")
    suspend fun getPlaylist(
        @Query("deviceId") deviceId: String,
        @Query("playlistVersion") playlistVersion: Long?
    ): PlaylistResponse

    @POST("/api/pad/playlog/upload")
    suspend fun uploadPlayLogs(@Body request: PlayLogUploadRequest): PlayLogUploadResponse

    @POST("/api/pad/device/status")
    suspend fun uploadDeviceStatus(@Body request: DeviceStatusRequest)
}
