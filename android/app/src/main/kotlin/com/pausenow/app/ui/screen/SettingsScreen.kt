package com.pausenow.app.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pausenow.app.model.DeviceSnapshot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    val device = remember { DeviceSnapshot.from(context) }

    Scaffold(topBar = { TopAppBar(title = { Text("设置") }) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("数据清除", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "清除所有规则、通行记录与事件日志（不影响系统权限和引导状态）。",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        onClick = {
                            listOf(
                                "pausenow_snapshot",
                                "pausenow_passes",
                                "pausenow_spike_events",
                                "pausenow_intervention_events",
                            ).forEach {
                                context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
                            }
                            message = "已清除全部数据"
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("清除全部数据")
                    }
                    message?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("诊断", style = MaterialTheme.typography.titleMedium)
                    Text("设备：${device.manufacturer} ${device.model}", style = MaterialTheme.typography.bodySmall)
                    Text("Android：${device.androidRelease} / API ${device.sdkInt}", style = MaterialTheme.typography.bodySmall)
                    Text("应用版本：${device.appVersion}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
