package com.locus.app

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.locus.app.designsystem.component.LocusBottomNav
import com.locus.app.designsystem.component.NavItem
import com.locus.app.designsystem.theme.InkBackground
import com.locus.app.designsystem.theme.LocusMotion
import com.locus.app.designsystem.theme.LocusTheme
import com.locus.app.feature.inspire.InspireScreen
import com.locus.app.feature.inspire.InspireViewModel
import com.locus.app.feature.review.ReviewScreen
import com.locus.app.feature.review.ReviewViewModel
import com.locus.app.feature.sober.SoberScreen
import com.locus.app.feature.sober.SoberViewModel
import com.locus.app.feature.sober.SurfingExerciseScreen
import com.locus.app.feature.timelog.TimeLogScreen
import com.locus.app.feature.timelog.TimeLogViewModel

@Composable
fun MainNavigation(
    onNavControllerReady: (NavHostController) -> Unit = {},
) {
    val navController = rememberNavController()
    LaunchedEffect(navController) { onNavControllerReady(navController) }
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
                // 冲浪练习是全屏沉浸页，不显示底部导航
                if (!currentRoute.startsWith("surfing")) {
                    LocusBottomNav(
                        items = navItems,
                        // 带可选参数的路由（inspire?category=...）按基础路由匹配选中态
                        selectedRoute = currentRoute.substringBefore("?"),
                        onItemSelected = { item ->
                            navController.navigate(item.route) {
                                popUpTo("sober") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "sober",
                modifier = Modifier.padding(innerPadding),
                enterTransition = {
                    fadeIn(tween(350, easing = LocusMotion.EaseOut)) +
                        slideInVertically(tween(350, easing = LocusMotion.EaseOut)) { it / 14 }
                },
                exitTransition = {
                    fadeOut(tween(250))
                },
                popEnterTransition = {
                    fadeIn(tween(350, easing = LocusMotion.EaseOut)) +
                        slideInVertically(tween(350, easing = LocusMotion.EaseOut)) { it / 14 }
                },
                popExitTransition = {
                    fadeOut(tween(250))
                },
            ) {
                composable(
                    route = "sober",
                    deepLinks = listOf(navDeepLink { uriPattern = "locus://sober" }),
                ) {
                    SoberScreen(
                        viewModel = viewModel(factory = SoberViewModel.Factory),
                        onNavigateToSurfing = {
                            navController.navigate("surfing") { launchSingleTop = true }
                        },
                        onNavigateToInspireEmergency = {
                            navController.navigate("inspire?category=EMERGENCY") { launchSingleTop = true }
                        },
                    )
                }
                composable(
                    route = "inspire?category={category}",
                    arguments = listOf(
                        navArgument("category") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    ),
                ) { InspireScreen(viewModel = viewModel(factory = InspireViewModel.Factory)) }
                composable(
                    route = "timelog",
                    deepLinks = listOf(navDeepLink { uriPattern = "locus://timelog" }),
                ) { TimeLogScreen(viewModel = viewModel(factory = TimeLogViewModel.Factory)) }
                composable("review") { ReviewScreen(viewModel = viewModel(factory = ReviewViewModel.Factory)) }
                composable("surfing") {
                    SurfingExerciseScreen(onFinish = { navController.popBackStack() })
                }
            }
        }
    }
}
