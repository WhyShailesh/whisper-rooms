package com.vanish.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanish.app.data.IdentityStore
import com.vanish.app.data.firestore.FirestoreRepository
import com.vanish.app.data.firestore.UserSearchResult
import com.vanish.app.data.socket.SocketHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Home: search user, create room, join room.
 */
class HomeViewModel(
    private val identityStore: IdentityStore,
    private val firestore: FirestoreRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResult = MutableStateFlow<UserSearchResult?>(null)
    val searchResult = _searchResult.asStateFlow()

    private val _searchLoading = MutableStateFlow(false)
    val searchLoading = _searchLoading.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError = _searchError.asStateFlow()

    private val _createRoomCode = MutableStateFlow<String?>(null)
    val createRoomCode = _createRoomCode.asStateFlow()

    private val _joinRoomCodeInput = MutableStateFlow("")
    val joinRoomCodeInput = _joinRoomCodeInput.asStateFlow()

    private val _joinError = MutableStateFlow<String?>(null)
    val joinError = _joinError.asStateFlow()

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
        _searchResult.value = null
        _searchError.value = null
    }

    fun search() {
        viewModelScope.launch {
            val q = _searchQuery.value.trim()
            if (q.isBlank()) return@launch
            _searchLoading.value = true
            _searchError.value = null
            _searchResult.value = null
            try {
                val result = firestore.findUser(q)
                _searchResult.value = result
                if (result == null) _searchError.value = "User not found or not discoverable"
            } catch (e: Exception) {
                _searchError.value = e.message ?: "Search failed"
            } finally {
                _searchLoading.value = false
            }
        }
    }

    fun clearSearchResult() {
        _searchResult.value = null
        _searchError.value = null
    }

    private val _navigateToRoom = MutableStateFlow<Pair<String, Boolean>?>(null)
    val navigateToRoom = _navigateToRoom.asStateFlow()

    fun setCreateRoomCode(code: String?) { _createRoomCode.value = code }
    fun setJoinRoomCodeInput(s: String) { _joinRoomCodeInput.value = s; _joinError.value = null }
    fun setJoinError(e: String?) { _joinError.value = e }
    fun clearNavigateToRoom() { _navigateToRoom.value = null }

    fun createRoom() {
        SocketHolder.getSocket()?.createRoom(
            onRoomCreated = { code -> _createRoomCode.value = code },
            onError = { _searchError.value = it }
        ) ?: run { _searchError.value = "Not connected" }
    }

    fun joinRoom(code: String) {
        val c = code.trim().uppercase()
        if (c.isBlank()) { _joinError.value = "Enter room code"; return }
        SocketHolder.getSocket()?.joinRoom(
            roomCode = c,
            onJoined = { _navigateToRoom.value = Pair(c, false) },
            onPending = { _navigateToRoom.value = Pair(c, true) },
            onError = { _joinError.value = it }
        ) ?: run { _joinError.value = "Not connected" }
    }

    fun getUsername(): String? = identityStore.getUsernameSync()
}
