package com.pausenow.app.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pausenow.app.model.DeviceSnapshot
import com.pausenow.app.permissions.AndroidPermissionGateway
import com.pausenow.app.report.InterventionTraceStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val gateway = remember { AndroidPermissionGateway(context) }
    val traceStore = remember { InterventionTraceStore(context) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val device = remember { DeviceSnapshot.from(context) }
    val perms = remember { gateway.permissionSnapshot() }
    val isHuawei = remember { gateway.isHuawei() }

    val latencies = remember(traceStore.hashCode()) {
        traceStore.latest(300).mapNotNull { t -> t.visibleAtMs?.takeIf { it > 0 }?.let { it - t.detectedAtMs } }.sorted()
    }
    val p50 = latencies.getOrNull(latencies.size / 2)
    val p95 = latencies.getOrNull((latencies.size * 0.95).toInt().coerceAtMost((latencies.size - 1).coerceAtLeast(0)))

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清除规则与使用记录？") },
            text = { Text("将清除所有规则、通行记录、事件日志与干预追踪；系统权限和引导状态保留。此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    listOf(
                        "pausenow_snapshot",
                        "pausenow_passes",
                        "pausenow_spike_events",
                        "pausenow_intervention_events",
                        "pausenow_intervention_trace",
                    ).forEach {
                        context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
                    }
                    confirmClear = false
                    message = "已清除规则与使用记录"
                }) { Text("清除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } },
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("设置") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SettingsCard("权限与厂商") {
                    Text("使用情况访问：${if (perms["usageAccessGranted"] == true) "已开" else "未开"}", style = MaterialTheme.typography.bodySmall)
                    Text("无障碍服务：${if (perms["accessibilityEnabled"] == true) "已开" else "未开"}", style = MaterialTheme.typography.bodySmall)
                    if (isHuawei) {
                        Text("华为机型需额外开启后台保活与弹出界面", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = { runCatching { gateway.openStartupManager() } }, modifier = Modifier.fillMaxWidth()) {
                            Text("打开启动管理")
                        }
                        OutlinedButton(onClick = { runCatching { gateway.openAppDetails() } }, modifier = Modifier.fillMaxWidth()) {
                            Text("打开应用权限（后台弹出界面）")
                        }
                    }
                }
            }
            item {
                SettingsCard("诊断") {
                    Text("设备：${device.manufacturer} ${device.model}", style = MaterialTheme.typography.bodySmall)
                    Text("Android：${device.androidRelease} / API ${device.sdkInt}", style = MaterialTheme.typography.bodySmall)
                    Text("应用版本：${device.appVersion}", style = MaterialTheme.typography.bodySmall)
                    val sampleText = if (latencies.isEmpty()) "暂无延迟样本" else "样本 ${latencies.size} · P50 ${p50}ms · P95 ${p95}ms"
                    Text("干预延迟：$sampleText", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                SettingsCard("隐私与反馈") {
                    Text("停一下只识别应用包名，不读取页面内容、输入或视频标题。", style = MaterialTheme.typography.bodySmall)
                    Text("反馈：暂用系统分享或应用商店评论（正式隐私政策与反馈入口将在 Beta 上线）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("数据重置", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        Button(
                            onClick = { confirmClear = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("清除规则与使用记录") }
                        OutlinedButton(
                            onClick = {
                                context.getSharedPreferences("pausenow_onboarding", Context.MODE_PRIVATE).edit().clear().commit()
                                message = "下次启动将重新运行新手引导"
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("重新运行新手引导") }
                        message?.let {
                            Text(it, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
