package com.skripsi.cnnfreshscan.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.skripsi.cnnfreshscan.presentation.screen.AboutScreen
import com.skripsi.cnnfreshscan.presentation.screen.CameraScreen
import com.skripsi.cnnfreshscan.presentation.screen.ResultScreen
import com.skripsi.cnnfreshscan.presentation.screen.SplashScreen

/**
 * Navigation graph composable that sets up all routes for the application.
 * Uses Jetpack Compose Navigation with Hilt for dependency injection.
 */
@Composable
fun MainNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = SPLASH_ROUTE
    ) {
        composable(SPLASH_ROUTE) {
            SplashScreen(
                onFinished = {
                    navController.navigate(CAMERA_ROUTE) {
                        popUpTo(SPLASH_ROUTE) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(CAMERA_ROUTE) {
            CameraScreen(navController = navController)
        }

        composable(ABOUT_ROUTE) {
            AboutScreen(navController = navController)
        }

        composable(
            route = "$RESULT_ROUTE/{$IMAGE_URI_ARG}",
            arguments = listOf(navArgument(IMAGE_URI_ARG) { type = NavType.StringType })
        ) {
            ResultScreen(navController = navController)
        }

    }
}
