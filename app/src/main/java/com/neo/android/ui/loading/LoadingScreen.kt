package com.neo.android.ui.loading

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.neo.android.model.DownloadState
import com.neo.android.model.ModelManager
import com.neo.android.ui.chat.neuShadow
import com.neo.android.ui.theme.AccentGradEnd
import com.neo.android.ui.theme.AccentGradStart
import com.neo.android.ui.theme.AccentPrimary
import com.neo.android.ui.theme.BorderDark
import com.neo.android.ui.theme.BorderLight
import com.neo.android.ui.theme.ColorDanger
import com.neo.android.ui.theme.SpatialBg
import com.neo.android.ui.theme.SpatialSurface
import com.neo.android.ui.theme.SpatialSurfaceRaised
import com.neo.android.ui.theme.TextMuted
import com.neo.android.ui.theme.TextPrimary
import com.neo.android.ui.theme.TextSecondary
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoadingScreen(onReady: () -> Unit) {
    val context = LocalContext.current
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var embeddingState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var showUsageDialog by remember { mutableStateOf(false) }
    var usagePermissionHandled by remember { mutableStateOf(false) }

    val llmReady = downloadState is DownloadState.Ready
    val embeddingReady = embeddingState is DownloadState.Ready

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (showUsageDialog && hasUsageStatsPermission(context)) {
                showUsageDialog = false
                usagePermissionHandled = true
            }
        }
    }

    LaunchedEffect(Unit) {
        ModelManager.downloadModel(context).collectLatest { state -> downloadState = state }
    }
    LaunchedEffect(Unit) {
        ModelManager.downloadEmbeddingModel(context).collectLatest { state -> embeddingState = state }
    }

    LaunchedEffect(llmReady, embeddingReady, usagePermissionHandled) {
        if (llmReady && embeddingReady) {
            if (!usagePermissionHandled && !hasUsageStatsPermission(context)) {
                showUsageDialog = true
            } else {
                onReady()
            }
        }
    }
    LaunchedEffect(usagePermissionHandled) {
        if (usagePermissionHandled && llmReady && embeddingReady) onReady()
    }

    Box(modifier = Modifier.fillMaxSize().background(SpatialBg)) {
        // ── Ambient background orbs ────────────────────────────
        AmbientOrbs()

        // ── Main content ────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // App wordmark
            Text(
                text = "Neo",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = (-1).sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Getting things ready…",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted,
            )

            Spacer(Modifier.height(52.dp))

            // ── Progress card ───────────────────────────────────
            DownloadCard(downloadState, embeddingState)

            // ── Error retry ─────────────────────────────────────
            val hasError = downloadState is DownloadState.Error || embeddingState is DownloadState.Error
            AnimatedVisibility(visible = hasError) {
                Spacer(Modifier.height(16.dp))
            }
            AnimatedVisibility(visible = hasError) {
                Box(
                    modifier = Modifier
                        .neuShadow(cornerRadius = 14.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SpatialSurface)
                        .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { downloadState = DownloadState.Idle }
                        .padding(horizontal = 28.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "Try Again",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentPrimary,
                    )
                }
            }
        }

        // ── Permissions overlay ─────────────────────────────────
        AnimatedVisibility(
            visible = showUsageDialog,
            enter = fadeIn(tween(300)) + slideInVertically(tween(400, easing = FastOutSlowInEasing)) { it / 2 },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(250)) { it / 2 },
        ) {
            UsagePermissionOverlay(
                onGrant = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                onSkip = {
                    showUsageDialog = false
                    usagePermissionHandled = true
                },
            )
        }
    }
}

// ── Download progress card ──────────────────────────────────────────
@Composable
private fun DownloadCard(downloadState: DownloadState, embeddingState: DownloadState) {
    val llmProgress = when (downloadState) {
        is DownloadState.Downloading -> downloadState.progress
        is DownloadState.MovingToInternal, is DownloadState.Ready -> 1f
        else -> 0f
    }
    val embProgress = when (embeddingState) {
        is DownloadState.Downloading -> embeddingState.progress
        is DownloadState.MovingToInternal, is DownloadState.Ready -> 1f
        else -> 0f
    }
    val combinedProgress = (llmProgress * 0.85f + embProgress * 0.15f)
    val animatedProgress by animateFloatAsState(
        targetValue = combinedProgress,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "progress",
    )

    val (label, sub) = when {
        downloadState is DownloadState.Error ->
            "Download failed" to ((downloadState as DownloadState.Error).message)
        embeddingState is DownloadState.Error ->
            "Setup failed" to ((embeddingState as DownloadState.Error).message)
        downloadState is DownloadState.Downloading -> {
            val pct = (downloadState.progress * 100).toInt()
            "Downloading AI model" to "$pct% complete"
        }
        downloadState is DownloadState.MovingToInternal ->
            "Installing AI model" to "Almost there…"
        embeddingState is DownloadState.Downloading -> {
            val pct = (embeddingState.progress * 100).toInt()
            "Setting up memory" to "$pct% complete"
        }
        embeddingState is DownloadState.MovingToInternal ->
            "Finalising setup" to "One moment…"
        downloadState is DownloadState.Ready && embeddingState is DownloadState.Ready ->
            "All set" to "Starting Neo…"
        else ->
            "Preparing" to "Connecting to download…"
    }

    val isError = downloadState is DownloadState.Error || embeddingState is DownloadState.Error
    val isIndeterminate = downloadState is DownloadState.Idle ||
        downloadState is DownloadState.MovingToInternal ||
        embeddingState is DownloadState.MovingToInternal

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neuShadow(cornerRadius = 24.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SpatialSurfaceRaised)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(BorderLight, Color(0x26FFFFFF))),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(28.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Arc progress ring
            GradientArcRing(
                progress = if (isIndeterminate) null else animatedProgress,
                isError = isError,
            )

            Spacer(Modifier.height(28.dp))

            // Status label
            Text(
                text = label,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isError) ColorDanger else TextPrimary,
                textAlign = TextAlign.Center,
            )

            if (sub.isNotEmpty()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = sub,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isError) ColorDanger.copy(alpha = 0.7f) else TextMuted,
                    textAlign = TextAlign.Center,
                )
            }

            // Step indicators
            Spacer(Modifier.height(24.dp))
            StepRow(
                label = "AI Model",
                state = downloadState,
                weight = 0.85f,
            )
            Spacer(Modifier.height(10.dp))
            StepRow(
                label = "Memory Engine",
                state = embeddingState,
                weight = 0.15f,
            )
        }
    }
}

