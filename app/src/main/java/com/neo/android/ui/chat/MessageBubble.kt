package com.neo.android.ui.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neo.android.ui.theme.AccentGradEnd
import com.neo.android.ui.theme.AccentGradStart
import com.neo.android.ui.theme.DmSans
import com.neo.android.ui.theme.SpatialSurfaceRaised
import com.neo.android.ui.theme.TextPrimary

// ── Neumorphic shadow modifier ────────────────────────────────────
fun Modifier.neuShadow(
    cornerRadius: Dp = 18.dp,
    darkColor: Color = Color(0x66A3B1C6),
    lightColor: Color = Color(0xCCFFFFFF),
    darkOffset: Dp = 6.dp,
    lightOffset: Dp = (-4).dp,
    blur: Dp = 14.dp,
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val cr = cornerRadius.toPx()
        val darkPaint = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(blur.toPx(), darkOffset.toPx(), darkOffset.toPx(), darkColor.toArgb())
            }
        }
        val lightPaint = Paint().apply {
            asFrameworkPaint().apply {
                isAntiAlias = true
                color = android.graphics.Color.TRANSPARENT
                setShadowLayer(blur.toPx(), lightOffset.toPx(), lightOffset.toPx(), lightColor.toArgb())
            }
        }
        canvas.drawRoundRect(0f, 0f, size.width, size.height, cr, cr, darkPaint)
        canvas.drawRoundRect(0f, 0f, size.width, size.height, cr, cr, lightPaint)
    }
}

// ── User bubble ────────────────────────────────────────────────────
@Composable
fun UserBubble(text: String, maxBubbleWidth: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .widthIn(max = maxBubbleWidth)
            .neuShadow(
                cornerRadius = 18.dp,
                darkColor = Color(0x554361EE),
                lightColor = Color(0xCCFFFFFF),
                darkOffset = 6.dp,
                lightOffset = (-3).dp,
                blur = 12.dp,
            )
            .background(
                brush = Brush.linearGradient(listOf(AccentGradStart, AccentGradEnd)),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = text,
            fontFamily = DmSans,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = Color.White,
        )
    }
}

// ── Assistant bubble ───────────────────────────────────────────────
@Composable
fun AssistantBubble(text: String, maxBubbleWidth: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .widthIn(max = maxBubbleWidth)
            .neuShadow(cornerRadius = 18.dp)
            .background(
                color = SpatialSurfaceRaised,
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = text,
            fontFamily = DmSans,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = TextPrimary,
        )
    }
}

// ── Router ─────────────────────────────────────────────────────────
@Composable
fun MessageBubble(message: Message) {
    // Subtle entrance: fade + slide up
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(12f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(250))
    }
    LaunchedEffect(Unit) {
        offsetY.animateTo(0f, tween(300))
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .graphicsLayer {
                this.alpha = alpha.value
                translationY = offsetY.value * density
            },
        contentAlignment = if (message.role == "user") Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        val maxBubbleWidth = maxWidth * 0.78f
        if (message.role == "user") {
            UserBubble(message.content, maxBubbleWidth)
        } else {
            val displayText = message.content.take(message.visibleChars)
            AssistantBubble(displayText, maxBubbleWidth)
        }
    }
}
