package com.example.androidrosapp

data class AutomationStep(
    val slNo: Int,
    val radius: Double,
    val angle: Double,
    val delaySeconds: Long,
    var movedRadius: Double? = null, // Nullable, as it will be filled in later by the robot
    var movedAngle: Double? = null,
    var error: String? = null
)