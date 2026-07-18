package com.pausenow.app.rule

import com.pausenow.app.pass.ActivePass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {

    private val rule = ProtectionRule(
        id = "rule-1",
        targetPackages = setOf("com.ss.android.ugc.aweme"),
        passDurationMs = 5 * 60 * 1000L,
    )

    private fun input(
        permissionsReady: Boolean = true,
        packageName: String = "com.ss.android.ugc.aweme",
        excluded: Set<String> = emptySet(),
        rules: List<ProtectionRule> = listOf(rule),
        pass: ActivePass? = null,
        now: Long = 1_000_000L,
    ) = EvaluationInput(permissionsReady, packageName, excluded, rules, pass, now)

    @Test
    fun `permissions not ready degrades`() {
        assertEquals(Decision.Degraded, RuleEngine.evaluate(input(permissionsReady = false)))
    }

    @Test
    fun `excluded package ignored`() {
        val decision = RuleEngine.evaluate(
            input(packageName = "com.pausenow.app", excluded = setOf("com.pausenow.app")),
        )
        assertEquals(Decision.Ignore, decision)
    }

    @Test
    fun `non-target package allowed`() {
        assertEquals(
            Decision.Allow,
            RuleEngine.evaluate(input(packageName = "com.other.app")),
        )
    }

    @Test
    fun `target package without pass requires open intervention`() {
        val decision = RuleEngine.evaluate(input(pass = null))
        assertEquals(Decision.RequireOpenIntervention("rule-1"), decision)
    }

    @Test
    fun `target package with valid pass allowed`() {
        val pass = ActivePass(
            ruleId = "rule-1",
            packageName = "com.ss.android.ugc.aweme",
            grantedAtMs = 900_000L,
            expiresAtMs = 1_200_000L,
        )
        assertEquals(Decision.Allow, RuleEngine.evaluate(input(pass = pass, now = 1_000_000L)))
    }

    @Test
    fun `target package with expired pass requires expired intervention`() {
        val pass = ActivePass(
            ruleId = "rule-1",
            packageName = "com.ss.android.ugc.aweme",
            grantedAtMs = 400_000L,
            expiresAtMs = 500_000L,
        )
        assertEquals(
            Decision.RequireExpiredIntervention("rule-1"),
            RuleEngine.evaluate(input(pass = pass, now = 1_000_000L)),
        )
    }

    @Test
    fun `pass for different package does not apply`() {
        val pass = ActivePass(
            ruleId = "rule-1",
            packageName = "com.other.app",
            grantedAtMs = 900_000L,
            expiresAtMs = 1_200_000L,
        )
        assertEquals(
            Decision.RequireOpenIntervention("rule-1"),
            RuleEngine.evaluate(input(pass = pass, now = 1_000_000L)),
        )
    }

    @Test
    fun `highest priority rule wins`() {
        val low = rule.copy(id = "low", priority = 1)
        val high = ProtectionRule(
            id = "high",
            targetPackages = setOf("com.ss.android.ugc.aweme"),
            passDurationMs = 60_000L,
            priority = 10,
        )
        val decision = RuleEngine.evaluate(input(rules = listOf(low, high)))
        assertEquals(Decision.RequireOpenIntervention("high"), decision)
    }

    @Test
    fun `disabled rules are skipped`() {
        val disabled = rule.copy(enabled = false)
        assertEquals(
            Decision.Allow,
            RuleEngine.evaluate(input(rules = listOf(disabled))),
        )
    }

    @Test
    fun `expired decision carries rule id`() {
        val pass = ActivePass(
            ruleId = "rule-1",
            packageName = "com.ss.android.ugc.aweme",
            grantedAtMs = 0L,
            expiresAtMs = 10L,
        )
        val decision = RuleEngine.evaluate(input(pass = pass, now = 1_000_000L))
        assertTrue(decision is Decision.RequireExpiredIntervention)
        assertEquals("rule-1", (decision as Decision.RequireExpiredIntervention).ruleId)
    }
}
