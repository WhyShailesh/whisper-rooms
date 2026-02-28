package com.vanish.app.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SplashScreen(
    onNavigateToSetup: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: SplashViewModel = viewModel(factory = SplashViewModelFactory(LocalContext.current))
) {
    val toSetup by viewModel.navigateToSetup.collectAsState()
    val toHome by viewModel.navigateToHome.collectAsState()

    LaunchedEffect(toSetup) {
        if (toSetup) {
            viewModel.consumedNavigateToSetup()
            onNavigateToSetup()
        }
    }
    LaunchedEffect(toHome) {
        if (toHome) {
            viewModel.consumedNavigateToHome()
            onNavigateToHome()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Vanish",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )
    }
}
