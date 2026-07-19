package com.pausenow.app.rule

/** 规则保存结果（docs/09 §9 RuleRepository.save）。 */
sealed interface SaveRuleResult {
    data class Created(val rule: ProtectionRule) : SaveRuleResult
    data class Updated(val rule: ProtectionRule) : SaveRuleResult
    data object DuplicatePackage : SaveRuleResult
    data class ValidationFailed(val reason: String) : SaveRuleResult
}

/** R-003 时长白名单（docs/09 §5.1）。Domain 层校验，UI/Store 都不得绕过。 */
object RuleWhitelist {
    val PASS_DURATION_SECONDS = setOf(180, 300, 600, 900) // 3/5/10/15 分钟
    val EXTENSION_DURATION_SECONDS = setOf(0, 180, 300) // 0/3/5 分钟

    fun validate(passDurationSeconds: Int, extensionDurationSeconds: Int): String? = when {
        passDurationSeconds !in PASS_DURATION_SECONDS -> "通行时长不在白名单"
        extensionDurationSeconds !in EXTENSION_DURATION_SECONDS -> "延长时长不在白名单"
        else -> null
    }
}
