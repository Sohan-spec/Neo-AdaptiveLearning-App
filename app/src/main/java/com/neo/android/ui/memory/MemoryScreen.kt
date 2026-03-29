package com.neo.android.ui.memory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neo.android.R
import com.neo.android.ui.chat.neuShadow
import com.neo.android.ui.theme.AccentGradEnd
import com.neo.android.ui.theme.AccentGradStart
import com.neo.android.ui.theme.AccentPrimary
import com.neo.android.ui.theme.BorderDim
import com.neo.android.ui.theme.BorderLight
import com.neo.android.ui.theme.ColorDanger
import com.neo.android.ui.theme.ColorSuccess
import com.neo.android.ui.theme.ColorWarn
import com.neo.android.ui.theme.DmSans
import com.neo.android.ui.theme.SpatialBg
import com.neo.android.ui.theme.SpatialSurface
import com.neo.android.ui.theme.SpatialSurfaceRaised
import com.neo.android.ui.theme.TextMuted
import com.neo.android.ui.theme.TextPrimary
import com.neo.android.ui.theme.TextSecondary

@Composable
fun MemoryScreen(
    onBack: () -> Unit,
    vm: MemoryViewModel = viewModel(),
) {
    val memories by vm.memories.collectAsState()
    val selectedCategory by vm.selectedCategory.collectAsState()
    val totalCount by vm.totalCount.collectAsState()
    val categoryCount by vm.categoryCount.collectAsState()
    val lastUpdated by vm.lastUpdated.collectAsState()
    val categories by vm.categories.collectAsState()
    val isExtracting by vm.isExtracting.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<MemoryEntry?>(null) }
    var deletingEntry by remember { mutableStateOf<MemoryEntry?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SpatialBg)
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // ── Header ───────────────────────────────────────
            MemoryHeader(onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // ── Learning indicator ───────────────────────
                AnimatedVisibility(
                    visible = isExtracting,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                ) {
                    LearningBanner()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── Overview Cards ───────────────────────────
                OverviewCardsRow(
                    totalEntries = totalCount,
                    categoryCount = categoryCount,
                    lastUpdated = lastUpdated,
                )

                Spacer(modifier = Modifier.height(28.dp))
                GradientDivider()
                Spacer(modifier = Modifier.height(28.dp))

                // ── Category Filter ──────────────────────────
                SectionTitle("CATEGORIES")
                Spacer(modifier = Modifier.height(12.dp))
                CategoryFilterRow(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { vm.selectCategory(it) },
                )

                Spacer(modifier = Modifier.height(24.dp))
                GradientDivider()
                Spacer(modifier = Modifier.height(24.dp))

                // ── Memory Entries ────────────────────────────
                SectionTitle("MEMORY ENTRIES")
                Spacer(modifier = Modifier.height(12.dp))

                if (memories.isEmpty()) {
                    EmptyState(onAddManually = { showAddDialog = true })
                } else {
                    memories.forEach { entry ->
                        MemoryCard(
                            entry = entry,
                            onEdit = { editingEntry = entry },
                            onDelete = { deletingEntry = entry },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // ── FAB ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .navigationBarsPadding()
                .size(52.dp)
                .neuShadow(
                    cornerRadius = 16.dp,
                    darkColor = Color(0x66A3B1C6),
                    lightColor = Color(0xCCFFFFFF),
                    darkOffset = 4.dp,
                    lightOffset = (-2).dp,
                    blur = 8.dp,
                )
                .background(
                    Brush.linearGradient(listOf(AccentGradStart, AccentGradEnd)),
                    RoundedCornerShape(16.dp),
                )
                .clip(RoundedCornerShape(16.dp))
                .clickable { showAddDialog = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = "Add memory",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
    }

    // ── Dialogs ──────────────────────────────────────────────
    if (showAddDialog) {
        MemoryFormDialog(
            title = "Add Memory",
            onDismiss = { showAddDialog = false },
            onSave = { category, memTitle, content ->
                vm.addMemory(category, memTitle, content)
                showAddDialog = false
            },
        )
    }

    if (editingEntry != null) {
        val entry = editingEntry!!
        MemoryFormDialog(
            title = "Edit Memory",
            initialCategory = entry.category,
            initialTitle = entry.title,
            initialContent = entry.content,
            onDismiss = { editingEntry = null },
            onSave = { category, memTitle, content ->
                vm.editMemory(entry.id, category, memTitle, content)
                editingEntry = null
            },
        )
    }

    if (deletingEntry != null) {
        val entry = deletingEntry!!
        AlertDialog(
            onDismissRequest = { deletingEntry = null },
            title = { Text("Delete Memory", fontFamily = DmSans, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Delete \"${entry.title}\"? This cannot be undone.",
                    fontFamily = DmSans,
                    color = TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteMemory(entry.id)
                    deletingEntry = null
                }) {
                    Text("Delete", color = ColorDanger, fontFamily = DmSans, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingEntry = null }) {
                    Text("Cancel", fontFamily = DmSans)
                }
            },
        )
    }
}

// ── Header ───────────────────────────────────────────────────────
@Composable
private fun MemoryHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Back button
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
                .clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = "Memory",
            fontFamily = DmSans,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            letterSpacing = (-0.03).sp,
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Brain icon accent badge
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    Brush.linearGradient(listOf(AccentGradStart, AccentGradEnd)),
                    RoundedCornerShape(8.dp),
                )
                .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_brain),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }

    // Divider
    GradientDivider()
}

