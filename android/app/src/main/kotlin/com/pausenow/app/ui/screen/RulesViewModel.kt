package com.pausenow.app.ui.screen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.pausenow.app.rule.ProtectionRule
import com.pausenow.app.rule.RuleRepositoryImpl
import com.pausenow.app.rule.SaveRuleResult
import com.pausenow.app.snapshot.SharedPreferencesSnapshotStore
import kotlinx.coroutines.flow.StateFlow

/** 规则列表/编辑的 ViewModel：通过 [RuleRepositoryImpl] 消费规则（docs/09 §11 任务9）。 */
class RulesViewModel(application: Application) : AndroidViewModel(application) {
    private val snapshotStore = SharedPreferencesSnapshotStore(application)
    private val repository = RuleRepositoryImpl(snapshotStore)
    val rules: StateFlow<List<ProtectionRule>> = repository.observeRules()

    /** repository Flow 已实时广播，保留以兼容 ON_RESUME 调用。 */
    fun load() = Unit

    fun getRule(ruleId: String): ProtectionRule? = repository.getRule(ruleId)

    fun findRuleForPackage(packageName: String, excludingRuleId: String? = null): ProtectionRule? =
        repository.getRuleByPackage(packageName)?.takeIf { it.id != excludingRuleId }

    fun saveRule(rule: ProtectionRule): SaveRuleResult = repository.save(rule)

    fun saveRuleWithResult(rule: ProtectionRule): SaveRuleResult = repository.save(rule)

    fun deleteRule(ruleId: String) = repository.delete(ruleId)

    fun setEnabled(ruleId: String, enabled: Boolean) = repository.setEnabled(ruleId, enabled)

    fun newRuleId(): String = "rule_" + System.currentTimeMillis()
    fun defaultPassDurationSeconds(): Int = snapshotStore.read().settings.defaultPassDurationSeconds
    fun defaultExtensionDurationSeconds(): Int = snapshotStore.read().settings.defaultExtensionDurationSeconds
}
