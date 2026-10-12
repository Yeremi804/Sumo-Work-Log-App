package com.resu.sumo.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.resu.sumo.Screen
import com.resu.sumo.data.WeeklyLog
import com.resu.sumo.data.WorkEntry
import com.resu.sumo.data.WorkLogStorage
import com.resu.sumo.util.GitHubReleaseInfo
import com.resu.sumo.util.GitHubUpdateChecker
import com.resu.sumo.util.PdfExporter
import com.resu.sumo.util.WorkLogParser
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WorkLogViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = WorkLogStorage(application)

    var currentScreen by mutableStateOf(Screen.HOME)

    var currentLanguage by mutableStateOf(AppLanguage.SPANISH)
        private set

    var workerName by mutableStateOf(storage.getWorkerName().ifEmpty { "Trabajador" })
        private set

    var periodTitle by mutableStateOf("21 Sept – 2 Oct")
        private set

    var activeEntries by mutableStateOf<List<WorkEntry>>(emptyList())
        private set

    var historyWeeks by mutableStateOf<List<WeeklyLog>>(emptyList())
        private set

    var isClockedIn by mutableStateOf(false)
        private set

    var clockInTimestamp by mutableStateOf(0L)
        private set

    var clockInLocation by mutableStateOf("")
        private set

    var exportedPdfFile by mutableStateOf<File?>(null)
        private set

    var editingEntryInForm by mutableStateOf<WorkEntry?>(null)

    var githubUpdateInfo by mutableStateOf<GitHubReleaseInfo?>(null)

    var showUpdateDialog by mutableStateOf(false)

    var isCheckingUpdates by mutableStateOf(false)

    var updateToastMessage by mutableStateOf<String?>(null)

    init {
        loadData()
        checkGitHubUpdates(manualClick = false)
    }

    fun openEntryForm(entry: WorkEntry? = null) {
        editingEntryInForm = entry
        currentScreen = Screen.ENTRY_FORM
    }

    fun checkGitHubUpdates(manualClick: Boolean = false) {
        viewModelScope.launch {
            isCheckingUpdates = true
            val info = GitHubUpdateChecker.checkForUpdates()
            isCheckingUpdates = false
            if (info != null && info.isUpdateAvailable) {
                githubUpdateInfo = info
                showUpdateDialog = true
            } else if (manualClick) {
                updateToastMessage = if (currentLanguage == AppLanguage.SPANISH)
                    "¡Tu app está en la versión más reciente! (${GitHubUpdateChecker.CURRENT_APP_VERSION})"
                else
                    "Your app is up to date! (${GitHubUpdateChecker.CURRENT_APP_VERSION})"
            }
        }
    }

    fun dismissUpdateDialog() {
        showUpdateDialog = false
    }

    fun clearUpdateToast() {
        updateToastMessage = null
    }

    private fun loadData() {
        activeEntries = storage.getActiveEntries()
        historyWeeks = storage.getHistoryWeeks()

        val ts = storage.getActiveClockInTimestamp()
        val loc = storage.getActiveClockInLocation()
        val now = System.currentTimeMillis()

        // Check if user forgot to clock out from a previous day (> 16 hours ago)
        if (ts > 0 && (now - ts) > (16 * 3600 * 1000L)) {
            // Auto-close previous day's shift as Pending/Unknown
            val dateDisp = formatDateDisplay(ts)
            val pendingEntry = WorkEntry(
                timestamp = ts,
                dateDisplay = dateDisp,
                location = loc.ifEmpty { "V Ranch" },
                clockInTime = formatTime(ts),
                clockOutTime = if (currentLanguage == AppLanguage.SPANISH) "Pendiente" else "Unknown",
                hadLunch = true,
                lunchMinutes = 30,
                description = if (currentLanguage == AppLanguage.SPANISH) "⚠️ Olvidó marcar salida el día anterior" else "⚠️ Forgot to clock out on previous day"
            )
            addOrUpdateEntry(pendingEntry)
            storage.clearActiveClockIn()
            isClockedIn = false
            clockInTimestamp = 0L
            clockInLocation = ""
        } else if (ts > 0) {
            isClockedIn = true
            clockInTimestamp = ts
            clockInLocation = loc
        }

        updateDynamicPeriodTitle()
    }

    private fun updateDynamicPeriodTitle() {
        if (activeEntries.isEmpty()) {
            periodTitle = LanguageManager.getString("no_entries", currentLanguage)
            return
        }
        val sorted = activeEntries.sortedBy { it.timestamp }
        val first = sorted.first()
        val last = sorted.last()

        periodTitle = if (first.id == last.id) {
            first.dateDisplay
        } else {
            "${first.dateDisplay} – ${last.dateDisplay}"
        }
    }

    fun toggleLanguage() {
        currentLanguage = if (currentLanguage == AppLanguage.SPANISH) AppLanguage.ENGLISH else AppLanguage.SPANISH
        LanguageManager.currentLanguage = currentLanguage
        updateDynamicPeriodTitle()
    }

    fun updateWorkerName(name: String) {
        workerName = name
        storage.saveWorkerName(name)
    }

    fun updatePeriodTitle(period: String) {
        periodTitle = period
        storage.saveActivePeriod(period)
    }

    fun startClockIn(location: String = "V Ranch") {
        val now = System.currentTimeMillis()
        isClockedIn = true
        clockInTimestamp = now
        clockInLocation = location
        storage.saveActiveClockIn(now, location)
    }

    fun finishClockOut(
        hadLunch: Boolean,
        lunchMinutes: Int,
        description: String,
        customClockInTime: String? = null,
        customClockOutTime: String? = null,
        customDateDisplay: String? = null
    ) {
        val now = System.currentTimeMillis()
        val inTimeStr = customClockInTime ?: formatTime(clockInTimestamp)
        val outTimeStr = customClockOutTime ?: formatTime(now)
        val dateDisp = customDateDisplay ?: formatDateDisplay(clockInTimestamp)

        val entry = WorkEntry(
            timestamp = if (clockInTimestamp > 0) clockInTimestamp else now,
            dateDisplay = dateDisp,
            location = clockInLocation.ifEmpty { "V Ranch" },
            clockInTime = inTimeStr,
            clockOutTime = outTimeStr,
            hadLunch = hadLunch,
            lunchMinutes = lunchMinutes,
            description = description
        )

        addOrUpdateEntry(entry)

        isClockedIn = false
        clockInTimestamp = 0L
        clockInLocation = ""
        storage.clearActiveClockIn()
    }

    fun parseAndImportParagraphText(textBlock: String): Int {
        val parsedList = WorkLogParser.parseParagraphToEntries(textBlock)
        if (parsedList.isNotEmpty()) {
            val current = activeEntries.toMutableList()
            parsedList.forEach { newEntry ->
                current.add(newEntry)
            }
            val sorted = current.sortedBy { it.timestamp }
            activeEntries = sorted
            storage.saveActiveEntries(sorted)
            updateDynamicPeriodTitle()
        }
        return parsedList.size
    }

    fun addOrUpdateEntry(entry: WorkEntry) {
        val updatedTimestamp = WorkEntry.parseDateDisplayToTimestamp(entry.dateDisplay)
        val fixedEntry = entry.copy(timestamp = updatedTimestamp)

        val current = activeEntries.toMutableList()
        val index = current.indexOfFirst { it.id == fixedEntry.id }
        if (index >= 0) {
            current[index] = fixedEntry
        } else {
            current.add(fixedEntry)
        }
        val sorted = current.sortedBy { it.timestamp }
        activeEntries = sorted
        storage.saveActiveEntries(sorted)
        updateDynamicPeriodTitle()
    }

    fun deleteEntry(entryId: String) {
        val updated = activeEntries.filterNot { it.id == entryId }.sortedBy { it.timestamp }
        activeEntries = updated
        storage.saveActiveEntries(updated)
        updateDynamicPeriodTitle()
    }

    fun saveCurrentWeekToDatabase() {
        val currentLog = WeeklyLog(
            workerName = workerName,
            periodTitle = periodTitle,
            entries = activeEntries
        )
        storage.saveWeekToHistory(currentLog)
        historyWeeks = storage.getHistoryWeeks()
    }

    fun deleteWeekFromHistory(weekId: String) {
        storage.deleteWeekFromHistory(weekId)
        historyWeeks = storage.getHistoryWeeks()
    }

    fun resetForNextWeek() {
        saveCurrentWeekToDatabase()
        activeEntries = emptyList()
        storage.saveActiveEntries(emptyList())
        updateDynamicPeriodTitle()
    }

    fun generatePdfReport(context: Context, useColor: Boolean = true, customFileName: String? = null): File? {
        val currentLog = WeeklyLog(
            workerName = workerName,
            periodTitle = periodTitle,
            entries = activeEntries
        )
        val file = PdfExporter.generatePdfReport(context, currentLog, currentLanguage, useColor, customFileName)
        exportedPdfFile = file
        return file
    }

    fun shareReportText(context: Context) {
        val currentLog = WeeklyLog(
            workerName = workerName,
            periodTitle = periodTitle,
            entries = activeEntries
        )
        PdfExporter.shareReportText(context, currentLog)
    }

    fun generateCsvReport(): String {
        val currentLog = WeeklyLog(
            workerName = workerName,
            periodTitle = periodTitle,
            entries = activeEntries
        )
        return PdfExporter.generateCsvReport(currentLog)
    }

    fun getFormattedCurrentDate(): String {
        return SimpleDateFormat("d 'de' MMM.", Locale("es", "ES")).format(Date())
    }

    fun getFormattedCurrentTime(): String {
        return SimpleDateFormat("h:mm a", Locale.US).format(Date())
    }

    fun getFormattedClockInTime(): String {
        return if (clockInTimestamp > 0) {
            SimpleDateFormat("h:mm a", Locale.US).format(Date(clockInTimestamp))
        } else {
            getFormattedCurrentTime()
        }
    }

    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("h:mm a", Locale.US).format(Date(timestamp))
    }

    private fun formatDateDisplay(timestamp: Long): String {
        return SimpleDateFormat("d 'de' MMM.", Locale("es", "ES")).format(Date(timestamp))
    }
}
