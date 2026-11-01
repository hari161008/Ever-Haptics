package com.hapticks.app.ui.screens

import android.app.DownloadManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.hapticks.app.ui.components.PetalGroupPosition
import com.hapticks.app.ui.components.PetalGroupRow
import com.hapticks.app.ui.components.PetalGroupToggleRow
import com.hapticks.app.ui.components.PetalSection
import com.hapticks.app.ui.haptics.performHapticClick
import com.hapticks.app.ui.theme.PetalMaterialShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

// ─── Constants ─────────────────────────────────────────────────────────────

private const val GITHUB_API_RELEASES = "https://api.github.com/repos/hari161008/Ever-Haptics/releases/latest"
private const val GITHUB_API_RELEASES_LIST = "https://api.github.com/repos/hari161008/Ever-Haptics/releases"
private const val APK_FILE_NAME = "EverHaptics_update.apk"

// ─── Data & State Models ───────────────────────────────────────────────────

data class UpdateReleaseInfo(
    val tagName: String,
    val apkUrl: String?,
    val releaseNotes: String? = null,
    val publishedAt: String? = null,
)

private sealed class CheckState {
    object Idle : CheckState()
    object Checking : CheckState()
    data class Done(val latest: UpdateReleaseInfo?, val isNewer: Boolean) : CheckState()
    object Failed : CheckState()
}

private sealed class DownloadState {
    object Idle : DownloadState()
    data class Confirm(val release: UpdateReleaseInfo, val readyToInstall: Boolean) : DownloadState()
    data class Downloading(val release: UpdateReleaseInfo, val downloadId: Long, val progress: Float) : DownloadState()
    object Failed : DownloadState()
}

// ─── Network & Install Helpers ─────────────────────────────────────────────

private fun parseReleaseJson(json: JSONObject): UpdateReleaseInfo {
    val tag = json.optString("tag_name", "")
    val assets = json.optJSONArray("assets")
    var apkUrl: String? = null
    if (assets != null) {
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.optString("name", "").endsWith(".apk", ignoreCase = true)) {
                apkUrl = asset.optString("browser_download_url")
                break
            }
        }
    }
    val notes = json.optString("body", "").trim().ifBlank { null }
    val publishedAt = json.optString("published_at", "").ifBlank { null }
    return UpdateReleaseInfo(
        tagName = tag.trimStart('v', 'V'),
        apkUrl = apkUrl,
        releaseNotes = notes,
        publishedAt = publishedAt,
    )
}

private suspend fun fetchLatestRelease(apiUrl: String): UpdateReleaseInfo? = withContext(Dispatchers.IO) {
    try {
        val connection = URL(apiUrl).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        if (connection.responseCode != 200) return@withContext null
        val body = connection.inputStream.bufferedReader().readText()
        parseReleaseJson(JSONObject(body))
    } catch (_: Exception) { null }
}

private suspend fun fetchReleaseForVersion(apiListUrl: String, version: String): UpdateReleaseInfo? = withContext(Dispatchers.IO) {
    try {
        val connection = URL(apiListUrl).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        if (connection.responseCode != 200) return@withContext null
        val body = connection.inputStream.bufferedReader().readText()
        val array = JSONArray(body)
        val target = version.trimStart('v', 'V')
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val tag = obj.optString("tag_name", "").trimStart('v', 'V')
            if (tag == target) return@withContext parseReleaseJson(obj)
        }
        null
    } catch (_: Exception) { null }
}

private fun isNewerVersion(latest: String, current: String): Boolean {
    fun parts(v: String) = v.split(".").mapNotNull { it.toIntOrNull() }
    val l = parts(latest); val c = parts(current)
    val len = maxOf(l.size, c.size)
    for (i in 0 until len) {
        val lp = l.getOrElse(i) { 0 }; val cp = c.getOrElse(i) { 0 }
        if (lp > cp) return true; if (lp < cp) return false
    }
    return false
}

private fun getApkDestinationFile(): File =
    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), APK_FILE_NAME)

private fun enqueueApkDownload(context: Context, apkUrl: String): Long? {
    return try {
        val file = getApkDestinationFile()
        if (file.exists()) file.delete()

        val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
            setTitle("Ever Haptics Update")
            setDescription("Downloading latest version…")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, APK_FILE_NAME)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)
    } catch (_: Exception) { null }
}

