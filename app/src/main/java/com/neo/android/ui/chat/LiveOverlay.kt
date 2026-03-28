package com.neo.android.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neo.android.ui.theme.AccentGradEnd
import com.neo.android.ui.theme.AccentGradStart
import com.neo.android.ui.theme.AccentPrimary
import com.neo.android.ui.theme.BorderLight
import com.neo.android.ui.theme.DmSans
import com.neo.android.ui.theme.ShadowDark
import com.neo.android.ui.theme.ShadowLight
import com.neo.android.ui.theme.TextMuted
import com.neo.android.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun BoxScope.LiveOverlay(
    visible: Boolean,
    liveMicState: MicState = MicState.IDLE,
    livePartialText: String = "",
    onClose: () -> Unit,
) {
    val statusText = when (liveMicState) {
        MicState.LISTENING -> "Listening..."
        MicState.SPEAKING  -> "Speaking..."
        MicState.IDLE      -> "Thinking..."
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400, easing = FastOutSlowInEasing)) +
                scaleIn(tween(500, easing = FastOutSlowInEasing), initialScale = 0.92f),
        exit = fadeOut(tween(300, easing = FastOutSlowInEasing)) +
                scaleOut(tween(350, easing = FastOutSlowInEasing), targetScale = 0.92f),
        modifier = Modifier.fillMaxSize(),
    ) {
        // Solid white backdrop – 100% opacity
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { /* consume touches */ },
        ) {
            // Close button - top right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 16.dp, end = 16.dp)
                    .size(42.dp)
                    .neuShadow(
                        cornerRadius = 14.dp,
                        darkColor = Color(0x44A3B1C6),
                        lightColor = Color(0xBBFFFFFF),
                        darkOffset = 3.dp,
                        lightOffset = (-2).dp,
                        blur = 6.dp,
                    )
                    .background(Color(0xCCFFFFFF), RoundedCornerShape(14.dp))
                    .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onClose() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close live mode",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Center content: blob + status + partial text
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Blob with neumorphic shadow
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .neuShadow(
                            cornerRadius = 110.dp,
                            darkColor = ShadowDark,
                            lightColor = ShadowLight,
                            darkOffset = 6.dp,
                            lightOffset = (-4).dp,
                            blur = 12.dp,
                        ),
                ) {
                    VoiceBlob(
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = statusText,
                    fontFamily = DmSans,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    letterSpacing = (-0.02).sp,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Show live partial transcription while listening
                if (liveMicState == MicState.LISTENING && livePartialText.isNotEmpty()) {
                    Text(
                        text = livePartialText,
                        fontFamily = DmSans,
                        fontSize = 14.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(horizontal = 32.dp)
                            .widthIn(max = 280.dp),
                    )
                } else {
                    Text(
                        text = "Tap the close button to exit",
                        fontFamily = DmSans,
                        fontSize = 13.sp,
                        color = TextMuted,
                    )
                }
            }
        }
    }
}

/**
 * An animated organic blob using simplex-noise displacement for rich,
 * fluid morphing. Inspired by Three.js shader orb with multi-color
 * gradient mixing, fresnel edge glow, and layered depth.
 */
