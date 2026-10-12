package com.resu.sumo.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.resu.sumo.data.WorkEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockInEntryScreen(
    viewModel: WorkLogViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val lang = viewModel.currentLanguage
    val scrollState = rememberScrollState()

    var showClockOutDialog by remember { mutableStateOf(false) }
    var showManualEntryDialog by remember { mutableStateOf(false) }

    // Dialog Input State
    var selectedLocation by remember { mutableStateOf("V Ranch") }
    var isReportingToday by remember { mutableStateOf(true) }

    var smartImportText by remember { mutableStateOf("") }

    var dialogDateDisplay by remember { mutableStateOf("") }
    var dialogClockInTime by remember { mutableStateOf("7:30 AM") }
    var dialogClockOutTime by remember { mutableStateOf("5:00 PM") }
    var dialogHadLunch by remember { mutableStateOf(true) }
    var dialogLunchMins by remember { mutableStateOf("30") }
    var dialogDescription by remember { mutableStateOf("") }

    // Speech-to-Text Voice Recognition Launcher 🎙️ for Dialog
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrEmpty()) {
                dialogDescription = if (dialogDescription.isEmpty()) spokenText else "$dialogDescription $spokenText"
            }
        }
    }

    // Speech-to-Text Voice Recognition Launcher 🎙️ for Smart Import
    val smartVoiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrEmpty()) {
                smartImportText = if (smartImportText.isEmpty()) spokenText else "$smartImportText $spokenText"
            }
        }
    }

    fun launchVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (lang == AppLanguage.SPANISH) "es-ES" else "en-US")
            putExtra(RecognizerIntent.EXTRA_PROMPT, LanguageManager.getString("voice_input_hint", lang))
        }
        try {
            voiceLauncher.launch(intent)
        } catch (e: Exception) {
            // Speech recognizer not available
        }
    }

    fun launchSmartVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (lang == AppLanguage.SPANISH) "es-ES" else "en-US")
            putExtra(RecognizerIntent.EXTRA_PROMPT, LanguageManager.getString("voice_input_hint", lang))
        }
        try {
            smartVoiceLauncher.launch(intent)
        } catch (e: Exception) {
            // Speech recognizer not available
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LanguageManager.getString("option_a_title", lang)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = "v1.1.0",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        onClick = { viewModel.toggleLanguage() },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Language, contentDescription = "Language", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (lang == AppLanguage.SPANISH) "ES 🇪🇸" else "EN 🇺🇸",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Reporting Today Auto-Fill Selector Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = LanguageManager.getString("reporting_today_question", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { isReportingToday = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isReportingToday) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            if (isReportingToday) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(LanguageManager.getString("reporting_today_yes", lang))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { isReportingToday = false },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (!isReportingToday) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            )
                        ) {
                            if (!isReportingToday) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(LanguageManager.getString("reporting_today_no", lang))
                        }
                    }
                }
            }

            // Display ONLY Live Clock-In when "Reporting Today = YES"
            if (isReportingToday) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (viewModel.isClockedIn) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!viewModel.isClockedIn) {
                            Text(
                                text = LanguageManager.getString("clock_in", lang),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.startClockIn() }, // Immediate 1-click clock in
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(LanguageManager.getString("clock_in", lang))
                            }
                        } else {
                            Text(
                                text = LanguageManager.getString("clocked_in_at", lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    val activeLocation = viewModel.clockInLocation.ifEmpty { "Casa del Patrón" }
                                    val nowEntry = WorkEntry(
                                        location = activeLocation,
                                        clockInTime = viewModel.getFormattedClockInTime(),
                                        clockOutTime = viewModel.getFormattedCurrentTime(),
                                        hadLunch = true,
                                        lunchMinutes = 30
                                    )
                                    viewModel.openEntryForm(nowEntry)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(LanguageManager.getString("clock_out", lang))
                            }
                        }
                    }
                }
            }

            // Display Manual Time Entry & Smart Paragraph Parser when "Reporting Today = NO"
            if (!isReportingToday) {
                // Manual Time Registration Option Button
                Card(
                    onClick = { viewModel.openEntryForm(null) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = LanguageManager.getString("manual_time_title", lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = LanguageManager.getString("manual_time_sub", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Smart Multi-Day Paragraph Text / Voice Import Box 📝🎙️
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = LanguageManager.getString("smart_import_title", lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = LanguageManager.getString("smart_import_desc", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = smartImportText,
                            onValueChange = { smartImportText = it },
                            placeholder = { Text("ej. Lunes 5 de octubre trabajé en la casa del patrón entré a las 7:30 salí a las 2:30 menos una hora de lonche...") },
                            trailingIcon = {
                                IconButton(onClick = { launchSmartVoiceInput() }) {
                                    Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )

                        Button(
                            onClick = {
                                if (smartImportText.isNotBlank()) {
                                    val count = viewModel.parseAndImportParagraphText(smartImportText)
                                    val msg = if (lang == AppLanguage.SPANISH) "¡Se importaron correctamente $count registros!" else "Successfully imported $count entries!"
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    smartImportText = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(LanguageManager.getString("smart_import_button", lang))
                        }
                    }
                }
            }
        }
    }

    // Clock Out Dialog (Asking Location at the end, Lunch Break, Description + Voice Input 🎙️)
    if (showClockOutDialog) {
        AlertDialog(
            onDismissRequest = { showClockOutDialog = false },
            title = { Text(LanguageManager.getString("clock_out_prompt", lang)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = selectedLocation,
                        onValueChange = { selectedLocation = it },
                        label = { Text(LanguageManager.getString("location_prompt_title", lang)) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = dialogHadLunch,
                            onCheckedChange = { dialogHadLunch = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(LanguageManager.getString("had_lunch_question", lang))
                    }
                    if (dialogHadLunch) {
                        OutlinedTextField(
                            value = dialogLunchMins,
                            onValueChange = { dialogLunchMins = it },
                            label = { Text(LanguageManager.getString("lunch_minutes", lang)) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = dialogDescription,
                        onValueChange = { dialogDescription = it },
                        label = { Text(LanguageManager.getString("description_label", lang)) },
                        trailingIcon = {
                            IconButton(onClick = { launchVoiceInput() }) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val lunchM = dialogLunchMins.toIntOrNull() ?: 30
                    viewModel.updateWorkerName(viewModel.workerName)
                    viewModel.finishClockOut(
                        hadLunch = dialogHadLunch,
                        lunchMinutes = lunchM,
                        description = dialogDescription
                    )
                    showClockOutDialog = false
                }) {
                    Text(LanguageManager.getString("clock_out", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClockOutDialog = false }) {
                    Text(LanguageManager.getString("cancel", lang))
                }
            }
        )
    }

    // Manual Entry Registration Dialog with Voice Input 🎙️
    if (showManualEntryDialog) {
        AlertDialog(
            onDismissRequest = { showManualEntryDialog = false },
            title = { Text(LanguageManager.getString("manual_time_title", lang)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dialogDateDisplay,
                        onValueChange = { dialogDateDisplay = it },
                        label = { Text(LanguageManager.getString("date_label", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = selectedLocation,
                        onValueChange = { selectedLocation = it },
                        label = { Text(LanguageManager.getString("location_label", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dialogClockInTime,
                            onValueChange = { dialogClockInTime = it },
                            label = { Text(LanguageManager.getString("in_label", lang)) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dialogClockOutTime,
                            onValueChange = { dialogClockOutTime = it },
                            label = { Text(LanguageManager.getString("out_label", lang)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = dialogHadLunch,
                            onCheckedChange = { dialogHadLunch = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(LanguageManager.getString("had_lunch_question", lang))
                    }
                    if (dialogHadLunch) {
                        OutlinedTextField(
                            value = dialogLunchMins,
                            onValueChange = { dialogLunchMins = it },
                            label = { Text(LanguageManager.getString("lunch_minutes", lang)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = dialogDescription,
                        onValueChange = { dialogDescription = it },
                        label = { Text(LanguageManager.getString("description_label", lang)) },
                        trailingIcon = {
                            IconButton(onClick = { launchVoiceInput() }) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val lunchM = dialogLunchMins.toIntOrNull() ?: 30
                    viewModel.finishClockOut(
                        hadLunch = dialogHadLunch,
                        lunchMinutes = lunchM,
                        description = dialogDescription,
                        customClockInTime = dialogClockInTime,
                        customClockOutTime = dialogClockOutTime,
                        customDateDisplay = dialogDateDisplay.ifEmpty { "21 de sept." }
                    )
                    showManualEntryDialog = false
                }) {
                    Text(LanguageManager.getString("save_entry", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualEntryDialog = false }) {
                    Text(LanguageManager.getString("cancel", lang))
                }
            }
        )
    }
}
