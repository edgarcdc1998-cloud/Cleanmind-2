package com.aistudio.cleanmind.app.presentation.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aistudio.cleanmind.app.R
import com.aistudio.cleanmind.app.domain.model.StorageCategory
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkBackground
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkOnPrimary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkPrimary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkSecondary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkSurfaceCard
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextMuted
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextPrimary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextSecondary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTrack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.provideFactory(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val isAnyGranted = permissionsMap.values.any { it }
        if (isAnyGranted) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    fun handleStartAnalysis() {
        val allGranted = requiredPermissions.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onAnalyzeRequested()
            permissionLauncher.launch(requiredPermissions)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(ElegantDarkBackground),
        containerColor = ElegantDarkBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElegantDarkPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = ElegantDarkOnPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(id = R.string.app_name),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.5).sp,
                            color = ElegantDarkTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onSettingsClicked() },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(id = R.string.settings_title),
                            tint = ElegantDarkTextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ElegantDarkBackground
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ElegantDarkBackground
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { handleStartAnalysis() },
                        enabled = !uiState.isAnalyzing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .height(56.dp)
                            .testTag("analyze_button"),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElegantDarkPrimary,
                            contentColor = ElegantDarkOnPrimary,
                            disabledContainerColor = ElegantDarkSurfaceCard,
                            disabledContentColor = ElegantDarkTextMuted
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        if (uiState.isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = ElegantDarkTextMuted,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(id = R.string.storage_status_analyzing),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Icon(
                                imageVector = if (uiState.isAnalyzed) Icons.Default.Refresh else Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.isAnalyzed) {
                                    stringResource(id = R.string.btn_reanalyze_storage)
                                } else {
                                    stringResource(id = R.string.btn_analyze_storage)
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
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
                    // Storage Summary Card
                    StorageSummaryCard(
                        uiState = uiState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("storage_summary_card")
                    )

                    // Dynamic Content based on Analysis Status
                    when (val status = uiState.status) {
                        is AnalysisStatus.Idle -> {
                            HeroOnboardingSection()
                            PrivacyBadge(modifier = Modifier.testTag("privacy_badge"))
                        }

                        is AnalysisStatus.Saved -> {
                            AnalysisResultsSection(uiState = uiState, isSaved = true)
                            PrivacyBadge(modifier = Modifier.testTag("privacy_badge"))
                        }

                        is AnalysisStatus.RequestingPermission -> {
                            RequestingPermissionSection()
                        }

                        is AnalysisStatus.Analyzing -> {
                            AnalyzingStateSection()
                        }

                        is AnalysisStatus.Success -> {
                            AnalysisResultsSection(uiState = uiState, isSaved = false)
                            PrivacyBadge(modifier = Modifier.testTag("privacy_badge"))
                        }

                        is AnalysisStatus.PermissionDenied -> {
                            PermissionDeniedCard(
                                onRetry = { handleStartAnalysis() },
                                onOpenSettings = { openAppSettings(context) },
                                modifier = Modifier.testTag("permission_denied_card")
                            )
                        }

                        is AnalysisStatus.PersistenceError -> {
                            ErrorStateCard(
                                message = status.errorMessage,
                                onRetry = { handleStartAnalysis() },
                                modifier = Modifier.testTag("error_card")
                            )
                        }

                        is AnalysisStatus.Error -> {
                            ErrorStateCard(
                                message = status.errorMessage,
                                onRetry = { handleStartAnalysis() },
                                modifier = Modifier.testTag("error_card")
                            )
                        }
                    }

                    // Architecture & Safety Info Card
                    SafeProcessingInfoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("info_card")
                    )
                }
            }
        }
    }

    if (uiState.showSettingsDialog) {
        SettingsDialog(
            onDismiss = { viewModel.onDismissSettings() }
        )
    }
}

