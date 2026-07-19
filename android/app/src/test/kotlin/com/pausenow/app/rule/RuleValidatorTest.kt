package com.pausenow.app.rule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleValidatorTest {

    private val rule = ProtectionRule(
        id = "r1",
        targetPackageName = "com.example",
        passDurationSeconds = 300,
        extensionDurationSeconds = 180,
    )

    @Test
    fun `valid rule passes`() {
        assertEquals(RuleValidation.Valid, RuleValidator.validate(rule, emptyList()))
    }

    @Test
    fun `duplicate package rejected`() {
        val existing = listOf(rule.copy(id = "other"))
        assertEquals(RuleValidation.DuplicatePackage, RuleValidator.validate(rule, existing))
    }

    @Test
    fun `same rule id not duplicate`() {
        assertEquals(RuleValidation.Valid, RuleValidator.validate(rule, listOf(rule)))
    }

    @Test
    fun `pass duration outside whitelist rejected`() {
        val bad = rule.copy(passDurationSeconds = 100) // 不在 {180,300,600,900}
        assertTrue(RuleValidator.validate(bad, emptyList()) is RuleValidation.ValidationFailed)
    }

    @Test
    fun `extension duration outside whitelist rejected`() {
        val bad = rule.copy(extensionDurationSeconds = 60) // 不在 {0,180,300}
        assertTrue(RuleValidator.validate(bad, emptyList()) is RuleValidation.ValidationFailed)
    }

    @Test
    fun `zero extension allowed`() {
        val zeroExt = rule.copy(extensionDurationSeconds = 0)
        assertEquals(RuleValidation.Valid, RuleValidator.validate(zeroExt, emptyList()))
    }

    @Test
    fun `all whitelist pass durations allowed`() {
        RuleWhitelist.PASS_DURATION_SECONDS.forEach { d ->
            assertEquals(RuleValidation.Valid, RuleValidator.validate(rule.copy(passDurationSeconds = d), emptyList()))
        }
    }
}
