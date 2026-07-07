package com.pockethermes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pockethermes.ui.screens.MainScreen
import com.pockethermes.ui.screens.SetupScreen
import com.pockethermes.ui.theme.PocketHermesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketHermesTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PocketHermesNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun PocketHermesNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "setup",
        modifier = modifier
    ) {
        composable("setup") {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate("main") {
                        popUpTo("setup") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            MainScreen()
        }
    }
}
