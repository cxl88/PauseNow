package com.pausenow.app.rule

/** R-001/R-003 Domain 层校验结果（docs/09 §11），纯逻辑便于单测。 */
sealed interface RuleValidation {
    data object Valid : RuleValidation
    data object DuplicatePackage : RuleValidation
    data class ValidationFailed(val reason: String) : RuleValidation
}

/** R-001 同包名唯一 + R-003 时长白名单 Domain 校验。UI/Store 不得绕过。 */
object RuleValidator {
    fun validate(rule: ProtectionRule, existingRules: List<ProtectionRule>): RuleValidation {
        RuleWhitelist.validate(rule.passDurationSeconds, rule.extensionDurationSeconds)?.let {
            return RuleValidation.ValidationFailed(it)
        }
        if (existingRules.any { it.id != rule.id && it.targetPackageName == rule.targetPackageName }) {
            return RuleValidation.DuplicatePackage
        }
        return RuleValidation.Valid
    }
}
