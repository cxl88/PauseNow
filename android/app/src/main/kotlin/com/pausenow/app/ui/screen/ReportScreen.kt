package com.pausenow.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.pausenow.app.report.InterventionEvent
import com.pausenow.app.report.InterventionEventStore
import com.pausenow.app.report.ProductEventType
import com.pausenow.app.ui.PauseGreen
import com.pausenow.app.ui.PauseGreenLight
import com.pausenow.app.ui.PauseMint
import com.pausenow.app.ui.component.AppIdentityIcon
import com.pausenow.app.ui.component.MainDestination
import com.pausenow.app.ui.component.MainNavigationBar
import com.pausenow.app.ui.component.rememberAppIdentity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(onHome: () -> Unit, onRules: () -> Unit) {
    val context = LocalContext.current
    var events by remember { mutableStateOf<List<InterventionEvent>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        events = InterventionEventStore(context).recentEvents()
        loaded = true
    }

    val report = remember(events) { buildReport(events) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("今日", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PauseGreen,
                    titleContentColor = Color.White,
                ),
            )
        },
        bottomBar = {
            MainNavigationBar(
                selected = MainDestination.REPORT,
                onHome = onHome,
                onRules = onRules,
                onReport = {},
            )
        },
    ) { padding ->
        if (!loaded) return@Scaffold
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ReportHero(report.endedToday, report.stopRate) }
            item { TodayMetrics(report) }
            item { SevenDayTrend(report.trend) }
            item { Text("按应用", style = MaterialTheme.typography.titleMedium) }
            if (report.apps.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Text(
                            "今天还没有完成选择。下一次停下来，本页会记录你的行动。",
                            modifier = Modifier.padding(18.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(report.apps, key = { it.packageName }) { app -> AppReportRow(app) }
            }
        }
    }
}

@Composable
private fun ReportHero(endedToday: Int, stopRate: Double?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PauseGreenLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("主动停下", style = MaterialTheme.typography.bodyLarge, color = PauseGreen)
            Text("$endedToday 次", fontSize = 38.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold, color = PauseGreen)
            // docs/09 §6.2：样本>=3 才显示主动停下率
            stopRate?.let {
                Text(
                    "主动停下率 ${"%.0f".format(it * 100)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PauseGreen,
                )
            }
            Text(
                if (endedToday > 0) "每一次主动结束，都比无意识地继续更重要。" else "今天仍可以从一次主动结束开始。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TodayMetrics(report: ReportUiModel) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("今日选择", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth()) {
                ReportMetric(report.interventionsToday, "干预次数", Modifier.weight(1f))
                ReportMetric(report.endedToday, "主动停下", Modifier.weight(1f))
                ReportMetric(report.passesToday, "临时通行", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ReportMetric(value: Int, label: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), style = MaterialTheme.typography.headlineMedium, color = PauseGreen)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SevenDayTrend(trend: List<DayCount>) {
    val max = trend.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("近 7 日主动停下", style = MaterialTheme.typography.titleMedium)
            trend.forEach { day ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(day.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(42.dp))
                    LinearProgressIndicator(
                        progress = { day.count.toFloat() / max },
                        modifier = Modifier.weight(1f).height(8.dp),
                        color = PauseMint,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(day.count.toString(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(22.dp))
                }
            }
        }
    }
}

@Composable
private fun AppReportRow(app: AppReport) {
    val identity = rememberAppIdentity(app.packageName)
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIdentityIcon(identity, 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(identity.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    "通行 ${app.passes} · 延长 ${app.extensions} · 主动结束 ${app.ended}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private data class ReportUiModel(
    val completedToday: Int,
    val passesToday: Int,
    val extensionsToday: Int,
    val endedToday: Int,
    val interventionsToday: Int,
    val stopRate: Double?, // 样本>=3 才有值（docs/09 §6.2）
    val trend: List<DayCount>,
    val apps: List<AppReport>,
)

private data class DayCount(val label: String, val count: Int)
private data class AppReport(val packageName: String, val passes: Int, val extensions: Int, val ended: Int)

private fun buildReport(events: List<InterventionEvent>): ReportUiModel {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    fun InterventionEvent.date(): LocalDate = Instant.ofEpochMilli(occurredAtMs).atZone(zone).toLocalDate()
    val todayAll = events.filter { it.date() == today }
    val interventionsToday = todayAll.count {
        it.type == ProductEventType.OPEN_INTERVENTION_VISIBLE ||
            it.type == ProductEventType.EXPIRED_INTERVENTION_VISIBLE
    }
    val actionEvents = events.filter {
        it.type == ProductEventType.PASS_GRANTED ||
            it.type == ProductEventType.PASS_EXTENDED ||
            it.type == ProductEventType.END_AT_EXPIRY ||
            it.type == ProductEventType.EXIT_BEFORE_OPEN
    }
    val todayEvents = actionEvents.filter { it.date() == today }
    val trend = (6 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        DayCount(
            label = if (offset == 0) "今天" else date.format(DateTimeFormatter.ofPattern("M/d")),
            count = actionEvents.count {
                (it.type == ProductEventType.END_AT_EXPIRY || it.type == ProductEventType.EXIT_BEFORE_OPEN) && it.date() == date
            },
        )
    }
    val apps = todayEvents.groupBy { it.packageName }.map { (packageName, appEvents) ->
        AppReport(
            packageName = packageName,
            passes = appEvents.count { it.type == ProductEventType.PASS_GRANTED },
            extensions = appEvents.count { it.type == ProductEventType.PASS_EXTENDED },
            ended = appEvents.count {
                it.type == ProductEventType.END_AT_EXPIRY || it.type == ProductEventType.EXIT_BEFORE_OPEN
            },
        )
    }.sortedByDescending { it.ended }
    val endedToday = todayEvents.count {
        it.type == ProductEventType.END_AT_EXPIRY || it.type == ProductEventType.EXIT_BEFORE_OPEN
    }
    // docs/09 §6.2：样本<3 不显示百分比
    val stopRate = if (interventionsToday >= 3) endedToday.toDouble() / interventionsToday else null

    return ReportUiModel(
        completedToday = todayEvents.size,
        passesToday = todayEvents.count { it.type == ProductEventType.PASS_GRANTED },
        extensionsToday = todayEvents.count { it.type == ProductEventType.PASS_EXTENDED },
        endedToday = endedToday,
        interventionsToday = interventionsToday,
        stopRate = stopRate,
        trend = trend,
        apps = apps,
    )
}
