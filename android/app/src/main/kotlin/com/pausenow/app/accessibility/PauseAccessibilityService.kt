package com.pausenow.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.PendingIntent
import android.app.usage.UsageStats
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.pausenow.app.MainActivity
import com.pausenow.app.R
import com.pausenow.app.events.ForegroundEventBus
import com.pausenow.app.intervention.ExpiryController
import com.pausenow.app.intervention.InterventionLauncher
import com.pausenow.app.notifications.NotificationChannels
import com.pausenow.app.pass.PassManager
import com.pausenow.app.pass.SharedPreferencesPassStore
import com.pausenow.app.permissions.AndroidPermissionGateway
import com.pausenow.app.rule.Decision
import com.pausenow.app.rule.EvaluationInput
import com.pausenow.app.rule.ProtectionRule
import com.pausenow.app.rule.RuleEngine
import com.pausenow.app.snapshot.SharedPreferencesSnapshotStore
import com.pausenow.app.snapshot.SnapshotStore

class PauseAccessibilityService : AccessibilityService() {
    private val debouncer = EventDebouncer()
    private lateinit var exclusionPolicy: PackageExclusionPolicy
    private lateinit var eventStore: ForegroundEventStore
    private lateinit var snapshotStore: SnapshotStore
    private lateinit var passManager: PassManager
    private lateinit var launcher: InterventionLauncher
    private lateinit var expiryController: ExpiryController
    private lateinit var permissionGateway: AndroidPermissionGateway
    private val detectionHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        exclusionPolicy = PackageExclusionPolicy(packageName)
        eventStore = ForegroundEventStore(applicationContext)
        snapshotStore = SharedPreferencesSnapshotStore(applicationContext)
        passManager = PassManager(SharedPreferencesPassStore(applicationContext))
        launcher = InterventionLauncher(applicationContext)
        expiryController = ExpiryController(applicationContext)
        permissionGateway = AndroidPermissionGateway(applicationContext)
        promoteToForeground()
        startDetectionLoop()
        Log.i(TAG, "serviceConnected=true contentRetrieval=false foreground=true detection=true")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Stage 1 保留：前台可见时实时记录包名事件（后台在国产 ROM 上不可靠，仅作补充）。
        if (event == null || event.eventType !in SUPPORTED_EVENT_TYPES) return

        val sourcePackage = event.packageName?.toString()?.trim().orEmpty()
        if (sourcePackage.isEmpty()) return

        if (!debouncer.shouldAccept(sourcePackage)) return
        if (exclusionPolicy.shouldExclude(sourcePackage)) return

        val record = ForegroundPackageEventRecord(
            packageName = sourcePackage,
            eventType = AccessibilityEvent.eventTypeToString(event.eventType),
            detectedAtMs = System.currentTimeMillis(),
        )
        eventStore.append(record)
        ForegroundEventBus.publish(record)

        Log.i(
            TAG,
            "detectedAtMs=" + record.detectedAtMs +
                " eventType=" + record.eventType +
                " packageName=" + record.packageName,
        )
    }

    override fun onInterrupt() {
        Log.w(TAG, "serviceInterrupted=true")
    }

    override fun onDestroy() {
        stopDetectionLoop()
        Log.i(TAG, "serviceDestroyed=true")
        super.onDestroy()
    }

    private fun promoteToForeground() {
        NotificationChannels.ensureChannels(applicationContext)
        val notification = buildForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NotificationChannels.FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(
                NotificationChannels.FOREGROUND_NOTIFICATION_ID,
                notification,
            )
        }
    }

    private fun buildForegroundNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, NotificationChannels.FOREGROUND_SERVICE_CHANNEL_ID)
            .setContentTitle(getString(R.string.foreground_notification_title))
            .setContentText(getString(R.string.foreground_notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    // 阶段 2 主检测循环：UsageStatsManager 轮询（国产 ROM 后台可靠），接 RuleEngine + 干预。
    private val detectionLoop = object : Runnable {
        override fun run() {
            runCatching { detectOnce() }
            detectionHandler.postDelayed(this, DETECTION_INTERVAL_MS)
        }
    }

    private fun startDetectionLoop() {
        detectionHandler.removeCallbacks(detectionLoop)
        detectionHandler.post(detectionLoop)
    }

    private fun stopDetectionLoop() {
        detectionHandler.removeCallbacks(detectionLoop)
    }

    private fun detectOnce() {
        val snapshot = snapshotStore.read()
        val rules = snapshot.rules.filter { it.enabled }
        if (rules.isEmpty()) return

        // 显式降级：Usage Access 关闭时 UsageStats 查不到前台，直接跳过干预。
        // 无障碍状态不在此处自检——服务在运行即说明无障碍已开启，关闭时系统会销毁服务、循环自然停止；
        // 在国产 ROM 上自检 enabled 列表存在误判风险，会回退成漏检，故只校验 Usage Access。
        if (!permissionGateway.hasUsageAccess()) {
            Log.i(DETECT_TAG, "degraded=usageAccessDenied")
            return
        }

        val foreground = queryCurrentForeground()
        if (foreground == null) {
            Log.i(DETECT_TAG, "foreground=null")
            return
        }
        val matchedRule = rules.filter { foreground in it.targetPackages }.maxByOrNull { it.priority }
        Log.i(DETECT_TAG, "foreground=$foreground matchedRule=${matchedRule?.id}")
        if (matchedRule == null) return

        val now = System.currentTimeMillis()
        val pass = passManager.currentPass(foreground)
        val decision = RuleEngine.evaluate(
            EvaluationInput(
                permissionsReady = true,
                packageName = foreground,
                excludedPackages = emptySet(),
                rules = rules,
                activePass = pass,
                now = now,
            ),
        )
        Log.i(DETECT_TAG, "decision=$decision passExpired=${pass != null && pass.isExpired(now)}")
        when (decision) {
            is Decision.RequireOpenIntervention ->
                launcher.launchOpen(
                    foreground, decision.ruleId, matchedRule.passDurationMs, REPEAT_COOLDOWN_MS,
                )
            is Decision.RequireExpiredIntervention ->
                launcher.launchExpired(
                    foreground, decision.ruleId, matchedRule.extensionSeconds, REPEAT_COOLDOWN_MS,
                )
            else -> Unit
        }
    }

    private fun queryCurrentForeground(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val now = System.currentTimeMillis()
        // 用 lastTimeUsed 取当前前台包，而非"最近5秒的切换事件"——
        // 用户停留超过几秒后 MOVE_TO_FOREGROUND 窗口会查不到，lastTimeUsed 始终可靠。
        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            now - FOREGROUND_WINDOW_MS,
            now,
        ) ?: return null
        return stats.maxByOrNull { it.lastTimeUsed }?.takeIf { it.lastTimeUsed > 0 }?.packageName
    }

    private companion object {
        const val TAG = "PauseNow.ForegroundEvent"
        const val DETECT_TAG = "PauseNow.Detection"
        const val DETECTION_INTERVAL_MS = 3000L
        const val FOREGROUND_WINDOW_MS = 60_000L
        const val REPEAT_COOLDOWN_MS = 30_000L
        val SUPPORTED_EVENT_TYPES = setOf(
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
        )
    }
}
