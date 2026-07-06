package com.sample.smartremote

import com.sample.smartremote.data.Config
import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration.Companion.seconds

class WebSocketService(private val client: HttpClient) {
    private var session: DefaultClientWebSocketSession? = null

    suspend fun connect(urlString: String) {
        disconnect()
        try {
            Napier.d(message = "[${Config.LOG_TAG}] WebSocketService: Opening session to $urlString", tag = Config.LOG_TAG)
            session = client.webSocketSession(urlString)
            Napier.d(message = "[${Config.LOG_TAG}] WebSocketService: Session opened successfully", tag = Config.LOG_TAG)
        } catch (e: Exception) {
            Napier.e(message = "[${Config.LOG_TAG}] WebSocketService: Failed to open session: ${e.message}", throwable = e, tag = Config.LOG_TAG)
            session = null
            throw e
        }
    }

    suspend fun sendAudioData(data: ByteArray) {
        val currentSession = session
        if (currentSession == null) {
            Napier.w(message = "[${Config.LOG_TAG}] WebSocketService: Cannot send audio data, session is null", tag = Config.LOG_TAG)
            return
        }
        try {
            Napier.d(message = "[${Config.LOG_TAG}] Sending audio data: ${data.size} bytes", tag = Config.LOG_TAG)
            currentSession.send(Frame.Binary(fin = true, data = data))
        } catch (e: Exception) {
            Napier.e(message = "[${Config.LOG_TAG}] Error sending audio data: ${e.message}", throwable = e, tag = Config.LOG_TAG)
            handleSessionError(e)
        }
    }

    suspend fun sendEventData(event: String) {
        val currentSession = session
        if (currentSession == null) {
            Napier.w(message = "[${Config.LOG_TAG}] WebSocketService: Cannot send event data, session is null", tag = Config.LOG_TAG)
            return
        }
        try {
            Napier.d(message = "[${Config.LOG_TAG}] Sending event data: $event", tag = Config.LOG_TAG)
            currentSession.send(Frame.Text(event))
        } catch (e: Exception) {
            Napier.e(message = "[${Config.LOG_TAG}] Error sending event data: ${e.message}", throwable = e, tag = Config.LOG_TAG)
            handleSessionError(e)
        }
    }

    fun receive(): Flow<String> = flow {
        val currentSession = session ?: throw IllegalStateException("WebSocket session not initialized")
        try {
            currentSession.incoming.consumeAsFlow().collect { frame ->
                if (frame is Frame.Text) {
                    emit(frame.readText())
                }
            }
        } catch (e: Exception) {
            Napier.e(message = "[${Config.LOG_TAG}] Error receiving data: ${e.message}", throwable = e, tag = Config.LOG_TAG)
            throw e
        }
    }

    private suspend fun handleSessionError(e: Exception) {
        val reason = try {
            session?.closeReason?.await()
        } catch (_: Exception) {
            null
        }
        Napier.e(message = "[${Config.LOG_TAG}] Session error occurred: ${e.message}. Close reason: $reason", throwable = e, tag = Config.LOG_TAG)
    }

    suspend fun disconnect() {
        try {
            session?.close()
            Napier.d(message = "[${Config.LOG_TAG}] WebSocketService: Session closed manually", tag = Config.LOG_TAG)
        } catch (e: Exception) {
            Napier.e(message = "[${Config.LOG_TAG}] Error during WebSocket disconnect: ${e.message}", throwable = e, tag = Config.LOG_TAG)
        } finally {
            session = null
        }
    }
}
