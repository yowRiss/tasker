package com.tasker.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

// ─────────────────────────────────────────────────────────────────
//  Route definitions — top-level destinations and nested routes
// ─────────────────────────────────────────────────────────────────

/** Sealed hierarchy of every navigable destination in the app. */
sealed class Screen(val route: String) {

    // ── Auth ────────────────────────────────────────────────────
    data object Login : Screen("login")

    // ── Bottom tab roots ────────────────────────────────────────
    data object Tasks  : Screen("tasks")
    data object Notes  : Screen("notes")
    data object Money  : Screen("money")
    data object Settings : Screen("settings")

    // ── Tasks nested ────────────────────────────────────────────
    data object TaskDetail : Screen("tasks/{taskId}") {
        fun route(taskId: String) = "tasks/$taskId"
    }
    data object CreateTask : Screen("tasks/create")

    // ── Notes nested ────────────────────────────────────────────
    data object NoteDetail : Screen("notes/{noteId}") {
        fun route(noteId: String) = "notes/$noteId"
    }
    data object CreateNote : Screen("notes/create")

    // ── Money nested ────────────────────────────────────────────
    data object AccountDetail : Screen("money/accounts/{accountId}") {
        fun route(accountId: String) = "money/accounts/$accountId"
    }
    data object TransactionDetail : Screen("money/transactions/{txId}") {
        fun route(txId: String) = "money/transactions/$txId"
    }
    data object CreateTransaction : Screen("money/transactions/create")
    data object Budgets : Screen("money/budgets")
    data object Recurring : Screen("money/recurring")
}

/** Bottom navigation tab descriptor. */
data class BottomTab(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

/** Ordered list of bottom tabs. */
val bottomTabs = listOf(
    BottomTab(
        screen        = Screen.Tasks,
        label         = "Tasks",
        selectedIcon  = Icons.Rounded.CheckBox,
        unselectedIcon = Icons.Outlined.CheckBox,
    ),
    BottomTab(
        screen        = Screen.Notes,
        label         = "Notes",
        selectedIcon  = Icons.Rounded.Notes,
        unselectedIcon = Icons.Outlined.Notes,
    ),
    BottomTab(
        screen        = Screen.Money,
        label         = "Money",
        selectedIcon  = Icons.Rounded.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet,
    ),
    BottomTab(
        screen        = Screen.Settings,
        label         = "Settings",
        selectedIcon  = Icons.Rounded.Settings,
        unselectedIcon = Icons.Outlined.Settings,
    ),
)
