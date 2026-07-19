package com.pausenow.app.ui.screen

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pausenow.app.pass.ActivePass
import com.pausenow.app.permissions.AndroidPermissionGateway
import com.pausenow.app.rule.ProtectionRule
import com.pausenow.app.ui.PauseGreen
import com.pausenow.app.ui.PauseGreenLight
import com.pausenow.app.ui.PauseMint
import com.pausenow.app.ui.SpikeViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onRules: () -> Unit,
    onReport: () -> Unit,
    onSettings: () -> Unit,
) {
    val viewModel: SpikeViewModel = viewModel()
    val rulesViewModel: RulesViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rules by rulesViewModel.rules.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val isHuawei = remember { AndroidPermissionGateway(context).isHuawei() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.onResumed()
                    rulesViewModel.load()
                }
                Lifecycle.Event.ON_PAUSE -> viewModel.onPaused()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("停一下", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PauseGreen,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
        bottomBar = { HomeBottomBar(onRules, onReport) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ProtectionHero(state.permissions.usageAccessGranted && state.permissions.accessibilityEnabled) }
            if (!state.permissions.usageAccessGranted || !state.permissions.accessibilityEnabled) {
                item {
                    PermissionActionsCard(
                        usageReady = state.permissions.usageAccessGranted,
                        accessibilityReady = state.permissions.accessibilityEnabled,
                        onUsageAccess = viewModel::openUsageAccessSettings,
                        onAccessibility = viewModel::openAccessibilitySettings,
                    )
                }
            }
            item {
                ActivePassCard(
                    activePass = state.activePass,
                    onExpired = viewModel::refresh,
                    onEnd = viewModel::endActivePass,
                )
            }
            item { TodayOverview(state.today, onReport) }
            item {
                RulesPreview(
                    rules = rules,
                    onManage = onRules,
                    onToggle = rulesViewModel::setEnabled,
                )
            }
            if (isHuawei) {
                item { HuaweiDeviceHint(context) }
            }
        }
    }
}

@Composable
private fun ProtectionHero(ready: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PauseGreenLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(46.dp).background(Color.White.copy(alpha = 0.75f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (ready) Icons.Filled.CheckCircle else Icons.Filled.Security,
                    contentDescription = null,
                    tint = PauseGreen,
                    modifier = Modifier.size(27.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    if (ready) "保护正在运行" else "完成设置，开始保护",
                    style = MaterialTheme.typography.titleLarge,
                    color = PauseGreen,
                )
                Text(
                    if (ready) "需要时我会先帮你停一下" else "还需要开启必要的系统权限",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PermissionActionsCard(
    usageReady: Boolean,
    accessibilityReady: Boolean,
    onUsageAccess: () -> Unit,
    onAccessibility: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("保护还未就绪", style = MaterialTheme.typography.titleMedium)
            Text("以下权限只用于识别受保护应用并展示干预页。", style = MaterialTheme.typography.bodyMedium)
            if (!usageReady) {
                OutlinedButton(onClick = onUsageAccess, modifier = Modifier.fillMaxWidth()) {
                    Text("开启使用情况访问")
                }
            }
            if (!accessibilityReady) {
                Button(onClick = onAccessibility, modifier = Modifier.fillMaxWidth()) {
                    Text("开启无障碍服务")
                }
            }
        }
    }
}

@Composable
private fun ActivePassCard(
    activePass: ActivePass?,
    onExpired: () -> Unit,
    onEnd: () -> Unit,
) {
    var now by remember(activePass?.expiresAtMs) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(activePass?.expiresAtMs) {
        val expiresAt = activePass?.expiresAtMs ?: return@LaunchedEffect
        while (now < expiresAt) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
        onExpired()
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = PauseGreen, contentColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (activePass == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Timer, contentDescription = null, tint = PauseMint, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("当前没有临时通行", style = MaterialTheme.typography.titleMedium)
                        Text("打开受保护应用时会先询问你", color = Color.White.copy(alpha = 0.75f))
                    }
                }
            } else {
                val app = rememberAppInfo(activePass.packageName)
                val remainingSeconds = ((activePass.expiresAtMs - now) / 1_000).coerceAtLeast(0)
                val total = (activePass.expiresAtMs - activePass.grantedAtMs).coerceAtLeast(1)
                val progress = ((now - activePass.grantedAtMs).toFloat() / total).coerceIn(0f, 1f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(app, 46.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(app.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("临时通行中", color = PauseMint, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(formatDuration(remainingSeconds), style = MaterialTheme.typography.titleLarge)
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(7.dp),
                    color = PauseMint,
                    trackColor = Color.White.copy(alpha = 0.18f),
                )
                Text(
                    "你可以随时结束，回到原本想做的事。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f),
                )
                Button(
                    onClick = onEnd,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = PauseGreen),
                ) {
                    Text("现在结束")
                }
            }
        }
    }
}

@Composable
private fun TodayOverview(summary: SpikeViewModel.TodaySummary, onReport: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("今天", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onReport) { Text("查看报告") }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Metric(summary.completedChoices, "完成选择")
                Metric(summary.passes, "临时通行")
                Metric(summary.ended, "主动结束")
            }
        }
    }
}

