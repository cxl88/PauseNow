package com.pausenow.app.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class MainDestination { HOME, RULES, REPORT }

@Composable
fun MainNavigationBar(
    selected: MainDestination,
    onHome: () -> Unit,
    onRules: () -> Unit,
    onReport: () -> Unit,
) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(
            selected = selected == MainDestination.HOME,
            onClick = onHome,
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("首页") },
        )
        NavigationBarItem(
            selected = selected == MainDestination.RULES,
            onClick = onRules,
            icon = { Icon(Icons.Filled.Security, contentDescription = null) },
            label = { Text("规则") },
        )
        NavigationBarItem(
            selected = selected == MainDestination.REPORT,
            onClick = onReport,
            icon = { Icon(Icons.Filled.Assessment, contentDescription = null) },
            label = { Text("今日") },
        )
    }
}