// ── Overview Cards Row ───────────────────────────────────────────
@Composable
private fun OverviewCardsRow(totalEntries: Int, categoryCount: Int, lastUpdated: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OverviewCard(
            label = "ENTRIES",
            value = totalEntries.toString(),
            modifier = Modifier.weight(1f),
        )
        OverviewCard(
            label = "CATEGORIES",
            value = categoryCount.toString(),
            modifier = Modifier.weight(1f),
        )
        OverviewCard(
            label = "LAST UPDATED",
            value = lastUpdated,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun OverviewCard(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .neuShadow(
                cornerRadius = 14.dp,
                darkColor = Color(0x66A3B1C6),
                lightColor = Color(0xCCFFFFFF),
                darkOffset = 4.dp,
                lightOffset = (-2).dp,
                blur = 8.dp,
            )
            .background(SpatialSurface, RoundedCornerShape(14.dp))
            .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Column {
            Text(
                text = label,
                fontFamily = DmSans,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextMuted,
                letterSpacing = 0.06.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontFamily = DmSans,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = (-0.02).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Category Filter Row ──────────────────────────────────────────
@Composable
private fun CategoryFilterRow(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        categories.forEach { category ->
            val isSelected = (category == "All" && selectedCategory == null) ||
                category == selectedCategory

            CategoryChip(
                label = category,
                isSelected = isSelected,
                onClick = { onCategorySelected(category) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(100.dp)

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color.Transparent else SpatialSurface,
        animationSpec = tween(200),
        label = "chipBg",
    )

    Box(
        modifier = Modifier
            .then(
                if (isSelected) {
                    Modifier
                        .background(
                            Brush.linearGradient(listOf(AccentGradStart, AccentGradEnd)),
                            shape,
                        )
                        .border(1.dp, Color(0x40FFFFFF), shape)
                } else {
                    Modifier
                        .neuShadow(
                            cornerRadius = 100.dp,
                            darkColor = Color(0x44A3B1C6),
                            lightColor = Color(0xAAFFFFFF),
                            darkOffset = 3.dp,
                            lightOffset = (-2).dp,
                            blur = 6.dp,
                        )
                        .background(bgColor, shape)
                        .border(1.dp, BorderLight, shape)
                },
            )
            .clip(shape)
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            fontFamily = DmSans,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) Color.White else AccentPrimary,
            letterSpacing = (-0.01).sp,
        )
    }
}

// ── Memory Card ──────────────────────────────────────────────────
@Composable
private fun MemoryCard(
    entry: MemoryEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val cardShape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neuShadow(
                cornerRadius = 18.dp,
                darkColor = Color(0x66A3B1C6),
                lightColor = Color(0xCCFFFFFF),
                darkOffset = 4.dp,
                lightOffset = (-2).dp,
                blur = 8.dp,
            )
            .background(SpatialSurface, cardShape)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(BorderLight, BorderDim),
                ),
                shape = cardShape,
            )
            .clip(cardShape)
            .clickable { onEdit() }
            .padding(16.dp),
    ) {
        // Top row: category badge + source + actions + timestamp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CategoryBadge(category = entry.category)
                SourceBadge(source = entry.source)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = formatRelativeTime(entry.updatedAt),
                    fontFamily = DmSans,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted,
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEdit() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit",
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = ColorDanger.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Title
        Text(
            text = entry.title,
            fontFamily = DmSans,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            letterSpacing = (-0.01).sp,
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Content
        Text(
            text = entry.content,
            fontFamily = DmSans,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            color = TextSecondary,
            lineHeight = 19.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Confidence bar
        ConfidenceBar(confidence = entry.confidence)
    }
}

// ── Category Badge ───────────────────────────────────────────────
@Composable
private fun CategoryBadge(category: String) {
    val (bgColor, textColor, borderColor) = when (category) {
        "Preferences" -> Triple(
            Color(0x1E4D7BFF), Color(0xFF2B4C9B), Color(0x4D4D7BFF),
        )
        "Facts" -> Triple(
            Color(0x2638A169), Color(0xFF276749), Color(0x4D38A169),
        )
        "Interests" -> Triple(
            Color(0x26D69E2E), Color(0xFF975A16), Color(0x4DD69E2E),
        )
        "Habits" -> Triple(
            Color(0x26E53E3E), Color(0xFF9B2C2C), Color(0x4DE53E3E),
        )
        "Context" -> Triple(
            Color(0x80FFFFFF), TextSecondary, BorderLight,
        )
        else -> Triple(
            Color(0x80FFFFFF), TextSecondary, BorderLight,
        )
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(100.dp))
            .border(1.dp, borderColor, RoundedCornerShape(100.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = category,
            fontFamily = DmSans,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            letterSpacing = 0.02.sp,
        )
    }
}

// ── Confidence Bar ───────────────────────────────────────────────
@Composable
private fun ConfidenceBar(confidence: Float) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Confidence",
                fontFamily = DmSans,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted,
            )
            Text(
                text = "${(confidence * 100).toInt()}%",
                fontFamily = DmSans,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    confidence >= 0.85f -> ColorSuccess
                    confidence >= 0.70f -> ColorWarn
                    else -> ColorDanger
                },
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color(0x1AA3B1C6), RoundedCornerShape(2.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(confidence.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(AccentGradStart, AccentGradEnd),
                        ),
                        RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}

// ── Source Badge ──────────────────────────────────────────────────
@Composable
private fun SourceBadge(source: String) {
    val label = if (source == "manual") "Manual" else "Auto"
    val (bgColor, textColor) = if (source == "manual") {
        Color(0x1E4D7BFF) to Color(0xFF2B4C9B)
    } else {
        Color(0x1AA3B1C6) to TextMuted
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(100.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            fontFamily = DmSans,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            letterSpacing = 0.04.sp,
        )
    }
}

// ── Learning Banner ──────────────────────────────────────────────
@Composable
private fun LearningBanner() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0x1E4D7BFF),
                RoundedCornerShape(12.dp),
            )
            .border(1.dp, Color(0x334D7BFF), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_brain),
            contentDescription = null,
            tint = AccentPrimary,
            modifier = Modifier
                .size(18.dp)
                .alpha(alpha),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Neo is learning from your conversations…",
            fontFamily = DmSans,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = AccentPrimary,
        )
    }
}

