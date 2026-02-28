package com.vanish.app.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vanish.app.data.IdentityStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Splash: ensure UUID exists; if no username is set, go to setup; else go to home.
 */
class SplashViewModel(
    private val identityStore: IdentityStore
) : ViewModel() {

    private val _navigateToSetup = MutableStateFlow(false)
    val navigateToSetup = _navigateToSetup.asStateFlow()

    private val _navigateToHome = MutableStateFlow(false)
    val navigateToHome = _navigateToHome.asStateFlow()

    init {
        viewModelScope.launch {
            identityStore.getOrCreateUserId()
            val username = identityStore.username.first()
            if (username.isNullOrBlank()) {
                _navigateToSetup.value = true
            } else {
                _navigateToHome.value = true
            }
        }
    }

    fun consumedNavigateToSetup() { _navigateToSetup.value = false }
    fun consumedNavigateToHome() { _navigateToHome.value = false }
}
