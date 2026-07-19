package com.pausenow.app.pass

/**
 * 通行状态（docs/09 §5.2 / §4 状态机）。
 */
enum class PassStatus {
    ACTIVE,
    EXPIRED_PENDING,
    ENDED,
    EXPIRED_UNUSED,
    CANCELLED_RULE_DISABLED,
    CANCELLED_RULE_DELETED,
    CANCELLED_TIME_CHANGED,
}
