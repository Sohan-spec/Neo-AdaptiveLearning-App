package com.neo.android.ui.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neo.android.ui.theme.SpatialBg

@Composable
fun ChatScreen(
    vm: ChatViewModel = viewModel(),
) {
    var activeTab by remember { mutableStateOf(AppTab.CHAT) }
    val messages = vm.messages
    val isThinking by vm.isThinking
    val listState = rememberLazyListState()

    val itemCount = messages.size + if (isThinking) 1 else 0
    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpatialBg)
            .imePadding(),
    ) {
        NeoHeader(
            activeTab = activeTab,
            onTabChange = { activeTab = it },
        )

        AnimatedContent(
            targetState = activeTab,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.96f))
                    .togetherWith(fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.96f))
            },
            label = "tab_content",
        ) { tab ->
            when (tab) {
                AppTab.CHAT -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(messages) { message -> MessageBubble(message) }
                        if (isThinking) {
                            item { TypingIndicator() }
                        }
                    }
                }
                AppTab.LIVE -> LiveScreen(modifier = Modifier.fillMaxSize())
                AppTab.ARCHIVE -> ArchiveScreen(modifier = Modifier.fillMaxSize())
            }
        }

        // Show input bar only on Chat tab
        if (activeTab == AppTab.CHAT) {
            InputBar(
                onSend = { vm.sendMessage(it) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

