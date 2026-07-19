package com.pausenow.app.pass

/** 通行结束原因（docs/09 R-009 / §9 ActivePassRepository.end）。 */
enum class PassEndReason {
    USER_ENDED,
    EXPIRY_ENDED,
    RULE_DISABLED,
    RULE_DELETED,
    TIME_CHANGED,
    EXPIRED_UNUSED,
}
