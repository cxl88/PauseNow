package com.pausenow.app.rule

import com.pausenow.app.snapshot.SnapshotStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 规则仓库（docs/09 §9）。UI 只消费本接口，不直接读 SnapshotStore。 */
interface RuleRepository {
    fun observeRules(): StateFlow<List<ProtectionRule>>
    fun getRule(id: String): ProtectionRule?
    fun getRuleByPackage(packageName: String): ProtectionRule?
    fun save(rule: ProtectionRule): SaveRuleResult
    fun setEnabled(id: String, enabled: Boolean)
    fun delete(id: String)
}

/** 委托 SnapshotStore + RuleValidator，MutableStateFlow 广播规则变更。 */
class RuleRepositoryImpl(private val store: SnapshotStore) : RuleRepository {
    private val _rules = MutableStateFlow(store.read().rules)

    override fun observeRules(): StateFlow<List<ProtectionRule>> = _rules.asStateFlow()

    override fun getRule(id: String): ProtectionRule? = _rules.value.find { it.id == id }

    override fun getRuleByPackage(packageName: String): ProtectionRule? =
        _rules.value.firstOrNull { it.targetPackageName == packageName }

    override fun save(rule: ProtectionRule): SaveRuleResult {
        val snapshot = store.read()
        when (val v = RuleValidator.validate(rule, snapshot.rules)) {
            is RuleValidation.ValidationFailed -> return SaveRuleResult.ValidationFailed(v.reason)
            RuleValidation.DuplicatePackage -> return SaveRuleResult.DuplicatePackage
            RuleValidation.Valid -> Unit
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
        store.write(snapshot.copy(rules = rules, updatedAt = now))
        _rules.value = rules
        return result
    }

    override fun setEnabled(id: String, enabled: Boolean) {
        val snapshot = store.read()
        val rules = snapshot.rules.map { if (it.id == id) it.copy(enabled = enabled) else it }
        store.write(snapshot.copy(rules = rules, updatedAt = System.currentTimeMillis()))
        _rules.value = rules
    }

    override fun delete(id: String) {
        val snapshot = store.read()
        val rules = snapshot.rules.filter { it.id != id }
        store.write(snapshot.copy(rules = rules, updatedAt = System.currentTimeMillis()))
        _rules.value = rules
    }

    /** 重新从 Store 读取并广播（用于 ON_RESUME 刷新，弥补多 Repository 实例不共享内存状态）。 */
    fun reload() {
        _rules.value = store.read().rules
    }
}
