package com.resu.sumo.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.resu.sumo.Screen
import com.resu.sumo.data.WorkEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizardEntryScreen(
    viewModel: WorkLogViewModel,
    onBack: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val lang = viewModel.currentLanguage
    val scrollState = rememberScrollState()

    val editingEntry = viewModel.editingEntryInForm

    var currentStep by remember { mutableIntStateOf(0) } // 0: Date/Loc, 1: Schedule, 2: Lunch, 3: Desc, 4: Review

    var dialogDateDisplay by remember(editingEntry) {
        mutableStateOf(
            if (!editingEntry?.dateDisplay.isNullOrBlank()) editingEntry.dateDisplay
            else viewModel.getFormattedCurrentDate()
        )
    }
    var dialogLocation by remember(editingEntry) {
        mutableStateOf(
            if (!editingEntry?.location.isNullOrBlank()) editingEntry.location
            else viewModel.clockInLocation.ifEmpty { "Casa del Patrón" }
        )
    }
    var dialogClockInTime by remember(editingEntry) {
        mutableStateOf(
            if (!editingEntry?.clockInTime.isNullOrBlank()) editingEntry.clockInTime
            else viewModel.getFormattedClockInTime()
        )
    }
    var dialogClockOutTime by remember(editingEntry) {
        mutableStateOf(
            if (!editingEntry?.clockOutTime.isNullOrBlank()) editingEntry.clockOutTime
            else viewModel.getFormattedCurrentTime()
        )
    }
    var dialogHadLunch by remember(editingEntry) { mutableStateOf(editingEntry?.hadLunch ?: true) }
    var dialogLunchMins by remember(editingEntry) { mutableStateOf(editingEntry?.lunchMinutes?.toString() ?: "30") }
    var dialogDescription by remember(editingEntry) { mutableStateOf(editingEntry?.description ?: "") }

    // Native Material3 TimePicker Dialog States ⏰
    var showClockInPicker by remember { mutableStateOf(false) }
    var showClockOutPicker by remember { mutableStateOf(false) }

    val clockInPickerState = rememberTimePickerState(
        initialHour = 7,
        initialMinute = 30,
        is24Hour = false
    )

    val clockOutPickerState = rememberTimePickerState(
        initialHour = 15,
        initialMinute = 30,
        is24Hour = false
    )

    // Speech-to-Text Voice Recognition Launcher 🎙️
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

    fun saveAndExit() {
        keyboardController?.hide()
        focusManager.clearFocus()
        val lunchM = if (dialogHadLunch) (dialogLunchMins.toIntOrNull() ?: 30) else 0

        if (viewModel.isClockedIn) {
            viewModel.finishClockOut(
                hadLunch = dialogHadLunch,
                lunchMinutes = lunchM,
                description = dialogDescription,
                customClockInTime = dialogClockInTime,
                customClockOutTime = dialogClockOutTime,
                customDateDisplay = dialogDateDisplay
            )
        } else {
            val newEntry = (editingEntry ?: WorkEntry()).copy(
                dateDisplay = dialogDateDisplay.ifEmpty { viewModel.getFormattedCurrentDate() },
                location = dialogLocation.ifEmpty { "Casa del Patrón" },
                clockInTime = dialogClockInTime,
                clockOutTime = dialogClockOutTime,
                hadLunch = dialogHadLunch,
                lunchMinutes = lunchM,
                description = dialogDescription
            )
            viewModel.addOrUpdateEntry(newEntry)
        }

        viewModel.editingEntryInForm = null
        viewModel.currentScreen = Screen.SUMMARY
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (editingEntry == null) LanguageManager.getString("add_entry", lang) else LanguageManager.getString("edit_entry", lang),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        if (currentStep > 0) currentStep-- else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Step Progress Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { (currentStep + 1) / 5f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Paso ${currentStep + 1} de 5",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            // Step Content Window
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (currentStep) {
                        0 -> {
                            // Step 1: Date & Location
                            Text(
                                text = "1. Fecha y Ubicación del Trabajo",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = dialogDateDisplay,
                                onValueChange = { dialogDateDisplay = it },
                                label = { Text(LanguageManager.getString("date_label", lang)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); focusManager.clearFocus() }),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = dialogLocation,
                                onValueChange = { dialogLocation = it },
                                label = { Text(LanguageManager.getString("location_label", lang)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); focusManager.clearFocus() }),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        1 -> {
                            // Step 2: Schedule with Native Android Clock TimePicker! ⏰
                            Text(
                                text = "2. Horario de Entrada y Salida ⏰",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Toca cualquier campo para abrir el reloj digital (AM/PM)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Clock In Picker Button
                            Card(
                                onClick = { showClockInPicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(if (lang == AppLanguage.SPANISH) "Hora de Entrada" else "Clock In Time", style = MaterialTheme.typography.labelSmall)
                                        Text(dialogClockInTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Clock Out Picker Button
                            Card(
                                onClick = { showClockOutPicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(if (lang == AppLanguage.SPANISH) "Hora de Salida" else "Clock Out Time", style = MaterialTheme.typography.labelSmall)
                                        Text(dialogClockOutTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Step 3: Lunch Break
                            Text(
                                text = "3. Descuento de Lonche",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = dialogHadLunch,
                                    onCheckedChange = { dialogHadLunch = it }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = LanguageManager.getString("had_lunch_question", lang),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (dialogHadLunch) {
                                OutlinedTextField(
                                    value = dialogLunchMins,
                                    onValueChange = { dialogLunchMins = it },
                                    label = { Text(LanguageManager.getString("lunch_minutes", lang)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); focusManager.clearFocus() }),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        3 -> {
                            // Step 4: Description & Voice Dictation
                            Text(
                                text = "4. Descripción del Trabajo Realizado",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = LanguageManager.getString("voice_input_hint", lang),
                                style = MaterialTheme.typography.bodySmall
                            )
                            OutlinedTextField(
                                value = dialogDescription,
                                onValueChange = { dialogDescription = it },
                                label = { Text(LanguageManager.getString("description_label", lang)) },
                                singleLine = false,
                                maxLines = 8,
                                trailingIcon = {
                                    IconButton(onClick = { launchVoiceInput() }) {
                                        Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); focusManager.clearFocus() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 160.dp)
                            )
                        }
                        4 -> {
                            // Step 5: Final Review & Save
                            Text(
                                text = "5. Confirmar y Guardar Registro",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("📅 Fecha: $dialogDateDisplay", fontWeight = FontWeight.Bold)
                                    Text("📍 Ubicación: $dialogLocation")
                                    Text("⏱️ Horario: $dialogClockInTime – $dialogClockOutTime")
                                    Text("🥗 Lonche: ${if (dialogHadLunch) "$dialogLunchMins mins" else "Sin lonche"}")
                                    Text("📝 Descripción: ${dialogDescription.ifEmpty { "(Sin comentario)" }}")
                                }
                            }
                        }
                    }
                }
            }

            // Wizard Step Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Atrás")
                    }
                }

                if (currentStep < 4) {
                    Button(
                        onClick = { currentStep++ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Siguiente")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                } else {
                    Button(
                        onClick = { saveAndExit() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(LanguageManager.getString("save_entry", lang))
                    }
                }
            }
        }
    }

    // Extended Native Material3 Clock-In TimePicker Dialog ⏰
    if (showClockInPicker) {
        Dialog(
            onDismissRequest = { showClockInPicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (lang == AppLanguage.SPANISH) "Seleccionar Hora de Entrada ⏰" else "Select Clock In Time ⏰",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TimePicker(state = clockInPickerState)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showClockInPicker = false }) {
                            Text(LanguageManager.getString("cancel", lang))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, clockInPickerState.hour)
                                set(Calendar.MINUTE, clockInPickerState.minute)
                            }
                            dialogClockInTime = SimpleDateFormat("h:mm a", Locale.US).format(cal.time)
                            showClockInPicker = false
                        }) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }

    // Extended Native Material3 Clock-Out TimePicker Dialog ⏰
    if (showClockOutPicker) {
        Dialog(
            onDismissRequest = { showClockOutPicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (lang == AppLanguage.SPANISH) "Seleccionar Hora de Salida ⏰" else "Select Clock Out Time ⏰",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TimePicker(state = clockOutPickerState)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showClockOutPicker = false }) {
                            Text(LanguageManager.getString("cancel", lang))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, clockOutPickerState.hour)
                                set(Calendar.MINUTE, clockOutPickerState.minute)
                            }
                            dialogClockOutTime = SimpleDateFormat("h:mm a", Locale.US).format(cal.time)
                            showClockOutPicker = false
                        }) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}
