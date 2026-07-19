package com.pausenow.app.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pausenow.app.rule.ProtectionRule
import com.pausenow.app.ui.component.AppIdentityIcon
import com.pausenow.app.ui.component.rememberAppIdentity

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
    var targetPackage by remember { mutableStateOf(existing?.targetPackageName.orEmpty()) }
    var passMinutes by remember {
        mutableStateOf(normalizePassMinutes((existing?.passDurationSeconds ?: viewModel.defaultPassDurationSeconds()) / 60))
    }
    var extensionMinutes by remember {
        mutableStateOf(normalizeExtensionMinutes((existing?.extensionDurationSeconds ?: viewModel.defaultExtensionDurationSeconds()) / 60))
    }
    var enabled by remember { mutableStateOf(existing?.enabled ?: true) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(selectedPackage) {
        if (!selectedPackage.isNullOrEmpty()) {
            targetPackage = selectedPackage
            error = null
            onConsumeSelected()
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除保护规则？") },
            text = { Text("删除后将不再拦截该应用；历史报告仍会保留。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRule(existing.id)
                    confirmDelete = false
                    onBack()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "添加保护应用" else "编辑保护规则") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            AppSelectionCard(
                packageName = targetPackage,
                canChange = existing == null,
                onPickApp = onPickApp,
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("每次通行多久？", style = MaterialTheme.typography.titleMedium)
                Text(
                    "到时间后会再次提醒你做选择。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    passDurationOptions.take(2).forEach { option ->
                        DurationChip(option, passMinutes == option, { passMinutes = option }, Modifier.weight(1f))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    passDurationOptions.drop(2).forEach { option ->
                        DurationChip(option, passMinutes == option, { passMinutes = option }, Modifier.weight(1f))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("到期后允许延长", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    extensionOptions.forEach { option ->
                        FilterChip(
                            selected = extensionMinutes == option,
                            onClick = { extensionMinutes = option },
                            label = { Text(if (option == 0) "不允许" else "$option 分钟") },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            if (existing != null) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("启用保护", style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (enabled) "打开应用前会显示干预" else "当前暂不干预该应用",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = enabled, onCheckedChange = { enabled = it })
                    }
                }
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                onClick = {
                    val pkg = targetPackage.trim()
                    if (pkg.isEmpty()) {
                        error = "请先选择一个应用"
                        return@Button
                    }
                    val conflict = viewModel.findRuleForPackage(pkg, excludingRuleId = existing?.id)
                    if (conflict != null) {
                        error = "该应用已经在保护中，请返回规则列表直接编辑。"
                        return@Button
                    }
                    viewModel.saveRule(
                        ProtectionRule(
                            id = existing?.id ?: viewModel.newRuleId(),
                            targetPackageName = pkg,
                            passDurationSeconds = passMinutes * 60,
                            extensionDurationSeconds = extensionMinutes * 60,
                            enabled = if (existing == null) true else enabled,
                        ),
                    )
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (existing == null) "开始保护" else "保存修改")
            }

            if (existing != null) {
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("删除保护规则", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun AppSelectionCard(packageName: String, canChange: Boolean, onPickApp: () -> Unit) {
    val app = rememberAppIdentity(packageName)
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIdentityIcon(app, 52.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (packageName.isBlank()) "从手机中选择" else "受保护应用",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (canChange) {
                OutlinedButton(onClick = onPickApp, modifier = Modifier.fillMaxWidth()) {
                    Text(if (packageName.isBlank()) "选择应用" else "重新选择")
                }
            }
        }
    }
}

@Composable
private fun DurationChip(
    minutes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text("$minutes 分钟") },
        modifier = modifier,
    )
}

private fun normalizePassMinutes(value: Int): Int = value.takeIf { it in passDurationOptions } ?: 5
private fun normalizeExtensionMinutes(value: Int): Int = value.takeIf { it in extensionOptions } ?: 3

private val passDurationOptions = listOf(3, 5, 10, 15)
private val extensionOptions = listOf(0, 3, 5)
