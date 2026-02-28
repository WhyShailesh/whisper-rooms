package com.vanish.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Stores the device's permanent identity using EncryptedSharedPreferences.
 * - userId: UUID generated on first launch, never changes on this device.
 * - username: Set once during setup (or changed in Settings), stored locally for quick access.
 */
class IdentityStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _userId: MutableStateFlow<String?> = MutableStateFlow(prefs.getString(KEY_USER_ID, null))
    val userId: Flow<String?> = _userId.asStateFlow()

    private val _username: MutableStateFlow<String?> = MutableStateFlow(prefs.getString(KEY_USERNAME, null))
    val username: Flow<String?> = _username.asStateFlow()

    /**
     * Gets or creates the permanent UUID for this device.
     * Call once at app start; if none exists, generate and persist.
     */
    fun getOrCreateUserId(): String {
        val existing = prefs.getString(KEY_USER_ID, null)
        if (existing != null) {
            _userId.value = existing
            return existing
        }
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_USER_ID, newId).apply()
        _userId.value = newId
        return newId
    }

    fun getUserIdSync(): String? = prefs.getString(KEY_USER_ID, null)

    /**
     * Save username locally after successful Firestore registration/update.
     */
    fun setUsernameLocal(username: String) {
        prefs.edit().putString(KEY_USERNAME, username).apply()
        _username.value = username
    }

    fun getUsernameSync(): String? = prefs.getString(KEY_USERNAME, null)

    fun clearUsernameLocal() {
        prefs.edit().remove(KEY_USERNAME).apply()
        _username.value = null
    }

    companion object {
        private const val PREFS_NAME = "vanish_identity"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
    }
}
