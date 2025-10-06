package com.example.androidrosapp

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream


@Composable
fun AutomatedControlScreen(viewModel: MainViewModel) {
    var automationSteps by remember { mutableStateOf<List<AutomationStep>>(emptyList()) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isConnected by viewModel.isConnected.collectAsState()
    val isAutomating by viewModel.isAutomating.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                val steps = parseExcelFile(context, it)
                launch(Dispatchers.Main) {
                    automationSteps = steps
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(onClick = {
            filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        }) {
            Text("Upload Excel")
        }

        Column(modifier = Modifier.weight(1f)) {
            TableHeader()
            Divider()
            LazyColumn {
                items(automationSteps) { step ->
                    TableRow(step = step)
                    Divider()
                }
            }
        }

        Button(
            onClick = {
                viewModel.startAutomation(automationSteps)
            },
            enabled = automationSteps.isNotEmpty() && !isAutomating && isConnected
        ) {
            Text(if (isAutomating) "Running..." else "Automate")
        }
    }
}



@Composable
fun TableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Sl.no", modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        Text("Radius", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        Text("Angle", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        Text("Delay", modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) // Shortened
        Text("Moved R", modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) // Given more weight
        Text("Moved A", modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold) // Given more weight
        Text("Error", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TableRow(step: AutomationStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(step.slNo.toString(), modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
        Text(step.radius.toString(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(step.angle.toString(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(step.delaySeconds.toString(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center) // NEW
        Text(step.movedRadius?.toString() ?: " ", modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(step.movedAngle?.toString() ?: " ", modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(step.error ?: " ", modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
    }
}

private fun parseExcelFile(context: Context, uri: Uri): List<AutomationStep> {
    val steps = mutableListOf<AutomationStep>()
    try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        inputStream?.use { stream ->
            val workbook = WorkbookFactory.create(stream)
            val sheet = workbook.getSheetAt(0)
            for (i in 1..sheet.lastRowNum) {
                val row = sheet.getRow(i) ?: continue
                val slNo = row.getCell(0)?.numericCellValue?.toInt()
                val radius = row.getCell(1)?.numericCellValue
                val angle = row.getCell(2)?.numericCellValue
                val delay = row.getCell(3)?.numericCellValue?.toLong()
                if (slNo != null && radius != null && angle != null && delay != null) {
                    steps.add(
                        AutomationStep(
                            slNo = slNo,
                            radius = radius,
                            angle = angle,
                            delaySeconds = delay
                        )
                    )
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return steps
}