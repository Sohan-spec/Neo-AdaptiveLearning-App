package com.neo.android.ui.dashboard

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neo.android.ui.chat.neuShadow
import com.neo.android.ui.theme.AccentGradEnd
import com.neo.android.ui.theme.AccentGradStart
import com.neo.android.ui.theme.AccentPrimary
import com.neo.android.ui.theme.BorderDim
import com.neo.android.ui.theme.BorderLight
import com.neo.android.ui.theme.DmSans
import com.neo.android.ui.theme.SpatialBg
import com.neo.android.ui.theme.SpatialSurface
import com.neo.android.ui.theme.SpatialSurfaceRaised
import com.neo.android.ui.theme.TextMuted
import com.neo.android.ui.theme.TextPrimary
import com.neo.android.ui.theme.TextSecondary
import com.neo.android.usage.AppUsageInfo

@Composable
fun UsageStatsDashboardScreen(
    onBack: () -> Unit,
    vm: UsageStatsDashboardViewModel = viewModel(),
) {
    val hasPermission by vm.hasPermission.collectAsState()
    val apps by vm.apps.collectAsState()
    val totalScreenTime by vm.totalScreenTime.collectAsState()
    val mostUsedApp by vm.mostUsedApp.collectAsState()
    val selectedBadge by vm.selectedBadge.collectAsState()
    val llmResponse by vm.llmResponse.collectAsState()
    val isGenerating by vm.isGenerating.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpatialBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // ── Header ───────────────────────────────────────────
        DashboardHeader(onBack = onBack)

        if (!hasPermission) {
            PermissionPrompt()
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // ── Summary Cards ────────────────────────────
                SummaryCardsRow(
                    totalMinutes = totalScreenTime,
                    mostUsedApp = mostUsedApp,
                    appCount = apps.size,
                )

                Spacer(modifier = Modifier.height(28.dp))

                // ── Top Apps ─────────────────────────────────
                SectionTitle("TOP APPS (24H)")
                Spacer(modifier = Modifier.height(12.dp))
                TopAppsList(apps = apps)

                Spacer(modifier = Modifier.height(28.dp))

                // ── Divider ──────────────────────────────────
                GradientDivider()

                Spacer(modifier = Modifier.height(28.dp))

                // ── AI Analysis ──────────────────────────────
                SectionTitle("AI ANALYSIS")
                Spacer(modifier = Modifier.height(12.dp))
                BadgeRow(
                    selectedBadge = selectedBadge,
                    onBadgeClick = { vm.generateContent(it) },
                )

                Spacer(modifier = Modifier.height(16.dp))

                // ── LLM Response Card ────────────────────────
                AnimatedVisibility(
                    visible = selectedBadge != null,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(200)),
                ) {
                    LlmResponseCard(
                        response = llmResponse,
                        isGenerating = isGenerating,
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ── Header ───────────────────────────────────────────────────────
@Composable
private fun DashboardHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Back button — spatial-ui btn-ghost style
        Box(
            modifier = Modifier
                .size(36.dp)
                .neuShadow(
                    cornerRadius = 10.dp,
                    darkColor = Color(0x44A3B1C6),
                    lightColor = Color(0xAAFFFFFF),
                    darkOffset = 3.dp,
                    lightOffset = (-2).dp,
                    blur = 6.dp,
                )
                .background(SpatialSurfaceRaised, RoundedCornerShape(10.dp))
                .border(1.dp, BorderLight, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = "Usage Stats",
            fontFamily = DmSans,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            letterSpacing = (-0.03).sp,
        )
    }

    // Divider
    GradientDivider()
}

// ── Summary Cards Row ────────────────────────────────────────────
@Composable
private fun SummaryCardsRow(totalMinutes: Long, mostUsedApp: String, appCount: Int) {
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    val timeText = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SummaryCard(
            label = "SCREEN TIME",
            value = timeText,
            modifier = Modifier.weight(1f),
        )
        SummaryCard(
            label = "MOST USED",
            value = mostUsedApp,
            modifier = Modifier.weight(1f),
        )
        SummaryCard(
            label = "APPS TRACKED",
            value = appCount.toString(),
            modifier = Modifier.weight(1f),
        )
    }
}

// spatial-ui: .spatial-card with inner stat boxes (.shadow-outer-sm, .surface, .border-light)
@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .neuShadow(
                cornerRadius = 14.dp,
                darkColor = Color(0x66A3B1C6),
                lightColor = Color(0xCCFFFFFF),
                darkOffset = 4.dp,
                lightOffset = (-2).dp,
                blur = 8.dp,
            )
            .background(SpatialSurface, RoundedCornerShape(14.dp))
            .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Column {
            Text(
                text = label,
                fontFamily = DmSans,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted,
                letterSpacing = 0.06.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontFamily = DmSans,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = (-0.02).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Top Apps List ────────────────────────────────────────────────
@Composable
private fun TopAppsList(apps: List<AppUsageInfo>) {
    if (apps.isEmpty()) {
        Text(
            text = "No app usage data available.",
            fontFamily = DmSans,
            fontSize = 14.sp,
            color = TextMuted,
        )
        return
    }

    val maxMinutes = apps.maxOf { it.totalMinutes }.coerceAtLeast(1)

    // spatial-ui: .spatial-card wrapper
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neuShadow(
                cornerRadius = 18.dp,
                darkColor = Color(0x66A3B1C6),
                lightColor = Color(0xCCFFFFFF),
                darkOffset = 6.dp,
                lightOffset = (-4).dp,
                blur = 14.dp,
            )
            .background(SpatialSurface, RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(BorderLight, BorderDim),
                ),
                shape = RoundedCornerShape(18.dp),
            )
            .clip(RoundedCornerShape(18.dp))
            .padding(vertical = 8.dp),
    ) {
        apps.forEachIndexed { index, app ->
            AppUsageRow(
                rank = index + 1,
                app = app,
                fraction = app.totalMinutes.toFloat() / maxMinutes,
            )
        }
    }
}

@Composable
private fun AppUsageRow(rank: Int, app: AppUsageInfo, fraction: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Rank badge — spatial-ui: .badge-neutral
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    Color(0x80FFFFFF),
                    RoundedCornerShape(8.dp),
                )
                .border(1.dp, BorderLight, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = rank.toString(),
                fontFamily = DmSans,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                fontFamily = DmSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Usage bar — spatial-ui: --accent-grad fill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color(0x1AA3B1C6), RoundedCornerShape(3.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(AccentGradStart, AccentGradEnd),
                            ),
                            RoundedCornerShape(3.dp),
                        ),
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "${app.totalMinutes} min",
            fontFamily = DmSans,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
        )
    }
}

