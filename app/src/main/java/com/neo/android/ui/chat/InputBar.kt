package com.neo.android.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neo.android.ui.theme.AccentGradEnd
import com.neo.android.ui.theme.AccentGradStart
import com.neo.android.ui.theme.DmSans
import com.neo.android.ui.theme.SpatialSurfaceRaised
import com.neo.android.ui.theme.TextMuted
import com.neo.android.ui.theme.TextPrimary

@Composable
fun InputBar(
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var micState by remember { mutableStateOf(MicState.IDLE) }
    val hasText = text.isNotBlank()

    Box(
        modifier = modifier
            .fillMaxWidth()
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
                            .size(48.dp)
                            .neuShadow(
                                cornerRadius = 24.dp,
                                darkColor = Color(0x554361EE),
                                lightColor = Color(0xCCFFFFFF),
                                darkOffset = 5.dp,
                                lightOffset = (-3).dp,
                                blur = 10.dp,
                            )
                            .background(
                                brush = Brush.linearGradient(listOf(AccentGradStart, AccentGradEnd)),
                                shape = CircleShape,
                            )
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
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
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
