package com.neo.android.ui.dashboard

import android.app.Application
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neo.android.engine.LlmEngine
import com.neo.android.usage.AppUsageInfo
import com.neo.android.usage.UsageStatsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class BadgeType(val label: String, @DrawableRes val icon: Int) {
    SUMMARISE("Summarise", com.neo.android.R.drawable.ic_badge_summarise),
    INSIGHTS("Insights", com.neo.android.R.drawable.ic_badge_insights),
    SUGGESTIONS("Suggestions", com.neo.android.R.drawable.ic_badge_suggestions),
    PATTERNS("Patterns", com.neo.android.R.drawable.ic_badge_patterns),
}

class UsageStatsDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "UsageDashboardVM"
    private val context get() = getApplication<Application>()

    private val _apps = MutableStateFlow<List<AppUsageInfo>>(emptyList())
    val apps: StateFlow<List<AppUsageInfo>> = _apps.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _totalScreenTime = MutableStateFlow(0L)
    val totalScreenTime: StateFlow<Long> = _totalScreenTime.asStateFlow()

    private val _mostUsedApp = MutableStateFlow("")
    val mostUsedApp: StateFlow<String> = _mostUsedApp.asStateFlow()

    private val _selectedBadge = MutableStateFlow<BadgeType?>(null)
    val selectedBadge: StateFlow<BadgeType?> = _selectedBadge.asStateFlow()

    private val _llmResponse = MutableStateFlow("")
    val llmResponse: StateFlow<String> = _llmResponse.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private var inferenceJob: Job? = null

    init {
        loadUsageData()
    }

    fun loadUsageData() {
        viewModelScope.launch {
            val permitted = UsageStatsHelper.hasPermission(context)
            _hasPermission.value = permitted

            if (permitted) {
                val topApps = withContext(Dispatchers.IO) {
                    UsageStatsHelper.getTopApps(context, 5)
                }
                _apps.value = topApps
                _totalScreenTime.value = topApps.sumOf { it.totalMinutes }
                _mostUsedApp.value = topApps.firstOrNull()?.appName ?: "—"
            }
        }
    }

    fun generateContent(badgeType: BadgeType) {
        inferenceJob?.cancel()
        _selectedBadge.value = badgeType
        _llmResponse.value = ""
        _isGenerating.value = true

        val appList = _apps.value
        if (appList.isEmpty()) {
            _llmResponse.value = "No usage data available to analyze."
            _isGenerating.value = false
            return
        }

        val usageData = UsageStatsHelper.formatForPrompt(appList)
        val systemPrompt = buildSystemPromptForBadge(badgeType, usageData)
        val prompt = buildLlmPrompt(systemPrompt)

        Log.d(TAG, "Generating content for badge: ${badgeType.label}")

        inferenceJob = viewModelScope.launch {
            val responseBuilder = StringBuilder()

            LlmEngine.runInference(prompt, maxTokens = 400)
                .catch { e ->
                    Log.e(TAG, "Inference error", e)
                    _llmResponse.value = "Sorry, something went wrong. Please try again."
                    _isGenerating.value = false
                }
                .collect { token ->
                    responseBuilder.append(token)
                    _llmResponse.value = responseBuilder.toString()
                }

            _isGenerating.value = false
        }
    }

    private fun buildSystemPromptForBadge(badgeType: BadgeType, usageData: String): String {
        val roleAndTask = when (badgeType) {
            BadgeType.SUMMARISE ->
                "You are a usage analyst. Given the user's app usage data, provide a brief 3-4 sentence summary of their screen time habits over the last 24 hours. Mention specific app names and times."

            BadgeType.INSIGHTS ->
                "You are a behavioral analyst. Given the user's app usage data, identify 2-3 interesting patterns or insights about their app usage behavior. Be specific and reference actual app names and times from the data."

            BadgeType.SUGGESTIONS ->
                "You are a digital wellness advisor. Given the user's app usage data, suggest 2-3 actionable ways to optimize or improve their screen time habits. Be constructive, not judgmental. Reference specific apps from the data."

            BadgeType.PATTERNS ->
                "You are a data analyst. Given the user's app usage data, describe usage patterns including app categories, time distribution, and app diversity. Use the actual data provided."
        }

        return buildString {
            append(roleAndTask)
            append("\n\n")
            append("User's app usage data (last 24 hours):\n")
            append(usageData)
            append("\n\n")
            append("Rules:\n")
            append("- Quote EXACT app names and times from the data above. NEVER invent numbers.\n")
            append("- Keep your response concise and well-structured.\n")
            append("- Use plain text, no markdown formatting.")
        }
    }

    private fun buildLlmPrompt(systemPrompt: String): String {
        return buildString {
            append("<|begin_of_text|>")
            append("<|start_header_id|>system<|end_header_id|>\n\n")
            append(systemPrompt)
            append("<|eot_id|>")
            append("<|start_header_id|>user<|end_header_id|>\n\n")
            append("Go ahead.")
            append("<|eot_id|>")
            append("<|start_header_id|>assistant<|end_header_id|>\n\n")
        }
    }

    override fun onCleared() {
        super.onCleared()
        inferenceJob?.cancel()
    }
}
