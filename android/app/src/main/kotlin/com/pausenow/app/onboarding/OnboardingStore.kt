package com.pausenow.app.onboarding

import android.content.Context

/** Onboarding 完成标志（SharedPreferences）。 */
class OnboardingStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isCompleted(): Boolean = prefs.getBoolean(KEY, false)

    fun setCompleted() {
        prefs.edit().putBoolean(KEY, true).apply()
    }

    private companion object {
        const val PREFS = "pausenow_onboarding"
        const val KEY = "completed"
    }
}
