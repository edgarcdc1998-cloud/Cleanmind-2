package com.aistudio.cleanmind.app.presentation.navigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aistudio.cleanmind.app.presentation.components.CleanMindBottomBar
import com.aistudio.cleanmind.app.presentation.home.HomeScreen
import com.aistudio.cleanmind.app.presentation.home.HomeViewModel
import com.aistudio.cleanmind.app.presentation.screens.analysis.AnalysisScreen
import com.aistudio.cleanmind.app.presentation.screens.recommendations.RecommendationsScreen
import com.aistudio.cleanmind.app.presentation.screens.settings.SettingsScreen
import com.aistudio.cleanmind.app.presentation.screens.storage.StorageScreen
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkBackground
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkPrimary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkSurfaceCard
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextMuted

@Composable
fun CleanMindMainScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.provideFactory(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: CleanMindDestination.Home.route
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

    fun navigateTo(destination: CleanMindDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val recommendationsBadgeCount = uiState.recommendationsSummary?.totalRecommendationsCount ?: 0

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(ElegantDarkBackground),
        containerColor = ElegantDarkBackground,
        bottomBar = {
            CleanMindBottomBar(
                currentRoute = currentRoute,
                onNavigateToDestination = { dest -> navigateTo(dest) },
                recommendationsBadgeCount = recommendationsBadgeCount
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = CleanMindDestination.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(CleanMindDestination.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToAnalysis = { navigateTo(CleanMindDestination.Analysis) },
                    onNavigateToRecommendations = { navigateTo(CleanMindDestination.Recommendations) },
                    onNavigateToStorage = { navigateTo(CleanMindDestination.Storage) },
                    onNavigateToSettings = { navigateTo(CleanMindDestination.Settings) }
                )
            }

            composable(CleanMindDestination.Analysis.route) {
                AnalysisScreen(
                    uiState = uiState,
                    onStartAnalysis = { handleStartAnalysis() },
                    onCancelAnalysis = { viewModel.cancelAnalysis() },
                    onNavigateToRecommendations = { navigateTo(CleanMindDestination.Recommendations) }
                )
            }

            composable(CleanMindDestination.Recommendations.route) {
                RecommendationsScreen(
                    uiState = uiState,
                    onFilterSelected = { filter -> viewModel.onFilterSelected(filter) },
                    onToggleSelection = { id -> viewModel.onToggleRecommendationSelection(id) },
                    onSelectAll = { viewModel.onSelectAllRecommendations() },
                    onClearSelection = { viewModel.onClearRecommendationSelection() },
                    onShowReviewConfirmation = { viewModel.onShowReviewConfirmation() },
                    onDismissReviewConfirmation = { viewModel.onDismissReviewConfirmation() }
                )
            }

            composable(CleanMindDestination.Storage.route) {
                StorageScreen(
                    uiState = uiState,
                    onAnalyzeStorage = { handleStartAnalysis() }
                )
            }

            composable(CleanMindDestination.Settings.route) {
                SettingsScreen(
                    uiState = uiState,
                    onToggleAutoAnalysis = { enabled -> viewModel.onToggleAutoAnalysis(enabled) },
                    onSetIntervalHours = { hours -> viewModel.onSetAutoAnalysisInterval(hours) },
                    onStartManualAnalysis = { viewModel.startStorageAnalysis() },
                    onCancelAnalysis = { viewModel.cancelAnalysis() }
                )
            }
        }
    }
}
