package com.vanish.app.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanish.app.data.socket.SocketHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * One-to-one chat. Messages exist only in UI state during session; not persisted.
 * If receiver goes offline, messages are dropped by server.
 */
class ChatViewModel(
    private val peerUsername: String
) : ViewModel() {

    private val _messages = MutableStateFlow<List<DisplayMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText = _inputText.asStateFlow()

    init {
        viewModelScope.launch {
            SocketHolder.getSocket()?.directMessageFlow()
                ?.onEach { dm ->
                    if (dm.from == peerUsername) {
                        _messages.value = _messages.value + DisplayMessage(from = dm.from, text = dm.text, isMe = false)
                    }
                }
                ?.catch { }
                ?.launchIn(viewModelScope)
        }
    }

    fun setInputText(s: String) { _inputText.value = s }

    fun send() {
        val text = _inputText.value.trim()
        if (text.isBlank()) return
        SocketHolder.getSocket()?.sendDirectMessage(peerUsername, text)
        _messages.value = _messages.value + DisplayMessage(from = "me", text = text, isMe = true)
        _inputText.value = ""
    }
}

data class DisplayMessage(val from: String, val text: String, val isMe: Boolean)
