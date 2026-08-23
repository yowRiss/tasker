package com.tasker.android.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tasker.android.ui.auth.LoginScreen
import com.tasker.android.ui.money.MoneyScreen
import com.tasker.android.ui.notes.NoteDetailScreen
import com.tasker.android.ui.notes.NotesScreen
import com.tasker.android.ui.settings.SettingsScreen
import com.tasker.android.ui.tasks.TaskDetailScreen
import com.tasker.android.ui.tasks.TasksScreen

private const val NAV_ANIM_DURATION = 220

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = mobileStartDestination(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom bar only on tab roots
    val tabRoutes = bottomTabs.map { it.screen.route }
    val showBottomBar = currentRoute in tabRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                TaskerBottomBar(
                    currentRoute  = currentRoute,
                    onTabSelected = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = Modifier.padding(innerPadding),
            enterTransition  = {
                fadeIn(animationSpec = tween(NAV_ANIM_DURATION)) +
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        tween(NAV_ANIM_DURATION),
                    )
            },
            exitTransition   = {
                fadeOut(animationSpec = tween(NAV_ANIM_DURATION))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(NAV_ANIM_DURATION))
            },
            popExitTransition  = {
                fadeOut(animationSpec = tween(NAV_ANIM_DURATION)) +
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        tween(NAV_ANIM_DURATION),
                    )
            },
        ) {
            // ── Auth ─────────────────────────────────────────────
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Tasks.route) {
                            popUpTo(navController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    },
                )
            }

            // ── Tasks ─────────────────────────────────────────────
            composable(Screen.Tasks.route) {
                TasksScreen(
                    onTaskClick   = { taskId -> navController.navigate(Screen.TaskDetail.route(taskId)) },
                    onCreateClick = { navController.navigate(Screen.CreateTask.route) },
                )
            }
            composable(Screen.TaskDetail.route) {
                TaskDetailScreen(
                    taskId  = it.arguments?.getString("taskId").orEmpty(),
                    onBack  = { navController.popBackStack() },
                )
            }
            composable(Screen.CreateTask.route) {
                TaskDetailScreen(
                    taskId  = null,
                    onBack  = { navController.popBackStack() },
                )
            }

            // ── Notes ─────────────────────────────────────────────
            composable(Screen.Notes.route) {
                NotesScreen(
                    onNoteClick   = { noteId -> navController.navigate(Screen.NoteDetail.route(noteId)) },
                    onCreateClick = { navController.navigate(Screen.CreateNote.route) },
                )
            }
            composable(Screen.NoteDetail.route) {
                NoteDetailScreen(
                    noteId = it.arguments?.getString("noteId").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.CreateNote.route) {
                NoteDetailScreen(
                    noteId = null,
                    onBack = { navController.popBackStack() },
                )
            }

            // ── Calendar ──────────────────────────────────────────
            composable(Screen.Calendar.route) {
                com.tasker.android.ui.calendar.CalendarScreen(
                    onNoteClick = { noteId -> navController.navigate(Screen.NoteDetail.route(noteId)) },
                    onTaskClick = { taskId -> navController.navigate(Screen.TaskDetail.route(taskId)) },
                )
            }

            // ── Money ─────────────────────────────────────────────
            composable(Screen.Money.route) {
                MoneyScreen(
                    onNavigate = { route -> navController.navigate(route) },
                )
            }
            composable(Screen.Budgets.route) {
                com.tasker.android.ui.money.BudgetsScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.Recurring.route) {
                com.tasker.android.ui.money.RecurringScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.CreateTransaction.route) {
                com.tasker.android.ui.money.TransactionDetailScreen(
                    txId   = null,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.TransactionDetail.route) {
                com.tasker.android.ui.money.TransactionDetailScreen(
                    txId   = it.arguments?.getString("txId").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }

            // ── Settings ──────────────────────────────────────────
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onConnectAccount = { navController.navigate(Screen.Login.route) },
                )
            }
        }
    }
}
