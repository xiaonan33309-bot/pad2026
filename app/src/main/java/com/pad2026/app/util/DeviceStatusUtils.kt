package com.pad2026.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import android.os.StatFs

object DeviceStatusUtils {
    fun networkType(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "OFFLINE"
        val cap = cm.getNetworkCapabilities(network) ?: return "OFFLINE"
        return when {
            cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            cap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            else -> "OTHER"
        }
    }

    fun freeStorageBytes(): Long {
        val stat = StatFs(Environment.getDataDirectory().absolutePath)
        return stat.availableBytes
    }
}
