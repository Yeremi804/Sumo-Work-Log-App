package com.resu.sumo.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resu.sumo.data.WeeklyLog
import com.resu.sumo.data.WorkEntry
import com.resu.sumo.util.PdfExporter
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPreviewScreen(
    viewModel: WorkLogViewModel,
    onNavigateToOptionB: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lang = viewModel.currentLanguage
    val scrollState = rememberScrollState()

    var useColorPdf by remember { mutableStateOf(true) }
    var showCycleDialog by remember { mutableStateOf(false) }

    // Download & Progress Dialog State
    var showDownloadProgressDialog by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }

    val activeLog = WeeklyLog(
        workerName = viewModel.workerName,
        periodTitle = viewModel.periodTitle,
        entries = viewModel.activeEntries
    )

    fun openFileInNativeViewer(file: File) {
        PdfExporter.openPdfFile(context, file)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LanguageManager.getString("option_c_title", lang)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Edit Report Navigation Button
            Button(
                onClick = onNavigateToOptionB,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = LanguageManager.getString("edit_report_b", lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Color vs B&W Selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { useColorPdf = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (useColorPdf) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (useColorPdf) Color.White else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.ColorLens, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(LanguageManager.getString("pdf_color_mode", lang))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = { useColorPdf = false },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (!useColorPdf) Color.DarkGray else Color.Transparent,
                            contentColor = if (!useColorPdf) Color.White else Color.DarkGray
                        )
                    ) {
                        Text(LanguageManager.getString("pdf_bw_mode", lang))
                    }
                }
            }

            // Actual Document Sheet Representation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Document Banner Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (useColorPdf) Color(0xFF1E3A8A) else Color.DarkGray,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = LanguageManager.getString("doc_main_title", lang),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = LanguageManager.getString("doc_subtitle", lang),
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${LanguageManager.getString("worker_name_label", lang)} ${activeLog.workerName}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                        Text(
                            text = "${LanguageManager.getString("period_label", lang)} ${activeLog.periodTitle}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                    }

                    // Metrics Banner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (useColorPdf) Color(0xFFDBEAFE) else Color(0xFFF1F5F9))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(LanguageManager.getString("total_property_time", lang), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                Text(activeLog.totalPropertyFormatted, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.Black)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (useColorPdf) Color(0xFFDBEAFE) else Color(0xFFF1F5F9))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(LanguageManager.getString("lunch_deducted", lang), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                Text(activeLog.totalLunchFormatted, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.Black)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(if (useColorPdf) Color(0xFFD1FAE5) else Color(0xFFE2E8F0))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(LanguageManager.getString("total_hours_worked", lang), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                Text(activeLog.totalNetFormatted, fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (useColorPdf) Color(0xFF059669) else Color.Black)
                            }
                        }
                    }

                    // Policy Note
                    Text(
                        text = LanguageManager.getString("policy_note", lang),
                        fontSize = 9.sp,
                        color = Color.Gray
                    )

                    // Table Header Preview
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (useColorPdf) Color(0xFF0284C7) else Color.DarkGray)
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Text(LanguageManager.getString("header_date", lang), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(LanguageManager.getString("header_location", lang), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                        Text(LanguageManager.getString("header_time", lang), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.3f))
                        Text(LanguageManager.getString("header_lunch", lang), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                        Text(LanguageManager.getString("header_hours", lang), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
                        Text(LanguageManager.getString("header_description", lang), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                    }

                    // Table Rows Preview
                    activeLog.entries.sortedBy { it.timestamp }.forEachIndexed { idx, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (idx % 2 == 1) Color(0xFFF8FAFC) else Color.White)
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text(entry.dateDisplay, fontSize = 8.sp, color = Color.Black, modifier = Modifier.weight(1f))
                            Text(entry.location, fontSize = 8.sp, color = Color.Black, modifier = Modifier.weight(1.2f))
                            Text("${entry.clockInTime}–${entry.clockOutTime}", fontSize = 8.sp, color = Color.Black, modifier = Modifier.weight(1.3f))
                            Text(entry.lunchFormatted, fontSize = 8.sp, color = Color.Black, modifier = Modifier.weight(0.9f))
                            Text(WorkEntry.formatMinutesToHoursMinutes(entry.calculatedNetMinutes), fontSize = 8.sp, color = Color.Black, modifier = Modifier.weight(0.8f))
                            Text(entry.description, fontSize = 8.sp, color = Color.Black, modifier = Modifier.weight(2f))
                        }
                    }
                }
            }

            // Export Actions Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            downloadProgress = 0f
                            downloadedFile = null
                            showDownloadProgressDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(LanguageManager.getString("download_pdf", lang))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.shareReportText(context)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(LanguageManager.getString("share_report", lang))
                        }

                        Button(
                            onClick = { showCycleDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(LanguageManager.getString("end_week_cycle", lang))
                        }
                    }
                }
            }
        }
    }

    // Download Real-Time Progress & Open File Dialog
    if (showDownloadProgressDialog) {
        LaunchedEffect(Unit) {
            downloadProgress = 0.2f
            delay(300)
            downloadProgress = 0.6f
            delay(300)
            val file = viewModel.generatePdfReport(context, useColorPdf)
            downloadProgress = 1.0f
            downloadedFile = file
        }

        AlertDialog(
            onDismissRequest = { showDownloadProgressDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (downloadProgress < 1.0f) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(if (lang == AppLanguage.SPANISH) "Generando PDF..." else "Generating PDF...")
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(if (lang == AppLanguage.SPANISH) "¡Descarga Completa!" else "Download Complete!")
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (downloadProgress < 1.0f) {
                        Text(if (lang == AppLanguage.SPANISH) "Compilando reporte y guardando en Descargas..." else "Compiling report and saving to Downloads...")
                    } else {
                        Text(
                            text = if (lang == AppLanguage.SPANISH)
                                "El informe PDF fue guardado exitosamente en la carpeta de Descargas:\n\n📄 ${downloadedFile?.name}"
                            else
                                "PDF report successfully saved to Downloads folder:\n\n📄 ${downloadedFile?.name}"
                        )
                    }
                }
            },
            confirmButton = {
                if (downloadProgress >= 1.0f && downloadedFile != null) {
                    Button(onClick = {
                        openFileInNativeViewer(downloadedFile!!)
                        showDownloadProgressDialog = false
                    }) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (lang == AppLanguage.SPANISH) "Abrir Archivo PDF" else "Open PDF File")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadProgressDialog = false }) {
                    Text(if (downloadProgress >= 1.0f) (if (lang == AppLanguage.SPANISH) "Salida" else "Exit") else LanguageManager.getString("cancel", lang))
                }
            }
        )
    }

    // Clean Stacked Cycle Management Dialog (Fixing button squishing!)
    if (showCycleDialog) {
        AlertDialog(
            onDismissRequest = { showCycleDialog = false },
            title = { Text(LanguageManager.getString("cycle_dialog_title", lang)) },
            text = { Text(LanguageManager.getString("cycle_dialog_msg", lang)) },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.saveCurrentWeekToDatabase()
                            Toast.makeText(context, LanguageManager.getString("saved_to_db_msg", lang), Toast.LENGTH_SHORT).show()
                            showCycleDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(LanguageManager.getString("save_and_resume", lang))
                    }

                    OutlinedButton(
                        onClick = { showCycleDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(LanguageManager.getString("edit_entries", lang))
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.resetForNextWeek()
                            Toast.makeText(context, LanguageManager.getString("reset_week_msg", lang), Toast.LENGTH_SHORT).show()
                            showCycleDialog = false
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(LanguageManager.getString("restart_next_week", lang))
                    }
                }
            }
        )
    }
}
