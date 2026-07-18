package com.pausenow.app.intervention

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pausenow.app.pass.PassManager
import com.pausenow.app.pass.SharedPreferencesPassStore
import com.pausenow.app.report.InterventionEvent
import com.pausenow.app.report.InterventionEventStore

/**
 * 干预页。两种模式：
 * - OPEN：打开目标应用前，询问是否放行 N 分钟。
 * - EXPIRED：通行到期，询问延长或结束。
 * 用户选择后直接调 [PassManager]，再确定性返回目标应用或桌面。
 */
class InterventionActivity : ComponentActivity() {

    private lateinit var targetPackage: String
    private val passManager by lazy {
        PassManager(SharedPreferencesPassStore(applicationContext))
    }
    private val expiryController by lazy {
        ExpiryController(applicationContext)
    }
    private val eventStore by lazy {
        InterventionEventStore(applicationContext)
    }
    private fun record(type: String, pkg: String) {
        eventStore.append(InterventionEvent(type, pkg, System.currentTimeMillis()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        targetPackage = intent?.getStringExtra(EXTRA_PACKAGE).orEmpty()
        if (targetPackage.isEmpty()) {
            finish()
            return
        }
        val mode = intent?.getStringExtra(EXTRA_MODE) ?: MODE_OPEN
        val ruleId = intent?.getStringExtra(EXTRA_RULE_ID) ?: "default"
        val passDurationMs = intent?.getLongExtra(EXTRA_PASS_DURATION, 5 * 60 * 1000L) ?: 5 * 60 * 1000L
        val extensionSeconds = intent?.getIntExtra(EXTRA_EXTENSION, 180) ?: 180

        setContent {
            MaterialTheme {
                InterventionScreen(
                    mode = mode,
                    packageName = targetPackage,
                    passDurationMs = passDurationMs,
                    extensionMinutes = extensionSeconds / 60,
                    onGrant = {
                        val pass = passManager.grantPass(targetPackage, ruleId, passDurationMs)
                        expiryController.scheduleExpiry(targetPackage, pass.expiresAtMs)
                        record("grant", targetPackage)
                        returnToTarget(targetPackage)
                        InterventionState.release(targetPackage)
                        finish()
                    },
                    onExtend = {
                        passManager.currentPass(targetPackage)?.let {
                            val extended = passManager.extendPass(it, extensionSeconds)
                            expiryController.scheduleExpiry(targetPackage, extended.expiresAtMs)
                        }
                        record("extend", targetPackage)
                        returnToTarget(targetPackage)
                        InterventionState.release(targetPackage)
                        finish()
                    },
                    onEnd = {
                        passManager.endPass(targetPackage)
                        expiryController.cancelExpiry(targetPackage)
                        record("end", targetPackage)
                        returnToDesktop()
                        InterventionState.release(targetPackage)
                        finish()
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::targetPackage.isInitialized) InterventionState.onShowing(targetPackage, true)
    }

    override fun onPause() {
        super.onPause()
        if (::targetPackage.isInitialized) InterventionState.onShowing(targetPackage, false)
    }

    override fun onDestroy() {
        if (::targetPackage.isInitialized) {
            InterventionState.release(targetPackage)
        }
        super.onDestroy()
    }

    private fun returnToTarget(packageName: String) {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { startActivity(launch) }
        } else {
            returnToDesktop()
        }
    }

    private fun returnToDesktop() {
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(home) }
    }

    companion object {
        const val MODE_OPEN = "open"
        const val MODE_EXPIRED = "expired"
        private const val EXTRA_MODE = "pausenow.intervention.mode"
        private const val EXTRA_PACKAGE = "pausenow.intervention.package"
        private const val EXTRA_RULE_ID = "pausenow.intervention.rule_id"
        private const val EXTRA_PASS_DURATION = "pausenow.intervention.pass_duration_ms"
        private const val EXTRA_EXTENSION = "pausenow.intervention.extension_seconds"

        fun newIntent(
            context: Context,
            mode: String,
            packageName: String,
            ruleId: String,
            passDurationMs: Long,
            extensionSeconds: Int,
        ): Intent = Intent(context, InterventionActivity::class.java).apply {
            putExtra(EXTRA_MODE, mode)
            putExtra(EXTRA_PACKAGE, packageName)
            putExtra(EXTRA_RULE_ID, ruleId)
            putExtra(EXTRA_PASS_DURATION, passDurationMs)
            putExtra(EXTRA_EXTENSION, extensionSeconds)
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun InterventionScreen(
    mode: String,
    packageName: String,
    passDurationMs: Long,
    extensionMinutes: Int,
    onGrant: () -> Unit,
    onExtend: () -> Unit,
    onEnd: () -> Unit,
) {
    val isExpired = mode == InterventionActivity.MODE_EXPIRED
    val title = if (isExpired) "时间到" else "停一下"
    val passDurationText = if (passDurationMs >= 60_000L) "${passDurationMs / 60_000L} 分钟" else "${passDurationMs / 1000L} 秒"
    val message = if (isExpired) {
        "$packageName 的限时通行已结束。"
    } else {
        "即将打开 $packageName，是否放行 $passDurationText？"
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("停一下 · 干预") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(message, style = MaterialTheme.typography.bodyLarge)
            if (isExpired) {
                Button(onClick = onExtend, modifier = Modifier.fillMaxWidth()) {
                    Text("延长 $extensionMinutes 分钟")
                }
            } else {
                Button(onClick = onGrant, modifier = Modifier.fillMaxWidth()) {
                    Text("放行 $passDurationText")
                }
            }
            OutlinedButton(onClick = onEnd, modifier = Modifier.fillMaxWidth()) {
                Text("结束并回桌面")
            }
        }
    }
}