@Composable
private fun StorageSummaryCard(
    uiState: HomeUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = ElegantDarkSurfaceCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "ESTADO ATUAL",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    color = ElegantDarkPrimary
                )
                Text(
                    text = when (uiState.status) {
                        is AnalysisStatus.Success -> stringResource(id = R.string.storage_status_analyzed)
                        is AnalysisStatus.Saved -> stringResource(id = R.string.last_analysis_header)
                        is AnalysisStatus.Analyzing -> stringResource(id = R.string.storage_status_analyzing)
                        is AnalysisStatus.PermissionDenied -> stringResource(id = R.string.permission_denied_title)
                        is AnalysisStatus.PersistenceError -> stringResource(id = R.string.error_persistence)
                        is AnalysisStatus.Error -> "Erro na Análise"
                        else -> stringResource(id = R.string.storage_status_not_analyzed)
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = ElegantDarkTextSecondary
                )
            }

            // Progress bar
            if (uiState.isAnalyzing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = ElegantDarkPrimary,
                    trackColor = ElegantDarkTrack
                )
            } else {
                LinearProgressIndicator(
                    progress = { uiState.usedPercentage ?: 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    color = ElegantDarkPrimary,
                    trackColor = ElegantDarkTrack
                )
            }

            // Real Storage Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StorageMetricColumn(
                    label = stringResource(id = R.string.storage_used_label),
                    value = uiState.deviceUsedSpaceFormatted ?: "-- GB",
                    modifier = Modifier.weight(1f)
                )

                StorageMetricColumn(
                    label = stringResource(id = R.string.storage_free_label),
                    value = uiState.deviceFreeSpaceFormatted ?: "-- GB",
                    modifier = Modifier.weight(1f),
                    alignment = Alignment.End
                )
            }

            if (uiState.deviceTotalSpaceFormatted != null) {
                Text(
                    text = "Capacidade total do disco: ${uiState.deviceTotalSpaceFormatted}",
                    fontSize = 12.sp,
                    color = ElegantDarkTextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AnalysisResultsSection(
    uiState: HomeUiState,
    isSaved: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = if (isSaved) stringResource(id = R.string.last_analysis_header) else stringResource(id = R.string.analysis_summary_header),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ElegantDarkTextPrimary
                )
                if (uiState.lastAnalyzedDateFormatted != null) {
                    Text(
                        text = stringResource(id = R.string.last_analysis_date_format, uiState.lastAnalyzedDateFormatted),
                        fontSize = 12.sp,
                        color = ElegantDarkTextMuted
                    )
                }
            }

            Text(
                text = if (uiState.totalAnalyzedSpaceFormatted != null) {
                    "${uiState.totalFilesAnalyzed} arquivos (${uiState.totalAnalyzedSpaceFormatted})"
                } else {
                    stringResource(
                        id = R.string.analysis_total_files_format,
                        uiState.totalFilesAnalyzed
                    )
                },
                fontSize = 13.sp,
                color = ElegantDarkPrimary,
                fontWeight = FontWeight.Medium
            )
        }

        if (uiState.categories.isEmpty() || uiState.totalFilesAnalyzed == 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceCard)
            ) {
                Text(
                    text = stringResource(id = R.string.analysis_empty_state),
                    fontSize = 14.sp,
                    color = ElegantDarkTextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(20.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                uiState.categories.forEach { categoryUi ->
                    CategoryItemCard(categoryUi = categoryUi)
                }
            }
        }
    }
}

@Composable
private fun CategoryItemCard(
    categoryUi: CategorySummaryUi,
    modifier: Modifier = Modifier
) {
    val icon = getCategoryIcon(categoryUi.category)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = ElegantDarkSurfaceCard
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ElegantDarkTrack.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ElegantDarkPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = stringResource(id = categoryUi.titleResId),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = ElegantDarkTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${categoryUi.fileCount} itens",
                        fontSize = 12.sp,
                        color = ElegantDarkTextMuted
                    )
                }
            }

            Text(
                text = categoryUi.formattedSize,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                color = ElegantDarkTextPrimary
            )
        }
    }
}

