package com.hapticks.app.ui.screens

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.view.ViewGroup
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hapticks.app.ui.components.PetalButton
import com.hapticks.app.ui.components.PetalContainedLoadingIndicator

private const val EXPLORE_APPS_URL = "https://hariprabhu.vercel.app/"

private fun downloadFile(
    context: Context,
    url: String,
    userAgent: String?,
    contentDisposition: String?,
    mimeType: String?,
) {
    try {
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setMimeType(mimeType)
            if (userAgent != null) addRequestHeader("User-Agent", userAgent)
            setTitle(fileName)
            setDescription("Downloading $fileName")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        Toast.makeText(context, "Downloading $fileName…", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(context, "Couldn't start the download", Toast.LENGTH_SHORT).show()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ExploreAppsWebViewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var pageLoaded by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val contentAlpha by animateFloatAsState(
        targetValue = if (pageLoaded && !hasError) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "webViewFade",
    )

    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceArgb = surfaceColor.toArgb()

    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent),
    ) {
        if (!hasError) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(contentAlpha),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        setBackgroundColor(surfaceArgb)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean = false

                            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                canGoBack = view.canGoBack()
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                canGoBack = view.canGoBack()
                                pageLoaded = true
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest?,
                                error: WebResourceError?,
                            ) {
                                if (request?.isForMainFrame == true) {
                                    hasError = true
                                    pageLoaded = false
                                }
                            }
                        }
                        webChromeClient = WebChromeClient()
                        setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                            downloadFile(ctx, url, userAgent, contentDisposition, mimeType)
                        }
                        loadUrl(EXPLORE_APPS_URL)
                        webViewRef = this
                    }
                },
                update = { view -> webViewRef = view },
            )
        }

        // Loading indicator
        if (!pageLoaded && !hasError) {
            PetalContainedLoadingIndicator(
                size = 64.dp,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Error display
        if (hasError) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(56.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Unable to load page",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Please check your network connection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PetalButton(
                        onClick = {
                            hasError = false
                            pageLoaded = false
                            webViewRef?.reload()
                        },
                    ) {
                        Text("Retry")
                    }
                    PetalButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(EXPLORE_APPS_URL)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                    ) {
                        Text("Open in Browser")
                    }
                }
            }
        }

        // Floating circular back and share buttons at the bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FloatingActionButton(
                onClick = {
                    if (canGoBack) webViewRef?.goBack() else onBack()
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(6.dp),
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(24.dp),
                )
            }

            FloatingActionButton(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Explore More Apps")
                        putExtra(Intent.EXTRA_TEXT, EXPLORE_APPS_URL)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share"))
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(6.dp),
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    Icons.Rounded.Share,
                    contentDescription = "Share",
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
