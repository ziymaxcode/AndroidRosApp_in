package com.example.androidrosapp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class RosRepository(private val viewModel: MainViewModel) {

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
            try {
                // 1. Decode the outer message to get the std_msgs/String
                val message = Json.decodeFromString<RosMessage<StringMessage>>(text)

                if (message.op == "publish" && message.msg != null) {
                    // 2. Extract the JSON string from the 'data' field
                    val feedbackJsonString = message.msg.data

                    // 3. Decode the INNER JSON string to get the RobotFeedback object
                    val feedbackObject = Json.decodeFromString<RobotFeedback>(feedbackJsonString)

                    // 4. Update the data stream with the final object
                    _feedbackMessage.value = feedbackObject
                }
            } catch (e: Exception) {
                println("Error parsing message: ${e.message}")
                // NEW: Update the ViewModel with the error message
                viewModel.setErrorMessage(e.message ?: "Unknown parsing error")
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
        // Tell rosbridge we want to subscribe to a 'std_msgs/String'
        val subscriptionMessage = RosMessage<Unit>(
            op = "subscribe",
            topic = "/robot_feedback",
            type = "std_msgs/String" // ADD THIS LINE
        )
        val jsonMessage = Json.encodeToString(RosMessage.serializer(Unit.serializer()), subscriptionMessage)
        sendCommand(jsonMessage)
    }

}