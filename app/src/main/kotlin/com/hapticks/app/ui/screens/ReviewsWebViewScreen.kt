package com.hapticks.app.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val REVIEWS_URL =
    "https://hari161008.github.io/Website-For-Everlasting-Android-Tweak/Ratings%20Reviews/Ever%20Haptics/Ever_Haptics.html"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReviewsWebViewScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isLoading by remember { mutableStateOf(true) }
    val bgColor = MaterialTheme.colorScheme.background

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
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            isLoading = false
                        }
                    }
                    webChromeClient = WebChromeClient()
                    loadUrl(REVIEWS_URL)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Loading indicator
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }

        // Floating back button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(56.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
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
