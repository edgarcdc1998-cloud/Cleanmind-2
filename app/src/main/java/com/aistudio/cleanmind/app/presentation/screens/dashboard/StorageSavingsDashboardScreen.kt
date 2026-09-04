package com.aistudio.cleanmind.app.presentation.screens.dashboard

import android.app.Application
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.HistoryToggleOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aistudio.cleanmind.app.R
import com.aistudio.cleanmind.app.domain.model.AnalysisTimePoint
import com.aistudio.cleanmind.app.domain.model.PeriodSavingsPoint
import com.aistudio.cleanmind.app.domain.model.SavingsDashboardData
import com.aistudio.cleanmind.app.domain.model.SavingsTrendSummary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkBackground
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkPrimary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkSurfaceCard
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkSurfaceElevated
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextMuted
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextPrimary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextSecondary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTrack
import com.aistudio.cleanmind.app.presentation.theme.SafeGreen
import com.aistudio.cleanmind.app.util.StorageFormatter
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSavingsDashboardScreen(
    onClearHistory: (() -> Unit)? = null,
    viewModel: SavingsDashboardViewModel = viewModel(
        factory = SavingsDashboardViewModel.provideFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.dashboard_title),
                            color = ElegantDarkTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = stringResource(R.string.dashboard_subtitle),
                            color = ElegantDarkTextMuted,
                            fontSize = 12.sp
                        )
                    }
                },
                actions = {
                    if (uiState is SavingsDashboardUiState.Success) {
                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier
                                .testTag("clear_history_top_btn")
                                .size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Limpar histórico do banco",
                                tint = ElegantDarkTextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ElegantDarkBackground
                )
            )
        },
        containerColor = ElegantDarkBackground,
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_root")
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (val state = uiState) {
                is SavingsDashboardUiState.Loading -> {
                    DashboardLoadingView()
                }
                is SavingsDashboardUiState.Empty -> {
                    DashboardEmptyView()
                }
                is SavingsDashboardUiState.Error -> {
                    DashboardErrorView(
                        message = state.message,
                        onRetry = { viewModel.loadDashboardData() }
                    )
                }
                is SavingsDashboardUiState.Success -> {
                    DashboardSuccessContent(
                        state = state,
                        onSelectPeriod = { viewModel.onSelectPeriodType(it) },
                        onClearHistoryClick = { showClearConfirmDialog = true }
                    )
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = ElegantDarkSurfaceElevated,
            titleContentColor = ElegantDarkTextPrimary,
            textContentColor = ElegantDarkTextSecondary,
            title = {
                Text(
                    text = stringResource(R.string.dashboard_clear_dialog_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = stringResource(R.string.dashboard_clear_dialog_desc))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        viewModel.clearHistory()
                        onClearHistory?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_clear_dialog_btn")
                ) {
                    Text(stringResource(R.string.dashboard_btn_clear_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showClearConfirmDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = ElegantDarkTextSecondary
                    )
                ) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun DashboardLoadingView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = ElegantDarkPrimary,
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = "Carregando histórico do banco de dados Room"
                    }
            )
            Text(
                text = "Carregando métricas de economia...",
                color = ElegantDarkTextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun DashboardEmptyView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .testTag("empty_dashboard_card"),
            colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceCard),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color(0xFF2C3240), Color(0xFF1E232F))))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1F293D),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = ElegantDarkPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.dashboard_empty_title),
                    color = ElegantDarkTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.dashboard_empty_desc),
                    color = ElegantDarkTextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun DashboardErrorView(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp),
            colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = "Não foi possível carregar o histórico",
                    color = ElegantDarkTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = message,
                    color = ElegantDarkTextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = ElegantDarkPrimary),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tentar Novamente")
                }
            }
        }
    }
}

