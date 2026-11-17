package com.songkeyuan.schedule.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.songkeyuan.schedule.data.ScheduleRepository
import com.songkeyuan.schedule.ui.editor.CourseEditorScreen
import com.songkeyuan.schedule.ui.editor.CourseEditorViewModel
import com.songkeyuan.schedule.ui.pending.PendingScreen
import com.songkeyuan.schedule.ui.pending.PendingViewModel
import com.songkeyuan.schedule.ui.schedule.ScheduleScreen
import com.songkeyuan.schedule.ui.schedule.ScheduleViewModel
import com.songkeyuan.schedule.ui.settings.SettingsScreen
import com.songkeyuan.schedule.ui.settings.SettingsViewModel
import com.songkeyuan.schedule.ui.importing.ImportScreen
import com.songkeyuan.schedule.ui.importing.ImportViewModel

@Composable
fun ScheduleApp(repository: ScheduleRepository) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val isEditor = route?.startsWith("editor") == true || route == "import"

    Scaffold(
        bottomBar = {
            if (!isEditor) {
                NavigationBar {
                    AppDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = route == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(AppDestination.Schedule.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(destination.symbol) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Schedule.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(AppDestination.Schedule.route) {
                val viewModel: ScheduleViewModel = viewModel(factory = ViewModelFactory(repository))
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                ScheduleScreen(
                    state = state,
                    onPreviousWeek = viewModel::previousWeek,
                    onNextWeek = viewModel::nextWeek,
                    onSelectDay = viewModel::selectWeekday,
                    onCourseClick = { navController.navigate("editor/$it") },
                    onAddCourse = { navController.navigate("editor/0") },
                    onRetry = viewModel::retry,
                    onViewModeChange = viewModel::selectViewMode,
                    onImport = { navController.navigate("import") },
                    onToday = viewModel::returnToToday,
                )
            }
            composable(AppDestination.Pending.route) {
                val viewModel: PendingViewModel = viewModel(factory = ViewModelFactory(repository))
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                PendingScreen(state = state, onCourseClick = { navController.navigate("editor/$it") })
            }
            composable(AppDestination.Settings.route) {
                val viewModel: SettingsViewModel = viewModel(factory = ViewModelFactory(repository))
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                SettingsScreen(
                    state = state,
                    onDateChange = viewModel::setFirstWeekMonday,
                    onWeeksChange = viewModel::setTotalWeeks,
                    onSave = viewModel::save,
                    onRequestReset = viewModel::requestReset,
                    onDismissReset = viewModel::dismissReset,
                    onConfirmReset = viewModel::confirmReset,
                    onImport = { navController.navigate("import") },
                    onRequestRestoreBackup = viewModel::requestRestoreBackup,
                    onDismissRestoreBackup = viewModel::dismissRestoreBackup,
                    onConfirmRestoreBackup = viewModel::confirmRestoreBackup,
                )
            }
            composable("import") {
                val model: ImportViewModel=viewModel(factory=ViewModelFactory(repository))
                val state by model.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(state.finished) { if(state.finished) navController.popBackStack() }
                ImportScreen(state,model::readFile,model::confirmImport,onBack={navController.popBackStack()})
            }
            composable(
                route = "editor/{courseId}",
                arguments = listOf(navArgument("courseId") { type = NavType.LongType }),
            ) { entry ->
                val rawId = entry.arguments?.getLong("courseId") ?: 0L
                val courseId = rawId.takeIf { it != 0L }
                val viewModel: CourseEditorViewModel = viewModel(
                    key = "editor-$rawId",
                    factory = ViewModelFactory(repository, courseId = courseId),
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                LaunchedEffect(state.finished) {
                    if (state.finished) navController.popBackStack()
                }
                CourseEditorScreen(
                    state = state,
                    onBack = { navController.popBackStack() },
                    onNameChange = viewModel::setName,
                    onCodeChange = viewModel::setCode,
                    onTeachersChange = viewModel::setTeachers,
                    onNoteChange = viewModel::setNote,
                    onAddMeeting = { viewModel.addMeeting() },
                    onUpdateMeeting = viewModel::updateMeeting,
                    onRemoveMeeting = viewModel::removeMeeting,
                    onSave = viewModel::save,
                    onRequestDelete = viewModel::requestDelete,
                    onDismissDelete = viewModel::dismissDelete,
                    onConfirmDelete = viewModel::confirmDelete,
                )
            }
        }
    }
}
