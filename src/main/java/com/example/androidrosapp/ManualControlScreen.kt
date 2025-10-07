package com.example.androidrosapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun ManualControlScreen(viewModel: MainViewModel) {
    var radiusInput by remember { mutableStateOf("") }
    var angleInput by remember { mutableStateOf("") }

    val feedback by viewModel.robotFeedback.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    //  Observe the error message state
    val errorMessage by viewModel.errorMessage.collectAsState()

    val movedRadius = String.format(Locale.US, "%.2f", feedback.movedRadius)
    val movedAngle = String.format(Locale.US, "%.2f", feedback.movedAngle)
    val errorVector = String.format(Locale.US, "%.2f%%", feedback.errorVector)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Polar Co-ordinates", fontSize = 20.sp, style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = radiusInput,
            onValueChange = { radiusInput = it },
            label = { Text("Radius (0-500 cms)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = angleInput,
            onValueChange = { angleInput = it },
            label = { Text("Angle (0-360 deg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Button(
            onClick = {
                val radius = radiusInput.toFloatOrNull() ?: 0f
                val angle = angleInput.toFloatOrNull() ?: 0f
                viewModel.sendManualMoveCommand(radius, angle)
            },
            enabled = isConnected
        ) {
            Text("Move")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider()
        Spacer(modifier = Modifier.height(24.dp))

        Text("Movement", fontSize = 20.sp, style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = movedRadius, onValueChange = {}, readOnly = true, label = { Text("Moved Radius") }
        )
        OutlinedTextField(
            value = movedAngle, onValueChange = {}, readOnly = true, label = { Text("Moved Angle") }
        )
        OutlinedTextField(
            value = errorVector, onValueChange = {}, readOnly = true, label = { Text("Error vector") }
        )

        //  Add a text field at the bottom to display errors
        if (errorMessage.isNotEmpty()) {
            Text(
                text = "Error: $errorMessage",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}