@Composable
private fun DashboardSuccessContent(
    state: SavingsDashboardUiState.Success,
    onSelectPeriod: (PeriodViewType) -> Unit,
    onClearHistoryClick: () -> Unit
) {
    val data = state.data

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 600.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Resumo Principal
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_stats_card"),
                colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceCard),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color(0xFF2C3240), Color(0xFF1E232F))))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Analytics,
                                contentDescription = null,
                                tint = ElegantDarkPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Resumo Geral do Histórico",
                                color = ElegantDarkTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Text(
                                text = if (data.totalAnalysesCount == 1) {
                                    stringResource(R.string.dashboard_one_analysis)
                                } else {
                                    stringResource(R.string.dashboard_total_analyses, data.totalAnalysesCount)
                                },
                                color = ElegantDarkPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = ElegantDarkTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Última análise registrada: ${state.formattedLatestDate}",
                            color = ElegantDarkTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // 2. Métricas de Diferenciação Transparente (Volume Analisado vs Potencial vs Realizado)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card 1: Volume Analisado
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.dashboard_metric_analyzed),
                        primaryValue = state.formattedLatestAnalyzedSize,
                        secondaryLabel = "Acumulado: ${state.formattedTotalVolumeAnalyzed}",
                        icon = Icons.Filled.Storage,
                        iconTint = ElegantDarkPrimary,
                        accessibilityDesc = "Volume efetivamente analisado: ${state.formattedLatestAnalyzedSize}"
                    )

                    // Card 2: Potencialmente Recuperável
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.dashboard_metric_potential),
                        primaryValue = state.formattedLatestPotentialReclaimable,
                        secondaryLabel = "${data.totalRecommendationsCount} itens identificados",
                        icon = Icons.Filled.AutoAwesome,
                        iconTint = SafeGreen,
                        accessibilityDesc = "Potencialmente recuperável: ${state.formattedLatestPotentialReclaimable}"
                    )
                }

                // Card 3: Transparência sobre Economia Realizada
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceElevated),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier
                                .size(24.dp)
                                .padding(top = 2.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.dashboard_metric_executed),
                                    color = ElegantDarkTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1E293B)
                                ) {
                                    Text(
                                        text = stringResource(R.string.dashboard_metric_executed_status),
                                        color = ElegantDarkTextMuted,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.dashboard_metric_executed_desc),
                                color = ElegantDarkTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. Detalhamento das Recomendações por Categoria (quando houver)
        if (data.totalRecommendationsCount > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Oportunidades de Recuperação Ativas",
                            color = ElegantDarkTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CategoryBadge(label = "Duplicatas", count = data.duplicatesCount)
                            CategoryBadge(label = "Arquivos Grandes", count = data.largeFilesCount)
                            CategoryBadge(label = "Arquivos Antigos", count = data.oldFilesCount)
                            CategoryBadge(label = "Temporários", count = data.tempFilesCount)
                        }
                    }
                }
            }
        }

        // 4. Evolução Temporal do Armazenamento
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("evolution_chart_card"),
                colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timeline,
                                contentDescription = null,
                                tint = ElegantDarkPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.dashboard_trend_title),
                                color = ElegantDarkTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    if (data.totalAnalysesCount == 1) {
                        // Apenas 1 registro: Não inventar tendência
                        SingleObservationNotice(timepoint = data.timePoints.first())
                    } else {
                        // 2 ou mais registros: visualização com períodos e gráfico proporcional
                        PeriodSelector(
                            selected = state.selectedPeriodType,
                            onSelect = onSelectPeriod
                        )

                        data.trendSummary?.let { trend ->
                            TrendSummaryHeader(trend = trend)
                        }

                        when (state.selectedPeriodType) {
                            PeriodViewType.OBSERVATIONS -> {
                                ObservationsBarsChart(points = data.timePoints)
                            }
                            PeriodViewType.WEEKLY -> {
                                if (data.weeklyAggregations.isNotEmpty()) {
                                    AggregatedPeriodBarsChart(points = data.weeklyAggregations)
                                } else {
                                    Text(
                                        text = "Dados semanais insuficientes.",
                                        color = ElegantDarkTextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            PeriodViewType.MONTHLY -> {
                                if (data.monthlyAggregations.isNotEmpty()) {
                                    AggregatedPeriodBarsChart(points = data.monthlyAggregations)
                                } else {
                                    Text(
                                        text = "Dados mensais insuficientes.",
                                        color = ElegantDarkTextMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Legenda do gráfico
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LegendItem(color = ElegantDarkPrimary, label = "Volume Analisado")
                            Spacer(modifier = Modifier.width(16.dp))
                            LegendItem(color = SafeGreen, label = "Potencial Recuperável")
                        }
                    }
                }
            }
        }

        // 5. Botão para Limpar Histórico do Room
        item {
            OutlinedButton(
                onClick = onClearHistoryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("clear_history_button"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error))
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Limpar Histórico de Análises",
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    primaryValue: String,
    secondaryLabel: String,
    icon: ImageVector,
    iconTint: Color,
    accessibilityDesc: String
) {
    Card(
        modifier = modifier.semantics { contentDescription = accessibilityDesc },
        colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceCard),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color(0xFF2C3240), Color(0xFF1E232F))))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    color = ElegantDarkTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = primaryValue,
                color = ElegantDarkTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = secondaryLabel,
                color = ElegantDarkTextMuted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun CategoryBadge(label: String, count: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF1F2430),
            modifier = Modifier.padding(2.dp)
        ) {
            Text(
                text = "$count",
                color = ElegantDarkPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Text(
            text = label,
            color = ElegantDarkTextMuted,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SingleObservationNotice(timepoint: AnalysisTimePoint) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF171B26))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.HistoryToggleOff,
                contentDescription = null,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Observação Única",
                color = Color(0xFFF59E0B),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
        Text(
            text = stringResource(R.string.dashboard_single_observation_notice),
            color = ElegantDarkTextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Tamanho: ${StorageFormatter.formatBytes(timepoint.analyzedSizeBytes)}",
                color = ElegantDarkTextMuted,
                fontSize = 11.sp
            )
            Text(
                text = "Potencial: ${StorageFormatter.formatBytes(timepoint.potentialReclaimableBytes)}",
                color = SafeGreen,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun PeriodSelector(
    selected: PeriodViewType,
    onSelect: (PeriodViewType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selected == PeriodViewType.OBSERVATIONS,
            onClick = { onSelect(PeriodViewType.OBSERVATIONS) },
            label = { Text(stringResource(R.string.dashboard_filter_observations)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = ElegantDarkPrimary,
                selectedLabelColor = Color.Black,
                containerColor = Color(0xFF1E2430),
                labelColor = ElegantDarkTextSecondary
            ),
            modifier = Modifier.height(48.dp)
        )
        FilterChip(
            selected = selected == PeriodViewType.WEEKLY,
            onClick = { onSelect(PeriodViewType.WEEKLY) },
            label = { Text(stringResource(R.string.dashboard_filter_weekly)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = ElegantDarkPrimary,
                selectedLabelColor = Color.Black,
                containerColor = Color(0xFF1E2430),
                labelColor = ElegantDarkTextSecondary
            ),
            modifier = Modifier.height(48.dp)
        )
        FilterChip(
            selected = selected == PeriodViewType.MONTHLY,
            onClick = { onSelect(PeriodViewType.MONTHLY) },
            label = { Text(stringResource(R.string.dashboard_filter_monthly)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = ElegantDarkPrimary,
                selectedLabelColor = Color.Black,
                containerColor = Color(0xFF1E2430),
                labelColor = ElegantDarkTextSecondary
            ),
            modifier = Modifier.height(48.dp)
        )
    }
}

@Composable
private fun TrendSummaryHeader(trend: SavingsTrendSummary) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF181D2A),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val deltaStr = if (trend.analyzedDeltaBytes >= 0) {
                "+${StorageFormatter.formatBytes(trend.analyzedDeltaBytes)}"
            } else {
                "-${StorageFormatter.formatBytes(-trend.analyzedDeltaBytes)}"
            }
            Text(
                text = "Variação: $deltaStr (${trend.observationsCount} varreduras)",
                color = ElegantDarkTextSecondary,
                fontSize = 12.sp
            )

            val reclaimDeltaStr = if (trend.reclaimableDeltaBytes >= 0) {
                "+${StorageFormatter.formatBytes(trend.reclaimableDeltaBytes)}"
            } else {
                "-${StorageFormatter.formatBytes(-trend.reclaimableDeltaBytes)}"
            }
            Text(
                text = "Potencial: $reclaimDeltaStr",
                color = SafeGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ObservationsBarsChart(points: List<AnalysisTimePoint>) {
    val maxBytes = max(1L, points.maxOfOrNull { it.analyzedSizeBytes } ?: 1L)

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        items(points) { point ->
            val analyzedHeightFraction = (point.analyzedSizeBytes.toFloat() / maxBytes).coerceIn(0.12f, 1f)
            val potentialHeightFraction = (point.potentialReclaimableBytes.toFloat() / maxBytes).coerceIn(0.05f, 1f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(48.dp)
                    .semantics {
                        contentDescription = "Varredura gravada em ${StorageFormatter.formatTimestamp(point.timestampEpochMillis)}: ${StorageFormatter.formatBytes(point.analyzedSizeBytes)} analisados, ${StorageFormatter.formatBytes(point.potentialReclaimableBytes)} recuperáveis"
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Barra Volume Analisado
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(analyzedHeightFraction)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(ElegantDarkPrimary)
                    )

                    // Barra Potencial Recuperável
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(potentialHeightFraction)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(SafeGreen)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = StorageFormatter.formatTimestamp(point.timestampEpochMillis).take(5),
                    color = ElegantDarkTextMuted,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun AggregatedPeriodBarsChart(points: List<PeriodSavingsPoint>) {
    val maxBytes = max(1L, points.maxOfOrNull { it.totalAnalyzedSizeBytes } ?: 1L)

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        items(points) { point ->
            val analyzedFraction = (point.totalAnalyzedSizeBytes.toFloat() / maxBytes).coerceIn(0.12f, 1f)
            val potentialFraction = (point.potentialReclaimableBytes.toFloat() / maxBytes).coerceIn(0.05f, 1f)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(60.dp)
                    .semantics {
                        contentDescription = "${point.periodLabel}: ${StorageFormatter.formatBytes(point.totalAnalyzedSizeBytes)} analisados, ${StorageFormatter.formatBytes(point.potentialReclaimableBytes)} recuperáveis"
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(analyzedFraction)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(ElegantDarkPrimary)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(potentialFraction)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(SafeGreen)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = point.periodLabel.take(8),
                    color = ElegantDarkTextMuted,
                    fontSize = 9.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(
            text = label,
            color = ElegantDarkTextSecondary,
            fontSize = 11.sp
        )
    }
}
