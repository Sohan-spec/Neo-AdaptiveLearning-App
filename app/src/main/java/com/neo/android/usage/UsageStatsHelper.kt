package com.neo.android.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process

data class AppUsageInfo(
    val appName: String,
    val packageName: String,
    val totalMinutes: Long,
)

object UsageStatsHelper {

    fun hasPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        // checkOpNoThrow is the non-deprecated equivalent of unsafeCheckOpNoThrow
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getTopApps(context: Context, count: Int = 3): List<AppUsageInfo> {
        if (!hasPermission(context)) return emptyList()

        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        // Rolling 24-hour window. "Today midnight → now" can be only a few hours
        // of data (e.g. 3 AM = 3 hours), which will appear empty on most devices.
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 24L * 60 * 60 * 1000

        val statsMap = usm.queryAndAggregateUsageStats(startTime, endTime)
            ?: return emptyList()

        val pm = context.packageManager

        return statsMap.values
            .filter { stats ->
                val minutes = stats.totalTimeInForeground / 60_000
                minutes > 0 &&
                    stats.packageName != context.packageName &&
                    isUserFacingApp(pm, stats.packageName)
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

    /**
     * Returns true for apps the user actually chose to install or that are
     * updatable system apps (Maps, Gmail, Chrome, etc.).
     *
     * getLaunchIntentForPackage() is NOT used here because Android 11+ package-
     * visibility restrictions cause it to return null for most user apps unless
     * the calling app declares explicit <queries> entries — making it useless as
     * a filter and causing system apps (exempt from visibility rules) to pass
     * while real user apps (WhatsApp, YouTube, etc.) are incorrectly excluded.
     *
     * FLAG_SYSTEM is set on all apps baked into the system image.
     * FLAG_UPDATED_SYSTEM_APP is additionally set when such an app has been
     * updated via the Play Store — these are meaningful user-facing apps.
     */
    private fun isUserFacingApp(pm: PackageManager, packageName: String): Boolean {
        return try {
            val info = pm.getApplicationInfo(packageName, 0)
            val isSystem = (info.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystem = (info.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            // Include: pure user-installed apps OR updatable system apps (Maps, Gmail…)
            // Exclude: stock system apps (Settings, Call, Launcher, etc.)
            !isSystem || isUpdatedSystem
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun formatForPrompt(apps: List<AppUsageInfo>): String {
        if (apps.isEmpty()) return "No app usage data available for the last 24 hours."
        return apps.mapIndexed { index, app ->
            "${index + 1}. ${app.appName} — ${app.totalMinutes} min"
        }.joinToString("\n")
    }
}
