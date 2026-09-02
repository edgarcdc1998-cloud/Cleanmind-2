package com.aistudio.cleanmind.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.cleanmind.app.R
import com.aistudio.cleanmind.app.domain.model.AnalysisRecommendationsSummary
import com.aistudio.cleanmind.app.domain.model.CleanupRecommendation
import com.aistudio.cleanmind.app.domain.model.RecommendationPriority
import com.aistudio.cleanmind.app.domain.model.RecommendationType
import com.aistudio.cleanmind.app.presentation.home.RecommendationFilter
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkPrimary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkSurfaceCard
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextMuted
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextPrimary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextSecondary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTrack
import com.aistudio.cleanmind.app.util.StorageFormatter

@Composable
fun RecommendationsSection(
    summary: AnalysisRecommendationsSummary?,
    recommendations: List<CleanupRecommendation>,
    selectedFilter: RecommendationFilter,
    onFilterSelected: (RecommendationFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    if (summary == null || summary.totalRecommendationsCount == 0) {
        NoRecommendationsCard(modifier = modifier)
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ElegantDarkPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ElegantDarkPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(id = R.string.recommendations_header),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ElegantDarkTextPrimary
                    )
                    Text(
                        text = stringResource(id = R.string.recommendations_subtitle),
                        fontSize = 11.sp,
                        color = ElegantDarkTextMuted
                    )
                }
            }

            Text(
                text = "${summary.totalRecommendationsCount} itens",
                fontSize = 13.sp,
                color = ElegantDarkPrimary,
                fontWeight = FontWeight.Medium
            )
        }

        // Summary Hero Card with Reclaimable Space
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = ElegantDarkSurfaceCard
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
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
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElegantDarkTextPrimary
                        )
                    }
                }

                // Distribution Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (summary.duplicateFilesCount > 0) {
                        SummaryPill(
                            label = "${summary.duplicateFilesCount} duplicatas",
                            color = Color(0xFFFF8A65)
                        )
                    }
                    if (summary.largeFilesCount > 0) {
                        SummaryPill(
                            label = "${summary.largeFilesCount} grandes",
                            color = Color(0xFFFFD54F)
                        )
                    }
                    if (summary.oldFilesCount > 0) {
                        SummaryPill(
                            label = "${summary.oldFilesCount} antigos",
                            color = Color(0xFF81D4FA)
                        )
                    }
                    if (summary.temporaryFilesCount > 0) {
                        SummaryPill(
                            label = "${summary.temporaryFilesCount} temporários",
                            color = Color(0xFFA5D6A7)
                        )
                    }
                }
            }
        }

        // Filter Chips Row
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
                testTag = "filter_chip_all"
            )
            if (summary.duplicateFilesCount > 0) {
                FilterChipItem(
                    label = stringResource(id = R.string.filter_duplicates),
                    count = summary.duplicateFilesCount,
                    selected = selectedFilter == RecommendationFilter.DUPLICATES,
                    onClick = { onFilterSelected(RecommendationFilter.DUPLICATES) },
                    testTag = "filter_chip_duplicates"
                )
            }
            if (summary.largeFilesCount > 0) {
                FilterChipItem(
                    label = stringResource(id = R.string.filter_large_files),
                    count = summary.largeFilesCount,
                    selected = selectedFilter == RecommendationFilter.LARGE_FILES,
                    onClick = { onFilterSelected(RecommendationFilter.LARGE_FILES) },
                    testTag = "filter_chip_large_files"
                )
            }
            if (summary.oldFilesCount > 0) {
                FilterChipItem(
                    label = stringResource(id = R.string.filter_old_files),
                    count = summary.oldFilesCount,
                    selected = selectedFilter == RecommendationFilter.OLD_FILES,
                    onClick = { onFilterSelected(RecommendationFilter.OLD_FILES) },
                    testTag = "filter_chip_old_files"
                )
            }
            if (summary.temporaryFilesCount > 0) {
                FilterChipItem(
                    label = stringResource(id = R.string.filter_temp_files),
                    count = summary.temporaryFilesCount,
                    selected = selectedFilter == RecommendationFilter.TEMPORARY_FILES,
                    onClick = { onFilterSelected(RecommendationFilter.TEMPORARY_FILES) },
                    testTag = "filter_chip_temp_files"
                )
            }
        }

        // Recommendations List
        if (recommendations.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceCard)
            ) {
                Text(
                    text = "Nenhum item nesta categoria de filtro.",
                    fontSize = 13.sp,
                    color = ElegantDarkTextMuted,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                recommendations.forEach { recommendation ->
                    RecommendationItemCard(
                        recommendation = recommendation,
                        modifier = Modifier.testTag("recommendation_card_${recommendation.id}")
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryPill(
    label: String,
    color: Color
) {
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
private fun RecommendationItemCard(
    recommendation: CleanupRecommendation,
    modifier: Modifier = Modifier
) {
    val typeIcon = when (recommendation.type) {
        RecommendationType.DUPLICATE -> Icons.Default.ContentCopy
        RecommendationType.LARGE_FILE -> Icons.Default.Storage
        RecommendationType.OLD_FILE -> Icons.Default.History
        RecommendationType.TEMPORARY_FILE -> Icons.Default.FolderZip
    }

    val priorityColor = when (recommendation.priority) {
        RecommendationPriority.HIGH -> Color(0xFFFF5252)
        RecommendationPriority.MEDIUM -> Color(0xFFFFB74D)
        RecommendationPriority.LOW -> Color(0xFF64B5F6)
    }

    val priorityText = when (recommendation.priority) {
        RecommendationPriority.HIGH -> stringResource(id = R.string.badge_priority_high)
        RecommendationPriority.MEDIUM -> stringResource(id = R.string.badge_priority_medium)
        RecommendationPriority.LOW -> stringResource(id = R.string.badge_priority_low)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceCard)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: File Name & Reclaimable Size
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
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

                // Priority Badge
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

            // Bottom Metric Row: Score & Reclaimable Space
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
private fun NoRecommendationsCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceCard)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(ElegantDarkPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = ElegantDarkPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = stringResource(id = R.string.no_recommendations_title),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = ElegantDarkTextPrimary
            )
            Text(
                text = stringResource(id = R.string.no_recommendations_desc),
                fontSize = 13.sp,
                color = ElegantDarkTextMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}
