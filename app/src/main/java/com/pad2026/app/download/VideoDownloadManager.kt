package com.pad2026.app.download

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile

class VideoDownloadManager(context: Context) {
    private val okHttp = OkHttpClient()
    private val root = context.filesDir
    private val videosDir = File(root, "videos").apply { mkdirs() }
    private val tmpDir = File(root, "tmp").apply { mkdirs() }

    data class DownloadResult(
        val success: Boolean,
        val finalPath: String,
        val downloadedBytes: Long,
        val error: String?
    )

    fun videoPath(adId: String): String = File(videosDir, "$adId.mp4").absolutePath
    fun tmpPath(adId: String): String = File(tmpDir, "$adId.part").absolutePath

    fun downloadWithResume(url: String, tmpPath: String, targetPath: String): DownloadResult {
        val tmpFile = File(tmpPath)
        tmpFile.parentFile?.mkdirs()
        val existing = if (tmpFile.exists()) tmpFile.length() else 0L

        val request = Request.Builder()
            .url(url)
            .addHeader("Range", "bytes=$existing-")
            .build()

        return try {
            okHttp.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return DownloadResult(false, targetPath, existing, "http_${resp.code}")
                }
                val body = resp.body ?: return DownloadResult(false, targetPath, existing, "empty_body")
                RandomAccessFile(tmpFile, "rw").use { raf ->
                    raf.seek(existing)
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            raf.write(buffer, 0, read)
                        }
                    }
                }
                val target = File(targetPath)
                target.parentFile?.mkdirs()
                tmpFile.copyTo(target, overwrite = true)
                tmpFile.delete()
                DownloadResult(true, target.absolutePath, target.length(), null)
            }
        } catch (e: Exception) {
            DownloadResult(false, targetPath, existing, e.message)
        }
    }
}