private fun installApkAndScheduleDelete(context: Context, file: File) {
    try {
        val packageInstaller = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        FileInputStream(file).use { fis ->
            session.openWrite("package", 0, file.length()).use { os ->
                fis.copyTo(os)
                session.fsync(os)
            }
        }

        val installResultAction = "${context.packageName}.INSTALL_RESULT"

        val resultReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                try { context.unregisterReceiver(this) } catch (_: Exception) {}
                val status = intent?.getIntExtra(PackageInstaller.EXTRA_STATUS, -1)
                if (status == PackageInstaller.STATUS_SUCCESS) {
                    try { file.delete() } catch (_: Exception) {}
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(resultReceiver, IntentFilter(installResultAction), Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(resultReceiver, IntentFilter(installResultAction))
        }

        val intent = Intent(installResultAction)
        val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(context, sessionId, intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        } else {
            PendingIntent.getBroadcast(context, sessionId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        session.commit(pi.intentSender)
    } catch (e: Exception) {
        try { installApkLegacy(context, file) } catch (_: Exception) {
            Toast.makeText(context, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

private fun installApkLegacy(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
    }
    context.startActivity(intent)
    Thread { Thread.sleep(90_000); try { file.delete() } catch (_: Exception) {} }.start()
}

// ─── Updates Screen ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreen(
    currentVersion: String,
    autoCheckUpdatesEnabled: Boolean,
    onAutoCheckUpdatesChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var checkState by remember { mutableStateOf<CheckState>(CheckState.Idle) }
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var installedNotes by remember { mutableStateOf<String?>(null) }
    var showCompareSheet by remember { mutableStateOf(false) }

    fun triggerHaptic() {
        context.performHapticClick()
    }

    fun runCheck() {
        triggerHaptic()
        scope.launch {
            checkState = CheckState.Checking
            val release = fetchLatestRelease(GITHUB_API_RELEASES)
            checkState = when {
                release == null -> CheckState.Failed
                isNewerVersion(release.tagName, currentVersion) -> CheckState.Done(release, true)
                else -> CheckState.Done(release, false)
            }
        }
    }

    LaunchedEffect(Unit) {
        runCheck()
    }

    LaunchedEffect(Unit) {
        installedNotes = fetchReleaseForVersion(GITHUB_API_RELEASES_LIST, currentVersion)?.releaseNotes
    }

    // ── Download confirmation & progress flow ──
    when (val ds = downloadState) {
        is DownloadState.Confirm -> {
            PermissionToDownloadDialog(
                currentVersion = currentVersion,
                latestVersion = ds.release.tagName,
                readyToInstall = ds.readyToInstall,
                onConfirm = {
                    triggerHaptic()
                    if (ds.readyToInstall) {
                        downloadState = DownloadState.Idle
                        installApkAndScheduleDelete(context, getApkDestinationFile())
                    } else {
                        val url = ds.release.apkUrl
                        if (url != null) {
                            val id = enqueueApkDownload(context, url)
                            downloadState = if (id != null) DownloadState.Downloading(ds.release, id, 0f) else DownloadState.Failed
                        } else {
                            downloadState = DownloadState.Failed
                        }
                    }
                },
                onDismiss = { downloadState = DownloadState.Idle },
            )
        }
        is DownloadState.Downloading -> {
            LaunchedEffect(ds.downloadId) {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                while (true) {
                    delay(300)
                    val query = DownloadManager.Query().setFilterById(ds.downloadId)
                    val cursor = dm.query(query)
                    if (!cursor.moveToFirst()) { cursor.close(); break }
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    cursor.close()
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            downloadState = DownloadState.Idle
                            installApkAndScheduleDelete(context, getApkDestinationFile())
                            break
                        }
                        DownloadManager.STATUS_FAILED -> {
                            downloadState = DownloadState.Failed
                            break
                        }
                        else -> {
                            val p = if (total > 0L) (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                            downloadState = ds.copy(progress = p)
                        }
                    }
                }
            }
            DownloadingDialog(latestVersion = ds.release.tagName, progress = ds.progress)
        }
        is DownloadState.Failed -> {
            AlertDialog(
                onDismissRequest = { downloadState = DownloadState.Idle },
                shape = RoundedCornerShape(28.dp),
                icon = {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
                        modifier = Modifier.size(48.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                },
                title = { Text("Download Failed", fontWeight = FontWeight.Bold) },
                text = { Text("Something went wrong while downloading the update. Please check your internet connection and try again.") },
                confirmButton = {
                    Button(
                        onClick = { downloadState = DownloadState.Idle },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Text("OK")
                    }
                },
            )
        }
        else -> {}
    }

    if (showCompareSheet) {
        val latestForCompare = (checkState as? CheckState.Done)?.latest
        CompareReleaseNotesSheet(
            installedVersion = currentVersion,
            installedNotes = installedNotes,
            latestVersion = latestForCompare?.tagName,
            latestNotes = latestForCompare?.releaseNotes,
            onDismiss = { showCompareSheet = false },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Updates", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Unified Top Hero Card ──
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shadowElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ExpressiveUpdateHeroCard(checkState = checkState, currentVersion = currentVersion)
                    VersionStatsRow(checkState = checkState, currentVersion = currentVersion)
                    ExpressiveActionArea(
                        checkState = checkState,
                        onCheckAgain = { runCheck() },
                        onUpdateClick = { latest ->
                            triggerHaptic()
                            val apkFile = getApkDestinationFile()
                            val readyToInstall = apkFile.exists() && apkFile.length() > 0L
                            downloadState = DownloadState.Confirm(latest, readyToInstall)
                        },
                    )
                }
            }

            // ── What's New ──
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "What's New",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp),
                )
                ExpressiveReleaseNotesCard(
                    checkState = checkState,
                    installedNotes = installedNotes,
                    currentVersion = currentVersion,
                    onCompareClick = { showCompareSheet = true },
                )
            }

            // ── Compare Release Notes ──
            PetalSection(title = "Version Details") {
                row { position ->
                    PetalGroupRow(
                        icon = Icons.Outlined.Difference,
                        title = "Compare Release Notes",
                        subtitle = "Compare installed v$currentVersion with the latest release",
                        position = position,
                        badgeShape = PetalMaterialShapes.Clover4Leaf,
                        onClick = { showCompareSheet = true },
                    )
                }
            }

            // ── Options ──
            PetalSection(title = "Options") {
                row { position ->
                    PetalGroupToggleRow(
                        icon = Icons.Rounded.Autorenew,
                        title = "Auto Check For Updates",
                        subtitle = "Automatically check for new releases when app launches",
                        checked = autoCheckUpdatesEnabled,
                        onCheckedChange = onAutoCheckUpdatesChange,
                        position = position,
                        badgeShape = PetalMaterialShapes.Sunny,
                    )
                }
            }
        }
    }
}

