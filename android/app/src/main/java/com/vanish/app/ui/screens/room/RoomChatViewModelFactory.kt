package com.vanish.app.ui.screens.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class RoomChatViewModelFactory(private val roomCode: String, private val isPending: Boolean = false) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RoomChatViewModel(roomCode, isPending) as T
    }
}
