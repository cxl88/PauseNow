package com.pausenow.app.events

import com.pausenow.app.accessibility.ForegroundPackageEventRecord

/**
 * 单进程内的事件总线：替代原 Flutter 时代的 EventChannel。
 *
 * AccessibilityService 在事件线程调用 [publish]，UI 层通过 [register] 注册监听者，
 * 并自行切换到主线程更新界面状态。监听者调用发生在 [publish] 的调用线程上。
 */
object ForegroundEventBus {

    private val listeners = mutableListOf<(ForegroundPackageEventRecord) -> Unit>()

    @Synchronized
    fun register(listener: (ForegroundPackageEventRecord) -> Unit) {
        if (listener !in listeners) listeners.add(listener)
    }

    @Synchronized
    fun unregister(listener: (ForegroundPackageEventRecord) -> Unit) {
        listeners.remove(listener)
    }

    fun publish(event: ForegroundPackageEventRecord) {
        val snapshot = synchronized(this) { listeners.toList() }
        snapshot.forEach { listener ->
            runCatching { listener(event) }
        }
    }
}
