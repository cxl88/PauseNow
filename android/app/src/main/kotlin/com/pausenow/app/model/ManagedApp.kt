package com.pausenow.app.model

import android.graphics.drawable.Drawable

/** 用户可选的目标应用。 */
data class ManagedApp(
    val packageName: String,
    val label: String,
    val icon: Drawable,
)
