package com.pausenow.app.intervention

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pausenow.app.pass.ExtendResult
import com.pausenow.app.pass.GrantPassCommand
import com.pausenow.app.pass.PassEndReason
import com.pausenow.app.pass.PassManager
import com.pausenow.app.pass.PassPurpose
import com.pausenow.app.pass.SharedPreferencesPassStore
import com.pausenow.app.report.ActionResultType
import com.pausenow.app.report.InterventionEvent
import com.pausenow.app.report.InterventionEventStore
import com.pausenow.app.report.InterventionTraceStore
import com.pausenow.app.report.ProductEventType
import com.pausenow.app.ui.PauseBackground
import com.pausenow.app.ui.PauseGreen
import com.pausenow.app.ui.PauseGreenLight
import com.pausenow.app.ui.PauseMint
import com.pausenow.app.ui.PauseNowTheme
import com.pausenow.app.ui.component.AppIdentityIcon
import com.pausenow.app.ui.component.rememberAppIdentity

/**
 * 干预页（docs/09 §4 状态机）。
 *
 * UI 只呈现当前可执行状态：首次打开时必须明确选择目的，到期后只在领域层允许时显示一次延长。
 */
class InterventionActivity : ComponentActivity() {

    private lateinit var targetPackage: String
    private var traceId: String = ""
    private val passManager by lazy { PassManager(SharedPreferencesPassStore(applicationContext)) }
    private val expiryController by lazy { ExpiryController(applicationContext) }
    private val eventStore by lazy { InterventionEventStore(applicationContext) }
    private val traceStore by lazy { InterventionTraceStore(applicationContext) }

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

        traceId = intent?.getStringExtra(EXTRA_TRACE_ID).orEmpty()
        val mode = intent?.getStringExtra(EXTRA_MODE) ?: MODE_OPEN
        val ruleId = intent?.getStringExtra(EXTRA_RULE_ID) ?: "default"
        val passDurationSeconds = intent?.getIntExtra(EXTRA_PASS_DURATION, 300) ?: 300
        val extensionDurationSeconds = intent?.getIntExtra(EXTRA_EXTENSION, 180) ?: 180
        val currentPass = passManager.currentPass(targetPackage)

