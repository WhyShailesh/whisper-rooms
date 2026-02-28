package com.vanish.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vanish.app.data.firestore.Presence
import com.vanish.app.data.firestore.UserSearchResult
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    onOpenChat: (String) -> Unit,
    onOpenRoom: (String, Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(LocalContext.current))
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResult by viewModel.searchResult.collectAsState()
    val searchLoading by viewModel.searchLoading.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    val createRoomCode by viewModel.createRoomCode.collectAsState()
    val createRoomCode by viewModel.createRoomCode.collectAsState()
    val joinRoomCodeInput by viewModel.joinRoomCodeInput.collectAsState()
    val joinError by viewModel.joinError.collectAsState()
    val navigateToRoom by viewModel.navigateToRoom.collectAsState()

    LaunchedEffect(createRoomCode) {
        createRoomCode?.let { code ->
            viewModel.setCreateRoomCode(null)
            onOpenRoom(code, false)
        }
    }
    LaunchedEffect(navigateToRoom) {
        navigateToRoom?.let { (code, isPending) ->
            viewModel.clearNavigateToRoom()
            onOpenRoom(code, isPending)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        TopAppBar(
            title = { Text("Vanish", color = Color.White) },
            actions = {
                IconButton(onClick = onOpenSettings) {
                    androidx.compose.material.icons.Icons.Default.Settings.let {
                        androidx.compose.material3.Icon(
                            imageVector = it,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
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
            Text("Search user", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Username", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = { viewModel.search() }, enabled = !searchLoading) {
                    if (searchLoading) CircularProgressIndicator(Modifier.height(24.dp).padding(4.dp), color = Color.White)
                    else Text("Search")
                }
            }
            searchError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            searchResult?.let { result ->
                UserSearchResultRow(
                    result = result,
                    onChat = { viewModel.clearSearchResult(); onOpenChat(result.username) },
                    onDismiss = { viewModel.clearSearchResult() }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Rooms", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.createRoom() }
            ) {
                Text("Create room")
            }
            OutlinedTextField(
                value = joinRoomCodeInput,
                onValueChange = { viewModel.setJoinRoomCodeInput(it) },
                placeholder = { Text("Room code", color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.joinRoom(joinRoomCodeInput) }
            ) {
                Text("Join room")
            }
            joinError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun UserSearchResultRow(
    result: UserSearchResult,
    onChat: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(result.username, color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Text(
                when (result.presence) {
                    Presence.ONLINE -> "Online"
                    Presence.OFFLINE_INVITE -> "Offline – Invite"
                },
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (result.presence == Presence.ONLINE) {
            Button(onClick = onChat) { Text("Chat") }
        } else {
            Button(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}
