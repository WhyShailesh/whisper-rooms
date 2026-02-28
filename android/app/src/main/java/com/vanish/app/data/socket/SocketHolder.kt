package com.vanish.app.data.socket

import com.vanish.app.config.ServerConfig

/**
 * Holds the single Socket.IO client for the app. Connect when app is in foreground (with username);
 * disconnect when app goes to background. Used by Home/Chat/Room for messaging.
 */
object SocketHolder {

    @Volatile
    private var client: VanishSocketClient? = null

    @Volatile
    private var currentUsername: String? = null

    fun getSocket(): VanishSocketClient? = client

    fun isConnected(): Boolean = client?.isConnected == true

    /**
     * Connect with username. Emits register; on register_ack from server, call onRegisterAck(socketId)
     * so caller can update Firestore online_users.
     */
    fun connect(
        username: String,
        onConnect: (() -> Unit)? = null,
        onRegisterAck: (socketId: String) -> Unit,
        onDisconnect: ((String?) -> Unit)? = null
    ) {
        disconnect()
        currentUsername = username.trim().lowercase()
        val c = VanishSocketClient(ServerConfig.SOCKET_URL)
        client = c
        c.connect(
            username = currentUsername!!,
            onConnect = onConnect,
            onDisconnect = onDisconnect,
            onRegisterAck = onRegisterAck
        )
    }

    fun disconnect() {
        currentUsername = null
        client?.disconnect()
        client = null
    }
}
