package com.pausenow.app.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pausenow.app.accessibility.ForegroundEventStore
import com.pausenow.app.accessibility.ForegroundPackageEventRecord
import com.pausenow.app.events.ForegroundEventBus
import com.pausenow.app.intervention.ExpiryController
import com.pausenow.app.model.DeviceSnapshot
import com.pausenow.app.model.PermissionSnapshot
import com.pausenow.app.pass.ActivePass
import com.pausenow.app.pass.PassManager
import com.pausenow.app.pass.SharedPreferencesPassStore
import com.pausenow.app.permissions.AndroidPermissionGateway
import com.pausenow.app.report.InterventionEvent
import com.pausenow.app.report.InterventionEventStore
import com.pausenow.app.rule.ProtectionRule
import com.pausenow.app.snapshot.SharedPreferencesSnapshotStore
import com.pausenow.app.snapshot.SnapshotStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * 阶段 1+2 Spike 页面的状态持有者。直接调用 Kotlin 原生网关与本地存储，
 * 替代原 Flutter 时代的 MethodChannel/EventChannel 桥接。
 */
class SpikeViewModel(application: Application) : AndroidViewModel(application) {

    data class TodaySummary(
        val completedChoices: Int = 0,
        val passes: Int = 0,
        val ended: Int = 0,
    )

    data class UiState(
        val loading: Boolean = true,
        val permissions: PermissionSnapshot = PermissionSnapshot.unavailable,
        val device: DeviceSnapshot = DeviceSnapshot.unavailable,
        val events: List<ForegroundPackageEventRecord> = emptyList(),
        val disclosureAccepted: Boolean = false,
        val protectedPackage: String = "",
        val currentPassInfo: String? = null,
        val activePass: ActivePass? = null,
        val today: TodaySummary = TodaySummary(),
        val error: String? = null,
    )

    private val gateway = AndroidPermissionGateway(application)
    private val eventStore = ForegroundEventStore(application)
    private val snapshotStore: SnapshotStore = SharedPreferencesSnapshotStore(application)
    private val passManager = PassManager(SharedPreferencesPassStore(application))
    private val expiryController = ExpiryController(application)
    private val interventionEventStore = InterventionEventStore(application)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val eventListener: (ForegroundPackageEventRecord) -> Unit = { event ->
        // AccessibilityService 的事件线程 -> 切到主线程更新 UI 状态。
        mainHandler.post {
            _state.update { current ->
                val deduped = listOf(event) +
                    current.events.filter { it.detectedAtMs != event.detectedAtMs }
                current.copy(
                    events = deduped.take(MAX_EVENTS),
                    loading = false,
                    error = null,
                )
            }
        }
    }

    fun onResumed() {
        ForegroundEventBus.register(eventListener)
        refresh()
    }

    fun onPaused() {
        ForegroundEventBus.unregister(eventListener)
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val permissionMap = gateway.permissionSnapshot()
                val device = DeviceSnapshot.from(getApplication())
                val events = eventStore.recentEvents()
                val snapshot = snapshotStore.read()
                val protectedPkg = snapshot.protectedPackages.firstOrNull().orEmpty()
                val now = System.currentTimeMillis()
                val allPasses = snapshot.protectedPackages.mapNotNull { passManager.currentPass(it) }
                val activePasses = allPasses.filter { !it.isExpired(now) }
                val activePass = activePasses.minByOrNull { it.expiresAtMs }
                val todayEvents = interventionEventStore.todayEvents()
                val today = TodaySummary(
                    completedChoices = todayEvents.count { it.type == "grant" || it.type == "extend" || it.type == "end" },
                    passes = todayEvents.count { it.type == "grant" },
                    ended = todayEvents.count { it.type == "end" },
                )
                val passInfo = when {
                    activePasses.isEmpty() && allPasses.isEmpty() -> null
                    activePasses.isEmpty() -> "通行已到期"
                    activePasses.size == 1 -> {
                        val remaining = ((activePasses[0].expiresAtMs - now) / 1000).coerceAtLeast(0)
                        "通行中：剩余 ${remaining}s"
                    }
                    else -> {
                        val nearest = activePasses.minBy { it.expiresAtMs }
                        val remaining = ((nearest.expiresAtMs - now) / 1000).coerceAtLeast(0)
                        "通行中 ${activePasses.size} 个，最近到期 ${remaining}s"
                    }
                }
                _state.update {
                    it.copy(
                        loading = false,
                        permissions = PermissionSnapshot.from(permissionMap),
                        device = device,
                        events = events,
                        protectedPackage = protectedPkg,
                        currentPassInfo = passInfo,
                        activePass = activePass,
                        today = today,
                    )
                }
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = "刷新失败：${error.message}") }
            }
        }
    }

    fun acceptDisclosure(value: Boolean) {
        _state.update { it.copy(disclosureAccepted = value) }
    }

    fun openUsageAccessSettings() {
        runCatching { gateway.openUsageAccessSettings() }
            .onFailure { _state.update { state -> state.copy(error = "无法打开设置：${it.message}") } }
    }

    fun openAccessibilitySettings() {
        runCatching { gateway.openAccessibilitySettings() }
            .onFailure { _state.update { state -> state.copy(error = "无法打开设置：${it.message}") } }
    }

    fun clearEvents() {
        eventStore.clear()
        _state.update { it.copy(events = emptyList()) }
    }

    /** 阶段 2 临时：手动设置保护包名（写成单条规则）。阶段 3 规则编辑 UI 会替代。 */
    fun setProtectedPackage(pkg: String) {
        val current = snapshotStore.read()
        val trimmed = pkg.trim()
        val rules = if (trimmed.isEmpty()) {
            emptyList()
        } else {
            listOf(
                ProtectionRule(
                    id = "default",
                    name = trimmed,
                    targetPackages = setOf(trimmed),
                    passDurationMs = current.settings.passDurationMs,
                    extensionSeconds = current.settings.extensionSeconds,
                ),
            )
        }
        snapshotStore.write(current.copy(rules = rules, updatedAt = System.currentTimeMillis()))
        refresh()
    }

    fun clearProtection() = setProtectedPackage("")

    fun endActivePass() {
        val activePass = _state.value.activePass ?: return
        // 先从 UI 状态移除，避免用户快速连点时重复记录结束事件。
        _state.update { current ->
            if (current.activePass?.packageName == activePass.packageName) {
                current.copy(activePass = null, currentPassInfo = null)
            } else {
                current
            }
        }
        passManager.endPass(activePass.packageName)
        expiryController.cancelExpiry(activePass.packageName)
        interventionEventStore.append(
            InterventionEvent("end", activePass.packageName, System.currentTimeMillis()),
        )
        refresh()
    }

    /**
     * 生成验收证据 JSON 并复制到剪贴板。返回 true 表示复制成功。
     */
    fun copyEvidence(): Boolean {
        val current = _state.value
        val eventsArray = JSONArray()
        current.events.forEach { eventsArray.put(it.toJson()) }
        val evidence = JSONObject()
            .put("generatedAt", Instant.now().toString())
            .put("device", current.device.toJson())
            .put("permissions", current.permissions.toJson())
            .put("protectedPackage", current.protectedPackage)
            .put("eventCount", current.events.size)
            .put("events", eventsArray)
            .toString(2)

        val clipboard = getApplication<Application>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return false
        clipboard.setPrimaryClip(ClipData.newPlainText("PauseNow evidence", evidence))
        return true
    }

    private companion object {
        const val MAX_EVENTS = 50
    }
}
