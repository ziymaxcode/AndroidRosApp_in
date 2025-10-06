package com.example.androidrosapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainViewModel : ViewModel() {

    private val rosRepository = RosRepository()
    val isConnected = rosRepository.connectionStatus
    val robotFeedback = rosRepository.feedbackMessage

    private val _isAutomating = MutableStateFlow(false)
    val isAutomating = _isAutomating.asStateFlow()

    fun connect(ipAddress: String) {
        viewModelScope.launch {
            rosRepository.connect(ipAddress)
        }
    }

    fun disconnect() {
        rosRepository.disconnect()
    }

    fun sendManualMoveCommand(radius: Float, angle: Float) {
        // This function now contains the new wrapping logic
        val command = PolarMoveCommand(radius = radius, angle = angle)
        val commandJsonString = Json.encodeToString(command)
        val stringMessage = StringMessage(data = commandJsonString)
        val rosMessage = RosMessage(
            op = "publish",
            topic = "/polar_move_cmd",
            type = "std_msgs/String",
            msg = stringMessage
        )
        val jsonMessage = Json.encodeToString(rosMessage)
        println("DEBUG: Sending JSON: $jsonMessage") // Add this line
        rosRepository.sendCommand(jsonMessage)
    }

    // --- UPDATED AUTOMATION FUNCTION ---
    fun startAutomation(steps: List<AutomationStep>) {
        if (_isAutomating.value) return

        viewModelScope.launch {
            _isAutomating.value = true
            for (step in steps) {
                // We can reuse the updated sendManualMoveCommand function
                // as it already contains the correct wrapping logic.
                sendManualMoveCommand(step.radius.toFloat(), step.angle.toFloat())
                delay(step.delaySeconds * 1000L)
            }
            _isAutomating.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}