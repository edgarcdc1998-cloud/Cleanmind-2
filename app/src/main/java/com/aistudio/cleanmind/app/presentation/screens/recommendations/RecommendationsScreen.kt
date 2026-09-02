package com.aistudio.cleanmind.app.presentation.screens.recommendations

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.cleanmind.app.R
import com.aistudio.cleanmind.app.domain.model.AnalysisRecommendationsSummary
import com.aistudio.cleanmind.app.domain.model.CleanupRecommendation
import com.aistudio.cleanmind.app.domain.model.RecommendationPriority
import com.aistudio.cleanmind.app.domain.model.RecommendationType
import com.aistudio.cleanmind.app.presentation.components.CleanMindTopAppBar
import com.aistudio.cleanmind.app.presentation.components.ReviewConfirmationDialog
import com.aistudio.cleanmind.app.presentation.home.HomeUiState
import com.aistudio.cleanmind.app.presentation.home.RecommendationFilter
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkBackground
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkPrimary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkSurfaceCard
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextMuted
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextPrimary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextSecondary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTrack
import com.aistudio.cleanmind.app.util.StorageFormatter

@Composable
fun RecommendationsScreen(
    uiState: HomeUiState,
    onFilterSelected: (RecommendationFilter) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onShowReviewConfirmation: () -> Unit,
    onDismissReviewConfirmation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = uiState.recommendationsSummary
    val filteredList = uiState.filteredRecommendations
    val selectedIds = uiState.selectedRecommendationIds
    val selectedCount = selectedIds.size

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(ElegantDarkBackground),
        containerColor = ElegantDarkBackground,
        topBar = {
            CleanMindTopAppBar(
                title = stringResource(id = R.string.recommendations_header)
            )
        },
        bottomBar = {
            if (selectedCount > 0) {
                Surface(
                    color = ElegantDarkSurfaceCard,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = onShowReviewConfirmation,
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 600.dp)
                                .height(54.dp)
                                .testTag("review_selected_btn"),
                            shape = RoundedCornerShape(27.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElegantDarkPrimary,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    id = R.string.review_selected_format,
                                    selectedCount,
                                    uiState.selectedReclaimableSpaceFormatted
                                ),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
            ) {
                if (summary == null || summary.totalRecommendationsCount == 0) {
                    EmptyRecommendationsView(modifier = Modifier.padding(top = 24.dp))
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            // Potential Reclaimable Space Header
                            ReclaimableHeroCard(summary = summary)
                        }

                        item {
                            // Filter Chips Row
                            FilterChipsRow(
                                summary = summary,
                                selectedFilter = uiState.selectedFilter,
                                onFilterSelected = onFilterSelected
                            )
                        }

                        item {
                            // Bulk Selection Bar
                            SelectionToolbar(
                                totalFiltered = filteredList.size,
                                selectedCount = selectedCount,
                                onSelectAll = onSelectAll,
                                onClearSelection = onClearSelection
                            )
                        }

                        if (filteredList.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceCard)
                                ) {
                                    Text(
                                        text = "Nenhum arquivo encontrado para o filtro selecionado.",
                                        fontSize = 13.sp,
                                        color = ElegantDarkTextMuted,
                                        modifier = Modifier.padding(20.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(filteredList, key = { it.id }) { item ->
                                val isSelected = selectedIds.contains(item.id)
                                RecommendationCard(
                                    recommendation = item,
                                    isSelected = isSelected,
                                    onToggleSelection = { onToggleSelection(item.id) }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }

    if (uiState.showReviewConfirmationDialog && selectedCount > 0) {
        ReviewConfirmationDialog(
            selectedRecommendations = uiState.selectedRecommendations,
            totalReclaimableFormatted = uiState.selectedReclaimableSpaceFormatted,
            onConfirm = {
                onDismissReviewConfirmation()
                onClearSelection()
            },
            onDismiss = onDismissReviewConfirmation
        )
    }
}

@Composable
private fun ReclaimableHeroCard(summary: AnalysisRecommendationsSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceCard)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "POTENCIAL DE LIMPEZA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        color = ElegantDarkPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            id = R.string.reclaimable_space_format,
                            StorageFormatter.formatBytes(summary.potentialReclaimableBytes)
                        ),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElegantDarkTextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ElegantDarkPrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${summary.totalRecommendationsCount} itens",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElegantDarkPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            // Distribution metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (summary.duplicateFilesCount > 0) {
                    CategoryPill(label = "${summary.duplicateFilesCount} duplicatas", color = Color(0xFFFF8A65))
                }
                if (summary.largeFilesCount > 0) {
                    CategoryPill(label = "${summary.largeFilesCount} grandes", color = Color(0xFFFFD54F))
                }
                if (summary.oldFilesCount > 0) {
                    CategoryPill(label = "${summary.oldFilesCount} antigos", color = Color(0xFF81D4FA))
                }
                if (summary.temporaryFilesCount > 0) {
                    CategoryPill(label = "${summary.temporaryFilesCount} temporários", color = Color(0xFFA5D6A7))
                }
            }
        }
    }
}

