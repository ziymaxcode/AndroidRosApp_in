package com.example.androidrosapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.androidrosapp.ui.theme.AndroidRosAppTheme

class MainActivity : ComponentActivity() {
    // Create the ViewModel instance
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidRosAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    RobotControllerApp(mainViewModel) // Pass ViewModel to the main app composable
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RobotControllerApp(viewModel: MainViewModel) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val isConnected by viewModel.isConnected.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ROS Robot Car") },
                actions = {
                    Text(
                        text = if (isConnected) "Connected" else "Disconnected",
                        color = if (isConnected) Color.Green else Color.Red,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            ConnectionBar(viewModel = viewModel, isConnected = isConnected)
            HorizontalPager(state = pagerState) { page ->
                // Inside the HorizontalPager in MainActivity.kt
                when (page) {
                    0 -> ManualControlScreen(viewModel = viewModel)
                    // UPDATE THIS LINE
                    1 -> AutomatedControlScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun ConnectionBar(viewModel: MainViewModel, isConnected: Boolean) {
    var ipAddress by remember { mutableStateOf("192.168.1.101") } // Default IP

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = ipAddress,
            onValueChange = { ipAddress = it },
            label = { Text("Robot IP Address") },
            modifier = Modifier.weight(1f),
            enabled = !isConnected
        )
        Button(
            onClick = {
                if (isConnected) {
                    viewModel.disconnect()
                } else {
                    viewModel.connect(ipAddress)
                }
            }
        ) {
            Text(if (isConnected) "Disconnect" else "Connect")
        }
    }
}