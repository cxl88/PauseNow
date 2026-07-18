package com.pausenow.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pausenow.app.rule.ProtectionRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditScreen(
    ruleId: String,
    selectedPackage: String?,
    onConsumeSelected: () -> Unit,
    onBack: () -> Unit,
    onPickApp: () -> Unit,
) {
    val viewModel: RulesViewModel = viewModel()
    val existing = remember(ruleId) { if (ruleId == "new") null else viewModel.getRule(ruleId) }
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var targetPackage by remember { mutableStateOf(existing?.targetPackages?.firstOrNull() ?: "") }
    var passMinutes by remember { mutableStateOf(((existing?.passDurationMs ?: viewModel.defaultDurationMs()) / 60_000L).toInt()) }
    var extMinutes by remember { mutableStateOf((existing?.extensionSeconds ?: viewModel.defaultExtensionSeconds()) / 60) }
    var enabled by remember { mutableStateOf(existing?.enabled ?: true) }

    LaunchedEffect(selectedPackage) {
        if (!selectedPackage.isNullOrEmpty()) {
            targetPackage = selectedPackage
            if (name.isEmpty()) name = selectedPackage.substringAfterLast('.')
            onConsumeSelected()
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(if (existing == null) "新建规则" else "编辑规则") }) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = targetPackage,
                onValueChange = { targetPackage = it },
                label = { Text("目标应用包名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = onPickApp, modifier = Modifier.fillMaxWidth()) {
                Text("从应用列表选择")
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("规则名（可选）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = passMinutes.toString(),
                onValueChange = { it.toIntOrNull()?.let { v -> passMinutes = v } },
                label = { Text("通行时长（分钟）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = extMinutes.toString(),
                onValueChange = { it.toIntOrNull()?.let { v -> extMinutes = v } },
                label = { Text("延长时长（分钟）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = enabled, onCheckedChange = { enabled = it })
                Text("启用")
            }
            Button(
                onClick = {
                    val pkg = targetPackage.trim()
                    if (pkg.isEmpty()) return@Button
                    val rule = ProtectionRule(
                        id = existing?.id ?: viewModel.newRuleId(),
                        name = name.trim(),
                        targetPackages = setOf(pkg),
                        passDurationMs = passMinutes.toLong() * 60_000L,
                        extensionSeconds = extMinutes * 60,
                        enabled = enabled,
                    )
                    viewModel.saveRule(rule)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("保存") }
            if (existing != null) {
                TextButton(
                    onClick = { viewModel.deleteRule(existing.id); onBack() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("删除规则", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
