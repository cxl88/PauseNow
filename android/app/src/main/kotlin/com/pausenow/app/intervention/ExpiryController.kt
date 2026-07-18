package com.pausenow.app.intervention

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pausenow.app.MainActivity
import com.pausenow.app.R
import com.pausenow.app.notifications.NotificationChannels

/**
 * 到期触发：AlarmManager 精确闹钟在 [expiresAtMs] 触发 [ExpiryAlarmReceiver] 发高优先级通知。
 * 事件驱动到期（用户下次交互时弹干预）由检测循环 + RuleEngine + InterventionLauncher 负责，
 * 这里是"到时即提醒"的兜底（docs/03 §5.3 Pivot 条件）。
 */
class ExpiryController(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleExpiry(packageName: String, expiresAtMs: Long) {
        if (System.currentTimeMillis() >= expiresAtMs) return
        val pi = pendingIntent(packageName)
        alarmManager.cancel(pi)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, expiresAtMs, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, expiresAtMs, pi)
        }
        Log.i(TAG, "scheduled packageName=$packageName expiresAtMs=$expiresAtMs exact=$canExact")
    }

    fun cancelExpiry(packageName: String) {
        alarmManager.cancel(pendingIntent(packageName))
    }

    private fun pendingIntent(packageName: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            packageName.hashCode(),
            Intent(context, ExpiryAlarmReceiver::class.java).putExtra(EXTRA_PACKAGE, packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private companion object {
        const val TAG = "PauseNow.Expiry"
        const val EXTRA_PACKAGE = "pausenow.expiry.package"
    }
}

/**
 * 接收到期闹钟 -> 发高优先级通知。点击通知打开 MainActivity。
 * 真正的到期拦截弹页由检测循环在用户下次打开目标应用时触发（事件驱动）。
 */
class ExpiryAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.getStringExtra(EXTRA_PACKAGE) ?: return
        NotificationChannels.ensureChannels(context)
        val contentIntent = PendingIntent.getActivity(
            context,
            packageName.hashCode(),
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, NotificationChannels.EXPIRY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("通行已到期")
            .setContentText("$packageName 的限时通行已结束，下次打开将拦截")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("PauseNow.Expiry", "skip notify packageName=$packageName (no notification permission)")
            return
        }
        runCatching {
            NotificationManagerCompat.from(context)
                .notify(NotificationChannels.EXPIRY_NOTIFICATION_ID + packageName.hashCode(), notification)
        }
        Log.i("PauseNow.Expiry", "fired packageName=$packageName")
    }

    private companion object {
        const val EXTRA_PACKAGE = "pausenow.expiry.package"
    }
}
