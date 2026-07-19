package com.pausenow.app.rule

import com.pausenow.app.pass.ActivePass
import com.pausenow.app.pass.PassPurpose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEngineTest {

    private val rule = ProtectionRule(
        id = "rule-1",
        targetPackageName = "com.ss.android.ugc.aweme",
        passDurationSeconds = 300,
    )

    private fun activePass(
        pkg: String = "com.ss.android.ugc.aweme",
        grantedAt: Long = 900_000L,
        expiresAt: Long = 1_200_000L,
    ) = ActivePass(
        sessionId = "sess-1",
        ruleId = "rule-1",
        packageName = pkg,
        purpose = PassPurpose.RELAX_BRIEFLY,
        plannedDurationSeconds = 300,
        extensionDurationSeconds = 180,
        grantedAtMs = grantedAt,
        expiresAtMs = expiresAt,
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
        assertEquals(Decision.Allow, RuleEngine.evaluate(input(packageName = "com.other.app")))
    }

    @Test
    fun `target package without pass requires open intervention`() {
        assertEquals(Decision.RequireOpenIntervention("rule-1"), RuleEngine.evaluate(input(pass = null)))
    }

    @Test
    fun `target package with valid pass allowed`() {
        val pass = activePass(grantedAt = 900_000L, expiresAt = 1_200_000L)
        assertEquals(Decision.Allow, RuleEngine.evaluate(input(pass = pass, now = 1_000_000L)))
    }

    @Test
    fun `target package with expired pass requires expired intervention`() {
        val pass = activePass(grantedAt = 400_000L, expiresAt = 500_000L)
        assertEquals(
            Decision.RequireExpiredIntervention("rule-1"),
            RuleEngine.evaluate(input(pass = pass, now = 1_000_000L)),
        )
    }

    @Test
    fun `pass for different package does not apply`() {
        val pass = activePass(pkg = "com.other.app", grantedAt = 900_000L, expiresAt = 1_200_000L)
        assertEquals(
            Decision.RequireOpenIntervention("rule-1"),
            RuleEngine.evaluate(input(pass = pass, now = 1_000_000L)),
        )
    }

    @Test
    fun `disabled rules are skipped`() {
        val disabled = rule.copy(enabled = false)
        assertEquals(Decision.Allow, RuleEngine.evaluate(input(rules = listOf(disabled))))
    }

    @Test
    fun `expired decision carries rule id`() {
        val pass = activePass(grantedAt = 0L, expiresAt = 10L)
        val decision = RuleEngine.evaluate(input(pass = pass, now = 1_000_000L))
        assertTrue(decision is Decision.RequireExpiredIntervention)
        assertEquals("rule-1", (decision as Decision.RequireExpiredIntervention).ruleId)
    }
}
