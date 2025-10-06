package com.example.androidrosapp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This file contains all the data class structures for messages
 * sent to and received from the rosbridge WebSocket server.
 */

// 1. Generic classes for the rosbridge protocol structure

@Serializable
data class RosMessage<T>(
    val op: String, // "publish", "subscribe", etc.
    val topic: String? = null,
    val msg: T? = null,
    val type: String? = null
)

// 2. Data structures for YOUR specific robot commands and feedback

@Serializable
data class PolarMoveCommand(
    val radius: Float,
    val angle: Float
)

@Serializable
data class RobotFeedback(
    @SerialName("moved_radius") // This maps the JSON key to your variable name
    val movedRadius: Float,
    @SerialName("moved_angle")
    val movedAngle: Float,
    @SerialName("error_vector")
    val errorVector: Float
)