package com.pausenow.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pausenow.app.rule.ProtectionRule
import com.pausenow.app.ui.PauseGreen
import com.pausenow.app.ui.component.AppIdentityIcon
import com.pausenow.app.ui.component.MainDestination
import com.pausenow.app.ui.component.MainNavigationBar
import com.pausenow.app.ui.component.rememberAppIdentity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onHome: () -> Unit,
    onReport: () -> Unit,
) {
    val viewModel: RulesViewModel = viewModel()
    val rules by viewModel.rules.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("保护规则", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onAdd) {
                        Icon(Icons.Filled.Add, contentDescription = "添加应用")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PauseGreen,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
        bottomBar = {
            MainNavigationBar(
                selected = MainDestination.RULES,
                onHome = onHome,
                onRules = {},
                onReport = onReport,
            )
        },
    ) { padding ->
        if (rules.isEmpty()) {
            EmptyRules(modifier = Modifier.fillMaxSize().padding(padding), onAdd = onAdd)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        "打开这些应用前，先给自己一个明确选择。",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(rules, key = { it.id }) { rule ->
                    RuleRow(
                        rule = rule,
                        onToggle = { viewModel.setEnabled(rule.id, it) },
                        onEdit = { onEdit(rule.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRules(modifier: Modifier, onAdd: () -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("还没有保护应用", style = MaterialTheme.typography.titleLarge)
                Text(
                    "从一个最容易刷过头的应用开始。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                    Text("添加第一个应用")
                }
            }
        }
    }
}

@Composable
private fun RuleRow(rule: ProtectionRule, onToggle: (Boolean) -> Unit, onEdit: () -> Unit) {
    val packageName = rule.targetPackages.firstOrNull().orEmpty()
    val app = rememberAppIdentity(packageName)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIdentityIcon(app, 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    app.label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildRuleSummary(rule),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = rule.enabled, onCheckedChange = onToggle)
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "编辑",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun buildRuleSummary(rule: ProtectionRule): String {
    val extension = if (rule.extensionSeconds <= 0) "不可延长" else "可延长 ${rule.extensionSeconds / 60} 分钟"
    return "每次 ${rule.passDurationMs / 60_000} 分钟 · $extension"
}
