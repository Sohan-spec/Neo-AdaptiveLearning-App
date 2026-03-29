package com.neo.android.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.neo.android.R
import com.neo.android.ui.theme.AccentPrimary
import com.neo.android.ui.theme.BorderLight
import com.neo.android.ui.theme.DmSans
import com.neo.android.ui.theme.SpatialSurface
import com.neo.android.ui.theme.SpatialSurfaceRaised
import com.neo.android.ui.theme.TextMuted
import com.neo.android.ui.theme.TextPrimary
import com.neo.android.ui.theme.TextSecondary

@Composable
fun BoxScope.ChatHistorySidebar(
    visible: Boolean,
    chatSessions: List<ChatSession>,
    onClose: () -> Unit,
    onSessionSelected: (ChatSession) -> Unit,
    onNewChat: () -> Unit = {},
    onDeleteChat: (ChatSession) -> Unit = {},
    onUsageStats: () -> Unit = {},
) {
    // Scrim
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(300)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(8.dp)
                .background(Color.Black.copy(alpha = 0.20f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onClose() },
        )
    }

    // Sidebar panel
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            animationSpec = tween(350, easing = FastOutSlowInEasing),
            initialOffsetX = { it },
        ) + fadeIn(tween(300)),
        exit = slideOutHorizontally(
            animationSpec = tween(300, easing = FastOutSlowInEasing),
            targetOffsetX = { it },
        ) + fadeOut(tween(250)),
        modifier = Modifier.align(Alignment.CenterEnd),
    ) {
        val sidebarShape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.80f)
                .neuShadow(
                    cornerRadius = 24.dp,
                    darkColor = Color(0x66A3B1C6),
                    lightColor = Color(0x00FFFFFF),
                    darkOffset = (-6).dp,
                    lightOffset = 0.dp,
                    blur = 16.dp,
                )
                .background(color = SpatialSurface, shape = sidebarShape)
                .border(1.dp, BorderLight, sidebarShape)
                .clip(sidebarShape),
        ) {
            // ── Header ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Close button (left)
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
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Usage stats button
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
                            .clickable { onUsageStats() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_chart_area),
                            contentDescription = "Usage Stats",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    // New chat icon
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "New chat",
                        tint = AccentPrimary,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onNewChat() },
                    )
                }
            }

            // ── Divider ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
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

            Spacer(modifier = Modifier.height(12.dp))

            // ── Section title ────────────────────────────────────
            Text(
                text = "CHAT HISTORY",
                fontFamily = DmSans,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 0.1.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Chat sessions list ───────────────────────────────
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(chatSessions, key = { it.id }) { session ->
                    ChatSessionItem(
                        session = session,
                        onClick = { onSessionSelected(session) },
                        onDelete = { onDeleteChat(session) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatSessionItem(
    session: ChatSession,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val bgColor = if (session.isActive) SpatialSurfaceRaised else Color(0x55FFFFFF)
    val borderColor = if (session.isActive) AccentPrimary.copy(alpha = 0.3f) else BorderLight

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neuShadow(
                cornerRadius = 14.dp,
                darkColor = Color(0x22A3B1C6),
                lightColor = Color(0x88FFFFFF),
                darkOffset = 3.dp,
                lightOffset = (-2).dp,
                blur = 6.dp,
            )
            .background(bgColor, RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Title + active dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = session.title,
                        fontFamily = DmSans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (session.isActive) AccentPrimary else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (session.isActive) {
                        Box(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(7.dp)
                                .background(AccentPrimary, CircleShape),
                        )
                    }
                }

                // Delete icon
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete chat",
                    tint = TextMuted.copy(alpha = 0.6f),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(16.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDelete,
                        ),
                )
            }
            Text(
                text = session.preview,
                fontFamily = DmSans,
                fontSize = 12.sp,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = session.timestamp,
                fontFamily = DmSans,
                fontSize = 11.sp,
                color = TextMuted.copy(alpha = 0.7f),
            )
        }
    }
}
