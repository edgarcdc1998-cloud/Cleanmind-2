package com.aistudio.cleanmind.app.presentation.screens.history

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.cleanmind.app.R
import com.aistudio.cleanmind.app.presentation.components.CleanMindTopAppBar
import com.aistudio.cleanmind.app.presentation.home.HomeUiState
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkBackground
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkPrimary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkSurfaceCard
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextMuted
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextPrimary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextSecondary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTrack

@Composable
fun HistoryScreen(
    uiState: HomeUiState,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(ElegantDarkBackground),
        containerColor = ElegantDarkBackground,
        topBar = {
            CleanMindTopAppBar(
                title = "Histórico & Estatísticas"
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
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Summary Stats Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_stats_card"),
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceCard)
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "ESTATÍSTICAS DO HISTÓRICO",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.sp,
                                        color = ElegantDarkPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (uiState.hasSavedAnalysis) "1 Análise Salva no Local" else "Nenhuma Análise Salva",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ElegantDarkTextPrimary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ElegantDarkPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = ElegantDarkPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            if (uiState.hasSavedAnalysis) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Data da Última Varredura:",
                                            fontSize = 13.sp,
                                            color = ElegantDarkTextMuted
                                        )
                                        Text(
                                            text = uiState.lastAnalyzedDateFormatted ?: "--",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = ElegantDarkTextPrimary
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Total de Arquivos Escaneados:",
                                            fontSize = 13.sp,
                                            color = ElegantDarkTextMuted
                                        )
                                        Text(
                                            text = "${uiState.totalFilesAnalyzed} arquivos",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = FontFamily.Monospace,
                                            color = ElegantDarkTextPrimary
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Volume Analisado:",
                                            fontSize = 13.sp,
                                            color = ElegantDarkTextMuted
                                        )
                                        Text(
                                            text = uiState.totalAnalyzedSpaceFormatted ?: "--",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = FontFamily.Monospace,
                                            color = ElegantDarkTextPrimary
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Espaço Potencialmente Recuperável:",
                                            fontSize = 13.sp,
                                            color = ElegantDarkTextMuted
                                        )
                                        Text(
                                            text = uiState.potentialReclaimableSpaceFormatted ?: "0 B",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFF81C784)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                OutlinedButton(
                                    onClick = onClearHistory,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("clear_history_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Limpar Histórico de Análises")
                                }
                            } else {
                                Text(
                                    text = "Realize uma análise na tela Inicial para registrar dados históricos de armazenamento localmente via Room.",
                                    fontSize = 13.sp,
                                    color = ElegantDarkTextMuted,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    // Explanation Card
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElegantDarkTrack.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = ElegantDarkPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "O histórico armazena de forma compacta e segura apenas metadados e estatísticas de varredura no banco de dados local Room, preservando sua privacidade e evitando envio de dados para fora do dispositivo.",
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                color = ElegantDarkTextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
