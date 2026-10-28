package com.hapticks.app.ui.screens

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.hapticks.app.ui.components.PetalContainedLoadingIndicator
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch

private const val EXPLORE_APPS_URL = "https://hariprabhu.vercel.app/"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ExploreAppsWebViewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isLoading by remember { mutableStateOf(true) }
    val bgColor = MaterialTheme.colorScheme.background
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(bgColor)) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        allowFileAccess = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            isLoading = false
                        }
                    }
                    webChromeClient = WebChromeClient()
                    setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                        try {
                            val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                                .let { name ->
                                    // Ensure .apk extension for APK downloads
                                    if (mimeType?.contains("apk", ignoreCase = true) == true ||
                                        url.contains(".apk", ignoreCase = true)) {
                                        if (!name.endsWith(".apk")) "${name.substringBeforeLast('.')}.apk" else name
                                    } else name
                                }
                            val request = DownloadManager.Request(Uri.parse(url)).apply {
                                setMimeType(
                                    if (mimeType.isNullOrBlank()) {
                                        MimeTypeMap.getSingleton()
                                            .getMimeTypeFromExtension(
                                                MimeTypeMap.getFileExtensionFromUrl(url)
                                            ) ?: "application/octet-stream"
                                    } else mimeType
                                )
                                addRequestHeader("cookie", CookieManager.getInstance().getCookie(url))
                                addRequestHeader("User-Agent", userAgent)
                                setDescription("Downloading $fileName…")
                                setTitle(fileName)
                                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                                allowScanningByMediaScanner()
                            }
                            val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            dm.enqueue(request)
                            showMessage("Downloading $fileName…")
                        } catch (e: Exception) {
                            showMessage("Download failed: ${e.message}")
                        }
                    })
                    loadUrl(EXPLORE_APPS_URL)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            PetalContainedLoadingIndicator(size = 72.dp)
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 64.dp),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(56.dp)
                    .shadow(8.dp, com.hapticks.app.ui.theme.PetalMaterialShapes.Cookie9Sided.toShape())
                    .clip(com.hapticks.app.ui.theme.PetalMaterialShapes.Cookie9Sided.toShape())
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
