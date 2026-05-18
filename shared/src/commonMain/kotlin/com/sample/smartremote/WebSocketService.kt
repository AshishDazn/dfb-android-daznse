package com.sample.smartremote

import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlin.time.Duration.Companion.seconds

class WebSocketService {
    private val client = HttpClient {
        install(WebSockets) {
            pingInterval = 20.seconds
        }
    }
    private var session: DefaultClientWebSocketSession? = null

    suspend fun connect(urlString: String) {
        disconnect()
        try {
            session = client.webSocketSession(urlString)
        } catch (e: Exception) {
            session = null
            throw e
        }
    }

    suspend fun sendAudioData(data: ByteArray) {
        try {
            session?.send(Frame.Binary(true, data))
        } catch (e: Exception) {
            Napier.e("Error sending audio data: ${e.message}", e)
        }
    }

    suspend fun sendEventData(event: String) {
        try {
            session?.send(Frame.Text(event))
        } catch (e: Exception) {
            Napier.e("Error sending event data: ${e.message}", e)
        }
    }

    fun receive(): Flow<String> {
        val currentSession = session ?: throw IllegalStateException("WebSocket session not initialized")
        return currentSession.incoming.consumeAsFlow().mapNotNull { frame ->
            (frame as? Frame.Text)?.readText()
        }
    }

    suspend fun disconnect() {
        try {
            session?.close()
        } catch (e: Exception) {
            Napier.e("Error during WebSocket disconnect: ${e.message}", e)
        } finally {
            session = null
        }
    }
}
