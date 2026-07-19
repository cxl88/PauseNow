package com.pausenow.app.intervention

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pausenow.app.pass.ExtendResult
import com.pausenow.app.pass.GrantPassCommand
import com.pausenow.app.pass.PassEndReason
import com.pausenow.app.pass.PassManager
import com.pausenow.app.pass.PassPurpose
import com.pausenow.app.pass.SharedPreferencesPassStore
import com.pausenow.app.report.InterventionEvent
import com.pausenow.app.report.InterventionEventStore
import com.pausenow.app.report.ProductEventType

/**
 * 干预页（docs/09 §4 状态机）。
 * - OPEN：选择使用目的（R-004）后放行，或"先不打开"（EXIT_BEFORE_OPEN）。
 * - EXPIRED：未用过延长则可延长一次（R-006），否则只能结束。
 */
class InterventionActivity : ComponentActivity() {

    private lateinit var targetPackage: String
    private val passManager by lazy { PassManager(SharedPreferencesPassStore(applicationContext)) }
    private val expiryController by lazy { ExpiryController(applicationContext) }
    private val eventStore by lazy { InterventionEventStore(applicationContext) }

    private fun record(
        type: ProductEventType,
        pkg: String,
        sessionId: String? = null,
        ruleId: String? = null,
        durationSeconds: Int? = null,
        purpose: PassPurpose? = null,
    ) {
        eventStore.append(
            InterventionEvent(
                eventId = "",
                sessionId = sessionId,
                ruleId = ruleId,
                packageName = pkg,
                type = type,
                occurredAtMs = System.currentTimeMillis(),
                purpose = purpose,
                durationSeconds = durationSeconds,
            ),
        )
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
        val passDurationSeconds = intent?.getIntExtra(EXTRA_PASS_DURATION, 300) ?: 300
        val extensionDurationSeconds = intent?.getIntExtra(EXTRA_EXTENSION, 180) ?: 180
        val currentPass = passManager.currentPass(targetPackage)
        val canExtend = currentPass?.canExtend() == true // R-006 UI：extensionCount<1 才显示延长

        setContent {
            MaterialTheme {
                InterventionScreen(
                    mode = mode,
                    packageName = targetPackage,
                    passDurationSeconds = passDurationSeconds,
                    extensionMinutes = extensionDurationSeconds / 60,
                    canExtend = canExtend,
                    onGrant = { purpose ->
                        val sessionId = "sess_${System.currentTimeMillis()}_${targetPackage.hashCode()}"
                        val pass = passManager.grant(
                            GrantPassCommand(
                                sessionId = sessionId,
                                ruleId = ruleId,
                                packageName = targetPackage,
                                purpose = purpose,
                                plannedDurationSeconds = passDurationSeconds,
                                extensionDurationSeconds = extensionDurationSeconds,
                            ),
                        )
                        expiryController.scheduleExpiry(targetPackage, pass.expiresAtMs)
                        record(
                            ProductEventType.PASS_GRANTED,
                            targetPackage,
                            sessionId,
                            ruleId,
                            passDurationSeconds,
                            purpose,
                        )
                        returnToTarget(targetPackage)
                        InterventionState.release(targetPackage)
                        finish()
                    },
                    onExtend = {
                        val pass = currentPass
                        if (pass != null) {
                            when (val r = passManager.extendOnce(pass.sessionId)) {
                                is ExtendResult.Extended -> {
                                    expiryController.scheduleExpiry(targetPackage, r.pass.expiresAtMs)
                                    record(
                                        ProductEventType.PASS_EXTENDED,
                                        targetPackage,
                                        pass.sessionId,
                                        ruleId,
                                        pass.extensionDurationSeconds,
                                    )
                                }
                                else -> Unit // AlreadyExtended / NotFound：R-006 不再延长
                            }
                        }
                        returnToTarget(targetPackage)
                        InterventionState.release(targetPackage)
                        finish()
                    },
                    onEnd = {
                        currentPass?.let { passManager.end(it.sessionId, PassEndReason.USER_ENDED) }
                        expiryController.cancelExpiry(targetPackage)
                        record(ProductEventType.END_AT_EXPIRY, targetPackage, currentPass?.sessionId, ruleId)
                        returnToDesktop()
                        InterventionState.release(targetPackage)
                        finish()
                    },
                    onExitBeforeOpen = {
                        record(ProductEventType.EXIT_BEFORE_OPEN, targetPackage, ruleId = ruleId)
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
        private const val EXTRA_PASS_DURATION = "pausenow.intervention.pass_duration_seconds"
        private const val EXTRA_EXTENSION = "pausenow.intervention.extension_seconds"

        fun newIntent(
            context: Context,
            mode: String,
            packageName: String,
            ruleId: String,
            passDurationSeconds: Int,
            extensionDurationSeconds: Int,
        ): Intent = Intent(context, InterventionActivity::class.java).apply {
            putExtra(EXTRA_MODE, mode)
            putExtra(EXTRA_PACKAGE, packageName)
            putExtra(EXTRA_RULE_ID, ruleId)
            putExtra(EXTRA_PASS_DURATION, passDurationSeconds)
            putExtra(EXTRA_EXTENSION, extensionDurationSeconds)
        }
    }
}

/** UI 可选的使用目的（排除 UNSPECIFIED_LEGACY，它仅供迁移）。 */
private val purposeOptions = listOf(
    PassPurpose.FIND_SPECIFIC_CONTENT,
    PassPurpose.HANDLE_ONE_TASK,
    PassPurpose.RELAX_BRIEFLY,
    PassPurpose.NO_CLEAR_PURPOSE,
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun InterventionScreen(
    mode: String,
    packageName: String,
    passDurationSeconds: Int,
    extensionMinutes: Int,
    canExtend: Boolean,
    onGrant: (PassPurpose) -> Unit,
    onExtend: () -> Unit,
    onEnd: () -> Unit,
    onExitBeforeOpen: () -> Unit,
) {
    val isExpired = mode == InterventionActivity.MODE_EXPIRED
    val title = if (isExpired) "时间到" else "停一下"
    val passDurationText = if (passDurationSeconds >= 60) "${passDurationSeconds / 60} 分钟" else "$passDurationSeconds 秒"
    var selectedPurpose by remember { mutableStateOf(PassPurpose.NO_CLEAR_PURPOSE) }

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
            if (isExpired) {
                Text("$packageName 的限时通行已结束。", style = MaterialTheme.typography.bodyLarge)
                if (canExtend) {
                    Button(onClick = onExtend, modifier = Modifier.fillMaxWidth()) {
                        Text("延长 $extensionMinutes 分钟")
                    }
                } else {
                    Text(
                        "本次延长已使用，无法再次延长。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = onEnd, modifier = Modifier.fillMaxWidth()) {
                    Text("结束并回桌面")
                }
            } else {
                Text("即将打开 $packageName，是否放行 $passDurationText？", style = MaterialTheme.typography.bodyLarge)
                Text("你打开它想做什么？", style = MaterialTheme.typography.titleMedium)
                purposeOptions.forEach { p ->
                    FilterChip(
                        selected = selectedPurpose == p,
                        onClick = { selectedPurpose = p },
                        label = { Text(p.label) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Button(
                    onClick = { onGrant(selectedPurpose) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("放行 $passDurationText")
                }
                OutlinedButton(onClick = onExitBeforeOpen, modifier = Modifier.fillMaxWidth()) {
                    Text("先不打开")
                }
            }
        }
    }
}
