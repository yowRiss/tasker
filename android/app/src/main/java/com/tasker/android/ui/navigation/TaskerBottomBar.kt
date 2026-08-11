package com.tasker.android.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tasker.android.ui.theme.TaskerTheme

@Composable
fun TaskerBottomBar(
    currentRoute: String?,
    onTabSelected: (Screen) -> Unit,
) {
    val colors = TaskerTheme.colors

    NavigationBar(
        containerColor = colors.surface,
        tonalElevation = 0.dp,          // flat, no M3 tonal lift
    ) {
        bottomTabs.forEach { tab ->
            val selected = currentRoute == tab.screen.route
            NavigationBarItem(
                selected = selected,
                onClick  = { onTabSelected(tab.screen) },
                icon = {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(24.dp),
                    )
                },
                label = {
                    AnimatedVisibility(
                        visible   = true,
                        enter     = fadeIn(),
                        exit      = fadeOut(),
                    ) {
                        Text(
                            text  = tab.label,
                            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        )
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = colors.accent,
                    selectedTextColor   = colors.accent,
                    unselectedIconColor = colors.textTertiary,
                    unselectedTextColor = colors.textTertiary,
                    indicatorColor      = colors.accentSubtle,
                ),
                alwaysShowLabel = true,
            )
        }
    }
}
