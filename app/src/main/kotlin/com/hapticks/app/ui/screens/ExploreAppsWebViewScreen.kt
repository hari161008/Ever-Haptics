package com.hapticks.app.ui.screens

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hapticks.app.ui.components.PetalButton
import com.hapticks.app.ui.components.PetalContainedLoadingIndicator
import kotlinx.coroutines.launch

private const val EXPLORE_APPS_URL = "https://hariprabhu.vercel.app/"

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ExploreAppsWebViewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val bgColor = MaterialTheme.colorScheme.background
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    fun openInExternalBrowser() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(EXPLORE_APPS_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = bgColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Explore More Apps",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            hasError = false
                            isLoading = true
                            webViewInstance?.reload()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh",
                        )
                    }
                    IconButton(onClick = ::openInExternalBrowser) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = "Open in browser",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(bgColor),
        ) {
            if (!hasError) {
                AndroidView(
                    factory = { ctx ->
                        try {
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

                                    override fun onReceivedError(
                                        view: WebView,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?,
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            isLoading = false
                                            hasError = true
                                        }
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView,
                                        request: WebResourceRequest?,
                                    ): Boolean {
                                        val url = request?.url?.toString() ?: return false
                                        return if (url.startsWith("http://") || url.startsWith("https://")) {
                                            false
                                        } else {
                                            runCatching {
                                                ctx.startActivity(
                                                    Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    },
                                                )
                                            }
                                            true
                                        }
                                    }
                                }
                                webChromeClient = WebChromeClient()
                                setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                                    try {
                                        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                                            .let { name ->
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
                                                            MimeTypeMap.getFileExtensionFromUrl(url),
                                                        ) ?: "application/octet-stream"
                                                } else mimeType,
                                            )
                                            addRequestHeader("cookie", CookieManager.getInstance().getCookie(url))
                                            addRequestHeader("User-Agent", userAgent)
                                            setDescription("Downloading $fileName…")
                                            setTitle(fileName)
                                            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                                        }
                                        val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                        dm.enqueue(request)
                                        showMessage("Downloading $fileName…")
                                    } catch (e: Exception) {
                                        showMessage("Download failed: ${e.message}")
                                    }
                                })
                                webViewInstance = this
                                loadUrl(EXPLORE_APPS_URL)
                            }
                        } catch (e: Throwable) {
                            hasError = true
                            isLoading = false
                            WebView(ctx)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            AnimatedVisibility(
                visible = isLoading && !hasError,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center),
            ) {
                PetalContainedLoadingIndicator(size = 72.dp)
            }

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
                        text = "Unable to load Explore Apps",
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
                                isLoading = true
                                webViewInstance?.reload() ?: run { webViewInstance = null }
                            },
                        ) {
                            Text("Retry")
                        }
                        PetalButton(
                            onClick = ::openInExternalBrowser,
                        ) {
                            Text("Open Browser")
                        }
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(20.dp),
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
        }
    }
}
