package com.sample.smartremote

import android.util.Log
import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.TimeUnit

class WebSocketService {
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null

    fun connect(url: String, listener: WebSocketListener) {
        // Disconnect existing if any
        disconnect()

        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, listener)
    }

    fun sendAudioData(data: ByteArray) {
        Log.d("WebSocket:" ,"${data.toByteString()}")
        webSocket?.send(data.toByteString())
    }

    fun sendEventData(event: String){
        Log.d("WebSocket:" , event)
        webSocket?.send(event)
    }

    fun disconnect() {
        webSocket?.close(1000, "Goodbye")
        webSocket = null
    }
}
