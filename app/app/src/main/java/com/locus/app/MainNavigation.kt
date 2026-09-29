package com.locus.app

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
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
import androidx.navigation.NavGraph.Companion.findStartDestination
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
                            // 已在该 Tab 时不动作（避免无谓的导航状态扰动）
                            if (currentRoute.substringBefore("?") == item.route) return@LocusBottomNav
                            navController.navigate(item.route) {
                                // 确定性单栈：弹回起始页重压目标 Tab。
                                // 不用 saveState/restoreState——该模式与裸 navigate
                                // （SOS 跳灵感页）混用会错位存档槽位，导致 Tab 切换失灵
                                popUpTo(navController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        },
                    )
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "sober",
                // 顶部不预留：内容（极光背景）延伸到状态栏后面，全屏沉浸；
                // 文字避让由各屏幕自行 statusBarsPadding。底部照旧避开导航栏。
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
                enterTransition = {
                    // 对称交叉淡入淡出：旧页面全程垫底，不存在透明空窗期（顶部闪黑的根因）
                    fadeIn(tween(300, easing = LocusMotion.EaseOut))
                },
                exitTransition = {
                    fadeOut(tween(300, easing = LocusMotion.EaseOut))
                },
                popEnterTransition = {
                    fadeIn(tween(300, easing = LocusMotion.EaseOut))
                },
                popExitTransition = {
                    fadeOut(tween(300, easing = LocusMotion.EaseOut))
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
                            // 与 Tab 导航同一模式：弹回起始页重压，保持单栈不变式
                            navController.navigate("inspire?category=EMERGENCY") {
                                popUpTo(navController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
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
