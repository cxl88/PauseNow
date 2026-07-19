package com.pausenow.app.report

/** 干预事件仓库（docs/09 §9）。 */
interface EventRepository {
    fun append(event: InterventionEvent)
    fun query(fromMs: Long, toMs: Long): List<InterventionEvent>
    fun todayEvents(): List<InterventionEvent>
    fun clear()
}

/** 委托 [InterventionEventStore]。 */
class EventRepositoryImpl(private val store: InterventionEventStore) : EventRepository {
    override fun append(event: InterventionEvent) = store.append(event)
    override fun query(fromMs: Long, toMs: Long) = store.query(fromMs, toMs)
    override fun todayEvents() = store.todayEvents()
    override fun clear() = store.clear()
}