        setContent {
            PauseNowTheme {
                InterventionScreen(
                    mode = mode,
                    packageName = targetPackage,
                    passDurationSeconds = passDurationSeconds,
                    extensionMinutes = extensionDurationSeconds / 60,
                    canExtend = currentPass?.canExtend() == true,
                    extensionConfigured = (currentPass?.extensionDurationSeconds ?: extensionDurationSeconds) > 0,
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
                        traceAction(ActionResultType.GRANTED)
                        returnToTarget(targetPackage)
                        InterventionState.release(targetPackage)
                        finish()
                    },
                    onExtend = extend@{
                        val pass = passManager.currentPass(targetPackage) ?: return@extend false
                        when (val result = passManager.extendOnce(pass.sessionId)) {
                            is ExtendResult.Extended -> {
                                expiryController.scheduleExpiry(targetPackage, result.pass.expiresAtMs)
                                record(
                                    ProductEventType.PASS_EXTENDED,
                                    targetPackage,
                                    pass.sessionId,
                                    ruleId,
                                    pass.extensionDurationSeconds,
                                )
                                traceAction(ActionResultType.EXTENDED)
                                returnToTarget(targetPackage)
                                InterventionState.release(targetPackage)
                                finish()
                                true
                            }

                            else -> false
                        }
                    },
                    onEnd = {
                        val pass = passManager.currentPass(targetPackage)
                        pass?.let { passManager.end(it.sessionId, PassEndReason.USER_ENDED) }
                        expiryController.cancelExpiry(targetPackage)
                        record(ProductEventType.END_AT_EXPIRY, targetPackage, pass?.sessionId, ruleId)
                        traceAction(ActionResultType.ENDED)
                        returnToDesktop()
                        InterventionState.release(targetPackage)
                        finish()
                    },
                    onExitBeforeOpen = {
                        record(ProductEventType.EXIT_BEFORE_OPEN, targetPackage, ruleId = ruleId)
                        traceAction(ActionResultType.EXITED_BEFORE_OPEN)
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
        if (::targetPackage.isInitialized) {
            InterventionState.onShowing(targetPackage, true)
            if (traceId.isNotEmpty()) traceStore.updateVisible(traceId, System.currentTimeMillis())
        }
    }

    override fun onPause() {
        super.onPause()
        if (::targetPackage.isInitialized) InterventionState.onShowing(targetPackage, false)
    }

    override fun onDestroy() {
        if (::targetPackage.isInitialized) InterventionState.release(targetPackage)
        super.onDestroy()
    }

    private fun traceAction(result: ActionResultType) {
        if (traceId.isNotEmpty()) traceStore.updateAction(traceId, System.currentTimeMillis(), result)
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
        private const val EXTRA_TRACE_ID = "pausenow.intervention.trace_id"
        private const val EXTRA_PASS_DURATION = "pausenow.intervention.pass_duration_seconds"
        private const val EXTRA_EXTENSION = "pausenow.intervention.extension_seconds"

        fun newIntent(
            context: Context,
            mode: String,
            packageName: String,
            ruleId: String,
            passDurationSeconds: Int,
            extensionDurationSeconds: Int,
            traceId: String,
        ): Intent = Intent(context, InterventionActivity::class.java).apply {
            putExtra(EXTRA_MODE, mode)
            putExtra(EXTRA_PACKAGE, packageName)
            putExtra(EXTRA_RULE_ID, ruleId)
            putExtra(EXTRA_TRACE_ID, traceId)
            putExtra(EXTRA_PASS_DURATION, passDurationSeconds)
            putExtra(EXTRA_EXTENSION, extensionDurationSeconds)
        }
    }
}

private val purposeOptions = listOf(
    PassPurpose.FIND_SPECIFIC_CONTENT,
    PassPurpose.HANDLE_ONE_TASK,
    PassPurpose.RELAX_BRIEFLY,
    PassPurpose.NO_CLEAR_PURPOSE,
)

@Composable
private fun InterventionScreen(
    mode: String,
    packageName: String,
    passDurationSeconds: Int,
    extensionMinutes: Int,
    canExtend: Boolean,
    extensionConfigured: Boolean,
    onGrant: (PassPurpose) -> Unit,
    onExtend: () -> Boolean,
    onEnd: () -> Unit,
    onExitBeforeOpen: () -> Unit,
) {
    val isExpired = mode == InterventionActivity.MODE_EXPIRED
    val app = rememberAppIdentity(packageName)
    val passDurationText = if (passDurationSeconds >= 60) "${passDurationSeconds / 60} 分钟" else "$passDurationSeconds 秒"
    var selectedPurpose by remember { mutableStateOf<PassPurpose?>(null) }
    var extensionError by remember { mutableStateOf<String?>(null) }

    Scaffold(containerColor = PauseBackground) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            InterventionBrand()
            Spacer(Modifier.size(24.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                InterventionHero(isExpired)
                Text(
                    text = if (isExpired) "这次通行结束了" else "先停一下",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = if (isExpired) "给自己一点停下来的空间。" else "你正准备打开一个受保护的应用。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                AppStatusCard(app.label, app, isExpired)

                if (!isExpired) {
                    PurposeCard(selectedPurpose, onSelect = { selectedPurpose = it })
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (isExpired) {
                    if (canExtend) {
                        Button(
                            onClick = {
                                if (!onExtend()) extensionError = "延长没有成功，请选择结束并回到桌面。"
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PauseGreen),
                        ) {
                            Text("再延长 $extensionMinutes 分钟")
                        }
                        Text(
                            "本次通行仅可延长一次。",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Text(
                            if (extensionConfigured) "本次延长已使用。" else "这条规则没有开启延长。",
                            modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    extensionError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                    EndButton(onEnd)
                } else {
                    Button(
                        onClick = { selectedPurpose?.let(onGrant) },
                        enabled = selectedPurpose != null,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PauseGreen),
                    ) {
                        Text("通行 $passDurationText")
                    }
                    Text(
                        if (selectedPurpose == null) "选择一个目的后，才可以开始通行。" else "到时间后，我会再提醒你一次。",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    OutlinedButton(
                        onClick = onExitBeforeOpen,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PauseGreen),
                    ) {
                        Text("先不打开")
                    }
                }
            }
        }
    }
}

@Composable
private fun InterventionBrand() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(34.dp).background(PauseGreen, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Security, contentDescription = null, tint = PauseMint, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text("停一下", style = MaterialTheme.typography.titleMedium, color = PauseGreen)
    }
}

@Composable
private fun InterventionHero(isExpired: Boolean) {
    Box(
        modifier = Modifier.size(76.dp).background(PauseGreenLight, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (isExpired) Icons.Filled.Timer else Icons.Filled.Security,
            contentDescription = null,
            tint = PauseGreen,
            modifier = Modifier.size(36.dp),
        )
    }
}

@Composable
private fun AppStatusCard(
    appName: String,
    app: com.pausenow.app.ui.component.AppIdentity,
    isExpired: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIdentityIcon(app, 50.dp)
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (isExpired) "临时通行已结束" else "准备开始临时通行",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PurposeCard(selected: PassPurpose?, onSelect: (PassPurpose) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("你打开它想做什么？", style = MaterialTheme.typography.titleMedium)
            Text(
                "明确这一刻的目的，再决定是否通行。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            purposeOptions.forEach { purpose ->
                FilterChip(
                    selected = selected == purpose,
                    onClick = { onSelect(purpose) },
                    label = { Text(purpose.label) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun EndButton(onEnd: () -> Unit) {
    OutlinedButton(
        onClick = onEnd,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = PauseGreen),
    ) {
        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("结束并回到桌面")
    }
}
