package com.sample.smartremote

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.mapNotNull

class WebSocketService {
    private val client = HttpClient {
        install(WebSockets)
    }
    private var session: DefaultClientWebSocketSession? = null

    suspend fun connect(url: String) {
        disconnect()
        session = client.webSocketSession(
            method = HttpMethod.Get,
            host = "your-websocket-host", // Replace with your actual host
            port = 8080, // Replace with your actual port
            path = url
        )
    }

    suspend fun sendAudioData(data: ByteArray) {
        session?.send(Frame.Binary(true, data))
    }

    suspend fun sendEventData(event: String) {
        session?.send(Frame.Text(event))
    }

    fun receive(): Flow<String> {
        return session!!.incoming.consumeAsFlow().mapNotNull {
            (it as? Frame.Text)?.readText()
        }
    }

    suspend fun disconnect() {
        session?.close()
        session = null
    }
}
