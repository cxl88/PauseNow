package com.pausenow.app.ui.screen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.pausenow.app.rule.ProtectionRule
import com.pausenow.app.rule.RuleValidator
import com.pausenow.app.rule.SaveRuleResult
import com.pausenow.app.snapshot.SharedPreferencesSnapshotStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 规则列表/编辑的 ViewModel：读写 SnapshotStore 的 rules。 */
class RulesViewModel(application: Application) : AndroidViewModel(application) {
    private val snapshotStore = SharedPreferencesSnapshotStore(application)
    private val _rules = MutableStateFlow<List<ProtectionRule>>(emptyList())
    val rules: StateFlow<List<ProtectionRule>> = _rules.asStateFlow()

    init { load() }

    fun load() {
        _rules.value = snapshotStore.read().rules
    }

    fun getRule(ruleId: String): ProtectionRule? = _rules.value.find { it.id == ruleId }

    fun findRuleForPackage(packageName: String, excludingRuleId: String? = null): ProtectionRule? =
        _rules.value.firstOrNull { rule ->
            rule.id != excludingRuleId && rule.targetPackageName == packageName
        }

    fun saveRule(rule: ProtectionRule): SaveRuleResult = saveRuleWithResult(rule)

    /** R-001 唯一性 Domain + R-003 白名单 Domain 校验（docs/09 §9）。 */
    fun saveRuleWithResult(rule: ProtectionRule): SaveRuleResult {
        val snapshot = snapshotStore.read()
        when (val v = RuleValidator.validate(rule, snapshot.rules)) {
            is com.pausenow.app.rule.RuleValidation.ValidationFailed ->
                return SaveRuleResult.ValidationFailed(v.reason)
            com.pausenow.app.rule.RuleValidation.DuplicatePackage ->
                return SaveRuleResult.DuplicatePackage
            com.pausenow.app.rule.RuleValidation.Valid -> Unit
        }
        val now = System.currentTimeMillis()
        val withTimestamp = if (rule.createdAtMs == 0L) {
            rule.copy(createdAtMs = now, updatedAtMs = now)
        } else {
            rule.copy(updatedAtMs = now)
        }
        val rules = snapshot.rules.toMutableList()
        val idx = rules.indexOfFirst { it.id == withTimestamp.id }
        val result = if (idx >= 0) {
            rules[idx] = withTimestamp
            SaveRuleResult.Updated(withTimestamp)
        } else {
            rules.add(withTimestamp)
            SaveRuleResult.Created(withTimestamp)
        }
        snapshotStore.write(snapshot.copy(rules = rules, updatedAt = now))
        load()
        return result
    }

    fun deleteRule(ruleId: String) {
        val snapshot = snapshotStore.read()
        snapshotStore.write(
            snapshot.copy(
                rules = snapshot.rules.filter { it.id != ruleId },
                updatedAt = System.currentTimeMillis(),
            ),
        )
        load()
    }

    fun setEnabled(ruleId: String, enabled: Boolean) {
        val snapshot = snapshotStore.read()
        val updated = snapshot.rules.map { rule ->
            if (rule.id == ruleId) rule.copy(enabled = enabled) else rule
        }
        snapshotStore.write(snapshot.copy(rules = updated, updatedAt = System.currentTimeMillis()))
        load()
    }

    fun newRuleId(): String = "rule_" + System.currentTimeMillis()
    fun defaultPassDurationSeconds(): Int = snapshotStore.read().settings.defaultPassDurationSeconds
    fun defaultExtensionDurationSeconds(): Int = snapshotStore.read().settings.defaultExtensionDurationSeconds
}
