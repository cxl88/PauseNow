package com.pausenow.app.intervention

/**
 * 干预防重复状态（单例，Service 与 Activity 共享）。
 * - 冷却：同一包上次干预 + cooldownMs 内不重复弹。
 * - 互斥：同一包已有干预 Activity 在显示时不重复弹。
 * 对应 docs/03 §7.2 "packageName+interventionType 短期互斥"。
 */
object InterventionState {
    private val lock = Any()
    private val lastLaunch = mutableMapOf<String, Long>()
    private val inFlight = mutableSetOf<String>()

    fun tryStart(packageName: String, cooldownMs: Long, now: Long = System.currentTimeMillis()): Boolean =
        synchronized(lock) {
            if (packageName in inFlight) return false
            val last = lastLaunch[packageName] ?: 0L
            if (last + cooldownMs > now) return false
            inFlight.add(packageName)
            lastLaunch[packageName] = now
            true
        }

    fun release(packageName: String) = synchronized(lock) { inFlight.remove(packageName) }

    fun isBlocked(packageName: String): Boolean = synchronized(lock) { packageName in inFlight }
}
