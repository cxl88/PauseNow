package com.pausenow.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pausenow.app.rule.ProtectionRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(onAdd: () -> Unit, onEdit: (String) -> Unit) {
    val viewModel: RulesViewModel = viewModel()
    val rules by viewModel.rules.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("保护规则") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "新增规则")
            }
        },
    ) { padding ->
        if (rules.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("还没有规则，点右下角 + 新建一条")
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            ) {
                items(rules) { rule ->
                    RuleRow(rule, onEdit = { onEdit(rule.id) })
                }
            }
        }
    }
}

@Composable
private fun RuleRow(rule: ProtectionRule, onEdit: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                rule.name.ifEmpty { rule.targetPackages.firstOrNull().orEmpty() },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "通行 ${rule.passDurationMs / 60_000} 分钟，延长 ${rule.extensionSeconds / 60} 分钟",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "目标：${rule.targetPackages.joinToString()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (rule.enabled) "已启用" else "已停用",
                style = MaterialTheme.typography.bodySmall,
                color = if (rule.enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}