@Composable
private fun StepRow(label: String, state: DownloadState, weight: Float) {
    val pct = when (state) {
        is DownloadState.Downloading -> (state.progress * 100).toInt()
        is DownloadState.MovingToInternal -> 100
        is DownloadState.Ready -> 100
        else -> 0
    }
    val animPct by animateFloatAsState(
        targetValue = pct.toFloat(),
        animationSpec = tween(500),
        label = "step",
    )
    val isReady = state is DownloadState.Ready
    val isError = state is DownloadState.Error

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Dot indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = when {
                        isError -> ColorDanger
                        isReady -> Color(0xFF38A169)
                        pct > 0 -> AccentPrimary
                        else -> Color(0xFFCBD5E0)
                    },
                    shape = CircleShape,
                ),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = when {
                isError -> "Error"
                isReady -> "Done"
                pct > 0 -> "${animPct.toInt()}%"
                else -> "Waiting"
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = when {
                isError -> ColorDanger
                isReady -> Color(0xFF38A169)
                else -> TextMuted
            },
        )
    }
}

// ── Gradient arc ring ───────────────────────────────────────────────
@Composable
private fun GradientArcRing(progress: Float?, isError: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val sweep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )

    val arcGradient = Brush.sweepGradient(
        listOf(AccentGradStart, AccentGradEnd, AccentGradStart),
    )
    val errorColor = ColorDanger

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(120.dp),
    ) {
        // Neumorphic backing disc
        Box(
            modifier = Modifier
                .size(120.dp)
                .neuShadow(
                    cornerRadius = 60.dp,
                    darkOffset = 6.dp,
                    lightOffset = (-4).dp,
                    blur = 12.dp,
                )
                .background(SpatialBg, CircleShape),
        )

        // Arc drawn with Canvas
        androidx.compose.foundation.Canvas(modifier = Modifier.size(100.dp)) {
            val strokeWidth = 7.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val topLeft = Offset((size.width - radius * 2) / 2, (size.height - radius * 2) / 2)
            val arcSize = Size(radius * 2, radius * 2)

            // Track
            drawArc(
                color = SpatialBg,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )

            if (isError) {
                drawArc(
                    color = errorColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                )
            } else if (progress != null) {
                drawArc(
                    brush = arcGradient,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                )
            } else {
                // Indeterminate: spinning segment
                drawArc(
                    brush = arcGradient,
                    startAngle = sweep - 90f,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round),
                )
            }
        }

        // Centre label
        if (progress != null && !isError) {
            val pct = (progress * 100).toInt()
            Text(
                text = "$pct%",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
        }
    }
}

// ── Ambient floating orbs ────────────────────────────────────────────
@Composable
private fun AmbientOrbs() {
    val inf = rememberInfiniteTransition(label = "orbs")
    val pulse by inf.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Top-right orb
        Box(
            modifier = Modifier
                .size((260 * pulse).dp)
                .align(Alignment.TopEnd)
                .blur(60.dp)
                .background(
                    Brush.radialGradient(
                        listOf(AccentGradStart.copy(alpha = 0.18f), Color.Transparent),
                    ),
                    CircleShape,
                ),
        )
        // Bottom-left orb
        Box(
            modifier = Modifier
                .size((220 * (1.3f - pulse * 0.3f)).dp)
                .align(Alignment.BottomStart)
                .blur(70.dp)
                .background(
                    Brush.radialGradient(
                        listOf(AccentGradEnd.copy(alpha = 0.14f), Color.Transparent),
                    ),
                    CircleShape,
                ),
        )
    }
}

// ── Usage permissions overlay ────────────────────────────────────────
@Composable
private fun UsagePermissionOverlay(onGrant: () -> Unit, onSkip: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x33000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSkip,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .neuShadow(cornerRadius = 28.dp, darkOffset = 8.dp, lightOffset = (-6).dp, blur = 18.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(SpatialSurfaceRaised)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(BorderLight, Color(0x40FFFFFF))),
                    shape = RoundedCornerShape(28.dp),
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(horizontal = 28.dp, vertical = 32.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Icon badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .neuShadow(cornerRadius = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SpatialSurface)
                        .border(1.dp, BorderLight, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "📊", fontSize = 24.sp)
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Personalise Neo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Neo can use your app usage to give smarter, more relevant responses. " +
                        "You can always change this later in Settings.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                )

                Spacer(Modifier.height(28.dp))

                // Grant button (accent gradient)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .neuShadow(cornerRadius = 14.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(listOf(AccentGradStart, AccentGradEnd)),
                        )
                        .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onGrant,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Allow Access",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Skip button (ghost)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SpatialSurface)
                        .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onSkip,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Maybe Later",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                    )
                }
            }
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
