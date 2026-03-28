package com.neo.android.ui.chat

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neo.android.R
import com.neo.android.ui.theme.AccentGradEnd
import com.neo.android.ui.theme.AccentGradStart
import com.neo.android.ui.theme.BorderLight
import com.neo.android.ui.theme.DmSans
import com.neo.android.ui.theme.SpatialBg
import com.neo.android.ui.theme.SpatialSurfaceRaised
import com.neo.android.ui.theme.TextMuted
import com.neo.android.ui.theme.TextPrimary
import com.neo.android.ui.theme.TextSecondary

@Composable
fun InputBar(
    onSend: (String) -> Unit,
    onLiveClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var micState by remember { mutableStateOf(MicState.IDLE) }
    val hasText = text.isNotBlank()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .neuShadow(
                cornerRadius = 0.dp,
                darkColor = Color(0x44A3B1C6),
                lightColor = Color(0xAAFFFFFF),
                darkOffset = 0.dp,
                lightOffset = (-2).dp,
                blur = 6.dp,
            )
            .background(SpatialBg)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(BorderLight, Color.Transparent),
                ),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Text field
            Box(
                modifier = Modifier
                    .weight(1f)
                    .neuShadow(
                        cornerRadius = 14.dp,
                        darkColor = Color(0x44A3B1C6),
                        lightColor = Color(0xBBFFFFFF),
                        darkOffset = 3.dp,
                        lightOffset = (-2).dp,
                        blur = 7.dp,
                    )
                    .background(
                        color = SpatialSurfaceRaised,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = "Message Neo...",
                        fontFamily = DmSans,
                        fontSize = 14.sp,
                        color = TextMuted,
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = TextStyle(
                        fontFamily = DmSans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = TextPrimary,
                        lineHeight = 20.sp,
                    ),
                    cursorBrush = SolidColor(AccentGradEnd),
                    maxLines = 4,
                )
            }

            // Live mode button (asterisk) with rainbow border
            RainbowLiveButton(
                onClick = onLiveClick,
            )

            // Single action button: mic when empty, send when text present
            AnimatedContent(
                targetState = hasText,
                transitionSpec = {
                    (fadeIn(tween(150)) + scaleIn(tween(150), initialScale = 0.8f))
                        .togetherWith(fadeOut(tween(100)) + scaleOut(tween(100), targetScale = 0.8f))
                },
                label = "mic_send_swap",
            ) { showSend ->
                if (showSend) {
                    // Send button
                    Box(
                        modifier = Modifier
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
                                onClick = {
                                    onSend(text)
                                    text = ""
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_send_horizontal),
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else {
                    // Mic button
                    MicButton(
                        state = micState,
                        onClick = {
                            micState = if (micState == MicState.IDLE) MicState.LISTENING else MicState.IDLE
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RainbowLiveButton(
    onClick: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rainbow")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rainbow_angle",
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "rainbow_glow",
    )

    val rainbowColors = listOf(
        Color(0xFFFF6B6B), // red-coral
        Color(0xFFFFA94D), // orange
        Color(0xFFFFD43B), // yellow
        Color(0xFF69DB7C), // green
        Color(0xFF4DABF7), // blue
        Color(0xFF9775FA), // violet
        Color(0xFFE599F7), // pink
        Color(0xFFFF6B6B), // loop back
    )

    Box(
        modifier = Modifier
            .size(42.dp)
            .drawBehind {
                // Rainbow glow behind the button
                val glowBrush = Brush.sweepGradient(
                    colors = rainbowColors.map { it.copy(alpha = glowAlpha) },
                )
                drawRoundRect(
                    brush = glowBrush,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(
                        size.width + 6.dp.toPx(),
                        size.height + 6.dp.toPx(),
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(-3.dp.toPx(), -3.dp.toPx()),
                )
            }
            .drawWithContent {
                // Draw the rotating rainbow border
                val borderWidth = 1.dp.toPx()
                val cr = 14.dp.toPx()

                rotate(angle) {
                    drawRoundRect(
                        brush = Brush.sweepGradient(rainbowColors),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(cr),
                    )
                }

                // Draw inner fill to create border effect
                drawRoundRect(
                    color = SpatialBg,
                    topLeft = androidx.compose.ui.geometry.Offset(borderWidth, borderWidth),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - borderWidth * 2,
                        size.height - borderWidth * 2,
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cr - borderWidth),
                )

                drawContent()
            }
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_asterisk),
            contentDescription = "Live mode",
            tint = TextSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}
