package com.vanish.app.data.socket

import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject

/**
 * Socket.IO client for real-time chat. All messages are relay-only; nothing is persisted.
 */
class VanishSocketClient(serverUrl: String) {

    private val options = IO.Options().apply {
        forceNew = true
        reconnection = true
        reconnectionAttempts = 5
        reconnectionDelay = 1000
        transports = arrayOf(WebSocket.NAME)
    }

    @Volatile
    private var socket: Socket? = null

    val isConnected: Boolean
        get() = socket?.connected() == true

    /**
     * Connect with username. Server associates socketId with username and sends register_ack(socketId).
     */
    fun connect(
        username: String,
        onConnect: (() -> Unit)? = null,
        onDisconnect: ((String?) -> Unit)? = null,
        onRegisterAck: ((socketId: String) -> Unit)? = null
    ) {
        disconnect()
        socket = IO.socket(serverUrl, options).apply {
            on(Socket.EVENT_CONNECT) { onConnect?.invoke() }
            on(Socket.EVENT_DISCONNECT) { onDisconnect?.invoke(null) }
            on(Socket.EVENT_CONNECT_ERROR) { args ->
                onDisconnect?.invoke((args.getOrNull(0) as? Exception)?.message)
            }
            on("register_ack") { args ->
                val obj = args.getOrNull(0) as? JSONObject
                obj?.optString("socketId")?.let { onRegisterAck?.invoke(it) }
            }
        }
        socket?.connect()
        socket?.emit(EVENT_REGISTER, username.trim().lowercase())
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
    }

    /**
     * Emit a one-to-one message. Server relays to receiver by username; if offline, message is dropped.
     */
    fun sendDirectMessage(toUsername: String, text: String) {
        socket?.emit(EVENT_DIRECT_MESSAGE, JSONObject().apply {
            put(KEY_TO, toUsername.trim().lowercase())
            put(KEY_TEXT, text)
        })
    }

    /**
     * Flow of incoming direct messages (only while connected; not persisted).
     */
    fun directMessageFlow(): Flow<DirectMessage> = callbackFlow {
        val s = socket ?: run { close(); return@callbackFlow }
        val handler: (Array<Any>) -> Unit = { args ->
            val obj = args.getOrNull(0) as? JSONObject ?: return@handler
            trySend(DirectMessage(
                from = obj.optString(KEY_FROM, ""),
                text = obj.optString(KEY_TEXT, "")
            ))
        }
        s.on(EVENT_DIRECT_MESSAGE, handler)
        awaitClose { s.off(EVENT_DIRECT_MESSAGE) }
    }

    // --- Room events ---
    fun createRoom(onRoomCreated: (String) -> Unit, onError: (String) -> Unit) {
        socket?.emit(EVENT_CREATE_ROOM, JSONObject()) { args ->
            val a = args.getOrNull(0)
            when (a) {
                is JSONObject -> {
                    val code = a.optString(KEY_ROOM_CODE, "")
                    val err = a.optString(KEY_ERROR, "")
                    if (code.isNotBlank()) onRoomCreated(code) else onError(err.ifBlank { "Unknown error" })
                }
                else -> onError("Invalid response")
            }
        }
    }

    fun joinRoom(roomCode: String, onJoined: () -> Unit, onPending: () -> Unit, onError: (String) -> Unit) {
        socket?.emit(EVENT_JOIN_ROOM, JSONObject().put(KEY_ROOM_CODE, roomCode.trim().uppercase())) { args ->
            val a = args.getOrNull(0) as? JSONObject
            when (a?.optString(KEY_STATUS, "")) {
                STATUS_JOINED -> onJoined()
                STATUS_PENDING -> onPending()
                else -> onError(a?.optString(KEY_ERROR, "Could not join") ?: "Unknown error")
            }
        }
    }

    fun approveJoin(roomCode: String, username: String) {
        socket?.emit(EVENT_APPROVE_JOIN, JSONObject().apply {
            put(KEY_ROOM_CODE, roomCode.trim().uppercase())
            put(KEY_USERNAME, username.trim().lowercase())
        })
    }

    fun leaveRoom(roomCode: String) {
        socket?.emit(EVENT_LEAVE_ROOM, JSONObject().put(KEY_ROOM_CODE, roomCode.trim().uppercase()))
    }

