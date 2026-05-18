package com.sample.smartremote

import com.sample.smartremote.data.Config
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
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
            Napier.d(message = "[${Config.LOG_TAG}] Sending audio data: ${data.size} bytes", tag = Config.LOG_TAG)
            session?.send(Frame.Binary(fin = true, data = data))
        } catch (e: Exception) {
            Napier.e(message = "[${Config.LOG_TAG}] Error sending audio data: ${e.message}", throwable = e, tag = Config.LOG_TAG)
        }
    }

    suspend fun sendEventData(event: String) {
        try {
            Napier.d(message = "[${Config.LOG_TAG}] Sending event data: $event", tag = Config.LOG_TAG)
            session?.send(Frame.Text(event))
        } catch (e: Exception) {
            Napier.e(message = "[${Config.LOG_TAG}] Error sending event data: ${e.message}", throwable = e, tag = Config.LOG_TAG)
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
            Napier.e(message = "[${Config.LOG_TAG}] Error during WebSocket disconnect: ${e.message}", throwable = e, tag = Config.LOG_TAG)
        } finally {
            session = null
        }
    }
}
