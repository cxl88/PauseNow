package com.pausenow.app.pass

/** 活跃通行仓库（docs/09 §9）。UI/服务通过本接口操作通行，不直接调 PassManager。 */
interface ActivePassRepository {
    fun get(packageName: String): ActivePass?
    fun grant(command: GrantPassCommand): ActivePass
    fun extendOnce(sessionId: String): ExtendResult
    fun end(sessionId: String, reason: PassEndReason)
    fun cancelForRule(ruleId: String, reason: PassEndReason)
}

/** 委托 [PassManager]。 */
class ActivePassRepositoryImpl(private val manager: PassManager) : ActivePassRepository {
    override fun get(packageName: String): ActivePass? = manager.currentPass(packageName)
    override fun grant(command: GrantPassCommand): ActivePass = manager.grant(command)
    override fun extendOnce(sessionId: String): ExtendResult = manager.extendOnce(sessionId)
    override fun end(sessionId: String, reason: PassEndReason) = manager.end(sessionId, reason)
    override fun cancelForRule(ruleId: String, reason: PassEndReason) = manager.cancelForRule(ruleId, reason)
}
