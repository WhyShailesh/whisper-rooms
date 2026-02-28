package com.vanish.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.vanish.app.data.IdentityStore
import com.vanish.app.data.firestore.FirestoreRepository
import com.vanish.app.data.socket.SocketHolder
import com.vanish.app.ui.navigation.NavGraph
import com.vanish.app.ui.theme.VanishTheme
import kotlinx.coroutines.launch
import androidx.navigation.compose.rememberNavController

/**
 * Single Activity. Connects socket when in foreground (with username), disconnects and removes from online_users when background.
 * FLAG_SECURE and black recents are set in theme/Window.
 */
class MainActivity : ComponentActivity() {

    private lateinit var identityStore: IdentityStore
    private lateinit var firestore: FirestoreRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        identityStore = IdentityStore(this)
        firestore = FirestoreRepository()
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent {
            VanishTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    NavGraph(navController = rememberNavController())
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val username = identityStore.getUsernameSync()
        if (!username.isNullOrBlank()) {
            SocketHolder.connect(
                username = username,
                onRegisterAck = { socketId ->
                    lifecycleScope.launch {
                        try {
                            firestore.setOnline(username, socketId)
                        } catch (_: Exception) { }
                    }
                }
            )
        }
    }

    override fun onStop() {
        super.onStop()
        val username = identityStore.getUsernameSync()
        SocketHolder.disconnect()
        if (!username.isNullOrBlank()) {
            lifecycleScope.launch {
                try {
                    firestore.setOffline(username)
                } catch (_: Exception) { }
            }
        }
    }
}
