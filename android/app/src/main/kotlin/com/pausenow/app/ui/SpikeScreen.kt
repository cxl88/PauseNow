@file:OptIn(ExperimentalMaterial3Api::class)

package com.pausenow.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pausenow.app.accessibility.ForegroundPackageEventRecord
import com.pausenow.app.model.DeviceSnapshot
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SpikeScreen() {
    val viewModel: SpikeViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onResumed()
                Lifecycle.Event.ON_PAUSE -> viewModel.onPaused()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PauseNowTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("停一下 · 阶段 1 Spike") },
                    actions = {
                        IconButton(
                            onClick = { viewModel.refresh() },
                            enabled = !state.loading,
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "刷新权限状态")
                        }
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { PurposeCard() }
                item { DeviceCard(state.device) }
                item {
                    PermissionCard(
                        title = "使用情况访问",
                        description = "阶段 1 仅验证授权状态，后续用于校验使用时长。",
                        granted = state.permissions.usageAccessGranted,
                        actionLabel = "打开使用情况访问设置",
                        onPressed = viewModel::openUsageAccessSettings,
                    )
                }
                item {
                    AccessibilityDisclosureCard(
                        granted = state.permissions.accessibilityEnabled,
                        accepted = state.disclosureAccepted,
                        onAcceptedChanged = viewModel::acceptDisclosure,
                        onOpenSettings = viewModel::openAccessibilitySettings,
                    )
                }
                item {
                    ProtectionCard(
                        protectedPackage = state.protectedPackage,
                        currentPassInfo = state.currentPassInfo,
                        onApply = viewModel::setProtectedPackage,
                        onClear = viewModel::clearProtection,
                    )
                }
                if (state.loading) {
                    item {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
                state.error?.let { error ->
                    item {
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                item {
                    EventsHeader(
                        hasEvents = state.events.isNotEmpty(),
                        onClear = viewModel::clearEvents,
                        onCopy = {
                            if (viewModel.copyEvidence()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("验收证据 JSON 已复制到剪贴板")
                                }
                            }
                        },
                    )
                }
                item {
                    Text("开启无障碍服务后，切换到任意第三方 App 再返回。日志不包含页面文字。")
                }
                if (state.events.isEmpty()) {
                    item {
                        Card {
                            Text(
                                "尚未收到有效事件",
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                } else {
                    items(state.events) { event ->
                        EventTile(event)
                    }
                }
            }
        }
    }
}

@Composable
private fun PurposeCard() {
    Card(
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("本阶段只证明一件事", fontWeight = FontWeight.Bold)
            Text(
                "在用户明确授权后，稳定获得第三方应用进入前台的包名事件。不会拦截应用，也不会读取页面内容。",
            )
        }
    }
}

@Composable
private fun DeviceCard(device: DeviceSnapshot) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Smartphone, contentDescription = null)
            Column(Modifier.padding(start = 16.dp)) {
                Text("${device.manufacturer} ${device.model}")
                Text(
                    "Android ${device.androidRelease} / API ${device.sdkInt} / " +
                        "Build ${device.buildId}\nPauseNow ${device.appVersion}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onPressed: () -> Unit,
) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(granted)
            }
            Text(description, modifier = Modifier.padding(top = 8.dp))
            FilledTonalButton(
                onClick = onPressed,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun AccessibilityDisclosureCard(
    granted: Boolean,
    accepted: Boolean,
    onAcceptedChanged: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Card {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "无障碍服务醒目披露",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(granted)
            }
            Text(
                "“停一下”使用无障碍服务，仅用于识别你主动选择的应用何时进入前台，并执行你预先设置的确定性规则。" +
                    "阶段 1 只记录事件时间、事件类型和应用包名。",
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                "我们不会读取、保存或上传页面文字、聊天内容、输入内容、视频标题、账号或支付信息；" +
                    "不会代替你点击其他应用。你可以随时在系统设置中关闭此权限。",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clickable { onAcceptedChanged(!accepted) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = accepted,
                    onCheckedChange = onAcceptedChanged,
                )
                Text("我已阅读并理解上述用途")
            }
            Button(
                onClick = onOpenSettings,
                enabled = accepted,
            ) {
                Text("打开无障碍设置")
            }
        }
    }
}

@Composable
private fun EventsHeader(
    hasEvents: Boolean,
    onClear: () -> Unit,
    onCopy: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "前台包名事件（本地最近 50 条）",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onClear, enabled = hasEvents) {
            Text("清空")
        }
        IconButton(onClick = onCopy, enabled = hasEvents) {
            Icon(Icons.Filled.ContentCopy, contentDescription = "复制验收证据")
        }
    }
}

@Composable
private fun EventTile(event: ForegroundPackageEventRecord) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val formatted = remember(event.detectedAtMs) {
        timeFormatter.format(Date(event.detectedAtMs))
    }
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.PhoneAndroid, contentDescription = null)
            Column(Modifier.padding(start = 16.dp)) {
                Text(
                    event.packageName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "$formatted · ${event.eventType}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun StatusChip(granted: Boolean) {
    AssistChip(
        onClick = {},
        label = { Text(if (granted) "已开启" else "未开启") },
        leadingIcon = {
            Icon(
                if (granted) {
                    Icons.Filled.CheckCircle
                } else {
                    Icons.Outlined.ErrorOutline
                },
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
    )
}

@Composable
private fun ProtectionCard(
    protectedPackage: String,
    currentPassInfo: String?,
    onApply: (String) -> Unit,
    onClear: () -> Unit,
) {
    var text by remember(protectedPackage) { mutableStateOf(protectedPackage) }
    Card {
        Column(Modifier.padding(16.dp)) {
            Text("阶段 2 保护配置", style = MaterialTheme.typography.titleMedium)
            Text(
                "设置目标应用包名，服务将在其进入前台时弹出干预。",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("目标包名") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = { onApply(text) }) { Text("应用保护") }
                OutlinedButton(onClick = onClear) { Text("清除") }
            }
            if (protectedPackage.isNotEmpty()) {
                Text(
                    "当前保护：$protectedPackage",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                currentPassInfo?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