// ─── Hero Card ─────────────────────────────────────────────────────────────

@Composable
private fun ExpressiveUpdateHeroCard(checkState: CheckState, currentVersion: String) {
    val containerColor = when {
        checkState is CheckState.Done -> if (checkState.isNewer) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.secondaryContainer
        checkState is CheckState.Failed -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val (titleColor, subtitleColor, tagColor) = when {
        checkState is CheckState.Done -> if (checkState.isNewer) {
            Triple(
                MaterialTheme.colorScheme.onPrimaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                MaterialTheme.colorScheme.primary,
            )
        } else {
            Triple(
                MaterialTheme.colorScheme.onSecondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                MaterialTheme.colorScheme.secondary,
            )
        }
        checkState is CheckState.Failed -> Triple(
            MaterialTheme.colorScheme.onErrorContainer,
            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
            MaterialTheme.colorScheme.error,
        )
        else -> Triple(
            MaterialTheme.colorScheme.onPrimaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
            MaterialTheme.colorScheme.primary,
        )
    }

    val animatedContainerColor by animateColorAsState(
        targetValue = containerColor,
        animationSpec = tween(400),
        label = "heroContainerColor",
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = animatedContainerColor,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HeroStatusIcon(checkState = checkState)

                AnimatedContent(
                    targetState = checkState,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "statusBadge",
                ) { state ->
                    when (state) {
                        is CheckState.Checking -> ExpressiveStatusChip(
                            text = "Checking…",
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            pulsing = true,
                        )
                        is CheckState.Done -> if (state.isNewer && state.latest != null) {
                            ExpressiveStatusChip(
                                text = "New v${state.latest.tagName}",
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            ExpressiveStatusChip(
                                text = "Latest Build",
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                            )
                        }
                        is CheckState.Failed -> ExpressiveStatusChip(
                            text = "Error",
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        )
                        else -> Spacer(Modifier.height(0.dp))
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "EVER HAPTICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = tagColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )

                AnimatedContent(
                    targetState = checkState,
                    transitionSpec = {
                        (fadeIn(tween(250)) + slideInVertically { it / 2 }) togetherWith
                            (fadeOut(tween(150)) + slideOutVertically { -it / 2 })
                    },
                    label = "heroTitle",
                ) { state ->
                    val (title, subtitle) = when (state) {
                        is CheckState.Checking -> "Checking for updates…" to "Connecting to release repository"
                        is CheckState.Done -> if (state.isNewer && state.latest != null) {
                            "Update Available!" to "Version v${state.latest.tagName} is ready to download"
                        } else {
                            "You're Up to Date" to "Ever Haptics v$currentVersion is the latest version"
                        }
                        is CheckState.Failed -> "Update Check Failed" to "Could not connect to release server"
                        else -> "Ever Haptics Updates" to "Current installed build v$currentVersion"
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = titleColor,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = subtitleColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStatusIcon(checkState: CheckState) {
    val infinite = rememberInfiniteTransition(label = "heroIconAnim")
    val spin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
        label = "heroSpin",
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "heroPulse",
    )

    val (icon, iconBgColor, iconTintColor) = when {
        checkState is CheckState.Checking -> Triple(
            Icons.Rounded.SystemUpdate,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.primary,
        )
        checkState is CheckState.Done -> if (checkState.isNewer) {
            Triple(
                Icons.Rounded.NewReleases,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                MaterialTheme.colorScheme.primary,
            )
        } else {
            Triple(
                Icons.Rounded.Verified,
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.20f),
                MaterialTheme.colorScheme.secondary,
            )
        }
        checkState is CheckState.Failed -> Triple(
            Icons.Rounded.CloudOff,
            MaterialTheme.colorScheme.error.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.error,
        )
        else -> Triple(
            Icons.Rounded.SystemUpdate,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.primary,
        )
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = iconBgColor,
        modifier = Modifier
            .size(64.dp)
            .scale(if (checkState is CheckState.Done && checkState.isNewer) pulse else 1f),
        shadowElevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = checkState,
                transitionSpec = {
                    (fadeIn(tween(250)) + scaleIn(initialScale = 0.6f)) togetherWith
                        (fadeOut(tween(150)) + scaleOut(targetScale = 0.6f))
                },
                label = "heroIconGlyph",
            ) { state ->
                val modifier = if (state is CheckState.Checking) Modifier.rotate(spin) else Modifier
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = modifier.size(30.dp),
                )
            }
        }
    }
}

@Composable
private fun ExpressiveStatusChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    pulsing: Boolean = false,
) {
    val infinite = rememberInfiniteTransition(label = "statusChipPulse")
    val alphaAnim by infinite.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "chipAlpha",
    )

    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        modifier = Modifier.alpha(if (pulsing) alphaAnim else 1f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

// ─── Version Stats Row ─────────────────────────────────────────────────────

@Composable
private fun VersionStatsRow(checkState: CheckState, currentVersion: String) {
    val latestVersionText = when (checkState) {
        is CheckState.Done -> checkState.latest?.tagName?.let { "v$it" } ?: "v$currentVersion"
        is CheckState.Checking -> "Checking…"
        is CheckState.Failed -> "Unavailable"
        else -> "v$currentVersion"
    }

    val isNewer = (checkState as? CheckState.Done)?.isNewer == true

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ExpressiveStatCard(
            label = "Installed",
            value = "v$currentVersion",
            icon = Icons.Outlined.PhoneAndroid,
            modifier = Modifier.weight(1f),
            iconTint = MaterialTheme.colorScheme.primary,
            containerBgColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )

        ExpressiveStatCard(
            label = "Latest Release",
            value = latestVersionText,
            icon = if (isNewer) Icons.Outlined.CloudDownload else Icons.Outlined.CheckCircle,
            modifier = Modifier.weight(1f),
            iconTint = MaterialTheme.colorScheme.primary,
            highlighted = isNewer,
            containerBgColor = if (isNewer) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }
}

@Composable
private fun ExpressiveStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    highlighted: Boolean = false,
    containerBgColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    val textColor = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val subtextColor = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.80f) else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerBgColor,
        shadowElevation = 0.dp,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconTint.copy(alpha = 0.16f),
                modifier = Modifier.size(38.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = subtextColor,
                    maxLines = 1,
                )
            }
        }
    }
}

