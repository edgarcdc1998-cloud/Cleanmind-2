package com.aistudio.cleanmind.app.presentation.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.cleanmind.app.R
import com.aistudio.cleanmind.app.domain.model.BackgroundWorkStatus
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
fun SettingsScreen(
    uiState: HomeUiState,
    onToggleAutoAnalysis: (Boolean) -> Unit,
    onSetIntervalHours: (Int) -> Unit,
    onStartManualAnalysis: () -> Unit,
    onCancelAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(ElegantDarkBackground),
        containerColor = ElegantDarkBackground,
        topBar = {
            CleanMindTopAppBar(
                title = stringResource(id = R.string.settings_title),
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElegantDarkPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = ElegantDarkPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
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
                    // 1. Automatic Periodic Analysis Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_auto_analysis_card"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceCard)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(ElegantDarkPrimary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = ElegantDarkPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = stringResource(id = R.string.settings_auto_analysis_title),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ElegantDarkTextPrimary
                                        )
                                        Text(
                                            text = stringResource(id = R.string.settings_auto_analysis_desc),
                                            fontSize = 12.sp,
                                            color = ElegantDarkTextMuted,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                Switch(
                                    checked = uiState.isAutoAnalysisEnabled,
                                    onCheckedChange = onToggleAutoAnalysis,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = ElegantDarkPrimary,
                                        uncheckedThumbColor = ElegantDarkTextMuted,
                                        uncheckedTrackColor = ElegantDarkTrack
                                    ),
                                    modifier = Modifier.testTag("auto_analysis_switch")
                                )
                            }

                            if (uiState.isAutoAnalysisEnabled) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ElegantDarkTrack.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.settings_auto_interval_title),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ElegantDarkPrimary
                                        )

                                        IntervalRadioOption(
                                            label = stringResource(id = R.string.settings_auto_interval_daily),
                                            selected = uiState.autoAnalysisIntervalHours == 24,
                                            onClick = { onSetIntervalHours(24) },
                                            testTag = "radio_interval_24h"
                                        )

                                        IntervalRadioOption(
                                            label = stringResource(id = R.string.settings_auto_interval_weekly),
                                            selected = uiState.autoAnalysisIntervalHours == 168,
                                            onClick = { onSetIntervalHours(168) },
                                            testTag = "radio_interval_168h"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. WorkManager Status & Controls
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_workmanager_card"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceCard)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF81D4FA).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = Color(0xFF81D4FA),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = stringResource(id = R.string.settings_workmanager_status_title),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ElegantDarkTextPrimary
                                        )
                                        Text(
                                            text = getWorkerStatusText(uiState.backgroundWorkStatus),
                                            fontSize = 12.sp,
                                            color = if (uiState.isBackgroundWorkActive) ElegantDarkPrimary else ElegantDarkTextMuted
                                        )
                                    }
                                }
                            }

                            Text(
                                text = stringResource(id = R.string.settings_workmanager_constraints_info),
                                fontSize = 12.sp,
                                color = ElegantDarkTextMuted,
                                lineHeight = 16.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onStartManualAnalysis,
                                    enabled = !uiState.isAnalyzing,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("start_background_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ElegantDarkPrimary,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Executar Agora", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                if (uiState.isBackgroundWorkActive) {
                                    OutlinedButton(
                                        onClick = onCancelAnalysis,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("cancel_background_btn"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Cancel,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Cancelar", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 3. Privacy & Security Section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_privacy_card"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceCard)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.privacy_title),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ElegantDarkTextPrimary
                            )

                            PrivacyFeatureRow(
                                icon = Icons.Default.Security,
                                title = stringResource(id = R.string.privacy_statement_1),
                                desc = stringResource(id = R.string.privacy_statement_1_desc)
                            )

                            PrivacyFeatureRow(
                                icon = Icons.Default.CloudOff,
                                title = stringResource(id = R.string.privacy_statement_2),
                                desc = stringResource(id = R.string.privacy_statement_2_desc)
                            )

                            PrivacyFeatureRow(
                                icon = Icons.Default.CheckCircle,
                                title = stringResource(id = R.string.privacy_statement_3),
                                desc = stringResource(id = R.string.privacy_statement_3_desc)
                            )
                        }
                    }

                    // 4. App Information Section
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElegantDarkTrack.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Aplicativo",
                                    fontSize = 13.sp,
                                    color = ElegantDarkTextMuted
                                )
                                Text(
                                    text = stringResource(id = R.string.app_name),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElegantDarkTextPrimary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Versão",
                                    fontSize = 13.sp,
                                    color = ElegantDarkTextMuted
                                )
                                Text(
                                    text = stringResource(id = R.string.app_version_label),
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ElegantDarkTextSecondary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Pacote",
                                    fontSize = 13.sp,
                                    color = ElegantDarkTextMuted
                                )
                                Text(
                                    text = stringResource(id = R.string.app_namespace_label),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ElegantDarkPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntervalRadioOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = ElegantDarkPrimary,
                unselectedColor = ElegantDarkTextMuted
            ),
            modifier = Modifier.testTag(testTag)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (selected) ElegantDarkTextPrimary else ElegantDarkTextSecondary
        )
    }
}

@Composable
private fun PrivacyFeatureRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ElegantDarkPrimary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ElegantDarkPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = ElegantDarkTextPrimary
            )
            Text(
                text = desc,
                fontSize = 12.sp,
                color = ElegantDarkTextMuted,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun getWorkerStatusText(status: BackgroundWorkStatus): String {
    return when (status) {
        BackgroundWorkStatus.IDLE -> stringResource(id = R.string.worker_status_idle)
        BackgroundWorkStatus.ENQUEUED -> stringResource(id = R.string.worker_status_enqueued)
        BackgroundWorkStatus.RUNNING -> stringResource(id = R.string.worker_status_running)
        BackgroundWorkStatus.SUCCEEDED -> stringResource(id = R.string.worker_status_succeeded)
        BackgroundWorkStatus.FAILED -> stringResource(id = R.string.worker_status_failed)
        BackgroundWorkStatus.CANCELLED -> stringResource(id = R.string.worker_status_cancelled)
    }
}
