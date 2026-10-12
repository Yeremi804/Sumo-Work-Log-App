package com.resu.sumo.util

import com.resu.sumo.data.WorkEntry
import java.util.Calendar
import java.util.regex.Pattern

object WorkLogParser {

    fun parseParagraphToEntries(paragraphText: String): List<WorkEntry> {
        if (paragraphText.isBlank()) return emptyList()

        val entries = mutableListOf<WorkEntry>()

        val dayPattern = Pattern.compile(
            "(?i)(lunes|martes|miércoles|miercoles|jueves|viernes|sábado|sabado|domingo|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\s*\\d*\\s*(de\\s*[a-z]+)?"
        )

        val matcher = dayPattern.matcher(paragraphText)
        val dayIndices = mutableListOf<Int>()

        while (matcher.find()) {
            dayIndices.add(matcher.start())
        }

        if (dayIndices.isEmpty()) {
            val single = parseSingleDayBlock(paragraphText)
            if (single != null) entries.add(single)
            return entries
        }

        for (i in dayIndices.indices) {
            val start = dayIndices[i]
            val end = if (i + 1 < dayIndices.size) dayIndices[i + 1] else paragraphText.length
            val block = paragraphText.substring(start, end).trim()
            val parsedEntry = parseSingleDayBlock(block)
            if (parsedEntry != null) {
                entries.add(parsedEntry)
            }
        }

        return entries.sortedBy { it.timestamp }
    }

    private fun parseSingleDayBlock(block: String): WorkEntry? {
        if (block.isBlank()) return null

        val lower = block.lowercase()

        // Extract Day Number (1-31)
        val dayMatch = Regex("\\b([1-9]|[12][0-9]|3[01])\\b").find(lower)
        val dayNum = dayMatch?.value?.toIntOrNull() ?: 1

        // Extract Month
        val monthsMap = mapOf(
            "enero" to 0, "ene" to 0, "january" to 0, "jan" to 0,
            "febrero" to 1, "feb" to 1, "february" to 1,
            "marzo" to 2, "march" to 2,
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

        var monthIdx = 9 // Default October
        var monthShortName = "oct"

        val monthMatch = Regex("(?i)\\b(octubre|oct|septiembre|sept|sep|noviembre|nov|diciembre|dic|enero|ene|febrero|feb|marzo|abril|abr|mayo|may|junio|jun|julio|jul|agosto|ago|january|jan|february|march|april|apr|june|july|august|aug|september|october|november|december|dec)\\b").find(lower)

        if (monthMatch != null) {
            val matchedWord = monthMatch.value.lowercase()
            monthIdx = monthsMap[matchedWord] ?: 9
            monthShortName = when (monthIdx) {
                0 -> "ene"
                1 -> "feb"
                2 -> "mar"
                3 -> "abr"
                4 -> "may"
                5 -> "jun"
                6 -> "jul"
                7 -> "ago"
                8 -> "sept"
                9 -> "oct"
                10 -> "nov"
                11 -> "dic"
                else -> "oct"
            }
        }

        val dateDisplay = "$dayNum de $monthShortName."

        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, monthIdx)
        cal.set(Calendar.DAY_OF_MONTH, dayNum)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val timestamp = cal.timeInMillis

        // Extract Explicit Clock In / Clock Out Times
        var inTime = "7:30 AM"
        var outTime = "3:30 PM"

        // Search for explicit Clock In (entré / entramos / entró / centró / in)
        val clockInMatch = Regex("(?i)(entré|entre|entramos|entró|entro|centró|centro|in)\\s*(a\\s*las)?\\s*(\\d{1,2}(?::\\d{2})?)").find(lower)
        if (clockInMatch != null) {
            val digits = clockInMatch.groupValues[3]
            inTime = formatClockIn(digits)
        }

        // Search for explicit Clock Out (salí / salimos / salió / out)
        val clockOutMatch = Regex("(?i)(salí|sali|salimos|salió|salio|out)\\s*(a\\s*las|a)?\\s*(\\d{1,2}(?::\\d{2})?)").find(lower)
        if (clockOutMatch != null) {
            val digits = clockOutMatch.groupValues[3]
            outTime = formatClockOut(digits)
        }

        // Extract Lunch
        var hadLunch = true
        var lunchMinutes = 30

        if (lower.contains("sin lonche") || lower.contains("no lonche") || lower.contains("no se agarró lonche") || lower.contains("no lunch") || lower.contains("no agarró lonche")) {
            hadLunch = false
            lunchMinutes = 0
        } else if (lower.contains("una hora") || lower.contains("1 hora") || lower.contains("one hour")) {
            hadLunch = true
            lunchMinutes = 60
        } else if (lower.contains("media hora") || lower.contains("30 min") || lower.contains("half hour")) {
            hadLunch = true
            lunchMinutes = 30
        }

        // Extract Location
        var location = "Propiedad General"
        if (lower.contains("patrón") || lower.contains("patron")) {
            location = "Casa del Patrón"
        } else if (lower.contains("fulton") || lower.contains("santa elena")) {
            location = "Fulton / Santa Elena"
        } else if (lower.contains("ranch") || lower.contains("big ranch") || lower.contains("v ranch")) {
            location = "V Ranch"
        }

        val description = block.trim()

        return WorkEntry(
            timestamp = timestamp,
            dateDisplay = dateDisplay,
            location = location,
            clockInTime = inTime,
            clockOutTime = outTime,
            hadLunch = hadLunch,
            lunchMinutes = lunchMinutes,
            description = description
        )
    }

    private fun formatClockIn(digits: String): String {
        val timeWithMin = if (digits.contains(":")) digits else "$digits:00"
        val parts = timeWithMin.split(":")
        val hour = parts[0].toIntOrNull() ?: 7
        return if (hour in 5..11) {
            "$timeWithMin AM"
        } else if (hour in 1..4) {
            "$timeWithMin PM"
        } else {
            "$timeWithMin AM"
        }
    }

    private fun formatClockOut(digits: String): String {
        val timeWithMin = if (digits.contains(":")) digits else "$digits:00"
        val parts = timeWithMin.split(":")
        val hour = parts[0].toIntOrNull() ?: 3
        return if (hour in 1..7 || hour == 12) {
            "$timeWithMin PM"
        } else if (hour in 8..11) {
            "$timeWithMin AM"
        } else {
            "$timeWithMin PM"
        }
    }
}
