package com.locus.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavHostController

class MainActivity : ComponentActivity() {

    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainNavigation(onNavControllerReady = { navController = it })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 小组件 deep link：singleTop 下把新 Intent 交给 NavController 路由
        navController?.handleDeepLink(intent)
    }
}
