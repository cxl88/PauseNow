package com.pausenow.app.report

/**
 * 面向报告的产品事件类型（docs/09 §5.4）。
 * 技术事件（SUPPRESSED/RECOVERED/LAUNCH_FAILED）只进 Trace，不进用户报告。
 */
enum class ProductEventType {
    OPEN_INTERVENTION_VISIBLE,
    EXIT_BEFORE_OPEN,
    PASS_GRANTED,
    EXPIRED_INTERVENTION_VISIBLE,
    PASS_EXTENDED,
    END_AT_EXPIRY,
    PASS_EXPIRED_UNUSED,
}
