package com.hapticks.app.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL

object UpdateChecker {

    private const val GITHUB_API_URL = "https://api.github.com/repos/hari161008/Ever-Haptics/releases/latest"

    data class UpdateInfo(
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String,
        val isUpdateAvailable: Boolean,
    )

    sealed class DownloadState {
        data class Progress(val percent: Int) : DownloadState()
        object Done : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    suspend fun checkForUpdate(currentVersion: String): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(GITHUB_API_URL).openConnection()
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            val response = connection.getInputStream().bufferedReader().readText()
            val json = JSONObject(response)
            val latestVersion = json.getString("tag_name").removePrefix("v")
            val releaseNotes = json.optString("body", "")
            val assets = json.getJSONArray("assets")
            var downloadUrl = ""
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.getString("name").endsWith(".apk")) {
                    downloadUrl = asset.getString("browser_download_url")
                    break
                }
            }
            Result.success(UpdateInfo(latestVersion, downloadUrl, releaseNotes, isNewerVersion(latestVersion, currentVersion)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadWithProgress(
        context: Context,
        downloadUrl: String,
        version: String,
        onState: (DownloadState) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val fileName = "EverHaptics-$version.apk"
        val apkFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)

        // If already downloaded, install directly without re-downloading
        if (apkFile.exists() && apkFile.length() > 0) {
            withContext(Dispatchers.Main) {
                onState(DownloadState.Progress(100))
                installApk(context, apkFile)
                onState(DownloadState.Done)
            }
            return@withContext
        }
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("Ever Haptics $version")
            setDescription("Downloading update…")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setMimeType("application/vnd.android.package-archive")
        }
        val downloadId = dm.enqueue(request)

        // Poll progress
        var running = true
        while (running) {
            delay(300)
            val cursor = dm.query(DownloadManager.Query().setFilterById(downloadId))
            if (cursor == null || !cursor.moveToFirst()) { cursor?.close(); break }
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            when (status) {
                DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED -> {
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val pct = if (total > 0) ((downloaded * 100L) / total).toInt().coerceIn(0, 99) else 0
                    withContext(Dispatchers.Main) { onState(DownloadState.Progress(pct)) }
                }
                DownloadManager.STATUS_SUCCESSFUL -> {
                    withContext(Dispatchers.Main) { onState(DownloadState.Progress(100)) }
                    delay(200)
                    val apk = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                    if (apk.exists()) withContext(Dispatchers.Main) { installApk(context, apk) }
                    withContext(Dispatchers.Main) { onState(DownloadState.Done) }
                    running = false
                }
                DownloadManager.STATUS_FAILED -> {
                    val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    withContext(Dispatchers.Main) { onState(DownloadState.Error("Download failed (code $reason)")) }
                    running = false
                }
                else -> {}
            }
            cursor.close()
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        })
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        return try {
            val r = remote.split(".").map { it.toInt() }
            val c = current.split(".").map { it.toInt() }
            for (i in 0 until maxOf(r.size, c.size)) {
                val diff = r.getOrElse(i) { 0 } - c.getOrElse(i) { 0 }
                if (diff != 0) return diff > 0
            }
            false
        } catch (_: Exception) { false }
    }
}
