package com.hapticks.app.ui.screens

import com.hapticks.app.ui.theme.PetalIcons
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.hapticks.app.ui.components.PetalBadgeVariant
import com.hapticks.app.ui.components.PetalButton
import com.hapticks.app.ui.components.PetalButtonStyle
import com.hapticks.app.ui.components.PetalGroupItem
import com.hapticks.app.ui.components.PetalGroupPosition
import com.hapticks.app.ui.components.PetalShapedIcon
import com.hapticks.app.ui.components.petalGroupPositionFor
import com.hapticks.app.ui.theme.ExpressiveShapeHolder
import com.hapticks.app.ui.theme.PetalMaterialShapes

/** First-launch welcome: Material 3 Expressive dialog with a shaped hero and connected info tiles. */
@Composable
fun WelcomeDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        AnimatedVisibility(
            visible = shown,
            enter = fadeIn() + scaleIn(
                initialScale = 0.82f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
            ),
        ) {
            Surface(
                shape = RoundedCornerShape(36.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // ── Hero ──
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    PetalMaterialShapes.Cookie12Sided.toShape(),
                                ),
                        )
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .background(MaterialTheme.colorScheme.primary, PetalMaterialShapes.Cookie9Sided.toShape()),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                PetalIcons.Vibration,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(46.dp),
                            )
                        }
                    }

                    // ── Body ──
                    Column(
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = "Welcome to Ever Haptics",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "You can support me only by joining my Telegram Channel and the App Support Group via Settings > About",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                        )

                        val tags = listOf(
                            Triple(Icons.Rounded.Campaign, "Announcements & Updates", PetalMaterialShapes.Cookie6Sided),
                            Triple(Icons.Rounded.BugReport, "Bug Fixes & Feature Requests", PetalMaterialShapes.Sunny),
                            Triple(Icons.Rounded.Forum, "Support", PetalMaterialShapes.Clover4Leaf),
                        )
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            tags.forEachIndexed { index, (icon, label, shape) ->
                                WelcomeTag(icon, label, shape, petalGroupPositionFor(index, tags.size))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            PetalButton(
                                onClick = onDismiss,
                                style = PetalButtonStyle.TONAL,
                                modifier = Modifier.weight(1f),
                            ) { Text("Continue", fontWeight = FontWeight.Medium) }

                            PetalButton(
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, "https://t.me/EverlastingAndroidTweak".toUri()))
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Join", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeTag(icon: ImageVector, label: String, shape: ExpressiveShapeHolder, position: PetalGroupPosition) {
    PetalGroupItem(position = position, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PetalShapedIcon(icon = icon, shape = shape, variant = PetalBadgeVariant.PRIMARY, size = 36.dp, iconSize = 18.dp)
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
