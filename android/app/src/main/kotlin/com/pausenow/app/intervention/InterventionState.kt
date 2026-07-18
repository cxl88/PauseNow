package com.pausenow.app.intervention

import android.util.Log

/**
 * 干预防重复状态（单例，Service 与 Activity 共享）。
 * - 冷却：同一包上次干预 + cooldownMs 内不重复弹。
 * - 互斥：同一包已有干预 Activity 在前台显示时不重复弹。
 * - 被盖重盖：干预页被目标应用盖到后台（showing=false）时，允许检测循环重新
 *   startActivity 盖回去（RECOVER），解决抖音 main 启动时序竞争盖回干预页的问题。
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
    private val showing = mutableMapOf<String, Boolean>()

    enum class LaunchResult { STARTED, RECOVER, SUPPRESSED }

    fun tryStart(packageName: String, cooldownMs: Long, now: Long = System.currentTimeMillis()): LaunchResult =
        synchronized(lock) {
            val startedAt = inFlight[packageName]
            if (startedAt != null) {
                if (now - startedAt >= STALE_MS) {
                    Log.w(TAG, "inFlightStaleCleared packageName=$packageName ageMs=${now - startedAt}")
                    inFlight.remove(packageName)
                    showing.remove(packageName)
                } else {
                    // 已有干预在途：在前台则抑制，被盖到后台则允许重盖
                    return if (showing[packageName] == true) LaunchResult.SUPPRESSED else LaunchResult.RECOVER
                }
            }
            val last = lastLaunch[packageName] ?: 0L
            if (last + cooldownMs > now) return LaunchResult.SUPPRESSED
            inFlight[packageName] = now
            lastLaunch[packageName] = now
            LaunchResult.STARTED
        }

    /** InterventionActivity.onResume/onPause 调用，标记干预页是否在前台。 */
    fun onShowing(packageName: String, isShowing: Boolean) = synchronized(lock) {
        if (inFlight.containsKey(packageName)) showing[packageName] = isShowing
    }

    fun release(packageName: String) = synchronized(lock) {
        inFlight.remove(packageName)
        showing.remove(packageName)
    }

    fun isBlocked(packageName: String): Boolean = synchronized(lock) { inFlight.containsKey(packageName) }

    @androidx.annotation.VisibleForTesting
    fun reset() = synchronized(lock) {
        lastLaunch.clear()
        inFlight.clear()
        showing.clear()
    }
}
