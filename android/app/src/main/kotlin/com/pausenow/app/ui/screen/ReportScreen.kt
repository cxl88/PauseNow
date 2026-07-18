package com.pausenow.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pausenow.app.report.InterventionEvent
import com.pausenow.app.report.InterventionEventStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen() {
    val context = LocalContext.current
    var events by remember { mutableStateOf<List<InterventionEvent>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        events = InterventionEventStore(context).todayEvents()
        loaded = true
    }

    Scaffold(topBar = { TopAppBar(title = { Text("今日报告") }) }) { padding ->
        if (!loaded) return@Scaffold
        val open = events.count { it.type == "open" }
        val expired = events.count { it.type == "expired" }
        val grant = events.count { it.type == "grant" }
        val extend = events.count { it.type == "extend" }
        val end = events.count { it.type == "end" }

        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("今日统计", style = MaterialTheme.typography.titleMedium)
                        Text("打开前干预弹出：$open 次")
                        Text("到期干预弹出：$expired 次")
                        Text("放行：$grant 次")
                        Text("延长：$extend 次")
                        Text("结束：$end 次")
                    }
                }
            }
            item { Text("按应用：", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
            items(events.groupBy { it.packageName }.toList()) { (pkg, evs) ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(pkg, style = MaterialTheme.typography.bodyMedium)
                        Text("共 ${evs.size} 次", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
