package com.neo.android.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import java.util.Calendar

data class AppUsageInfo(
    val appName: String,
    val packageName: String,
    val totalMinutes: Long,
)

object UsageStatsHelper {

    fun hasPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getTopApps(context: Context, count: Int = 3): List<AppUsageInfo> {
        if (!hasPermission(context)) return emptyList()

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val cal = Calendar.getInstance()
        val endTime = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis

        val statsMap = usm.queryAndAggregateUsageStats(startTime, endTime)
            ?: return emptyList()

        val pm = context.packageManager

        val systemPrefixes = listOf(
            "com.android.", "android", "com.google.android.inputmethod",
            "com.google.android.permissioncontroller",
            "com.google.android.ext.", "com.samsung.android.app.routines",
            "com.sec.android.", "com.google.android.providers.",
        )

        return statsMap.values
            .filter { stats ->
                val pkg = stats.packageName
                val minutes = stats.totalTimeInForeground / 60_000
                minutes > 0 &&
                    pkg != context.packageName &&
                    systemPrefixes.none { pkg.startsWith(it) } &&
                    isLaunchableApp(pm, pkg)
            }
            .sortedByDescending { it.totalTimeInForeground }
            .take(count)
            .map { stats ->
                val appName = try {
                    val ai = pm.getApplicationInfo(stats.packageName, 0)
                    pm.getApplicationLabel(ai).toString()
                } catch (_: PackageManager.NameNotFoundException) {
                    stats.packageName
                }
                AppUsageInfo(
                    appName = appName,
                    packageName = stats.packageName,
                    totalMinutes = stats.totalTimeInForeground / 60_000,
                )
            }
    }

    private fun isLaunchableApp(pm: PackageManager, packageName: String): Boolean {
        return pm.getLaunchIntentForPackage(packageName) != null
    }

    fun formatForPrompt(apps: List<AppUsageInfo>): String {
        if (apps.isEmpty()) return "No app usage data available for today."
        return apps.mapIndexed { index, app ->
            "${index + 1}. ${app.appName} — ${app.totalMinutes} min"
        }.joinToString("\n")
    }
}