@Composable
private fun CategoryPill(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun FilterChipsRow(
    summary: AnalysisRecommendationsSummary,
    selectedFilter: RecommendationFilter,
    onFilterSelected: (RecommendationFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChipItem(
            label = stringResource(id = R.string.filter_all),
            count = summary.totalRecommendationsCount,
            selected = selectedFilter == RecommendationFilter.ALL,
            onClick = { onFilterSelected(RecommendationFilter.ALL) },
            testTag = "rec_filter_all"
        )
        if (summary.duplicateFilesCount > 0) {
            FilterChipItem(
                label = stringResource(id = R.string.filter_duplicates),
                count = summary.duplicateFilesCount,
                selected = selectedFilter == RecommendationFilter.DUPLICATES,
                onClick = { onFilterSelected(RecommendationFilter.DUPLICATES) },
                testTag = "rec_filter_duplicates"
            )
        }
        if (summary.largeFilesCount > 0) {
            FilterChipItem(
                label = stringResource(id = R.string.filter_large_files),
                count = summary.largeFilesCount,
                selected = selectedFilter == RecommendationFilter.LARGE_FILES,
                onClick = { onFilterSelected(RecommendationFilter.LARGE_FILES) },
                testTag = "rec_filter_large"
            )
        }
        if (summary.oldFilesCount > 0) {
            FilterChipItem(
                label = stringResource(id = R.string.filter_old_files),
                count = summary.oldFilesCount,
                selected = selectedFilter == RecommendationFilter.OLD_FILES,
                onClick = { onFilterSelected(RecommendationFilter.OLD_FILES) },
                testTag = "rec_filter_old"
            )
        }
        if (summary.temporaryFilesCount > 0) {
            FilterChipItem(
                label = stringResource(id = R.string.filter_temp_files),
                count = summary.temporaryFilesCount,
                selected = selectedFilter == RecommendationFilter.TEMPORARY_FILES,
                onClick = { onFilterSelected(RecommendationFilter.TEMPORARY_FILES) },
                testTag = "rec_filter_temp"
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = "$label ($count)",
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        shape = RoundedCornerShape(16.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = ElegantDarkPrimary,
            selectedLabelColor = Color.Black,
            containerColor = ElegantDarkSurfaceCard,
            labelColor = ElegantDarkTextSecondary
        ),
        modifier = Modifier.testTag(testTag)
    )
}

@Composable
private fun SelectionToolbar(
    totalFiltered: Int,
    selectedCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (selectedCount > 0) "$selectedCount de $totalFiltered selecionados" else "$totalFiltered itens disponíveis",
            fontSize = 13.sp,
            color = ElegantDarkTextMuted
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(
                onClick = onSelectAll,
                modifier = Modifier.testTag("select_all_btn")
            ) {
                Text(
                    text = stringResource(id = R.string.select_all),
                    fontSize = 12.sp,
                    color = ElegantDarkPrimary
                )
            }

            if (selectedCount > 0) {
                TextButton(
                    onClick = onClearSelection,
                    modifier = Modifier.testTag("deselect_all_btn")
                ) {
                    Text(
                        text = stringResource(id = R.string.deselect_all),
                        fontSize = 12.sp,
                        color = ElegantDarkTextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: CleanupRecommendation,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeIcon: ImageVector = when (recommendation.type) {
        RecommendationType.DUPLICATE -> Icons.Default.ContentCopy
        RecommendationType.LARGE_FILE -> Icons.Default.Storage
        RecommendationType.OLD_FILE -> Icons.Default.History
        RecommendationType.TEMPORARY_FILE -> Icons.Default.FolderZip
    }

    val priorityColor: Color = when (recommendation.priority) {
        RecommendationPriority.HIGH -> Color(0xFFFF5252)
        RecommendationPriority.MEDIUM -> Color(0xFFFFB74D)
        RecommendationPriority.LOW -> Color(0xFF64B5F6)
    }

    val priorityText: String = when (recommendation.priority) {
        RecommendationPriority.HIGH -> stringResource(id = R.string.badge_priority_high)
        RecommendationPriority.MEDIUM -> stringResource(id = R.string.badge_priority_medium)
        RecommendationPriority.LOW -> stringResource(id = R.string.badge_priority_low)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggleSelection() }
            .testTag("rec_item_${recommendation.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ElegantDarkSurfaceCard.copy(alpha = 0.9f) else ElegantDarkSurfaceCard
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, ElegantDarkPrimary) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelection() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = ElegantDarkPrimary,
                            checkmarkColor = Color.Black,
                            uncheckedColor = ElegantDarkTextMuted
                        ),
                        modifier = Modifier.testTag("checkbox_${recommendation.id}")
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElegantDarkTrack.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = ElegantDarkPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = recommendation.file.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = ElegantDarkTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = StorageFormatter.formatBytes(recommendation.file.sizeBytes),
                            fontSize = 12.sp,
                            color = ElegantDarkTextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = priorityColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = priorityText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = priorityColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Explicable Reason Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ElegantDarkTrack.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = stringResource(id = R.string.why_review_label),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElegantDarkPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = recommendation.reason,
                        fontSize = 12.sp,
                        color = ElegantDarkTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            // Bottom metric: Score & Reclaimable
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.score_label, recommendation.score),
                    fontSize = 11.sp,
                    color = ElegantDarkTextMuted,
                    fontWeight = FontWeight.Medium
                )

                if (recommendation.reclaimableSizeBytes > 0) {
                    Text(
                        text = "Recuperável: ${StorageFormatter.formatBytes(recommendation.reclaimableSizeBytes)}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = ElegantDarkPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRecommendationsView(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceCard)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(ElegantDarkPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ElegantDarkPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = stringResource(id = R.string.no_recommendations_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ElegantDarkTextPrimary
            )
            Text(
                text = stringResource(id = R.string.no_recommendations_desc),
                fontSize = 13.sp,
                color = ElegantDarkTextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}
