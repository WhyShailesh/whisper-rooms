package com.vanish.app

import android.app.Application

/**
 * Application entry point. No global state that persists messages.
 * Identity (UUID, username) is managed via IdentityStore and Firestore.
 */
class VanishApplication : Application()
