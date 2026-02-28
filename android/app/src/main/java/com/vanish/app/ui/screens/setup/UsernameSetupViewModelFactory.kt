package com.vanish.app.ui.screens.setup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vanish.app.data.IdentityStore
import com.vanish.app.data.firestore.FirestoreRepository

class UsernameSetupViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return UsernameSetupViewModel(
            IdentityStore(context),
            FirestoreRepository()
        ) as T
    }
}
