package com.resu.sumo.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.resu.sumo.Screen
import com.resu.sumo.data.WorkEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryFormScreen(
    viewModel: WorkLogViewModel,
    onBack: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val lang = viewModel.currentLanguage
    val scrollState = rememberScrollState()

    val editingEntry = viewModel.editingEntryInForm

    // Key state on editingEntry so Entrada and Salida refresh when editing any record!
    var dialogDateDisplay by remember(editingEntry) {
        mutableStateOf(
            if (!editingEntry?.dateDisplay.isNullOrBlank()) editingEntry!!.dateDisplay
            else viewModel.getFormattedCurrentDate()
        )
    }
    var dialogLocation by remember(editingEntry) {
        mutableStateOf(
            if (!editingEntry?.location.isNullOrBlank()) editingEntry!!.location
            else viewModel.clockInLocation.ifEmpty { "Casa del Patrón" }
        )
    }
    var dialogClockInTime by remember(editingEntry) {
        mutableStateOf(
            if (!editingEntry?.clockInTime.isNullOrBlank()) editingEntry!!.clockInTime
            else viewModel.getFormattedClockInTime()
        )
    }
    var dialogClockOutTime by remember(editingEntry) {
        mutableStateOf(
            if (!editingEntry?.clockOutTime.isNullOrBlank()) editingEntry!!.clockOutTime
            else viewModel.getFormattedCurrentTime()
        )
    }
    var dialogHadLunch by remember(editingEntry) { mutableStateOf(editingEntry?.hadLunch ?: true) }
    var dialogLunchMins by remember(editingEntry) { mutableStateOf(editingEntry?.lunchMinutes?.toString() ?: "30") }
    var dialogDescription by remember(editingEntry) { mutableStateOf(editingEntry?.description ?: "") }

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
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { saveAndExit() },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(LanguageManager.getString("save_entry", lang))
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
                .verticalScroll(scrollState)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dialogClockInTime,
                            onValueChange = { dialogClockInTime = it },
                            label = { Text(if (lang == AppLanguage.SPANISH) "Entrada" else "In") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); focusManager.clearFocus() }),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dialogClockOutTime,
                            onValueChange = { dialogClockOutTime = it },
                            label = { Text(if (lang == AppLanguage.SPANISH) "Salida" else "Out") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); focusManager.clearFocus() }),
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
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); focusManager.clearFocus() }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = dialogDescription,
                        onValueChange = { dialogDescription = it },
                        label = { Text(LanguageManager.getString("description_label", lang)) },
                        placeholder = { Text(LanguageManager.getString("voice_input_hint", lang)) },
                        singleLine = false,
                        maxLines = 6,
                        trailingIcon = {
                            IconButton(onClick = { launchVoiceInput() }) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); focusManager.clearFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 120.dp)
                    )
                }
            }

            // Bottom Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        onBack()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(LanguageManager.getString("cancel", lang))
                }

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
