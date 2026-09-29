package com.locus.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.locus.app.designsystem.component.LocusBottomNav
import com.locus.app.designsystem.component.NavItem
import com.locus.app.designsystem.theme.InkBackground
import com.locus.app.designsystem.theme.LocusTheme
import com.locus.app.designsystem.theme.LocusTypography
import com.locus.app.designsystem.theme.Stone
import com.locus.app.feature.sober.SoberScreen

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val navItems = listOf(
        NavItem("sober", "守护", Icons.Filled.Shield),
        NavItem("inspire", "灵感", Icons.Filled.Star),
        NavItem("timelog", "记录", Icons.Filled.AccessTime),
        NavItem("review", "复盘", Icons.Filled.BarChart),
    )

    LocusTheme {
        Scaffold(
            containerColor = InkBackground,
            bottomBar = {
                val currentRoute = navController.currentBackStackEntryAsState()
                    .value?.destination?.route ?: "sober"
                LocusBottomNav(
                    items = navItems,
                    selectedRoute = currentRoute,
                    onItemSelected = { item ->
                        navController.navigate(item.route) {
                            popUpTo("sober") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "sober",
                modifier = Modifier.padding(innerPadding),
            ) {
                composable("sober") { SoberScreen() }
                composable("inspire") { PlaceholderScreen("灵感") }
                composable("timelog") { PlaceholderScreen("记录") }
                composable("review") { PlaceholderScreen("复盘 · 敬请期待") }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(
        modifier = Modifier.fillMaxSize().background(InkBackground),
        contentAlignment = Alignment.Center,
    ) {
        Text(name, style = LocusTypography.displaySmall, color = Stone)
    }
}
