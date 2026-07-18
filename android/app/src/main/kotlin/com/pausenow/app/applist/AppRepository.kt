package com.pausenow.app.applist

import android.content.Context
import android.content.Intent
import com.pausenow.app.model.ManagedApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 查询可启动应用列表（MAIN+LAUNCHER）。
 * 依赖 manifest 的 <queries> 声明，无需 QUERY_ALL_PACKAGES（符合 ADR-006）。
 */
class AppRepository(context: Context) {
    private val packageManager = context.packageManager

    suspend fun loadLaunchableApps(): List<ManagedApp> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(intent, 0)
            .map { resolveInfo ->
                ManagedApp(
                    packageName = resolveInfo.activityInfo.packageName,
                    label = resolveInfo.loadLabel(packageManager).toString(),
                    icon = resolveInfo.loadIcon(packageManager),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