    fun sendRoomMessage(roomCode: String, text: String) {
        socket?.emit(EVENT_ROOM_MESSAGE, JSONObject().apply {
            put(KEY_ROOM_CODE, roomCode.trim().uppercase())
            put(KEY_TEXT, text)
        })
    }

    fun roomMessageFlow(): Flow<RoomMessage> = callbackFlow {
        val s = socket ?: run { close(); return@callbackFlow }
        val handler: (Array<Any>) -> Unit = { args ->
            val obj = args.getOrNull(0) as? JSONObject ?: return@handler
            trySend(RoomMessage(
                roomCode = obj.optString(KEY_ROOM_CODE, ""),
                from = obj.optString(KEY_FROM, ""),
                text = obj.optString(KEY_TEXT, "")
            ))
        }
        s.on(EVENT_ROOM_MESSAGE, handler)
        awaitClose { s.off(EVENT_ROOM_MESSAGE) }
    }

    fun joinRequestFlow(): Flow<JoinRequest> = callbackFlow {
        val s = socket ?: run { close(); return@callbackFlow }
        val handler: (Array<Any>) -> Unit = { args ->
            val obj = args.getOrNull(0) as? JSONObject ?: return@handler
            trySend(JoinRequest(
                roomCode = obj.optString(KEY_ROOM_CODE, ""),
                username = obj.optString(KEY_USERNAME, "")
            ))
        }
        s.on(EVENT_JOIN_REQUEST, handler)
        awaitClose { s.off(EVENT_JOIN_REQUEST) }
    }

    fun roomMembersFlow(): Flow<RoomMembersUpdate> = callbackFlow {
        val s = socket ?: run { close(); return@callbackFlow }
        val handler: (Array<Any>) -> Unit = { args ->
            val obj = args.getOrNull(0) as? JSONObject ?: return@handler
            val list = obj.optJSONArray(KEY_MEMBERS)?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()
            trySend(RoomMembersUpdate(roomCode = obj.optString(KEY_ROOM_CODE, ""), members = list))
        }
        s.on(EVENT_ROOM_MEMBERS, handler)
        awaitClose { s.off(EVENT_ROOM_MEMBERS) }
    }

    fun joinRoomAckFlow(): Flow<String> = callbackFlow {
        val s = socket ?: run { close(); return@callbackFlow }
        val handler: (Array<Any>) -> Unit = { args ->
            val obj = args.getOrNull(0) as? JSONObject ?: return@handler
            if (obj.optString(KEY_STATUS, "") == STATUS_JOINED) {
                trySend(obj.optString(KEY_ROOM_CODE, ""))
            }
        }
        s.on("join_room_ack", handler)
        awaitClose { s.off("join_room_ack") }
    }

    fun roomClosedFlow(): Flow<String> = callbackFlow {
        val s = socket ?: run { close(); return@callbackFlow }
        val handler: (Array<Any>) -> Unit = { args ->
            val obj = args.getOrNull(0) as? JSONObject ?: return@handler
            trySend(obj.optString(KEY_ROOM_CODE, ""))
        }
        s.on("room_closed", handler)
        awaitClose { s.off("room_closed") }
    }

    companion object {
        const val EVENT_REGISTER = "register"
        const val EVENT_DIRECT_MESSAGE = "direct_message"
        const val EVENT_CREATE_ROOM = "create_room"
        const val EVENT_JOIN_ROOM = "join_room"
        const val EVENT_APPROVE_JOIN = "approve_join"
        const val EVENT_LEAVE_ROOM = "leave_room"
        const val EVENT_ROOM_MESSAGE = "room_message"
        const val EVENT_JOIN_REQUEST = "join_request"
        const val EVENT_ROOM_MEMBERS = "room_members"
        const val KEY_TO = "to"
        const val KEY_FROM = "from"
        const val KEY_TEXT = "text"
        const val KEY_ROOM_CODE = "roomCode"
        const val KEY_USERNAME = "username"
        const val KEY_STATUS = "status"
        const val KEY_ERROR = "error"
        const val KEY_MEMBERS = "members"
        const val STATUS_JOINED = "joined"
        const val STATUS_PENDING = "pending"
    }
}

data class DirectMessage(val from: String, val text: String)
data class RoomMessage(val roomCode: String, val from: String, val text: String)
data class JoinRequest(val roomCode: String, val username: String)
data class RoomMembersUpdate(val roomCode: String, val members: List<String>)
