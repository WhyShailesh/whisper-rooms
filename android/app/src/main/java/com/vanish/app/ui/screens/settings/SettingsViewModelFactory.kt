package com.vanish.app.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vanish.app.data.IdentityStore
import com.vanish.app.data.firestore.FirestoreRepository

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(
            IdentityStore(context),
            FirestoreRepository()
        ) as T
    }
}