@Composable
private fun VoiceBlob(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "blob")

    // Time driver – continuous loop
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_000_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "time",
    )

    // Breathing scale pulse
    val breathe by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    // Glow intensity pulse
    val glowAlpha by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    // Reference colors from the shader: indigo, pink, sky-blue
    val color1 = Color(0xFF6366F1) // indigo
    val color2 = Color(0xFFEC4899) // pink
    val color3 = Color(0xFF0EA5E9) // sky

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val baseRadius = size.minDimension * 0.32f * breathe

        // Outer glow halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color1.copy(alpha = glowAlpha),
                    color2.copy(alpha = glowAlpha * 0.4f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = baseRadius * 2.0f,
            ),
            radius = baseRadius * 2.0f,
            center = Offset(cx, cy),
        )

        // Layer 3 – back, largest, most transparent
        drawNoiseBlob(
            cx = cx, cy = cy,
            baseRadius = baseRadius * 1.18f,
            time = time * 0.3f,
            noiseScale = 2.0f,
            noiseAmplitude = 0.22f,
            steps = 140,
            brush = Brush.linearGradient(
                colors = listOf(
                    color3.copy(alpha = 0.15f),
                    color1.copy(alpha = 0.10f),
                ),
                start = Offset(cx - baseRadius, cy - baseRadius),
                end = Offset(cx + baseRadius, cy + baseRadius),
            ),
        )

        // Layer 2 – middle
        drawNoiseBlob(
            cx = cx, cy = cy,
            baseRadius = baseRadius * 1.07f,
            time = time * 0.4f + 17f,
            noiseScale = 2.5f,
            noiseAmplitude = 0.18f,
            steps = 140,
            brush = Brush.linearGradient(
                colors = listOf(
                    color2.copy(alpha = 0.25f),
                    color1.copy(alpha = 0.18f),
                ),
                start = Offset(cx - baseRadius, cy),
                end = Offset(cx + baseRadius, cy + baseRadius),
            ),
        )

        // Layer 1 – front, main blob
        drawNoiseBlob(
            cx = cx, cy = cy,
            baseRadius = baseRadius,
            time = time * 0.4f + 42f,
            noiseScale = 2.5f,
            noiseAmplitude = 0.20f,
            steps = 160,
            brush = Brush.linearGradient(
                colors = listOf(
                    color1.copy(alpha = 0.65f),
                    color2.copy(alpha = 0.50f),
                    color3.copy(alpha = 0.45f),
                ),
                start = Offset(cx - baseRadius, cy - baseRadius),
                end = Offset(cx + baseRadius, cy + baseRadius),
            ),
        )

        // Inner fresnel-like specular highlight
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.40f),
                    Color.White.copy(alpha = 0.10f),
                    Color.Transparent,
                ),
                center = Offset(cx - baseRadius * 0.22f, cy - baseRadius * 0.28f),
                radius = baseRadius * 0.65f,
            ),
            radius = baseRadius * 0.65f,
            center = Offset(cx - baseRadius * 0.22f, cy - baseRadius * 0.28f),
        )

        // Rim glow (fresnel edge) – a ring around the blob
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Transparent,
                    Color.White.copy(alpha = 0.18f),
                    Color.White.copy(alpha = 0.06f),
                    Color.Transparent,
                ),
                center = Offset(cx, cy),
                radius = baseRadius * 1.15f,
            ),
            radius = baseRadius * 1.15f,
            center = Offset(cx, cy),
        )
    }
}

/**
 * Draws an organic blob using 3D simplex noise sampled along a circle
 * to produce smooth, fluid deformations – matching the look of the
 * vertex-shader displaced sphere from the reference.
 */
private fun DrawScope.drawNoiseBlob(
    cx: Float,
    cy: Float,
    baseRadius: Float,
    time: Float,
    noiseScale: Float,
    noiseAmplitude: Float,
    steps: Int,
    brush: Brush,
) {
    val path = Path()
    for (i in 0..steps) {
        val angle = (i.toFloat() / steps) * 2f * PI.toFloat()
        val nx = cos(angle) * noiseScale
        val ny = sin(angle) * noiseScale
        val noise = snoise3(nx, ny, time)
        val r = baseRadius * (1f + noise * noiseAmplitude)
        val x = cx + r * cos(angle)
        val y = cy + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, brush = brush, style = Fill)
}

// ── Simplex Noise (3D) ───────────────────────────────────────────
// Port of the GLSL simplex noise from the reference vertex shader.

private fun mod289(x: Float): Float = x - floor(x * (1f / 289f)) * 289f

private fun permute(x: Float): Float = mod289(((x * 34f) + 1f) * x)

private fun taylorInvSqrt(r: Float): Float = 1.79284291400159f - 0.85373472095314f * r