@Composable
private fun RowScope.Metric(value: Int, label: String) {
    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), style = MaterialTheme.typography.headlineMedium, color = PauseGreen)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RulesPreview(
    rules: List<ProtectionRule>,
    onManage: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("保护规则", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onManage) { Text("管理") }
            }
            if (rules.isEmpty()) {
                Text("还没有规则，添加一个想少刷一会儿的应用。", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onManage, modifier = Modifier.fillMaxWidth()) { Text("添加保护应用") }
            } else {
                rules.take(3).forEach { rule ->
                    val packageName = rule.targetPackages.firstOrNull().orEmpty()
                    val app = rememberAppInfo(packageName)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        AppIcon(app, 42.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                rule.name.ifBlank { app.label },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "通行 ${rule.passDurationMs / 60_000} 分钟 · 可延长 ${rule.extensionSeconds / 60} 分钟",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = rule.enabled, onCheckedChange = { onToggle(rule.id, it) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HuaweiDeviceHint(context: Context) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("华为设备设置建议", style = MaterialTheme.typography.titleMedium)
            Text(
                "若保护偶尔不出现，请允许停一下自启动、后台活动和后台弹出界面。",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { runCatching { AndroidPermissionGateway(context).openStartupManager() } }) {
                    Text("启动管理")
                }
                TextButton(onClick = { runCatching { AndroidPermissionGateway(context).openAppDetails() } }) {
                    Text("应用权限")
                }
            }
        }
    }
}

@Composable
private fun HomeBottomBar(onRules: () -> Unit, onReport: () -> Unit) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("首页") },
        )
        NavigationBarItem(
            selected = false,
            onClick = onRules,
            icon = { Icon(Icons.Filled.Security, contentDescription = null) },
            label = { Text("规则") },
        )
        NavigationBarItem(
            selected = false,
            onClick = onReport,
            icon = { Icon(Icons.Filled.Assessment, contentDescription = null) },
            label = { Text("今日") },
        )
    }
}

private data class DisplayApp(val label: String, val icon: ImageBitmap?)

@Composable
private fun rememberAppInfo(packageName: String): DisplayApp {
    val context = LocalContext.current
    return remember(packageName) {
        if (packageName.isBlank()) return@remember DisplayApp("受保护应用", null)
        runCatching {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            DisplayApp(
                label = context.packageManager.getApplicationLabel(info).toString(),
                icon = context.packageManager.getApplicationIcon(info).toBitmap(96, 96).asImageBitmap(),
            )
        }.getOrElse { DisplayApp(packageName.substringAfterLast('.'), null) }
    }
}

@Composable
private fun AppIcon(app: DisplayApp, size: Dp) {
    Box(
        modifier = Modifier.size(size).background(Color.White.copy(alpha = 0.92f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (app.icon != null) {
            Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.size(size - 8.dp))
        } else {
            Icon(Icons.Filled.Security, contentDescription = null, tint = PauseGreen, modifier = Modifier.size(size - 16.dp))
        }
    }
}

private fun formatDuration(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
