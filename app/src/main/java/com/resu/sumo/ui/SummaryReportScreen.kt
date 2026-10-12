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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resu.sumo.data.WeeklyLog
import com.resu.sumo.data.WorkEntry
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryReportScreen(
    viewModel: WorkLogViewModel,
    onNavigateToOptionC: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val lang = viewModel.currentLanguage

    var showEditDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<WorkEntry?>(null) }

    // Dialog state
    var dialogLocation by remember { mutableStateOf("V Ranch") }
    var dialogDateDisplay by remember { mutableStateOf("") }
    var dialogClockInTime by remember { mutableStateOf("7:30 AM") }
    var dialogClockOutTime by remember { mutableStateOf("5:00 PM") }
    var dialogHadLunch by remember { mutableStateOf(true) }
    var dialogLunchMins by remember { mutableStateOf("30") }
    var dialogDescription by remember { mutableStateOf("") }

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

    val activeLog = WeeklyLog(
        workerName = viewModel.workerName,
        periodTitle = viewModel.periodTitle,
        entries = viewModel.activeEntries
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LanguageManager.getString("option_b_title", lang)) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Navigation Button to Option C Preview & Export
            item {
                Button(
                    onClick = onNavigateToOptionC,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = LanguageManager.getString("go_to_preview_c", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Header Info & Worker Name Input
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = LanguageManager.getString("doc_main_title", lang),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = LanguageManager.getString("doc_subtitle", lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = viewModel.workerName,
                                onValueChange = { viewModel.updateWorkerName(it) },
                                label = { Text(LanguageManager.getString("worker_name_label", lang)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); focusManager.clearFocus() }),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = viewModel.periodTitle,
                                onValueChange = { viewModel.updatePeriodTitle(it) },
                                label = { Text(LanguageManager.getString("period_label", lang)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); focusManager.clearFocus() }),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Metrics Row Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        title = LanguageManager.getString("total_property_time", lang),
                        value = activeLog.totalPropertyFormatted,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = LanguageManager.getString("lunch_deducted", lang),
                        value = activeLog.totalLunchFormatted,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = LanguageManager.getString("total_hours_worked", lang),
                        value = activeLog.totalNetFormatted,
                        modifier = Modifier.weight(1f),
                        isHighlight = true
                    )
                }
            }

            // Policy Note Card
            item {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = LanguageManager.getString("policy_note", lang),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Entry List Header & Add Manual Past Entry Button (Full width on own line to avoid squishing!)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (lang == AppLanguage.SPANISH) "Tabla de Registros de Trabajo:" else "Work Log Data Table:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = {
                            viewModel.openEntryForm(null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(LanguageManager.getString("add_entry", lang))
                    }
                }
            }

            // Entries List
            items(viewModel.activeEntries) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${entry.dateDisplay} — ${entry.location}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row {
                                IconButton(onClick = {
                                    viewModel.openEntryForm(entry)
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { viewModel.deleteEntry(entry.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Text(
                            text = "${LanguageManager.getString("header_time", lang)}: ${entry.clockInTime} – ${entry.clockOutTime} | ${LanguageManager.getString("header_lunch", lang)}: ${entry.lunchFormatted} | Total: ${entry.netFormatted}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (entry.description.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${LanguageManager.getString("header_description", lang)}: ${entry.description}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Saved Weekly Database History Section
            if (viewModel.historyWeeks.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = LanguageManager.getString("historical_weeks", lang),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(viewModel.historyWeeks) { week ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${week.workerName} (${week.periodTitle})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Total Horas: ${week.totalNetFormatted} (${week.entries.size} días)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { viewModel.deleteWeekFromHistory(week.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Week", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Entry Dialog (Auto-Sorts Chronologically + Voice Input 🎙️)
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    if (editingEntry == null) LanguageManager.getString("add_entry", lang)
                    else LanguageManager.getString("edit_entry", lang)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        singleLine = false,
                        maxLines = 4,
                        trailingIcon = {
                            IconButton(onClick = { launchVoiceInput() }) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide(); focusManager.clearFocus() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    val lunchM = dialogLunchMins.toIntOrNull() ?: 30
                    val newEntry = (editingEntry ?: WorkEntry()).copy(
                        dateDisplay = dialogDateDisplay.ifEmpty { "21 de sept." },
                        location = dialogLocation.ifEmpty { "V Ranch" },
                        clockInTime = dialogClockInTime,
                        clockOutTime = dialogClockOutTime,
                        hadLunch = dialogHadLunch,
                        lunchMinutes = lunchM,
                        description = dialogDescription
                    )
                    viewModel.addOrUpdateEntry(newEntry)
                    showEditDialog = false
                }) {
                    Text(LanguageManager.getString("save_entry", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    showEditDialog = false
                }) {
                    Text(LanguageManager.getString("cancel", lang))
                }
            }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    isHighlight: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
