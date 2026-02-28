package com.vanish.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanish.app.data.IdentityStore
import com.vanish.app.data.firestore.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Settings: change username, toggle offline discoverability.
 */
class SettingsViewModel(
    private val identityStore: IdentityStore,
    private val firestore: FirestoreRepository
) : ViewModel() {

    private val _username = MutableStateFlow<String?>(null)
    val username = _username.asStateFlow()

    private val _discoverableOffline = MutableStateFlow(false)
    val discoverableOffline = _discoverableOffline.asStateFlow()

    private val _newUsernameInput = MutableStateFlow("")
    val newUsernameInput = _newUsernameInput.asStateFlow()

    private val _changeUsernameError = MutableStateFlow<String?>(null)
    val changeUsernameError = _changeUsernameError.asStateFlow()

    private val _changeUsernameLoading = MutableStateFlow(false)
    val changeUsernameLoading = _changeUsernameLoading.asStateFlow()

    private val _toggleLoading = MutableStateFlow(false)
    val toggleLoading = _toggleLoading.asStateFlow()

    init {
        viewModelScope.launch {
            _username.value = identityStore.username.first()
            val userId = identityStore.userId.first() ?: return@launch
            firestore.getUser(userId)?.let { user ->
                _discoverableOffline.value = user.discoverableOffline
            }
        }
    }

    fun setNewUsernameInput(s: String) {
        _newUsernameInput.value = s
        _changeUsernameError.value = null
    }

    fun changeUsername() {
        viewModelScope.launch {
            val old = _username.value ?: return@launch
            val new = _newUsernameInput.value.trim()
            if (new.isBlank() || new == old) {
                _changeUsernameError.value = "Enter a different username"
                return@launch
            }
            _changeUsernameLoading.value = true
            _changeUsernameError.value = null
            try {
                val userId = identityStore.getOrCreateUserId()
                firestore.changeUsername(userId, old, new)
                identityStore.setUsernameLocal(new)
                _username.value = new
                _newUsernameInput.value = ""
            } catch (e: Exception) {
                _changeUsernameError.value = e.message ?: "Failed to change username"
            } finally {
                _changeUsernameLoading.value = false
            }
        }
    }

    fun setDiscoverableOffline(enabled: Boolean) {
        viewModelScope.launch {
            val user = _username.value ?: return@launch
            val userId = identityStore.getOrCreateUserId()
            _toggleLoading.value = true
            try {
                firestore.setDiscoverableOffline(userId, user, enabled)
                _discoverableOffline.value = enabled
            } finally {
                _toggleLoading.value = false
            }
        }
    }
}
