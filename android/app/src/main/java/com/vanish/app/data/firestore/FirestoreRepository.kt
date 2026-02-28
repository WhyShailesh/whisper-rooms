package com.vanish.app.data.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
/**
 * Firestore access for users, usernames, online_users, discoverable_users.
 * No messages are stored in Firestore.
 */
class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    private val usersCol = db.collection(COLL_USERS)
    private val usernamesCol = db.collection(COLL_USERNAMES)
    private val onlineUsersCol = db.collection(COLL_ONLINE_USERS)
    private val discoverableUsersCol = db.collection(COLL_DISCOVERABLE_USERS)

    /**
     * Register a new username for the given userId.
     * Uses Firestore transaction: create users/{userId}, usernames/{username}.
     * Fails if username document already exists (uniqueness).
     */
    suspend fun registerUsername(userId: String, username: String) {
        val normalized = username.trim().lowercase()
        require(normalized.isNotBlank()) { "Username cannot be blank" }
        db.runTransaction { tx ->
            val usernameRef = usernamesCol.document(normalized)
            val usernameDoc = tx.get(usernameRef)
            if (usernameDoc.exists()) throw IllegalStateException("Username already taken")
            val userRef = usersCol.document(userId)
            val now = com.google.firebase.Timestamp.now()
            tx.set(userRef, mapOf(
                FIELD_USERNAME to normalized,
                FIELD_DISCOVERABLE_OFFLINE to false,
                FIELD_CREATED_AT to now,
                FIELD_UPDATED_AT to now
            ))
            tx.set(usernameRef, mapOf(FIELD_USER_ID to userId))
        }.await()
    }

    /**
     * Change username from old to new. Only owner (userId) can do this.
     * Transaction: delete usernames/{old}, create usernames/{new}, update users/{userId}.
     */
    suspend fun changeUsername(userId: String, oldUsername: String, newUsername: String) {
        val newNormalized = newUsername.trim().lowercase()
        require(newNormalized.isNotBlank()) { "Username cannot be blank" }
        require(oldUsername.trim().lowercase() != newNormalized) { "New username must differ" }
        db.runTransaction { tx ->
            val newRef = usernamesCol.document(newNormalized)
            if (tx.get(newRef).exists()) throw IllegalStateException("Username already taken")
            val oldRef = usernamesCol.document(oldUsername.trim().lowercase())
            tx.delete(oldRef)
            tx.set(newRef, mapOf(FIELD_USER_ID to userId))
            val userRef = usersCol.document(userId)
            tx.update(userRef, mapOf(
                FIELD_USERNAME to newNormalized,
                FIELD_UPDATED_AT to com.google.firebase.Timestamp.now()
            ))
        }.await()
    }

    /**
     * Get user document by userId (for current user profile).
     */
    suspend fun getUser(userId: String): UserDoc? {
        val snap = usersCol.document(userId).get().await()
        return snap.toObject(UserDoc::class.java)?.copy(userId = snap.id)
    }

    /**
     * Update discoverableOffline and sync discoverable_users:
     * - ON: set user doc + add to discoverable_users/{username}
     * - OFF: set user doc + remove discoverable_users/{username}
     */
    suspend fun setDiscoverableOffline(userId: String, username: String, discoverable: Boolean) {
        val normalized = username.trim().lowercase()
        val userRef = usersCol.document(userId)
        val discRef = discoverableUsersCol.document(normalized)
        db.runTransaction { tx ->
            tx.update(userRef, mapOf(
                FIELD_DISCOVERABLE_OFFLINE to discoverable,
                FIELD_UPDATED_AT to com.google.firebase.Timestamp.now()
            ))
            if (discoverable) {
                tx.set(discRef, mapOf(FIELD_USER_ID to userId))
            } else {
                tx.delete(discRef)
            }
        }.await()
    }

    /**
     * Add this user to online_users when app comes to foreground.
     */
    suspend fun setOnline(username: String, socketId: String) {
        onlineUsersCol.document(username.trim().lowercase()).set(mapOf(
            FIELD_SOCKET_ID to socketId,
            FIELD_LAST_SEEN to com.google.firebase.Timestamp.now()
        )).await()
    }

    /**
     * Remove from online_users when app goes to background or closes.
     */
    suspend fun setOffline(username: String) {
        onlineUsersCol.document(username.trim().lowercase()).delete().await()
    }

    /**
     * Search for a user by username.
     * Returns discovery result: Online, Offline (discoverable), or null (not found / not discoverable).
     */
    suspend fun findUser(username: String): UserSearchResult? {
        val normalized = username.trim().lowercase()
        if (normalized.isBlank()) return null
        val usernameDoc = usernamesCol.document(normalized).get().await()
        if (!usernameDoc.exists()) return null
        val userId = usernameDoc.getString(FIELD_USER_ID) ?: return null
        val onlineDoc = onlineUsersCol.document(normalized).get().await()
        val discoverableDoc = discoverableUsersCol.document(normalized).get().await()
        val isOnline = onlineDoc.exists()
        val discoverableOffline = discoverableDoc.exists()
        return when {
            isOnline -> UserSearchResult(normalized, userId, Presence.ONLINE)
            discoverableOffline -> UserSearchResult(normalized, userId, Presence.OFFLINE_INVITE)
            else -> null
        }
    }

    companion object {
        const val COLL_USERS = "users"
        const val COLL_USERNAMES = "usernames"
        const val COLL_ONLINE_USERS = "online_users"
        const val COLL_DISCOVERABLE_USERS = "discoverable_users"
        const val FIELD_USERNAME = "username"
        const val FIELD_USER_ID = "userId"
        const val FIELD_DISCOVERABLE_OFFLINE = "discoverableOffline"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val FIELD_SOCKET_ID = "socketId"
        const val FIELD_LAST_SEEN = "lastSeen"
    }
}

data class UserDoc(
    val username: String = "",
    val discoverableOffline: Boolean = false,
    val createdAt: Any? = null,
    val updatedAt: Any? = null,
    val userId: String = ""
)

enum class Presence { ONLINE, OFFLINE_INVITE }

data class UserSearchResult(val username: String, val userId: String, val presence: Presence)
