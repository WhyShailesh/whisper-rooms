package com.vanish.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(LocalContext.current))
) {
    val username by viewModel.username.collectAsState()
    val discoverableOffline by viewModel.discoverableOffline.collectAsState()
    val newUsernameInput by viewModel.newUsernameInput.collectAsState()
    val changeUsernameError by viewModel.changeUsernameError.collectAsState()
    val changeUsernameLoading by viewModel.changeUsernameLoading.collectAsState()
    val toggleLoading by viewModel.toggleLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        TopAppBar(
            title = { Text("Settings", color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black,
                titleContentColor = Color.White
            )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Current username: $username", color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text("Change username", color = Color.White, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = newUsernameInput,
                onValueChange = { viewModel.setNewUsernameInput(it) },
                placeholder = { Text("New username", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !changeUsernameLoading
            )
            if (changeUsernameLoading) CircularProgressIndicator(color = Color.White)
            else androidx.compose.material3.Button(onClick = { viewModel.changeUsername() }) { Text("Change") }
            changeUsernameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Allow people to find me when I am offline",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (toggleLoading) CircularProgressIndicator(Modifier.padding(8.dp), color = Color.White)
                else Switch(
                    checked = discoverableOffline,
                    onCheckedChange = { viewModel.setDiscoverableOffline(it) }
                )
            }
        }
    }
}
