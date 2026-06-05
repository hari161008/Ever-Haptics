package com.hapticks.app.ui.screens.tapHaptics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.AppBlocking
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hapticks.app.R
import com.hapticks.app.data.HapticsSettings
import com.hapticks.app.haptics.HapticPattern
import com.hapticks.app.ui.components.HapticToggleRow
import com.hapticks.app.ui.components.PatternSelectorWithCustom
import com.hapticks.app.ui.components.SectionCard
import com.hapticks.app.ui.haptics.SliderTickStepsDefault
import com.hapticks.app.ui.haptics.performHapticSliderTick
import com.hapticks.app.ui.haptics.slider01ToTickIndex
import kotlin.math.roundToInt

@Composable
internal fun FeelEveryTapBackPill(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(id = R.string.back), tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
internal fun FeelEveryTapInteractionSection(
    settings: HapticsSettings,
    onTapEnabledChange: (Boolean) -> Unit,
    onIntensityCommit: (Float) -> Unit,
    onOpenAppExclusions: () -> Unit,
) {
    SectionCard {
        HapticToggleRow(
            title = stringResource(id = R.string.toggle_tap_title),
            subtitle = stringResource(id = R.string.toggle_tap_subtitle),
            checked = settings.tapEnabled,
            onCheckedChange = onTapEnabledChange,
            leadingIcon = Icons.Rounded.TouchApp,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
        FeelEveryTapIntensityControl(intensity = settings.intensity, onIntensityCommit = onIntensityCommit)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))
        AppExclusionRow(excludedCount = settings.tapExcludedPackages.size, onClick = onOpenAppExclusions)
    }
}

@Composable
private fun AppExclusionRow(excludedCount: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(Icons.Rounded.AppBlocking, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.app_exclusions_row_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = if (excludedCount == 0) stringResource(R.string.app_exclusions_row_subtitle_none)
                       else stringResource(R.string.app_exclusions_row_subtitle_some, excludedCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun FeelEveryTapIntensityControl(intensity: Float, onIntensityCommit: (Float) -> Unit) {
    val context = LocalContext.current
    var draft by remember(intensity) { mutableFloatStateOf(intensity) }
    var lastTickIndex by remember(intensity) { mutableIntStateOf(slider01ToTickIndex(intensity)) }
    val percent = (draft * 100f).roundToInt()
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(id = R.string.intensity_label), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape) {
                Text(stringResource(id = R.string.intensity_value, percent), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }
        }
        Slider(
            value = draft,
            onValueChange = { newValue ->
                draft = newValue
                val t = slider01ToTickIndex(newValue)
                if (t != lastTickIndex) { lastTickIndex = t; context.performHapticSliderTick() }
            },
            onValueChangeFinished = { onIntensityCommit(draft) },
            valueRange = 0f..1f,
            steps = SliderTickStepsDefault,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary, inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        )
    }
}

@Composable
internal fun FeelEveryTapPatternSection(
    settings: HapticsSettings,
    onPatternSelected: (HapticPattern) -> Unit,
    onOpenCustomEditor: () -> Unit,
) {
    SectionCard {
        PatternSelectorWithCustom(
            selectedPattern = settings.pattern,
            intensity = settings.intensity,
            customSequence = settings.tapHapticCustomSequence,
            onPatternSelected = onPatternSelected,
            onIntensityCommit = {},
            onOpenCustomEditor = onOpenCustomEditor,
        )
    }
}
