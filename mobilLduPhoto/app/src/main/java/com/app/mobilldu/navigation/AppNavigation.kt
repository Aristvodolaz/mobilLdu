package com.app.mobilldu.navigation

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.mobilldu.data.local.AppDatabase
import com.app.mobilldu.data.repository.PhotoRepositoryImpl
import com.app.mobilldu.domain.usecase.GetHistoryUseCase
import com.app.mobilldu.domain.usecase.UploadPhotoUseCase
import com.app.mobilldu.presentation.capture.CaptureScreen
import com.app.mobilldu.presentation.capture.CaptureViewModel
import com.app.mobilldu.presentation.history.HistoryIntent
import com.app.mobilldu.presentation.history.HistoryScreen
import com.app.mobilldu.presentation.history.HistoryViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Capture : Screen("capture", "Съёмка", Icons.Default.PhotoCamera)
    object History : Screen("history", "История", Icons.Default.History)
}

val bottomScreens = listOf(Screen.Capture, Screen.History)

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val navController = rememberNavController()

    // DI — manual simple injection
    val db = remember { AppDatabase.getInstance(context) }
    val dao = remember { db.photoRecordDao() }
    val repository = remember { PhotoRepositoryImpl(dao) }
    val uploadUseCase = remember { UploadPhotoUseCase(repository, dao) }
    val getHistoryUseCase = remember { GetHistoryUseCase(repository) }

    val captureViewModel: CaptureViewModel = viewModel(
        factory = CaptureViewModel.Factory(uploadUseCase, context)
    )
    val historyViewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModel.Factory(getHistoryUseCase, repository)
    )

    // Load saved server URL on first launch
    LaunchedEffect(Unit) {
        captureViewModel.loadServerUrl()
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                bottomScreens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Capture.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Capture.route) {
                val state by captureViewModel.state.collectAsState()
                CaptureScreen(
                    state = state,
                    onIntent = captureViewModel::processIntent
                )
            }
            composable(Screen.History.route) {
                val state by historyViewModel.state.collectAsState()
                HistoryScreen(
                    state = state,
                    onIntent = historyViewModel::processIntent
                )
            }
        }
    }
}
