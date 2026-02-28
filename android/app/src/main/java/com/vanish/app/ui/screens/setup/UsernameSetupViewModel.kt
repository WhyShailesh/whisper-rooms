package com.vanish.app.ui.screens.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanish.app.data.IdentityStore
import com.vanish.app.data.firestore.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Username setup: register globally unique username for this device's UUID.
 */
class UsernameSetupViewModel(
    private val identityStore: IdentityStore,
    private val firestore: FirestoreRepository
) : ViewModel() {

    private val _usernameInput = MutableStateFlow("")
    val usernameInput = _usernameInput.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()

    private val _success = MutableStateFlow(false)
    val success = _success.asStateFlow()

    fun setUsernameInput(value: String) {
        _usernameInput.value = value
        _error.value = null
    }

    fun submit() {
        viewModelScope.launch {
            val raw = _usernameInput.value.trim()
            if (raw.isBlank()) {
                _error.value = "Enter a username"
                return@launch
            }
            val normalized = raw.lowercase()
            if (normalized.length < 2) {
                _error.value = "Username must be at least 2 characters"
                return@launch
            }
            _loading.value = true
            _error.value = null
            try {
                val userId = identityStore.getOrCreateUserId()
                firestore.registerUsername(userId, normalized)
                identityStore.setUsernameLocal(normalized)
                _success.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Registration failed"
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() { _error.value = null }
}
