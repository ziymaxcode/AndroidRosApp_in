package com.example.androidrosapp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class RosRepository {

    private var webSocket: WebSocket? = null
    private val client: OkHttpClient

    // Flow for connection status
    private val _connectionStatus = MutableStateFlow(false)
    val connectionStatus: StateFlow<Boolean> = _connectionStatus

    // NEW: Flow for incoming feedback messages
    private val _feedbackMessage = MutableStateFlow(RobotFeedback(0f, 0f, 0f))
    val feedbackMessage: StateFlow<RobotFeedback> = _feedbackMessage

    init {
        val logging = HttpLoggingInterceptor().apply { setLevel(HttpLoggingInterceptor.Level.BODY) }
        client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    }

    fun connect(ipAddress: String) {
        if (webSocket != null) return
        val request = Request.Builder().url("ws://$ipAddress:9090").build()
        webSocket = client.newWebSocket(request, RosWebSocketListener())
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionStatus.value = false
    }

    fun sendCommand(jsonMessage: String) {
    if (_connectionStatus.value) {
            webSocket?.send(jsonMessage)
        } else {
            println("Cannot send command, not connected.")
        }
    }

    private inner class RosWebSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            println("WebSocket Connection Opened")
            _connectionStatus.value = true
            // NEW: Subscribe to the feedback topic as soon as we connect
            subscribeToFeedback()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            println("Received message: $text")
            // NEW: Parse the incoming message
            try {
                val message = Json.decodeFromString<RosMessage<RobotFeedback>>(text)
                if (message.op == "publish" && message.msg != null) {
                    _feedbackMessage.value = message.msg
                }
            } catch (e: Exception) {
                println("Error parsing message: ${e.message}")
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
            _connectionStatus.value = false
            println("WebSocket Closing: $code / $reason")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _connectionStatus.value = false
            println("WebSocket Error: " + t.message)
            this@RosRepository.webSocket = null
        }
    }

    // NEW: Helper function to subscribe
    private fun subscribeToFeedback() {
        val subscriptionMessage = RosMessage<Unit>(
            op = "subscribe",
            topic = "/robot_feedback" // The feedback topic you get from the ECE team
        )
        val jsonMessage = Json.encodeToString(subscriptionMessage)
        sendCommand(jsonMessage)
    }
}