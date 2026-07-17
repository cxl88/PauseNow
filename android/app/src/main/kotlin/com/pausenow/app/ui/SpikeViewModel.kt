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
import com.pausenow.app.model.DeviceSnapshot
import com.pausenow.app.model.PermissionSnapshot
import com.pausenow.app.permissions.AndroidPermissionGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/**
 * 阶段 1 Spike 页面的状态持有者。直接调用 Kotlin 原生网关与本地存储，
 * 替代原 Flutter 时代的 MethodChannel/EventChannel 桥接。
 */
class SpikeViewModel(application: Application) : AndroidViewModel(application) {

    data class UiState(
        val loading: Boolean = true,
        val permissions: PermissionSnapshot = PermissionSnapshot.unavailable,
        val device: DeviceSnapshot = DeviceSnapshot.unavailable,
        val events: List<ForegroundPackageEventRecord> = emptyList(),
        val disclosureAccepted: Boolean = false,
        val error: String? = null,
    )

    private val gateway = AndroidPermissionGateway(application)
    private val eventStore = ForegroundEventStore(application)
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
                _state.update {
                    it.copy(
                        loading = false,
                        permissions = PermissionSnapshot.from(permissionMap),
                        device = device,
                        events = events,
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
