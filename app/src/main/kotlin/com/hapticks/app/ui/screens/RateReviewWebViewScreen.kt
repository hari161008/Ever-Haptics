package com.hapticks.app.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
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

private const val RATE_REVIEW_URL =
    "https://docs.google.com/forms/d/e/1FAIpQLSci14N7YdirM32f2I7bT6GBv_sQ3KJ4hjM6qDKTVZfsCuIPtw/viewform?usp=header"

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RateReviewWebViewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val bgColor = MaterialTheme.colorScheme.background

    fun openInExternalBrowser() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(RATE_REVIEW_URL)).apply {
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
                        text = "Rate & Review",
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
                                webViewInstance = this
                                loadUrl(RATE_REVIEW_URL)
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
                PetalContainedLoadingIndicator(size = 64.dp)
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
                        text = "Unable to load form in-app",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Check your internet connection or open in your browser.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PetalButton(
                            onClick = {
                                hasError = false
                                isLoading = true
                                webViewInstance?.reload() ?: run {
                                    webViewInstance = null
                                }
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
        }
    }
}
