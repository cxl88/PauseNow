package com.pausenow.app.intervention

import android.util.Log

/**
 * 干预防重复状态（单例，Service 与 Activity 共享）。
 * - 冷却：同一包上次干预 + cooldownMs 内不重复弹。
 * - 互斥：同一包已有干预 Activity 在显示时不重复弹。
 * - 泄漏自愈：inFlight 带启动时间，超过 [STALE_MS] 视为 Activity 未正常 onDestroy，
 *   自动清除并允许重新弹（华为等 ROM 可能不走 onDestroy 导致 release 缺失）。
 * 对应 docs/03 §7.2 "packageName+interventionType 短期互斥"。
 */
object InterventionState {
    private const val TAG = "PauseNow.Intervention"
    private const val STALE_MS = 5 * 60 * 1000L
    private val lock = Any()
    private val lastLaunch = mutableMapOf<String, Long>()
    private val inFlight = mutableMapOf<String, Long>()

    fun tryStart(packageName: String, cooldownMs: Long, now: Long = System.currentTimeMillis()): Boolean =
        synchronized(lock) {
            val startedAt = inFlight[packageName]
            if (startedAt != null) {
                if (now - startedAt < STALE_MS) return false
                Log.w(TAG, "inFlightStaleCleared packageName=$packageName ageMs=${now - startedAt}")
                inFlight.remove(packageName)
            }
            val last = lastLaunch[packageName] ?: 0L
            if (last + cooldownMs > now) return false
            inFlight[packageName] = now
            lastLaunch[packageName] = now
            true
        }

    fun release(packageName: String) = synchronized(lock) { inFlight.remove(packageName) }

    fun isBlocked(packageName: String): Boolean = synchronized(lock) { inFlight.containsKey(packageName) }

    @androidx.annotation.VisibleForTesting
    fun reset() = synchronized(lock) {
        lastLaunch.clear()
        inFlight.clear()
    }
}

