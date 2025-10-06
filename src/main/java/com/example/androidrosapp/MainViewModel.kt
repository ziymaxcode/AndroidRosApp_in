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

    // State to track if automation is running
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
        val command = PolarMoveCommand(radius = radius, angle = angle)
        val rosMessage = RosMessage(
            op = "publish",
            topic = "/polar_move_cmd",
            type = "my_robot_interfaces/msg/PolarMove",
            msg = command
        )
        val jsonMessage = Json.encodeToString(rosMessage)
          println("DEBUG: Sending JSON: $jsonMessage")
        rosRepository.sendCommand(jsonMessage)
    }

    // Function to run the automation sequence
// In MainViewModel.kt

    fun startAutomation(steps: List<AutomationStep>) {
        if (_isAutomating.value) return

        viewModelScope.launch {
            _isAutomating.value = true
            for (step in steps) {
                sendManualMoveCommand(step.radius.toFloat(), step.angle.toFloat())
                // UPDATE: Use the delay from the Excel file
                // Convert seconds to milliseconds by multiplying by 1000
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