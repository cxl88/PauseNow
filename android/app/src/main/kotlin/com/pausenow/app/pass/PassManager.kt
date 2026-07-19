package com.pausenow.app.pass

/**
 * 通行管理器（docs/09 §9 ActivePassRepository 领域逻辑）。v3：grant 命令、extendOnce 三层约束（R-006）、end(reason)、cancelForRule。
 * clock 可注入便于单测。
 */
class PassManager(
    private val store: PassStore,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    fun grant(command: GrantPassCommand): ActivePass {
        val now = clock()
        val pass = ActivePass(
            sessionId = command.sessionId,
            ruleId = command.ruleId,
            packageName = command.packageName,
            purpose = command.purpose,
            plannedDurationSeconds = command.plannedDurationSeconds,
            extensionDurationSeconds = command.extensionDurationSeconds,
            grantedAtMs = now,
            expiresAtMs = now + command.plannedDurationSeconds * 1000L,
            extensionCount = 0,
            status = PassStatus.ACTIVE,
        )
        store.upsert(pass)
        return pass
    }

    fun currentPass(packageName: String): ActivePass? = store.getByPackage(packageName)

    /** R-006 Domain 层：extensionCount>=1 返回 AlreadyExtended。 */
    fun extendOnce(sessionId: String): ExtendResult {
        val pass = store.load()[sessionId] ?: return ExtendResult.NotFound
        if (!pass.canExtend()) return ExtendResult.AlreadyExtended
        val now = clock()
        val base = if (pass.expiresAtMs > now) pass.expiresAtMs else now
        val extended = pass.copy(
            expiresAtMs = base + pass.extensionDurationSeconds * 1000L,
            extensionCount = pass.extensionCount + 1,
        )
        store.upsert(extended)
        return ExtendResult.Extended(extended)
    }

    fun end(sessionId: String, @Suppress("UNUSED_PARAMETER") reason: PassEndReason) {
        store.remove(sessionId)
    }

    fun cancelForRule(ruleId: String, @Suppress("UNUSED_PARAMETER") reason: PassEndReason) {
        store.load().values.filter { it.ruleId == ruleId }.forEach { store.remove(it.sessionId) }
    }

    fun isExpired(pass: ActivePass): Boolean = pass.isExpired(clock())
}

data class GrantPassCommand(
    val sessionId: String,
    val ruleId: String,
    val packageName: String,
    val purpose: PassPurpose,
    val plannedDurationSeconds: Int,
    val extensionDurationSeconds: Int,
)

sealed interface ExtendResult {
    data object NotFound : ExtendResult
    data object AlreadyExtended : ExtendResult
    data class Extended(val pass: ActivePass) : ExtendResult
}