// ─── Expressive Action Button Area ─────────────────────────────────────────

@Composable
private fun ExpressiveActionArea(
    checkState: CheckState,
    onCheckAgain: () -> Unit,
    onUpdateClick: (UpdateReleaseInfo) -> Unit,
) {
    AnimatedContent(
        targetState = checkState,
        transitionSpec = {
            (fadeIn(tween(250)) + slideInVertically { it / 3 }) togetherWith
                (fadeOut(tween(140)) + slideOutVertically { -it / 3 })
        },
        label = "actionArea",
    ) { state ->
        when {
            state is CheckState.Done && state.isNewer && state.latest != null -> {
                Button(
                    onClick = { onUpdateClick(state.latest) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Download & Install v${state.latest.tagName}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            state is CheckState.Failed -> {
                FilledTonalButton(
                    onClick = onCheckAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Try Again", fontWeight = FontWeight.Bold)
                }
            }

            state is CheckState.Checking || state == CheckState.Idle -> {
                FilledTonalButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Checking for updates…", fontWeight = FontWeight.Medium)
                }
            }

            else -> {
                FilledTonalButton(
                    onClick = onCheckAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Check for Updates", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─── Expressive Release Notes Card ─────────────────────────────────────────

@Composable
private fun ExpressiveReleaseNotesCard(
    checkState: CheckState,
    installedNotes: String?,
    currentVersion: String,
    onCompareClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            AnimatedContent(
                targetState = checkState,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
                label = "notesAnimatedCard",
            ) { state ->
                when (state) {
                    CheckState.Idle, is CheckState.Checking -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            NotesShimmerPlaceholder()
                        }
                    }

                    is CheckState.Done -> {
                        if (state.isNewer && state.latest != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                            modifier = Modifier.size(38.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Rounded.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                "v${state.latest.tagName}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                            Text(
                                                "New Release",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                    }

                                    if (state.latest.publishedAt != null) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            ) {
                                                Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                                Text(
                                                    text = state.latest.publishedAt.substringBefore("T"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Medium,
                                                )
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Box(modifier = Modifier.padding(14.dp)) {
                                        SelectionContainer {
                                            ReleaseNotesText(state.latest.releaseNotes)
                                        }
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    modifier = Modifier.size(56.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(30.dp),
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        "You're on the latest build",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )

                                    Text(
                                        "There are no pending updates. You're enjoying the most recent version of Ever Haptics.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )

                                    if (!installedNotes.isNullOrBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Surface(
                                            onClick = onCompareClick,
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Description,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                                Text(
                                                    "View Installed Version Notes",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is CheckState.Failed -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.18f),
                                modifier = Modifier.size(56.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.CloudOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(30.dp),
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    "Couldn't check for updates",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "Unable to reach the GitHub releases repository. Please verify your internet connection.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesShimmerPlaceholder() {
    val infinite = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by infinite.animateFloat(
        initialValue = -1f, targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmerX",
    )
    val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val highlight = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(0.7f, 1f, 0.88f, 0.65f).forEach { widthFraction ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .height(14.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(base, highlight, base),
                            start = Offset(shimmerX * 300f, 0f),
                            end = Offset(shimmerX * 300f + 300f, 0f),
                        ),
                    ),
            )
        }
    }
}

// ─── Markdown Renderer ─────────────────────────────────────────────────────

@Composable
private fun ReleaseNotesText(rawNotes: String?) {
    if (rawNotes.isNullOrBlank()) {
        Text(
            "No release notes were provided for this version.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val codeBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    val lines = rawNotes.lines()
    var inCodeBlock = false
    val codeBlockLines = mutableListOf<String>()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (rawLine in lines) {
            val trimmed = rawLine.trim()

            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    val codeContent = codeBlockLines.joinToString("\n")
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = codeContent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = onSurfaceColor,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                    codeBlockLines.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                continue
            }

            if (inCodeBlock) {
                codeBlockLines.add(rawLine)
                continue
            }

            when {
                trimmed.isBlank() -> Spacer(Modifier.height(4.dp))

                trimmed.startsWith("#### ") -> {
                    Text(
                        text = parseMarkdownInline(trimmed.removePrefix("#### ").trim(), primaryColor, onSurfaceColor, codeBgColor),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = onSurfaceVariantColor,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                trimmed.startsWith("### ") -> {
                    Text(
                        text = parseMarkdownInline(trimmed.removePrefix("### ").trim(), primaryColor, onSurfaceColor, codeBgColor),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = onSurfaceColor,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                trimmed.startsWith("## ") -> {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            text = parseMarkdownInline(trimmed.removePrefix("## ").trim(), primaryColor, onSurfaceColor, codeBgColor),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        )
                    }
                }
                trimmed.startsWith("# ") -> {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            text = parseMarkdownInline(trimmed.removePrefix("# ").trim(), primaryColor, onSurfaceColor, codeBgColor),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryColor,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        )
                    }
                }

                trimmed.startsWith("> ") || trimmed.startsWith(">") -> {
                    val quoteText = trimmed.removePrefix(">").trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = primaryColor.copy(alpha = 0.7f),
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(22.dp),
                        ) {}
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = parseMarkdownInline(quoteText, primaryColor, onSurfaceVariantColor, codeBgColor),
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            color = onSurfaceVariantColor,
                        )
                    }
                }

                trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                }

                trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ") -> {
                    val content = trimmed.substring(2).trim()
                    val leadingSpaces = rawLine.takeWhile { it == ' ' }.length
                    val indent = (leadingSpaces / 2 * 12).dp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = indent),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = primaryColor,
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .size(5.dp),
                        ) {}
                        Text(
                            text = parseMarkdownInline(content, primaryColor, onSurfaceColor, codeBgColor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = onSurfaceColor,
                        )
                    }
                }

                trimmed.matches(Regex("""^\d+\.\s+.*""")) -> {
                    val numPrefix = trimmed.substringBefore(".") + "."
                    val content = trimmed.substringAfter(".").trim()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = numPrefix,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor,
                        )
                        Text(
                            text = parseMarkdownInline(content, primaryColor, onSurfaceColor, codeBgColor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = onSurfaceColor,
                        )
                    }
                }

                else -> {
                    Text(
                        text = parseMarkdownInline(trimmed, primaryColor, onSurfaceColor, codeBgColor),
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceColor,
                    )
                }
            }
        }

        if (inCodeBlock && codeBlockLines.isNotEmpty()) {
            val codeContent = codeBlockLines.joinToString("\n")
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = codeContent,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = onSurfaceColor,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

private fun parseMarkdownInline(
    text: String,
    primaryColor: Color,
    onSurfaceColor: Color,
    codeBgColor: Color,
): AnnotatedString {
    return buildAnnotatedString {
        val pattern = Regex("""(\[(.*?)\]\((.*?)\)|`([^`]+)`|\*\*\*([^*]+)\*\*\*|\*\*([^*]+)\*\*|__([^_]+)__|(?<!\*)\*([^*]+)\*(?!\*)|(?<!_)_([^_]+)_(?!_)|~~([^~]+)~~|(#[0-9]+|@[a-zA-Z0-9_/-]+))""")
        var currentIndex = 0
        val matches = pattern.findAll(text)

        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1

            if (start > currentIndex) {
                append(text.substring(currentIndex, start))
            }

            val fullMatch = match.value
            when {
                fullMatch.startsWith("[") && fullMatch.contains("](") -> {
                    val label = match.groupValues[2]
                    pushStyle(SpanStyle(color = primaryColor, textDecoration = TextDecoration.Underline, fontWeight = FontWeight.Medium))
                    append(label)
                    pop()
                }
                fullMatch.startsWith("`") && fullMatch.endsWith("`") -> {
                    val codeText = match.groupValues[4]
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBgColor,
                            color = primaryColor,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    append(" $codeText ")
                    pop()
                }
                fullMatch.startsWith("***") && fullMatch.endsWith("***") -> {
                    val content = match.groupValues[5]
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                    append(content)
                    pop()
                }
                (fullMatch.startsWith("**") && fullMatch.endsWith("**")) || (fullMatch.startsWith("__") && fullMatch.endsWith("__")) -> {
                    val content = if (match.groupValues[6].isNotEmpty()) match.groupValues[6] else match.groupValues[7]
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(content)
                    pop()
                }
                (fullMatch.startsWith("*") && fullMatch.endsWith("*")) || (fullMatch.startsWith("_") && fullMatch.endsWith("_")) -> {
                    val content = if (match.groupValues[8].isNotEmpty()) match.groupValues[8] else match.groupValues[9]
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(content)
                    pop()
                }
                fullMatch.startsWith("~~") && fullMatch.endsWith("~~") -> {
                    val content = match.groupValues[10]
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                    append(content)
                    pop()
                }
                fullMatch.startsWith("#") || fullMatch.startsWith("@") -> {
                    pushStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.SemiBold))
                    append(fullMatch)
                    pop()
                }
                else -> {
                    append(fullMatch)
                }
            }
            currentIndex = end
        }

        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}

// ─── Dialogs ───────────────────────────────────────────────────────────────

@Composable
private fun PermissionToDownloadDialog(
    currentVersion: String,
    latestVersion: String,
    readyToInstall: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        icon = {
            val iconColor = if (readyToInstall) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = iconColor.copy(alpha = 0.16f),
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (readyToInstall) Icons.Rounded.InstallMobile else Icons.Rounded.Download,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        },
        title = {
            Text(
                if (readyToInstall) "Install Update?" else "Download Update?",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                if (readyToInstall)
                    "Ever Haptics v$latestVersion has already been downloaded. Would you like to install it now?"
                else
                    "Ever Haptics v$latestVersion is available (you have v$currentVersion). The APK will be downloaded to your Downloads folder.",
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(if (readyToInstall) "Install Now" else "Download")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Not Now")
            }
        },
    )
}

@Composable
private fun DownloadingDialog(latestVersion: String, progress: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(250),
        label = "dlProgress",
    )

    AlertDialog(
        onDismissRequest = {},
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        shape = RoundedCornerShape(28.dp),
        icon = {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Downloading,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        },
        title = { Text("Downloading v$latestVersion", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    strokeCap = StrokeCap.Round,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Installing when ready…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {},
    )
}

// ─── Compare Release Notes Sheet ───────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompareReleaseNotesSheet(
    installedVersion: String,
    installedNotes: String?,
    latestVersion: String?,
    latestNotes: String?,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    fun closeWithAnimation() {
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Compare Release Notes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                IconButton(
                    onClick = { closeWithAnimation() },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val hasLatest = latestVersion != null
            if (hasLatest) {
                ExpressiveSegmentedTab(
                    options = listOf("Installed · v$installedVersion", "Latest · v$latestVersion"),
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it },
                )
            } else {
                Text(
                    "Showing release notes for your installed version.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedContent(
                targetState = if (hasLatest) selectedTab else 0,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInVertically { it / 4 } + fadeIn(tween(200))) togetherWith
                            (slideOutVertically { -it / 4 } + fadeOut(tween(150)))
                    } else {
                        (slideInVertically { -it / 4 } + fadeIn(tween(200))) togetherWith
                            (slideOutVertically { it / 4 } + fadeOut(tween(150)))
                    }
                },
                label = "compareContent",
            ) { tab ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .heightIn(min = 160.dp, max = 380.dp)
                            .padding(18.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        SelectionContainer {
                            ReleaseNotesText(if (tab == 0) installedNotes else latestNotes)
                        }
                    }
                }
            }

            Button(
                onClick = { closeWithAnimation() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ExpressiveSegmentedTab(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            .padding(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val bg by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                animationSpec = tween(220),
                label = "tabBg",
            )
            val fg by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(220),
                label = "tabFg",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = fg,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}
