package com.pausenow.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * 通知渠道：常驻前台服务通知（低优先级）+ 通行到期高优先级提醒。
 */
object NotificationChannels {
    const val FOREGROUND_SERVICE_CHANNEL_ID = "pausenow_foreground"
    const val EXPIRY_CHANNEL_ID = "pausenow_expiry"

    const val FOREGROUND_NOTIFICATION_ID = 1001
    const val EXPIRY_NOTIFICATION_ID = 1002

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val foreground = NotificationChannel(
            FOREGROUND_SERVICE_CHANNEL_ID,
            "停一下保护",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "保护运行中的常驻状态通知"
            setShowBadge(false)
        }

        val expiry = NotificationChannel(
            EXPIRY_CHANNEL_ID,
            "通行到期提醒",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "限时通行到期时的高优先级提醒"
        }

        manager.createNotificationChannel(foreground)
        manager.createNotificationChannel(expiry)
    }
}
