package com.aistudio.cleanmind.app.presentation.screens.cleanup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.cleanmind.app.R
import com.aistudio.cleanmind.app.domain.model.CleanupRecommendation
import com.aistudio.cleanmind.app.presentation.components.CleanMindTopAppBar
import com.aistudio.cleanmind.app.presentation.components.ReviewConfirmationDialog
import com.aistudio.cleanmind.app.presentation.home.HomeUiState
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkBackground
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkPrimary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkSurfaceCard
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextMuted
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextPrimary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextSecondary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTrack
import com.aistudio.cleanmind.app.util.StorageFormatter

@Composable
fun CleanupCenterScreen(
    uiState: HomeUiState,
    onNavigateBack: () -> Unit,
    onRemoveFromSelection: (Long) -> Unit,
    onExecuteCleanup: () -> Unit,
    onClearSuccessState: () -> Unit,
    modifier: Modifier = Modifier,
    onShowReviewConfirmation: () -> Unit = {},
    onDismissReviewConfirmation: () -> Unit = {},
    onAuthorizationResult: (Boolean) -> Unit = {}
) {
    val selectedItems = uiState.selectedRecommendations
    val isDeleting = uiState.isDeleting
    val deletionSummary = uiState.deletionSummary

    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        onAuthorizationResult(result.resultCode == android.app.Activity.RESULT_OK)
    }

    LaunchedEffect(uiState.pendingIntentSender) {
        uiState.pendingIntentSender?.let { sender ->
            intentSenderLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }

    if (uiState.showReviewConfirmationDialog) {
        ReviewConfirmationDialog(
            selectedRecommendations = selectedItems,
            totalReclaimableFormatted = uiState.selectedReclaimableSpaceFormatted,
            onConfirm = {
                onDismissReviewConfirmation()
                onExecuteCleanup()
            },
            onDismiss = onDismissReviewConfirmation
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(ElegantDarkBackground),
        containerColor = ElegantDarkBackground,
        topBar = {
            CleanMindTopAppBar(
                title = stringResource(id = R.string.cleanup_center_header),
                navigationIcon = {
                    if (deletionSummary == null && !isDeleting) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("cleanup_center_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Voltar",
                                tint = ElegantDarkTextPrimary
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedContent(
                    targetState = when {
                        deletionSummary != null -> CleanupState.SUCCESS
                        isDeleting -> CleanupState.DELETING
                        selectedItems.isEmpty() -> CleanupState.EMPTY
                        else -> CleanupState.REVIEW
                    },
                    transitionSpec = {
                        fadeIn(animationSpec = tween(durationMillis = 220)) togetherWith
                                fadeOut(animationSpec = tween(durationMillis = 180))
                    },
                    label = "cleanupStateTransition"
                ) { state ->
                    when (state) {
                        CleanupState.SUCCESS -> {
                            SuccessView(
                                summary = deletionSummary!!,
                                onFinish = {
                                    onClearSuccessState()
                                    onNavigateBack()
                                }
                            )
                        }
                        CleanupState.DELETING -> {
                            DeletingProgressView()
                        }
                        CleanupState.EMPTY -> {
                            EmptyQueueView(onGoBack = onNavigateBack)
                        }
                        CleanupState.REVIEW -> {
                            ActiveReviewView(
                                selectedItems = selectedItems,
                                reclaimableBytes = uiState.selectedReclaimableBytes,
                                reclaimableSpaceFormatted = uiState.selectedReclaimableSpaceFormatted,
                                onRemoveItem = onRemoveFromSelection,
                                onConfirmCleanup = onShowReviewConfirmation
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveReviewView(
    selectedItems: List<CleanupRecommendation>,
    reclaimableBytes: Long,
    reclaimableSpaceFormatted: String,
    onRemoveItem: (Long) -> Unit,
    onConfirmCleanup: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceCard)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.cleanup_center_space_before_after),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElegantDarkPrimary,
                        letterSpacing = 0.5.sp
                    )

                    Text(
                        text = stringResource(id = R.string.cleanup_center_space_gain, reclaimableSpaceFormatted),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElegantDarkTextPrimary
                    )

                    LinearProgressIndicator(
                        progress = { 1.0f },
                        color = ElegantDarkPrimary,
                        trackColor = ElegantDarkTrack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = ElegantDarkPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.cleanup_center_warning),
                            fontSize = 11.sp,
                            color = ElegantDarkTextMuted,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "ITENS SELECIONADOS PARA REVISÃO (${selectedItems.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ElegantDarkTextSecondary,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        items(selectedItems, key = { it.id }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.file.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElegantDarkTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.reason,
                            fontSize = 11.sp,
                            color = ElegantDarkTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = StorageFormatter.formatBytes(item.reclaimableSizeBytes),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ElegantDarkPrimary,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    IconButton(
                        onClick = { onRemoveItem(item.id) },
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("remove_item_btn_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RemoveCircleOutline,
                            contentDescription = "Remover da exclusão",
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onConfirmCleanup,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("execute_cleanup_btn"),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElegantDarkPrimary,
                    contentColor = Color.Black
                )
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.cleanup_center_confirm_btn, reclaimableSpaceFormatted),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun DeletingProgressView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        CircularProgressIndicator(
            color = ElegantDarkPrimary,
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = "Excluindo arquivos com segurança...",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = ElegantDarkTextPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Liberando espaço em disco local",
            fontSize = 12.sp,
            color = ElegantDarkTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SuccessView(
    summary: com.aistudio.cleanmind.app.presentation.home.DeletionSummaryUi,
    onFinish: () -> Unit
) {
    val isFullSuccess = summary.deletedCount > 0 && summary.failedFileNames.isEmpty()
    val isPartialSuccess = summary.deletedCount > 0 && summary.failedFileNames.isNotEmpty()
    val isFailure = summary.deletedCount == 0 && summary.failedFileNames.isNotEmpty()

    val headerIcon = when {
        isFailure -> Icons.Default.Info
        isPartialSuccess -> Icons.Default.Info
        else -> Icons.Default.CheckCircle
    }

    val iconColor = when {
        isFailure -> Color(0xFFEF5350)
        isPartialSuccess -> Color(0xFFFFB74D)
        else -> ElegantDarkPrimary
    }

    val title = when {
        isFailure -> stringResource(id = R.string.cleanup_center_failed_title)
        isPartialSuccess -> stringResource(id = R.string.cleanup_center_partial_title)
        else -> stringResource(id = R.string.cleanup_center_success_title)
    }

    val description = when {
        isFailure -> stringResource(id = R.string.cleanup_center_failed_desc)
        isPartialSuccess -> stringResource(
            id = R.string.cleanup_center_partial_desc,
            summary.deletedCount,
            summary.reclaimedSpaceFormatted,
            summary.failedFileNames.size
        )
        else -> stringResource(id = R.string.cleanup_center_success_desc, summary.reclaimedSpaceFormatted)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = headerIcon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElegantDarkTextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = ElegantDarkTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }

        if (summary.deletedCount > 0) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ElegantDarkSurfaceCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = ElegantDarkPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.cleanup_center_deleted_count, summary.deletedCount),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ElegantDarkTextPrimary
                            )
                        }

                        Text(
                            text = summary.reclaimedSpaceFormatted,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ElegantDarkPrimary
                        )
                    }
                }
            }
        }

        if (summary.failedFileNames.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ElegantDarkSurfaceCard)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.cleanup_center_failed_files_header),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF5350)
                        )
                        summary.failedFileNames.forEach { name ->
                            Text(
                                text = "• $name",
                                fontSize = 12.sp,
                                color = ElegantDarkTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("cleanup_success_done_btn"),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElegantDarkPrimary,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = stringResource(id = R.string.cleanup_center_success_btn),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun EmptyQueueView(onGoBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(ElegantDarkTrack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = ElegantDarkTextMuted,
                modifier = Modifier.size(30.dp)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(id = R.string.cleanup_center_empty_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ElegantDarkTextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.cleanup_center_empty_desc),
                fontSize = 13.sp,
                color = ElegantDarkTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onGoBack,
            modifier = Modifier
                .widthIn(min = 200.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ElegantDarkTrack,
                contentColor = ElegantDarkTextPrimary
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(text = "Voltar", fontWeight = FontWeight.Bold)
        }
    }
}

private enum class CleanupState {
    REVIEW, DELETING, SUCCESS, EMPTY
}
