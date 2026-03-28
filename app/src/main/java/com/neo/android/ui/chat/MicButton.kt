package com.neo.android.ui.chat

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.neo.android.ui.theme.AccentGradEnd
import com.neo.android.ui.theme.AccentGradStart

enum class MicState { IDLE, LISTENING }

@Composable
fun MicButton(
    state: MicState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .neuShadow(
                cornerRadius = 14.dp,
                darkColor = Color(0x554361EE),
                lightColor = Color(0xCCFFFFFF),
                darkOffset = 4.dp,
                lightOffset = (-2).dp,
                blur = 8.dp,
            )
            .background(
                brush = Brush.linearGradient(listOf(AccentGradStart, AccentGradEnd)),
                shape = RoundedCornerShape(14.dp),
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (state == MicState.IDLE) {
            Icon(
                imageVector = Icons.Outlined.Mic,
                contentDescription = "Microphone",
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        } else {
            // LISTENING: ping rings + waveform bars
            ListeningContent()
        }
    }
}

@Composable
private fun ListeningContent() {
    val transition = rememberInfiniteTransition(label = "listening")

    // Ping ring 1
    val ring1Scale by transition.animateFloat(
        initialValue = 1f, targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ), label = "ring1_scale"
    )
    val ring1Alpha by transition.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ), label = "ring1_alpha"
    )
    // Ping ring 2 (offset)
    val ring2Scale by transition.animateFloat(
        initialValue = 1f, targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, delayMillis = 300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ), label = "ring2_scale"
    )
    val ring2Alpha by transition.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, delayMillis = 300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ), label = "ring2_alpha"
    )

    // Waveform bar heights (5 bars, staggered)
    val barDelays = listOf(0, 100, 200, 300, 400)
    val barScales = barDelays.map { d ->
        transition.animateFloat(
            initialValue = 0.3f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(500, delayMillis = d, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ), label = "bar_$d"
        )
    }

    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        // Ring 1
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(ring1Scale)
                .graphicsLayer { alpha = ring1Alpha }
                .background(
                    color = AccentGradStart.copy(alpha = 0.35f),
                    shape = CircleShape,
                ),
        )
        // Ring 2
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(ring2Scale)
                .graphicsLayer { alpha = ring2Alpha }
                .background(
                    color = AccentGradStart.copy(alpha = 0.25f),
                    shape = CircleShape,
                ),
        )
        // Waveform bars
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(24.dp),
        ) {
            barScales.forEach { scaleState ->
                val sc by scaleState
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height((22 * sc).dp)
                        .background(Color.White, CircleShape),
                )
            }
        }
    }
}
