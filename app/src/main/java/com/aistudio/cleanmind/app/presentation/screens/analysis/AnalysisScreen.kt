package com.aistudio.cleanmind.app.presentation.screens.analysis

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.cleanmind.app.R
import com.aistudio.cleanmind.app.presentation.components.CategorySummaryCard
import com.aistudio.cleanmind.app.presentation.components.CleanMindTopAppBar
import com.aistudio.cleanmind.app.presentation.home.AnalysisStatus
import com.aistudio.cleanmind.app.presentation.home.HomeUiState
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkBackground
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkOnPrimary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkPrimary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkSurfaceCard
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextMuted
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextPrimary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextSecondary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTrack

@Composable
fun AnalysisScreen(
    uiState: HomeUiState,
    onStartAnalysis: () -> Unit,
    onCancelAnalysis: () -> Unit,
    onNavigateToRecommendations: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(ElegantDarkBackground),
        containerColor = ElegantDarkBackground,
        topBar = {
            CleanMindTopAppBar(
                title = stringResource(id = R.string.analysis_details_title)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status Banner Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analysis_status_card"),
                        shape = RoundedCornerShape(24.dp),
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (uiState.isAnalyzing) ElegantDarkPrimary.copy(alpha = 0.2f)
                                                else Color(0xFF81C784).copy(alpha = 0.2f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.isAnalyzing) Icons.Default.Analytics else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (uiState.isAnalyzing) ElegantDarkPrimary else Color(0xFF81C784),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = stringResource(id = R.string.analysis_status_title),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ElegantDarkPrimary,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = when (uiState.status) {
                                                is AnalysisStatus.Analyzing -> stringResource(id = R.string.storage_status_analyzing)
                                                is AnalysisStatus.Success -> stringResource(id = R.string.storage_status_analyzed)
                                                is AnalysisStatus.Saved -> stringResource(id = R.string.last_analysis_header)
                                                is AnalysisStatus.PermissionDenied -> stringResource(id = R.string.permission_denied_title)
                                                is AnalysisStatus.PersistenceError -> stringResource(id = R.string.error_persistence)
                                                is AnalysisStatus.Error -> "Erro na Análise"
                                                else -> stringResource(id = R.string.storage_status_not_analyzed)
                                            },
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ElegantDarkTextPrimary
                                        )
                                    }
                                }
                            }

                            if (uiState.lastAnalyzedDateFormatted != null) {
                                Text(
                                    text = stringResource(id = R.string.last_analysis_date_format, uiState.lastAnalyzedDateFormatted),
                                    fontSize = 12.sp,
                                    color = ElegantDarkTextMuted
                                )
                            }
                        }
                    }

                    if (uiState.isAnalyzing) {
                        // Live Scanning Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("analyzing_progress_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceCard)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = ElegantDarkPrimary,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = if (uiState.isBackgroundWorkActive) {
                                        "Análise em segundo plano via WorkManager..."
                                    } else {
                                        "Catalogando arquivos locais..."
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ElegantDarkTextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Varredura 100% on-device. Nenhum dado é transmitido para a rede.",
                                    fontSize = 12.sp,
                                    color = ElegantDarkTextMuted,
                                    textAlign = TextAlign.Center
                                )
                                OutlinedButton(
                                    onClick = onCancelAnalysis,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.testTag("cancel_analysis_btn")
                                ) {
                                    Text(stringResource(id = R.string.btn_cancel_analysis))
                                }
                            }
                        }
                    } else if (uiState.isAnalyzed) {
                        // Key Findings Section
                        val summary = uiState.recommendationsSummary
                        if (summary != null && summary.totalRecommendationsCount > 0) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.findings_title),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ElegantDarkTextPrimary
                                )

                                if (summary.duplicateFilesCount > 0) {
                                    FindingCard(
                                        icon = Icons.Default.ContentCopy,
                                        iconTint = Color(0xFFFF8A65),
                                        text = stringResource(id = R.string.findings_duplicates_format, summary.duplicateFilesCount),
                                        subtext = "Identificados via agrupamento por tamanho e hash SHA-256 local."
                                    )
                                }

                                if (summary.largeFilesCount > 0) {
                                    FindingCard(
                                        icon = Icons.Default.Storage,
                                        iconTint = Color(0xFFFFD54F),
                                        text = stringResource(id = R.string.findings_large_format, summary.largeFilesCount),
                                        subtext = "Arquivos com tamanho acima do limiar de 100 MB."
                                    )
                                }

                                if (summary.oldFilesCount > 0) {
                                    FindingCard(
                                        icon = Icons.Default.History,
                                        iconTint = Color(0xFF81D4FA),
                                        text = stringResource(id = R.string.findings_old_format, summary.oldFilesCount),
                                        subtext = "Não modificados há mais de 30 dias."
                                    )
                                }

                                if (summary.temporaryFilesCount > 0) {
                                    FindingCard(
                                        icon = Icons.Default.FolderZip,
                                        iconTint = Color(0xFFA5D6A7),
                                        text = stringResource(id = R.string.findings_temp_format, summary.temporaryFilesCount),
                                        subtext = "Extensões transitórias (.tmp, .log, .cache)."
                                    )
                                }

                                Button(
                                    onClick = onNavigateToRecommendations,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("go_to_recommendations_btn"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ElegantDarkPrimary,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(id = R.string.btn_go_to_recommendations),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Category Breakdown Section
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.storage_breakdown_title),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ElegantDarkTextPrimary
                            )

                            if (uiState.categories.isEmpty()) {
                                Text(
                                    text = stringResource(id = R.string.analysis_empty_state),
                                    fontSize = 13.sp,
                                    color = ElegantDarkTextMuted
                                )
                            } else {
                                val totalCategoryBytes = uiState.categories.sumOf { summary ->
                                    // Parse or calculate from file counts
                                    1L
                                }
                                uiState.categories.forEach { categoryUi ->
                                    CategorySummaryCard(summary = categoryUi)
                                }
                            }
                        }

                        // Reanalyze Button
                        OutlinedButton(
                            onClick = onStartAnalysis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("reanalyze_button_analysis_screen"),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(id = R.string.btn_reanalyze_storage), fontSize = 14.sp)
                        }
                    } else {
                        // Empty State Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceCard)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(ElegantDarkTrack.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Analytics,
                                        contentDescription = null,
                                        tint = ElegantDarkPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = "Nenhuma análise recente",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ElegantDarkTextPrimary
                                )
                                Text(
                                    text = "Inicie a varredura para diagnosticar o armazenamento do dispositivo e descobrir oportunidades seguras de otimização.",
                                    fontSize = 13.sp,
                                    color = ElegantDarkTextMuted,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                                Button(
                                    onClick = onStartAnalysis,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ElegantDarkPrimary,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(stringResource(id = R.string.btn_analyze_storage))
                                }
                            }
                        }
                    }

                    // Privacy Assurance Pill
                    Surface(
                        shape = CircleShape,
                        color = ElegantDarkSurfaceCard,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = ElegantDarkPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.privacy_badge_text),
                                fontSize = 11.sp,
                                color = ElegantDarkTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FindingCard(
    icon: ImageVector,
    iconTint: Color,
    text: String,
    subtext: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceCard)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ElegantDarkTextPrimary
                )
                Text(
                    text = subtext,
                    fontSize = 12.sp,
                    color = ElegantDarkTextMuted
                )
            }
        }
    }
}