private fun snoise3(xIn: Float, yIn: Float, zIn: Float): Float {
    val C1 = 1f / 6f
    val C2 = 1f / 3f

    // Skew the input space
    val s = (xIn + yIn + zIn) * C2
    val i = floor(xIn + s)
    val j = floor(yIn + s)
    val k = floor(zIn + s)

    val t = (i + j + k) * C1
    val x0 = xIn - i + t
    val y0 = yIn - j + t
    val z0 = zIn - k + t

    // Determine simplex
    val i1: Float; val j1: Float; val k1: Float
    val i2: Float; val j2: Float; val k2: Float
    if (x0 >= y0) {
        if (y0 >= z0) {
            i1 = 1f; j1 = 0f; k1 = 0f; i2 = 1f; j2 = 1f; k2 = 0f
        } else if (x0 >= z0) {
            i1 = 1f; j1 = 0f; k1 = 0f; i2 = 1f; j2 = 0f; k2 = 1f
        } else {
            i1 = 0f; j1 = 0f; k1 = 1f; i2 = 1f; j2 = 0f; k2 = 1f
        }
    } else {
        if (y0 < z0) {
            i1 = 0f; j1 = 0f; k1 = 1f; i2 = 0f; j2 = 1f; k2 = 1f
        } else if (x0 < z0) {
            i1 = 0f; j1 = 1f; k1 = 0f; i2 = 0f; j2 = 1f; k2 = 1f
        } else {
            i1 = 0f; j1 = 1f; k1 = 0f; i2 = 1f; j2 = 1f; k2 = 0f
        }
    }

    val x1 = x0 - i1 + C1; val y1 = y0 - j1 + C1; val z1 = z0 - k1 + C1
    val x2 = x0 - i2 + C2; val y2 = y0 - j2 + C2; val z2 = z0 - k2 + C2
    val x3 = x0 - 0.5f;    val y3 = y0 - 0.5f;    val z3 = z0 - 0.5f

    val ii = mod289(i)
    val jj = mod289(j)
    val kk = mod289(k)

    val p0 = permute(permute(permute(kk) + jj) + ii)
    val p1 = permute(permute(permute(kk + k1) + jj + j1) + ii + i1)
    val p2 = permute(permute(permute(kk + k2) + jj + j2) + ii + i2)
    val p3 = permute(permute(permute(kk + 1f) + jj + 1f) + ii + 1f)

    // Gradients
    val n_ = 0.142857142857f
    val nsX = n_ * 2f - 0f      // D.w * n_ adjusted
    val nsY = n_ * 1f - 0.5f    // D.y * n_ - D.x adjusted
    val nsZ = n_ * 0f - 1f / 7f // for floor

    fun grad(p: Float): Triple<Float, Float, Float> {
        val jv = p - 49f * floor(p / 49f)
        val xv = floor(jv / 7f)
        val yv = jv - 7f * xv
        val gx = xv * n_ + nsY
        val gy = yv * n_ + nsY
        val gz = 1f - abs(gx) - abs(gy)
        val offX = if (gz < 0f) (if (gx >= 0f) 1f else -1f) else 0f
        val offY = if (gz < 0f) (if (gy >= 0f) 1f else -1f) else 0f
        val fgx = gx + offX * if (gz < 0f) (-floor(gx) * 2f - 1f).let { 0f } else 0f
        // Simplified: use raw gradients normalised
        val len = sqrt(gx * gx + gy * gy + gz * gz).coerceAtLeast(0.0001f)
        return Triple(gx / len, gy / len, gz / len)
    }

    val g0 = grad(p0); val g1 = grad(p1)
    val g2 = grad(p2); val g3 = grad(p3)

    val t0 = (0.6f - x0 * x0 - y0 * y0 - z0 * z0).coerceAtLeast(0f)
    val t1 = (0.6f - x1 * x1 - y1 * y1 - z1 * z1).coerceAtLeast(0f)
    val t2 = (0.6f - x2 * x2 - y2 * y2 - z2 * z2).coerceAtLeast(0f)
    val t3 = (0.6f - x3 * x3 - y3 * y3 - z3 * z3).coerceAtLeast(0f)

    val n0 = t0 * t0 * t0 * t0 * (g0.first * x0 + g0.second * y0 + g0.third * z0)
    val n1 = t1 * t1 * t1 * t1 * (g1.first * x1 + g1.second * y1 + g1.third * z1)
    val n2 = t2 * t2 * t2 * t2 * (g2.first * x2 + g2.second * y2 + g2.third * z2)
    val n3 = t3 * t3 * t3 * t3 * (g3.first * x3 + g3.second * y3 + g3.third * z3)

    return 42f * (n0 + n1 + n2 + n3)
}
