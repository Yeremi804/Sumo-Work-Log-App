package com.resu.sumo.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class WorkLogStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sumo_work_log_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_WORKER_NAME = "key_worker_name"
        private const val KEY_ACTIVE_ENTRIES = "key_active_entries"
        private const val KEY_ACTIVE_PERIOD = "key_active_period"
        private const val KEY_HISTORY_WEEKS = "key_history_weeks"
        private const val KEY_CLOCK_IN_TIMESTAMP = "key_clock_in_timestamp"
        private const val KEY_CLOCK_IN_LOCATION = "key_clock_in_location"
    }

    fun saveWorkerName(name: String) {
        prefs.edit().putString(KEY_WORKER_NAME, name).apply()
        autoBackup()
    }

    fun getWorkerName(): String {
        return prefs.getString(KEY_WORKER_NAME, "") ?: ""
    }

    fun saveActiveClockIn(timestamp: Long, location: String) {
        prefs.edit()
            .putLong(KEY_CLOCK_IN_TIMESTAMP, timestamp)
            .putString(KEY_CLOCK_IN_LOCATION, location)
            .apply()
    }

    fun clearActiveClockIn() {
        prefs.edit()
            .remove(KEY_CLOCK_IN_TIMESTAMP)
            .remove(KEY_CLOCK_IN_LOCATION)
            .apply()
    }

    fun getActiveClockInTimestamp(): Long {
        return prefs.getLong(KEY_CLOCK_IN_TIMESTAMP, 0L)
    }

    fun getActiveClockInLocation(): String {
        return prefs.getString(KEY_CLOCK_IN_LOCATION, "") ?: ""
    }

    fun saveActivePeriod(period: String) {
        prefs.edit().putString(KEY_ACTIVE_PERIOD, period).apply()
        autoBackup()
    }

    fun getActivePeriod(): String {
        return prefs.getString(KEY_ACTIVE_PERIOD, "") ?: ""
    }

    fun saveActiveEntries(entries: List<WorkEntry>) {
        val sorted = entries.sortedBy { it.timestamp }
        val array = JSONArray()
        sorted.forEach { entry ->
            array.put(entryToJson(entry))
        }
        prefs.edit().putString(KEY_ACTIVE_ENTRIES, array.toString()).apply()
        autoBackup()
    }

    fun getActiveEntries(): List<WorkEntry> {
        val jsonStr = prefs.getString(KEY_ACTIVE_ENTRIES, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<WorkEntry>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(jsonToEntry(obj))
            }
            list.sortedBy { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveWeekToHistory(week: WeeklyLog) {
        val history = getHistoryWeeks().toMutableList()
        val index = history.indexOfFirst { it.id == week.id }
        if (index >= 0) {
            history[index] = week
        } else {
            history.add(0, week)
        }

        val array = JSONArray()
        history.forEach { w ->
            array.put(weeklyLogToJson(w))
        }
        prefs.edit().putString(KEY_HISTORY_WEEKS, array.toString()).apply()
        autoBackup()
    }

    fun getHistoryWeeks(): List<WeeklyLog> {
        val jsonStr = prefs.getString(KEY_HISTORY_WEEKS, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<WeeklyLog>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(jsonToWeeklyLog(obj))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteWeekFromHistory(weekId: String) {
        val history = getHistoryWeeks().filterNot { it.id == weekId }
        val array = JSONArray()
        history.forEach { w ->
            array.put(weeklyLogToJson(w))
        }
        prefs.edit().putString(KEY_HISTORY_WEEKS, array.toString()).apply()
        autoBackup()
    }

    private fun autoBackup() {
        try {
            exportBackupToDownloads()
        } catch (e: Exception) {
            // Ignore background auto-backup exceptions
        }
    }

    fun exportBackupToDownloads(): File? {
        return try {
            val backupObj = JSONObject().apply {
                put("workerName", getWorkerName())
                put("activePeriod", getActivePeriod())
                put("activeEntries", JSONArray().apply {
                    getActiveEntries().forEach { put(entryToJson(it)) }
                })
                put("historyWeeks", JSONArray().apply {
                    getHistoryWeeks().forEach { put(weeklyLogToJson(it)) }
                })
            }

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val backupFile = File(downloadsDir, "Sumo_WorkLog_Backup.json")
            FileOutputStream(backupFile).use { out ->
                out.write(backupObj.toString(2).toByteArray())
            }
            backupFile
        } catch (e: Exception) {
            null
        }
    }

    fun restoreBackupFromDownloads(): Boolean {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val backupFile = File(downloadsDir, "Sumo_WorkLog_Backup.json")
            if (!backupFile.exists()) return false

            val jsonStr = backupFile.readText()
            val backupObj = JSONObject(jsonStr)

            val worker = backupObj.optString("workerName", "")
            if (worker.isNotEmpty()) saveWorkerName(worker)

            val period = backupObj.optString("activePeriod", "")
            if (period.isNotEmpty()) saveActivePeriod(period)

            val activeArr = backupObj.optJSONArray("activeEntries")
            if (activeArr != null) {
                val list = mutableListOf<WorkEntry>()
                for (i in 0 until activeArr.length()) {
                    list.add(jsonToEntry(activeArr.getJSONObject(i)))
                }
                saveActiveEntries(list)
            }

            val historyArr = backupObj.optJSONArray("historyWeeks")
            if (historyArr != null) {
                val list = mutableListOf<WeeklyLog>()
                for (i in 0 until historyArr.length()) {
                    list.add(jsonToWeeklyLog(historyArr.getJSONObject(i)))
                }
                val array = JSONArray()
                list.forEach { array.put(weeklyLogToJson(it)) }
                prefs.edit().putString(KEY_HISTORY_WEEKS, array.toString()).apply()
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    private fun entryToJson(entry: WorkEntry): JSONObject {
        return JSONObject().apply {
            put("id", entry.id)
            put("weekId", entry.weekId)
            put("timestamp", entry.timestamp)
            put("dateDisplay", entry.dateDisplay)
            put("location", entry.location)
            put("clockInTime", entry.clockInTime)
            put("clockOutTime", entry.clockOutTime)
            put("hadLunch", entry.hadLunch)
            put("lunchMinutes", entry.lunchMinutes)
            put("description", entry.description)
            entry.grossMinutesOverride?.let { put("grossMinutesOverride", it) }
        }
    }

    private fun jsonToEntry(obj: JSONObject): WorkEntry {
        return WorkEntry(
            id = obj.optString("id"),
            weekId = obj.optString("weekId"),
            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
            dateDisplay = obj.optString("dateDisplay"),
            location = obj.optString("location"),
            clockInTime = obj.optString("clockInTime"),
            clockOutTime = obj.optString("clockOutTime"),
            hadLunch = obj.optBoolean("hadLunch", true),
            lunchMinutes = obj.optInt("lunchMinutes", 30),
            description = obj.optString("description"),
            grossMinutesOverride = if (obj.has("grossMinutesOverride")) obj.getInt("grossMinutesOverride") else null
        )
    }

    private fun weeklyLogToJson(week: WeeklyLog): JSONObject {
        val entriesArray = JSONArray()
        week.entries.sortedBy { it.timestamp }.forEach {
            entriesArray.put(entryToJson(it))
        }
        return JSONObject().apply {
            put("id", week.id)
            put("workerName", week.workerName)
            put("periodTitle", week.periodTitle)
            put("createdTimestamp", week.createdTimestamp)
            put("isCompleted", week.isCompleted)
            put("entries", entriesArray)
        }
    }

    private fun jsonToWeeklyLog(obj: JSONObject): WeeklyLog {
        val entriesArray = obj.optJSONArray("entries") ?: JSONArray()
        val entriesList = mutableListOf<WorkEntry>()
        for (i in 0 until entriesArray.length()) {
            entriesList.add(jsonToEntry(entriesArray.getJSONObject(i)))
        }
        return WeeklyLog(
            id = obj.optString("id"),
            workerName = obj.optString("workerName"),
            periodTitle = obj.optString("periodTitle"),
            createdTimestamp = obj.optLong("createdTimestamp"),
            entries = entriesList.sortedBy { it.timestamp },
            isCompleted = obj.optBoolean("isCompleted")
        )
    }
}
