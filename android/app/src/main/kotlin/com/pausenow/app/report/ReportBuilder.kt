package com.pausenow.app.report

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 报告 UI 模型（docs/09 §6.2）。 */
data class ReportUiModel(
    val completedToday: Int,
    val passesToday: Int,
    val extensionsToday: Int,
    val endedToday: Int,
    val interventionsToday: Int,
    val stopRate: Double?, // 样本>=3 才有值
    val trend: List<DayCount>,
    val apps: List<AppReport>,
)

data class DayCount(val label: String, val count: Int)
data class AppReport(val packageName: String, val passes: Int, val extensions: Int, val ended: Int)

/** 报告聚合（docs/09 §6.2 口径）。纯逻辑，便于单测。 */
object ReportBuilder {
    fun build(events: List<InterventionEvent>): ReportUiModel {
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
}
