package com.resu.sumo.data

import java.util.Calendar
import java.util.Locale
import java.util.UUID

data class WorkEntry(
    val id: String = UUID.randomUUID().toString(),
    val weekId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val dateDisplay: String = "",
    val location: String = "",
    val clockInTime: String = "",
    val clockOutTime: String = "",
    val hadLunch: Boolean = true,
    val lunchMinutes: Int = 30,
    val description: String = "",
    val grossMinutesOverride: Int? = null
) {
    val calculatedGrossMinutes: Int
        get() {
            if (grossMinutesOverride != null) return grossMinutesOverride
            return parseAndCalculateGrossMinutes(clockInTime, clockOutTime)
        }

    val calculatedLunchMinutes: Int
        get() = if (hadLunch && lunchMinutes > 0) lunchMinutes else 0

    val calculatedNetMinutes: Int
        get() = (calculatedGrossMinutes - calculatedLunchMinutes).coerceAtLeast(0)

    val grossFormatted: String
        get() = formatMinutesToHoursMinutes(calculatedGrossMinutes) + " hrs"

    val lunchFormatted: String
        get() = if (hadLunch && lunchMinutes > 0) {
            formatMinutesToHoursMinutes(lunchMinutes)
        } else {
            "Sin lonche"
        }

    val netFormatted: String
        get() = formatMinutesToHoursMinutes(calculatedNetMinutes) + " hrs"

    // Backward compatibility getters for decimal
    val calculatedGrossHours: Double
        get() = Math.round((calculatedGrossMinutes / 60.0) * 100.0) / 100.0

    val calculatedNetHours: Double
        get() = Math.round((calculatedNetMinutes / 60.0) * 100.0) / 100.0

    companion object {
        fun formatMinutesToHoursMinutes(totalMins: Int): String {
            val hrs = totalMins / 60
            val mins = Math.abs(totalMins % 60)
            return String.format(Locale.US, "%d:%02d", hrs, mins)
        }

        fun parseAndCalculateGrossMinutes(inTime: String, outTime: String): Int {
            val inMins = parseTimeToMinutes(inTime) ?: return 0
            val outMins = parseTimeToMinutes(outTime) ?: return 0
            return if (outMins >= inMins) outMins - inMins else (outMins + 24 * 60) - inMins
        }

        private fun parseTimeToMinutes(timeStr: String): Int? {
            val clean = timeStr.trim().uppercase()
            if (clean.isEmpty()) return null
            val isPm = clean.contains("PM")
            val isAm = clean.contains("AM")
            val digitsOnly = clean.replace("AM", "").replace("PM", "").trim()
            val parts = digitsOnly.split(":")
            if (parts.isEmpty()) return null
            var hour = parts[0].trim().toIntOrNull() ?: return null
            val min = if (parts.size > 1) parts[1].trim().toIntOrNull() ?: 0 else 0

            if (isPm && hour < 12) hour += 12
            if (isAm && hour == 12) hour = 0

            return hour * 60 + min
        }

        fun parseDateDisplayToTimestamp(dateStr: String): Long {
            if (dateStr.isBlank()) return System.currentTimeMillis()
            val clean = dateStr.trim().lowercase(Locale.US)

            val monthsMap = mapOf(
                "enero" to 0, "ene" to 0, "january" to 0, "jan" to 0,
                "febrero" to 1, "feb" to 1, "february" to 1,
                "marzo" to 2, "mar" to 2, "march" to 2,
                "abril" to 3, "abr" to 3, "april" to 3, "apr" to 3,
                "mayo" to 4, "may" to 4,
                "junio" to 5, "jun" to 5, "june" to 5,
                "julio" to 6, "jul" to 6, "july" to 6,
                "agosto" to 7, "ago" to 7, "august" to 7, "aug" to 7,
                "septiembre" to 8, "sept" to 8, "sep" to 8, "september" to 8,
                "octubre" to 9, "oct" to 9, "october" to 9,
                "noviembre" to 10, "nov" to 10, "november" to 10,
                "diciembre" to 11, "dic" to 11, "december" to 11, "dec" to 11
            )

            var foundMonth: Int? = null
            for ((key, idx) in monthsMap) {
                if (clean.contains(key)) {
                    foundMonth = idx
                    break
                }
            }

            val dayMatch = Regex("\\b([1-9]|[12][0-9]|3[01])\\b").find(clean)
            val foundDay = dayMatch?.value?.toIntOrNull()

            if (foundMonth != null && foundDay != null) {
                val cal = Calendar.getInstance()
                cal.set(Calendar.MONTH, foundMonth)
                cal.set(Calendar.DAY_OF_MONTH, foundDay)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                return cal.timeInMillis
            }

            return System.currentTimeMillis()
        }
    }
}

data class WeeklyLog(
    val id: String = UUID.randomUUID().toString(),
    val workerName: String = "",
    val periodTitle: String = "",
    val createdTimestamp: Long = System.currentTimeMillis(),
    val entries: List<WorkEntry> = emptyList(),
    val isCompleted: Boolean = false
) {
    val totalPropertyMinutes: Int
        get() = entries.sumOf { it.calculatedGrossMinutes }

    val totalLunchMinutes: Int
        get() = entries.sumOf { it.calculatedLunchMinutes }

    val totalNetMinutes: Int
        get() = entries.sumOf { it.calculatedNetMinutes }

    val totalPropertyFormatted: String
        get() = WorkEntry.formatMinutesToHoursMinutes(totalPropertyMinutes) + " hrs"

    val totalLunchFormatted: String
        get() = WorkEntry.formatMinutesToHoursMinutes(totalLunchMinutes) + " hrs"

    val totalNetFormatted: String
        get() = WorkEntry.formatMinutesToHoursMinutes(totalNetMinutes) + " hrs"

    val totalPropertyHours: Double
        get() = Math.round((totalPropertyMinutes / 60.0) * 100.0) / 100.0

    val totalLunchHours: Double
        get() = Math.round((totalLunchMinutes / 60.0) * 100.0) / 100.0

    val totalNetHours: Double
        get() = Math.round((totalNetMinutes / 60.0) * 100.0) / 100.0
}
