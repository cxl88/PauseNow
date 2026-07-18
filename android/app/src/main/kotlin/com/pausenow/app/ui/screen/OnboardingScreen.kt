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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pausenow.app.permissions.AndroidPermissionGateway
import com.pausenow.app.ui.SpikeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val viewModel: SpikeViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val showHuaweiTip = remember { AndroidPermissionGateway(context).isHuawei() }

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

    val usageOk = state.permissions.usageAccessGranted
    val a11yOk = state.permissions.accessibilityEnabled

    Scaffold(topBar = { TopAppBar(title = { Text("停一下 · 初始设置") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("欢迎使用停一下", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            item {
                Text(
                    "停一下在你打开选定的应用时弹出干预，限时放行，到时再提醒。" +
                        "只识别应用包名，不读取任何页面文字、输入或内容。",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp),
                )
            }
            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("第 1 步：开启权限", style = MaterialTheme.typography.titleMedium)
                        Text("使用情况访问：${if (usageOk) "已开" else "未开"}", style = MaterialTheme.typography.bodyMedium)
                        Text("无障碍服务：${if (a11yOk) "已开" else "未开"}", style = MaterialTheme.typography.bodyMedium)
                        if (!usageOk) {
                            Button(onClick = { viewModel.openUsageAccessSettings() }, modifier = Modifier.fillMaxWidth()) {
                                Text("打开使用情况访问")
                            }
                        }
                        if (!a11yOk) {
                            Button(onClick = { viewModel.openAccessibilitySettings() }, modifier = Modifier.fillMaxWidth()) {
                                Text("打开无障碍设置")
                            }
                        }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("第 2 步：添加保护规则", style = MaterialTheme.typography.titleMedium)
                        Text("完成初始设置后，在首页点「保护规则」-> 新建 -> 从应用列表选择目标应用。")
                    }
                }
            }
            if (showHuaweiTip) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("华为机型必看：后台保活", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "华为/鸿蒙默认会杀后台进程，停一下退后台约半分钟就会被系统回收，" +
                                    "导致检测失效。必须手动放行：",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "设置 -> 电池 -> 启动管理 -> 停一下 -> 关闭自动管理 ->" +
                                    " 勾选「自启动 + 关联启动 + 后台活动」。",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Button(
                                onClick = { runCatching { AndroidPermissionGateway(context).openStartupManager() } },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("打开启动管理")
                            }
                        }
                    }
                }
            }
            item {
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                    Text(if (usageOk && a11yOk) "完成，开始使用" else "完成（稍后可在设置里补全权限）")
                }
            }
        }
    }
}
