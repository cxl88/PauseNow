package com.pausenow.app.report

/** 干预追踪仓库（docs/09 §9）。 */
interface TraceRepository {
    fun appendOrUpdate(trace: InterventionTrace)
    fun updateLaunch(traceId: String, ms: Long, result: LaunchResultType)
    fun updateVisible(traceId: String, ms: Long)
    fun updateAction(traceId: String, ms: Long, result: ActionResultType)
    fun latest(limit: Int): List<InterventionTrace>
    fun clear()
}

/** 委托 [InterventionTraceStore]。 */
class TraceRepositoryImpl(private val store: InterventionTraceStore) : TraceRepository {
    override fun appendOrUpdate(trace: InterventionTrace) = store.appendOrUpdate(trace)
    override fun updateLaunch(traceId: String, ms: Long, result: LaunchResultType) = store.updateLaunch(traceId, ms, result)
    override fun updateVisible(traceId: String, ms: Long) = store.updateVisible(traceId, ms)
    override fun updateAction(traceId: String, ms: Long, result: ActionResultType) = store.updateAction(traceId, ms, result)
    override fun latest(limit: Int): List<InterventionTrace> = store.latest(limit)
    override fun clear() { store.clear() }
}
