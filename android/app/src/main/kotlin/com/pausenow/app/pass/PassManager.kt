package com.pausenow.app.pass

/**
 * 通行管理器。grant/extend/end/current + 持久化（经 [PassStore]）。
 * 单线程串行调用（FGS 检测循环），持久化层自带锁。clock 可注入便于单测。
 */
class PassManager(
    private val store: PassStore,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    fun grantPass(packageName: String, ruleId: String, durationMs: Long): ActivePass {
        val now = clock()
        val pass = ActivePass(
            ruleId = ruleId,
            packageName = packageName,
            grantedAtMs = now,
            expiresAtMs = now + durationMs,
        )
        store.upsert(pass)
        return pass
    }

    fun currentPass(packageName: String): ActivePass? = store.load()[packageName]

    fun extendPass(pass: ActivePass, extensionSeconds: Int): ActivePass {
        val now = clock()
        // 已过期的通行从"现在"起算延长，避免延长后仍处于过期状态。
        val base = if (pass.expiresAtMs > now) pass.expiresAtMs else now
        val extended = pass.copy(
            expiresAtMs = base + extensionSeconds * 1000L,
            extensionCount = pass.extensionCount + 1,
        )
        store.upsert(extended)
        return extended
    }

    fun endPass(packageName: String) {
        store.remove(packageName)
    }

    fun isExpired(pass: ActivePass): Boolean = pass.isExpired(clock())
}