// ── Memory Form Dialog (Add / Edit) ─────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoryFormDialog(
    title: String,
    initialCategory: String = "Preferences",
    initialTitle: String = "",
    initialContent: String = "",
    onDismiss: () -> Unit,
    onSave: (category: String, title: String, content: String) -> Unit,
) {
    val categoryOptions = listOf("Preferences", "Facts", "Interests", "Habits", "Context")
    var category by remember { mutableStateOf(initialCategory) }
    var memTitle by remember { mutableStateOf(initialTitle) }
    var content by remember { mutableStateOf(initialContent) }
    var categoryExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, fontFamily = DmSans, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category", fontFamily = DmSans) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false },
                    ) {
                        categoryOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, fontFamily = DmSans) },
                                onClick = {
                                    category = option
                                    categoryExpanded = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = memTitle,
                    onValueChange = { memTitle = it },
                    label = { Text("Title", fontFamily = DmSans) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content", fontFamily = DmSans) },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (memTitle.isNotBlank() && content.isNotBlank()) {
                        onSave(category, memTitle.trim(), content.trim())
                    }
                },
                enabled = memTitle.isNotBlank() && content.isNotBlank(),
            ) {
                Text(
                    "Save",
                    color = if (memTitle.isNotBlank() && content.isNotBlank()) AccentPrimary else TextMuted,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontFamily = DmSans)
            }
        },
    )
}

// ── Empty State ──────────────────────────────────────────────────
@Composable
private fun EmptyState(onAddManually: () -> Unit) {
    val cardShape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .neuShadow(
                cornerRadius = 18.dp,
                darkColor = Color(0x66A3B1C6),
                lightColor = Color(0xCCFFFFFF),
                darkOffset = 6.dp,
                lightOffset = (-4).dp,
                blur = 14.dp,
            )
            .background(SpatialSurface, cardShape)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(BorderLight, BorderDim),
                ),
                shape = cardShape,
            )
            .clip(cardShape)
            .padding(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_brain),
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No memories yet",
                fontFamily = DmSans,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Neo will learn about you as you chat",
                fontFamily = DmSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = TextMuted,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Or add a memory manually",
                fontFamily = DmSans,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccentPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAddManually() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

// ── Utilities ────────────────────────────────────────────────────
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontFamily = DmSans,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TextMuted,
        letterSpacing = 0.1.sp,
    )
}

@Composable
private fun GradientDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
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
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / (60 * 1000)
    val hours = diff / (60 * 60 * 1000)
    val days = diff / (24 * 60 * 60 * 1000)

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> "${days / 7}w ago"
    }
}
