package com.pausenow.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pausenow.app.permissions.AndroidPermissionGateway
import com.pausenow.app.rule.ProtectionRule
import com.pausenow.app.ui.SpikeViewModel

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
    val showHuaweiTip = remember { AndroidPermissionGateway(context).isHuawei() }

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

    Scaffold(topBar = { TopAppBar(title = { Text("停一下") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { PermissionStatusCard(state, onUsageAccess = { viewModel.openUsageAccessSettings() }, onAccessibility = { viewModel.openAccessibilitySettings() }) }
            if (showHuaweiTip) {
                item { HuaweiBackgroundKeepaliveCard(context) }
            }
            item { ProtectionStatusCard(rules, state.currentPassInfo) }
            item {
                Button(onClick = onRules, modifier = Modifier.fillMaxWidth()) { Text("保护规则") }
            }
            item {
                OutlinedButton(onClick = onReport, modifier = Modifier.fillMaxWidth()) { Text("今日报告") }
            }
            item {
                OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("设置") }
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(state: SpikeViewModel.UiState, onUsageAccess: () -> Unit, onAccessibility: () -> Unit) {
    val usageOk = state.permissions.usageAccessGranted
    val a11yOk = state.permissions.accessibilityEnabled
    val allOk = usageOk && a11yOk
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (allOk) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                if (allOk) "权限已就绪" else "权限未就绪，保护无法工作",
                style = MaterialTheme.typography.titleMedium,
            )
            Text("使用情况访问：${if (usageOk) "已开" else "未开"}", style = MaterialTheme.typography.bodySmall)
            Text("无障碍服务：${if (a11yOk) "已开" else "未开"}", style = MaterialTheme.typography.bodySmall)
            if (!usageOk) {
                Button(onClick = onUsageAccess, modifier = Modifier.fillMaxWidth()) { Text("打开使用情况访问") }
            }
            if (!a11yOk) {
                Button(onClick = onAccessibility, modifier = Modifier.fillMaxWidth()) { Text("打开无障碍设置") }
            }
        }
    }
}

@Composable
private fun ProtectionStatusCard(rules: List<ProtectionRule>, passInfo: String?) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("保护状态", style = MaterialTheme.typography.titleMedium)
            val enabledCount = rules.count { it.enabled }
            Text("规则 ${rules.size} 条（启用 $enabledCount 条）", style = MaterialTheme.typography.bodyMedium)
            val targets = rules.flatMap { it.targetPackages }.distinct()
            Text(
                "目标：${targets.joinToString().ifEmpty { "未设置" }}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(passInfo ?: "未放行", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun HuaweiBackgroundKeepaliveCard(context: android.content.Context) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("华为机型：未开后台保活", style = MaterialTheme.typography.titleMedium)
            Text(
                "华为/鸿蒙默认杀后台，停一下退后台约半分钟会被回收，检测失效。" +
                    "去 设置 -> 电池 -> 启动管理 -> 停一下 -> 关闭自动管理 ->" +
                    " 勾选「自启动 + 关联启动 + 后台活动」。",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = {
                    runCatching { AndroidPermissionGateway(context).openStartupManager() }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("打开启动管理")
            }
        }
    }
}
