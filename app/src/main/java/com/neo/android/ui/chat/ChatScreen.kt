package com.neo.android.ui.chat

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

        when (activeTab) {
            AppTab.CHAT -> {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
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
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AppTab.LIVE -> LiveScreen(modifier = Modifier.weight(1f))
            AppTab.ARCHIVE -> ArchiveScreen(modifier = Modifier.weight(1f))
        }
    }
}

