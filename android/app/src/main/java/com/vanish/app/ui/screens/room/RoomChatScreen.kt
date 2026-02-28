package com.vanish.app.ui.screens.room

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RoomChatScreen(
    roomCode: String,
    isPending: Boolean = false,
    onBack: () -> Unit,
    viewModel: RoomChatViewModel = viewModel(factory = RoomChatViewModelFactory(roomCode, isPending))
) {
    val messages by viewModel.messages.collectAsState()
    val members by viewModel.members.collectAsState()
    val pendingRequests by viewModel.pendingRequests.collectAsState()
    val joined by viewModel.joined.collectAsState()
    val roomClosed by viewModel.roomClosed.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }
    LaunchedEffect(roomClosed) {
        if (roomClosed) onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        TopAppBar(
            title = { Text("Room $roomCode", color = Color.White) },
            navigationIcon = {
                IconButton(onClick = { viewModel.leave(); onBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Leave", tint = Color.White)
                }
            },
            colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black,
                titleContentColor = Color.White
            )
        )
        if (joined == false && pendingRequests.isEmpty()) {
            Text(
                "Waiting for approval…",
                color = Color.Gray,
                modifier = Modifier.padding(16.dp)
            )
        }
        if (pendingRequests.isNotEmpty()) {
            Text("Join requests:", color = Color.White, modifier = Modifier.padding(8.dp))
            pendingRequests.forEach { username ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(username, color = Color.White)
                    Button(onClick = { viewModel.approveJoin(username) }) { Text("Approve") }
                }
            }
        }
        if (members.isNotEmpty()) {
            Text("Members: ${members.joinToString()}", color = Color.Gray, modifier = Modifier.padding(8.dp))
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isMe) Arrangement.End else Arrangement.Start
                ) {
                    Text(
                        text = "${msg.from}: ${msg.text}",
                        color = if (msg.isMe) Color(0xFFBB86FC) else Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .pointerInput(Unit) { detectTapGestures(onPress = {}) }
                            .padding(8.dp)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { viewModel.setInputText(it) },
                placeholder = { Text("Message", color = Color.Gray) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(onClick = { viewModel.send() }, modifier = Modifier.padding(start = 8.dp)) {
                Text("Send")
            }
        }
    }
}
