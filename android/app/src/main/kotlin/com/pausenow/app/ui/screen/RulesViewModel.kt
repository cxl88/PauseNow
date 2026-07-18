package com.pausenow.app.ui.screen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.pausenow.app.rule.ProtectionRule
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

    fun saveRule(rule: ProtectionRule) {
        val snapshot = snapshotStore.read()
        val rules = snapshot.rules.toMutableList()
        val idx = rules.indexOfFirst { it.id == rule.id }
        if (idx >= 0) rules[idx] = rule else rules.add(rule)
        snapshotStore.write(snapshot.copy(rules = rules, updatedAt = System.currentTimeMillis()))
        load()
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

    fun newRuleId(): String = "rule_" + System.currentTimeMillis()
    fun defaultDurationMs(): Long = snapshotStore.read().settings.passDurationMs
    fun defaultExtensionSeconds(): Int = snapshotStore.read().settings.extensionSeconds
}
