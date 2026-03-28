package com.neo.android.ui.loading

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.neo.android.model.DownloadState
import com.neo.android.model.ModelManager
import com.neo.android.ui.theme.AccentGradEnd
import com.neo.android.ui.theme.AccentGradStart
import com.neo.android.ui.theme.AccentPrimary
import com.neo.android.ui.theme.ShadowDark
import com.neo.android.ui.theme.ShadowLight
import com.neo.android.ui.theme.SpatialBg
import com.neo.android.ui.theme.TextMuted
import com.neo.android.ui.theme.TextPrimary
import com.neo.android.ui.theme.TextSecondary
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoadingScreen(
    onReady: () -> Unit,
) {
    val context = LocalContext.current
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var showUsageDialog by remember { mutableStateOf(false) }
    var usagePermissionHandled by remember { mutableStateOf(false) }

    // Check usage stats permission on resume (user may grant it in Settings)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (showUsageDialog && hasUsageStatsPermission(context)) {
                showUsageDialog = false
                usagePermissionHandled = true
            }
        }
    }

    // Start download flow
    LaunchedEffect(Unit) {
        ModelManager.downloadModel(context).collectLatest { state ->
            downloadState = state
            if (state is DownloadState.Ready) {
                // Model downloaded — prompt for usage stats if not yet handled
                if (!usagePermissionHandled && !hasUsageStatsPermission(context)) {
                    showUsageDialog = true
                } else {
                    onReady()
                }
            }
        }
    }

    // If user already has permission after dialog closes, proceed
    LaunchedEffect(usagePermissionHandled) {
        if (usagePermissionHandled && downloadState is DownloadState.Ready) {
            onReady()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpatialBg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Neumorphic spinner ring
            NeumorphicSpinner(downloadState)

            Spacer(Modifier.height(40.dp))

            // Status text
            val (statusText, subText) = when (downloadState) {
                is DownloadState.Idle -> "Preparing…" to ""
                is DownloadState.Downloading -> {
                    val pct = ((downloadState as DownloadState.Downloading).progress * 100).toInt()
                    "Downloading model" to "$pct%"
                }
                is DownloadState.MovingToInternal -> "Setting up…" to "Moving model to storage"
                is DownloadState.Ready -> "Ready" to ""
                is DownloadState.Error -> "Error" to (downloadState as DownloadState.Error).message
            }

            Text(
                text = statusText,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )

            if (subText.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = subText,
                    fontSize = 14.sp,
                    color = if (downloadState is DownloadState.Error) Color(0xFFE53E3E) else TextSecondary,
                )
            }

            // Retry on error
            if (downloadState is DownloadState.Error) {
                Spacer(Modifier.height(20.dp))
                TextButton(onClick = {
                    downloadState = DownloadState.Idle
                    // Re-trigger by recomposition — but we need a new key
                }) {
                    Text("Retry", color = AccentPrimary)
                }
            }
        }
    }

    // Usage stats permission dialog
    if (showUsageDialog) {
        AlertDialog(
            onDismissRequest = {
                showUsageDialog = false
                usagePermissionHandled = true
            },
            title = { Text("Usage Stats Access") },
            text = {
                Text("Neo uses app usage data to personalize responses. " +
                     "Grant usage access on the next screen to enable this feature.")
            },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    )
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUsageDialog = false
                    usagePermissionHandled = true
                }) {
                    Text("Skip")
                }
            },
        )
    }
}

@Composable
private fun NeumorphicSpinner(state: DownloadState) {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
        ),
        label = "rotation",
    )

    val size = 120.dp
    val neuOffset = 8.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .drawBehind {
                // Dark shadow (bottom-right)
                drawCircle(
                    color = ShadowDark,
                    radius = this.size.minDimension / 2,
                    center = center + Offset(neuOffset.toPx(), neuOffset.toPx()),
                )
                // Light shadow (top-left)
                drawCircle(
                    color = ShadowLight,
                    radius = this.size.minDimension / 2,
                    center = center + Offset(-neuOffset.toPx(), -neuOffset.toPx()),
                )
            }
            .background(SpatialBg, CircleShape),
    ) {
        val progress = when (state) {
            is DownloadState.Downloading -> state.progress
            is DownloadState.Ready -> 1f
            else -> null
        }

        if (progress != null) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(80.dp),
                strokeWidth = 6.dp,
                trackColor = SpatialBg,
                color = AccentPrimary,
                strokeCap = StrokeCap.Round,
            )
        } else {
            // Indeterminate spinning gradient arc
            CircularProgressIndicator(
                modifier = Modifier
                    .size(80.dp)
                    .rotate(rotation),
                strokeWidth = 6.dp,
                trackColor = SpatialBg,
                color = AccentPrimary,
                strokeCap = StrokeCap.Round,
            )
        }

        // Center percentage text for download
        if (state is DownloadState.Downloading) {
            val pct = (state.progress * 100).toInt()
            Text(
                text = "$pct%",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
        }
    }
}

private fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.unsafeCheckOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName,
    )
    return mode == AppOpsManager.MODE_ALLOWED
}
