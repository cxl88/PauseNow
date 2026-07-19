package com.pausenow.app.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.pausenow.app.ui.PauseGreen

data class AppIdentity(val packageName: String, val label: String, val icon: ImageBitmap?)

@Composable
fun rememberAppIdentity(packageName: String): AppIdentity {
    val context = LocalContext.current
    return remember(packageName) {
        if (packageName.isBlank()) return@remember AppIdentity("", "选择一个应用", null)
        runCatching {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            AppIdentity(
                packageName = packageName,
                label = context.packageManager.getApplicationLabel(info).toString(),
                icon = context.packageManager.getApplicationIcon(info).toBitmap(96, 96).asImageBitmap(),
            )
        }.getOrElse {
            AppIdentity(packageName, packageName.substringAfterLast('.'), null)
        }
    }
}

@Composable
fun AppIdentityIcon(identity: AppIdentity, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(size).background(Color.White.copy(alpha = 0.92f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (identity.icon != null) {
            Image(
                bitmap = identity.icon,
                contentDescription = identity.label,
                modifier = Modifier.size(size - 8.dp),
            )
        } else {
            Icon(
                Icons.Filled.Security,
                contentDescription = null,
                tint = PauseGreen,
                modifier = Modifier.size(size - 16.dp),
            )
        }
    }
}