// ── Badge Row ────────────────────────────────────────────────────
@Composable
private fun BadgeRow(selectedBadge: BadgeType?, onBadgeClick: (BadgeType) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BadgeType.entries.forEach { badge ->
            val isSelected = badge == selectedBadge
            AnalysisBadge(
                label = badge.label,
                icon = badge.icon,
                isSelected = isSelected,
                onClick = { onBadgeClick(badge) },
            )
        }
    }
}

// spatial-ui: selected = .btn-solid (accent-grad + white text)
//             unselected = .btn-ghost (surface bg + accent text + border-light + shadow-outer-sm)
@Composable
private fun AnalysisBadge(
    label: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(100.dp) // pill

    Box(
        modifier = Modifier
            .then(
                if (isSelected) {
                    Modifier
                        .background(
                            Brush.linearGradient(listOf(AccentGradStart, AccentGradEnd)),
                            shape,
                        )
                        .border(1.dp, Color(0x40FFFFFF), shape)
                } else {
                    Modifier
                        .neuShadow(
                            cornerRadius = 100.dp,
                            darkColor = Color(0x44A3B1C6),
                            lightColor = Color(0xAAFFFFFF),
                            darkOffset = 3.dp,
                            lightOffset = (-2).dp,
                            blur = 6.dp,
                        )
                        .background(SpatialSurface, shape)
                        .border(1.dp, BorderLight, shape)
                },
            )
            .clip(shape)
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = icon,
                fontSize = 14.sp,
            )
            Text(
                text = label,
                fontFamily = DmSans,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Color.White else AccentPrimary,
                letterSpacing = (-0.01).sp,
            )
        }
    }
}

// ── LLM Response Card ────────────────────────────────────────────
// spatial-ui: .spatial-card (shadow-outer-lg, surface, border gradient, radius-xl)
@Composable
private fun LlmResponseCard(response: String, isGenerating: Boolean) {
    val cardShape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neuShadow(
                cornerRadius = 18.dp,
                darkColor = Color(0x66A3B1C6),
                lightColor = Color(0xCCFFFFFF),
                darkOffset = 6.dp,
                lightOffset = (-4).dp,
                blur = 14.dp,
            )
            .background(SpatialSurface, cardShape)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(BorderLight, BorderDim),
                ),
                shape = cardShape,
            )
            .clip(cardShape)
            .padding(20.dp),
    ) {
        if (response.isEmpty() && isGenerating) {
            // Pulsing loading indicator
            PulsingText("Generating…")
        } else if (response.isNotEmpty()) {
            Text(
                text = response,
                fontFamily = DmSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextPrimary,
                lineHeight = 22.sp,
            )
            if (isGenerating) {
                Spacer(modifier = Modifier.height(8.dp))
                PulsingText("●")
            }
        }
    }
}

@Composable
private fun PulsingText(text: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    Text(
        text = text,
        fontFamily = DmSans,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = TextMuted.copy(alpha = alpha),
    )
}

// ── Permission Prompt ────────────────────────────────────────────
@Composable
private fun PermissionPrompt() {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Usage Access Required",
                fontFamily = DmSans,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Grant usage access permission to view your app usage statistics.",
                fontFamily = DmSans,
                fontSize = 14.sp,
                color = TextSecondary,
                lineHeight = 20.sp,
            )
            Spacer(modifier = Modifier.height(24.dp))

            // spatial-ui: .btn-solid (accent gradient, white text, accent shadow)
            Box(
                modifier = Modifier
                    .neuShadow(
                        cornerRadius = 14.dp,
                        darkColor = Color(0x73435FEE),
                        lightColor = Color(0x66FFFFFF),
                        darkOffset = 4.dp,
                        lightOffset = (-2).dp,
                        blur = 10.dp,
                    )
                    .background(
                        Brush.linearGradient(listOf(AccentGradStart, AccentGradEnd)),
                        RoundedCornerShape(14.dp),
                    )
                    .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .clickable {
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Open Settings",
                    fontFamily = DmSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}

// ── Utilities ────────────────────────────────────────────────────
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontFamily = DmSans,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextMuted,
        letterSpacing = 0.1.sp,
    )
}

// spatial-ui: .spatial-divider (linear-gradient 90deg transparent→mid→transparent)
@Composable
private fun GradientDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp)
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        Color(0x66A3B1C6),
                        Color(0x66A3B1C6),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}
