package com.neo.android.ui.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import com.neo.android.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neo.android.ui.theme.BorderLight
import com.neo.android.ui.theme.SpatialBg
import com.neo.android.ui.theme.TextSecondary

@Composable
fun ChatScreen(
    vm: ChatViewModel = viewModel(),
) {
    val messages = vm.messages
    val isThinking by vm.isThinking
    val listState = rememberLazyListState()
    var sidebarVisible by remember { mutableStateOf(false) }
    var liveOverlayVisible by remember { mutableStateOf(false) }

    BackHandler(enabled = sidebarVisible || liveOverlayVisible) {
        if (liveOverlayVisible) liveOverlayVisible = false
        else sidebarVisible = false
    }

    val itemCount = messages.size + if (isThinking) 1 else 0
    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpatialBg),
    ) {
        // ── Layer 1: Chat content ────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            Spacer(
                modifier = Modifier
                    .statusBarsPadding(),
            )

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 56.dp,
                    bottom = 20.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                items(messages) { message -> MessageBubble(message) }
                if (isThinking) {
                    item { TypingIndicator() }
                }
            }

            InputBar(
                onSend = { vm.sendMessage(it) },
                onLiveClick = { liveOverlayVisible = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // ── Layer 2: Floating Plus button ────────────────────
        AnimatedVisibility(
            visible = !sidebarVisible,
            enter = fadeIn(tween(250)) + scaleIn(
                animationSpec = tween(300, easing = FastOutSlowInEasing),
                initialScale = 0.8f,
            ),
            exit = fadeOut(tween(200)) + scaleOut(
                animationSpec = tween(250, easing = FastOutSlowInEasing),
                targetScale = 0.8f,
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 14.dp, end = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .neuShadow(
                        cornerRadius = 14.dp,
                        darkColor = Color(0x99A3B1C6),
                        lightColor = Color(0xCCFFFFFF),
                        darkOffset = 4.dp,
                        lightOffset = (-2).dp,
                        blur = 8.dp,
                    )
                    .background(SpatialBg, RoundedCornerShape(14.dp))
                    .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { sidebarVisible = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_menu),
                    contentDescription = "Menu",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // ── Layer 3: Sidebar overlay ─────────────────────────
        ChatHistorySidebar(
            visible = sidebarVisible,
            chatSessions = vm.chatSessions,
            onClose = { sidebarVisible = false },
            onSessionSelected = { /* handle session switch */ },
        )

        // ── Layer 4: Live mode overlay ───────────────────────
        LiveOverlay(
            visible = liveOverlayVisible,
            onClose = { liveOverlayVisible = false },
        )
    }
}

