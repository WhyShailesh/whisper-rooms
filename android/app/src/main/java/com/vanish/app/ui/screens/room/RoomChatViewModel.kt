package com.vanish.app.ui.screens.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanish.app.data.socket.RoomMessage
import com.vanish.app.data.socket.RoomMembersUpdate
import com.vanish.app.data.socket.SocketHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Room chat. Messages and members exist only in UI state; not persisted.
 * Admin approves join requests; room is deleted when admin leaves.
 */
class RoomChatViewModel(
    private val roomCode: String,
    private val isPending: Boolean
) : ViewModel() {

    private val _messages = MutableStateFlow<List<RoomDisplayMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _members = MutableStateFlow<List<String>>(emptyList())
    val members = _members.asStateFlow()

    private val _pendingRequests = MutableStateFlow<List<String>>(emptyList())
    val pendingRequests = _pendingRequests.asStateFlow()

    private val _joined = MutableStateFlow<Boolean?>(if (isPending) false else true)
    val joined = _joined.asStateFlow()

    private val _roomClosed = MutableStateFlow(false)
    val roomClosed = _roomClosed.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText = _inputText.asStateFlow()

    init {
        viewModelScope.launch {
            SocketHolder.getSocket()?.roomMessageFlow()
                ?.onEach { msg ->
                    if (msg.roomCode == roomCode) {
                        _messages.value = _messages.value + RoomDisplayMessage(from = msg.from, text = msg.text, isMe = false)
                    }
                }
                ?.catch { }
                ?.launchIn(viewModelScope)
        }
        viewModelScope.launch {
            SocketHolder.getSocket()?.roomMembersFlow()
                ?.onEach { update ->
                    if (update.roomCode == roomCode) _members.value = update.members
                }
                ?.catch { }
                ?.launchIn(viewModelScope)
        }
        viewModelScope.launch {
            SocketHolder.getSocket()?.joinRequestFlow()
                ?.onEach { req ->
                    if (req.roomCode == roomCode) {
                        _pendingRequests.value = _pendingRequests.value + req.username
                    }
                }
                ?.catch { }
                ?.launchIn(viewModelScope)
        }
        viewModelScope.launch {
            SocketHolder.getSocket()?.joinRoomAckFlow()
                ?.onEach { code ->
                    if (code == roomCode) _joined.value = true
                }
                ?.catch { }
                ?.launchIn(viewModelScope)
        }
        viewModelScope.launch {
            SocketHolder.getSocket()?.roomClosedFlow()
                ?.onEach { code ->
                    if (code == roomCode) _roomClosed.value = true
                }
                ?.catch { }
                ?.launchIn(viewModelScope)
        }
    }

    fun setInputText(s: String) { _inputText.value = s }

    fun send() {
        val text = _inputText.value.trim()
        if (text.isBlank()) return
        SocketHolder.getSocket()?.sendRoomMessage(roomCode, text)
        val me = "me"
        _messages.value = _messages.value + RoomDisplayMessage(from = me, text = text, isMe = true)
        _inputText.value = ""
    }

    fun approveJoin(username: String) {
        SocketHolder.getSocket()?.approveJoin(roomCode, username)
        _pendingRequests.value = _pendingRequests.value - username
    }

    fun leave() {
        SocketHolder.getSocket()?.leaveRoom(roomCode)
    }

    fun markJoined() { _joined.value = true }
}

data class RoomDisplayMessage(val from: String, val text: String, val isMe: Boolean)
