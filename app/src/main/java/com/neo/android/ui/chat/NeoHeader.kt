package com.neo.android.ui.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neo.android.ui.theme.AccentPrimary
import com.neo.android.ui.theme.BorderDark
import com.neo.android.ui.theme.BorderLight
import com.neo.android.ui.theme.DmSans
import com.neo.android.ui.theme.SpatialSurfaceRaised
import com.neo.android.ui.theme.TextSecondary

enum class AppTab { CHAT, LIVE, ARCHIVE }

private data class TabItem(val tab: AppTab, val label: String, val icon: ImageVector)

@Composable
fun NeoHeader(
    activeTab: AppTab,
    onTabChange: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = remember {
        listOf(
            TabItem(AppTab.CHAT, "Chat", Icons.AutoMirrored.Outlined.Chat),
            TabItem(AppTab.LIVE, "Live", Icons.Outlined.Sensors),
            TabItem(AppTab.ARCHIVE, "Archive", Icons.Outlined.Archive),
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Tab switcher pill
            Row(
                modifier = Modifier
                    .background(Color(0x08000000), RoundedCornerShape(20.dp))
                    .border(1.dp, BorderDark, RoundedCornerShape(20.dp))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { item ->
                    NavTabItem(
                        item = item,
                        isActive = activeTab == item.tab,
                        onClick = { onTabChange(item.tab) },
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // Plus button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .neuShadow(
                        cornerRadius = 14.dp,
                        darkColor = Color(0x33A3B1C6),
                        lightColor = Color(0xAAFFFFFF),
                        darkOffset = 4.dp,
                        lightOffset = (-2).dp,
                        blur = 8.dp,
                    )
                    .background(SpatialSurfaceRaised, RoundedCornerShape(14.dp))
                    .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { /* placeholder */ },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "New chat",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun NavTabItem(
    item: TabItem,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val textColor by animateColorAsState(
        targetValue = if (isActive) AccentPrimary else TextSecondary,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tab_text_${item.tab}",
    )
    val bgColor by animateColorAsState(
        targetValue = if (isActive) SpatialSurfaceRaised else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tab_bg_${item.tab}",
    )
    val shadowBlur by animateDpAsState(
        targetValue = if (isActive) 8.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tab_shadow_${item.tab}",
    )
    val borderAlpha by animateColorAsState(
        targetValue = if (isActive) BorderLight else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tab_border_${item.tab}",
    )

    Row(
        modifier = Modifier
            .neuShadow(
                cornerRadius = 14.dp,
                darkColor = Color(0x33A3B1C6),
                lightColor = Color(0xAAFFFFFF),
                darkOffset = 4.dp,
                lightOffset = (-2).dp,
                blur = shadowBlur,
            )
            .background(bgColor, RoundedCornerShape(14.dp))
            .border(1.dp, borderAlpha, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = textColor,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = item.label,
            fontFamily = DmSans,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
        )
    }
}