private fun getCategoryIcon(category: StorageCategory): ImageVector {
    return when (category) {
        StorageCategory.IMAGES -> Icons.Default.Image
        StorageCategory.VIDEOS -> Icons.Default.Movie
        StorageCategory.AUDIOS -> Icons.Default.Audiotrack
        StorageCategory.DOCUMENTS -> Icons.Default.Description
        StorageCategory.LARGE_FILES -> Icons.Default.Folder
        StorageCategory.OTHERS -> Icons.Default.Folder
    }
}

@Composable
private fun HeroOnboardingSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(ElegantDarkTrack.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = ElegantDarkTextMuted,
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = "Pronto para começar?",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = ElegantDarkTextPrimary,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Inicie uma análise para identificar arquivos reais no dispositivo e entender a distribuição do seu armazenamento.",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = ElegantDarkTextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun RequestingPermissionSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CircularProgressIndicator(
            color = ElegantDarkPrimary,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = "Aguardando confirmação da permissão...",
            fontSize = 14.sp,
            color = ElegantDarkTextSecondary
        )
    }
}

@Composable
private fun AnalyzingStateSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(
            color = ElegantDarkPrimary,
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = "Consultando catálogo de mídias locais...",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = ElegantDarkTextPrimary
        )
        Text(
            text = "Processando de forma 100% segura e on-device.",
            fontSize = 12.sp,
            color = ElegantDarkTextMuted
        )
    }
}

@Composable
private fun PermissionDeniedCard(
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = ElegantDarkSurfaceCard
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = ElegantDarkPrimary,
                modifier = Modifier.size(32.dp)
            )

            Text(
                text = stringResource(id = R.string.permission_denied_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ElegantDarkTextPrimary
            )

            Text(
                text = stringResource(id = R.string.permission_denied_desc),
                fontSize = 13.sp,
                color = ElegantDarkTextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(stringResource(id = R.string.btn_open_settings), fontSize = 13.sp)
                }

                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElegantDarkPrimary,
                        contentColor = ElegantDarkOnPrimary
                    )
                ) {
                    Text(stringResource(id = R.string.btn_grant_permission), fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ErrorStateCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = ElegantDarkSurfaceCard
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )

            Text(
                text = stringResource(id = R.string.error_reading_storage),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ElegantDarkTextPrimary
            )

            Text(
                text = message,
                fontSize = 13.sp,
                color = ElegantDarkTextMuted,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElegantDarkPrimary,
                    contentColor = ElegantDarkOnPrimary
                )
            ) {
                Text("Tentar Novamente", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PrivacyBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = ElegantDarkSurfaceCard
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = ElegantDarkPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.privacy_badge_text),
                style = MaterialTheme.typography.labelMedium,
                color = ElegantDarkTextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun StorageMetricColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(
        modifier = modifier,
        horizontalAlignment = alignment
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = ElegantDarkTextMuted
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = ElegantDarkTextPrimary
        )
    }
}

@Composable
private fun SafeProcessingInfoCard(
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color.Transparent
        ),
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
                text = stringResource(id = R.string.local_processing_desc),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = ElegantDarkTextMuted
            )
        }
    }
}

@Composable
private fun SettingsDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElegantDarkSurfaceCard,
        titleContentColor = ElegantDarkTextPrimary,
        textContentColor = ElegantDarkTextSecondary,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = stringResource(id = R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "CleanMind v1.0 (Fase 2: Acesso Real)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = ElegantDarkPrimary
                )
                Text(
                    text = "Análise local e em tempo real dos arquivos multimídia e documentos através de MediaStore e StatFs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ElegantDarkTextMuted
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_close_button")
            ) {
                Text("Fechar", color = ElegantDarkPrimary)
            }
        }
    )
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
