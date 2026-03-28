package com.neo.android.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neo.android.ui.theme.DmSans
import com.neo.android.ui.theme.TextMuted
import com.neo.android.ui.theme.TextSecondary

@Composable
fun ArchiveScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Archive,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = "Archive",
                fontFamily = DmSans,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
            )
            Text(
                text = "No past sessions yet",
                fontFamily = DmSans,
                fontSize = 14.sp,
                color = TextMuted,
            )
        }
    }
}
