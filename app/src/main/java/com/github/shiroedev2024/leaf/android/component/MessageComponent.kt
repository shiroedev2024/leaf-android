package com.github.shiroedev2024.leaf.android.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.github.shiroedev2024.leaf.android.R
import com.github.shiroedev2024.leaf.android.ui.theme.*

enum class MessageType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO,
}

@Composable
fun MessageComponent(
    type: MessageType,
    message: String,
    actionLabel: String? = null,
    action: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
) {
    val (backgroundColor, contentColor, icon) =
        when (type) {
            MessageType.SUCCESS ->
                Triple(
                    SuccessBackground,
                    SuccessContent,
                    ImageVector.vectorResource(id = R.drawable.baseline_check_24),
                )
            MessageType.ERROR ->
                Triple(
                    ErrorBackground,
                    ErrorContent,
                    ImageVector.vectorResource(id = R.drawable.baseline_error_24),
                )
            MessageType.WARNING ->
                Triple(
                    WarningBackground,
                    WarningContent,
                    ImageVector.vectorResource(id = R.drawable.baseline_warning_24),
                )
            MessageType.INFO ->
                Triple(
                    InfoBackground,
                    InfoContent,
                    ImageVector.vectorResource(id = R.drawable.baseline_info_24),
                )
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                )
            }
            if (action != null && actionLabel != null) {
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedButton(
                    onClick = action,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
                ) {
                    if (actionIcon != null) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = actionLabel,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(text = actionLabel)
                }
            }
        }
    }
}
