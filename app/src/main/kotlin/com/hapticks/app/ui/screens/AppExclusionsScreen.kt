package com.hapticks.app.ui.screens

import com.hapticks.app.ui.theme.PetalIcons
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hapticks.app.R
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.hapticks.app.ui.components.PetalBadgeVariant
import com.hapticks.app.ui.components.PetalButton
import com.hapticks.app.ui.components.PetalButtonStyle
import com.hapticks.app.ui.components.PetalCollapsingTopAppBar
import com.hapticks.app.ui.components.PetalContainedLoadingIndicator
import com.hapticks.app.ui.components.PetalGroupItem
import com.hapticks.app.ui.components.PetalGroupPosition
import com.hapticks.app.ui.components.PetalShapedIcon
import com.hapticks.app.ui.components.petalGroupPositionFor
import com.hapticks.app.ui.components.rememberPetalHeaderScrollBehavior
import com.hapticks.app.ui.theme.PetalMaterialShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppExclusionsScreen(
    title: String,
    excludedPackages: Set<String>,
    onExcludedPackagesChange: (Set<String>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            @Suppress("DEPRECATION")
            val resolveInfoList = pm.queryIntentActivities(intent, 0)
            val result = resolveInfoList
                .mapNotNull { ri ->
                    val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
                    AppInfo(
                        packageName = pkg,
                        label = ri.loadLabel(pm).toString(),
                        icon = try { ri.loadIcon(pm) } catch (e: Exception) { null },
                    )
                }
                .sortedBy { it.label.lowercase() }
                .distinctBy { it.packageName }
            apps = result
        }
        isLoading = false
    }

    val allSelected = apps.isNotEmpty() && apps.all { it.packageName in excludedPackages }

    val scrollBehavior = rememberPetalHeaderScrollBehavior()
    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            PetalCollapsingTopAppBar(
                title = title,
                scrollBehavior = scrollBehavior,
                onNavigateBack = onBack,
                actions = {
                    PetalButton(
                        onClick = {
                            if (allSelected) {
                                onExcludedPackagesChange(emptySet())
                            } else {
                                onExcludedPackagesChange(apps.map { it.packageName }.toSet())
                            }
                        },
                        enabled = apps.isNotEmpty(),
                        style = PetalButtonStyle.TONAL,
                        height = 40.dp,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text(
                            text = if (allSelected) {
                                stringResource(R.string.app_exclusions_deselect_all)
                            } else {
                                stringResource(R.string.app_exclusions_select_all)
                            },
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                },
            )
        },
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    PetalContainedLoadingIndicator(size = 72.dp)
                }
            }
            apps.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        PetalShapedIcon(
                            icon = PetalIcons.Android,
                            shape = PetalMaterialShapes.Clover4Leaf,
                            variant = PetalBadgeVariant.SURFACE_TONAL,
                            size = 88.dp,
                            iconSize = 40.dp,
                        )
                        Text(
                            text = stringResource(R.string.app_exclusions_no_apps),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                        AppExclusionItem(
                            app = app,
                            isExcluded = app.packageName in excludedPackages,
                            position = petalGroupPositionFor(index, apps.size),
                            onToggle = { checked ->
                                val newSet = if (checked) {
                                    excludedPackages + app.packageName
                                } else {
                                    excludedPackages - app.packageName
                                }
                                onExcludedPackagesChange(newSet)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppExclusionItem(
    app: AppInfo,
    isExcluded: Boolean,
    position: PetalGroupPosition,
    onToggle: (Boolean) -> Unit,
) {
    PetalGroupItem(position = position, onClick = { onToggle(!isExcluded) }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppIconImage(
                drawable = app.icon,
                modifier = Modifier.size(48.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Switch(
                checked = isExcluded,
                onCheckedChange = null,
                thumbContent = if (isExcluded) {
                    {
                        Icon(
                            imageVector = PetalIcons.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun AppIconImage(drawable: Drawable?, modifier: Modifier = Modifier) {
    if (drawable == null) {
        Icon(
            imageVector = PetalIcons.Android,
            contentDescription = null,
            modifier = modifier,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val bitmap: ImageBitmap = remember(drawable) {
        val w = drawable.intrinsicWidth.coerceAtLeast(1)
        val h = drawable.intrinsicHeight.coerceAtLeast(1)
        val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bm)
        drawable.setBounds(0, 0, w, h)
        drawable.draw(canvas)
        bm.asImageBitmap()
    }
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = modifier,
    )
}
