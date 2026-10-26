package com.hapticks.app.ui.screens.tapHaptics

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hapticks.app.R

@Composable
internal fun FeelEveryTapBackPill(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(id = R.string.back), tint = MaterialTheme.colorScheme.onSurface)
    }
}
