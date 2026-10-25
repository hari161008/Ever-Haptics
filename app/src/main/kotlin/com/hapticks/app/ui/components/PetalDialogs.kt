package com.hapticks.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.hapticks.app.ui.theme.ExpressiveShapeHolder
import com.hapticks.app.ui.theme.PetalMaterialShapes

/**
 * Material 3 Expressive alert dialog: 32dp container, an expressive shaped icon badge, centred
 * title and pill buttons that morph on press. [content] sits between the message and the buttons.
 */
@Composable
fun PetalAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    icon: ImageVector,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    dismissText: String? = null,
    onDismiss: (() -> Unit)? = null,
    badgeShape: ExpressiveShapeHolder = PetalMaterialShapes.Cookie9Sided,
    badgeVariant: PetalBadgeVariant = PetalBadgeVariant.PRIMARY,
    destructive: Boolean = false,
    dismissible: Boolean = true,
    content: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        properties = DialogProperties(dismissOnBackPress = dismissible, dismissOnClickOutside = dismissible),
        icon = {
            PetalShapedIcon(
                icon = icon,
                shape = badgeShape,
                variant = if (destructive) PetalBadgeVariant.ERROR else badgeVariant,
                size = 60.dp,
                iconSize = 30.dp,
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        },
        text = if (message != null || content != null) {
            {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (message != null) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    content?.invoke()
                }
            }
        } else {
            null
        },
        confirmButton = {
            PetalButton(
                onClick = onConfirm,
                height = 44.dp,
                colors = if (destructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    )
                } else {
                    null
                },
            ) { Text(confirmText, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = if (dismissText != null) {
            {
                PetalButton(
                    onClick = onDismiss ?: onDismissRequest,
                    style = PetalButtonStyle.TEXT,
                    height = 44.dp,
                ) { Text(dismissText) }
            }
        } else {
            null
        },
    )
}
