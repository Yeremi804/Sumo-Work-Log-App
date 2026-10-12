package com.resu.sumo

import com.resu.sumo.data.WeeklyLog
import com.resu.sumo.data.WorkEntry
import com.resu.sumo.util.WorkLogParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkLogUnitTest {

    @Test
    fun testGrossAndNetHoursCalculation() {
        // Test entry from PDF: 7:30 AM – 5:00 PM with 30 min (0:30) lunch -> 9:00 net hrs
        val entry1 = WorkEntry(
            clockInTime = "7:30 AM",
            clockOutTime = "5:00 PM",
            hadLunch = true,
            lunchMinutes = 30
        )
        assertEquals(570, entry1.calculatedGrossMinutes) // 9 hrs 30 mins
        assertEquals("0:30", entry1.lunchFormatted)
        assertEquals("9:00 hrs", entry1.netFormatted)

        // Test entry without lunch: 7:30 AM – 3:30 PM with Sin lonche -> 8:00 hrs
        val entry2 = WorkEntry(
            clockInTime = "7:30 AM",
            clockOutTime = "3:30 PM",
            hadLunch = false,
            lunchMinutes = 0
        )
        assertEquals(480, entry2.calculatedGrossMinutes) // 8 hrs
        assertEquals("Sin lonche", entry2.lunchFormatted)
        assertEquals("8:00 hrs", entry2.netFormatted)
    }

    @Test
    fun testNinetyHoursMinusNinetyMinutesLunchSubtractionMath() {
        // Test 90 hours total work (5400 mins) minus 90 minutes lunch (1:30 hrs) -> 88:30 hrs
        val entry = WorkEntry(
            grossMinutesOverride = 90 * 60, // 5400 mins = 90:00 hrs
            hadLunch = true,
            lunchMinutes = 90 // 1:30 hrs
        )
        assertEquals("1:30", entry.lunchFormatted)
        assertEquals("88:30 hrs", entry.netFormatted)
    }

    @Test
    fun testWeeklyLogTotals() {
        val entry1 = WorkEntry(
            clockInTime = "7:30 AM",
            clockOutTime = "5:00 PM",
            hadLunch = true,
            lunchMinutes = 30
        ) // Gross: 9:30, Lunch: 0:30, Net: 9:00

        val entry2 = WorkEntry(
            clockInTime = "7:30 AM",
            clockOutTime = "3:30 PM",
            hadLunch = false,
            lunchMinutes = 0
        ) // Gross: 8:00, Lunch: 0:00, Net: 8:00

        val weeklyLog = WeeklyLog(
            workerName = "Daniel",
            periodTitle = "21 Sept – 2 Oct",
            entries = listOf(entry1, entry2)
        )

        assertEquals("17:30 hrs", weeklyLog.totalPropertyFormatted)
        assertEquals("0:30 hrs", weeklyLog.totalLunchFormatted)
        assertEquals("17:00 hrs", weeklyLog.totalNetFormatted)
    }

    @Test
    fun testChronologicalSorting() {
        val entry1 = WorkEntry(timestamp = 2000L, dateDisplay = "22 de sept.")
        val entry2 = WorkEntry(timestamp = 1000L, dateDisplay = "21 de sept.")
        val entry3 = WorkEntry(timestamp = 3000L, dateDisplay = "23 de sept.")

        val unsorted = listOf(entry1, entry2, entry3)
        val sorted = unsorted.sortedBy { it.timestamp }

        assertEquals("21 de sept.", sorted[0].dateDisplay)
        assertEquals("22 de sept.", sorted[1].dateDisplay)
        assertEquals("23 de sept.", sorted[2].dateDisplay)
    }

    @Test
    fun testSeptemberTwelveAndOctoberTenChronologicalSorting() {
        val dateSept12 = "12 de sept."
        val dateOct10 = "10 de oct."

        val tsSept12 = WorkEntry.parseDateDisplayToTimestamp(dateSept12)
        val tsOct10 = WorkEntry.parseDateDisplayToTimestamp(dateOct10)

        assertTrue(tsSept12 < tsOct10)

        val entryOct = WorkEntry(timestamp = tsOct10, dateDisplay = dateOct10)
        val entrySept = WorkEntry(timestamp = tsSept12, dateDisplay = dateSept12)

        val list = listOf(entryOct, entrySept).sortedBy { it.timestamp }

        assertEquals("12 de sept.", list[0].dateDisplay)
        assertEquals("10 de oct.", list[1].dateDisplay)
    }

    @Test
    fun testSmartImportUserParagraphTextChronologicalOrder() {
        val sampleParagraph = """
            Lunes 5 de octubre Trabajé en la casa del patrón entré a las 7:30 salí a las 2:30 menos una hora de lonche...
            Martes 6 de octubre trabajé en la casa del patrón entré a las 7:30 salí a las 3:30 menos la media hora de lonche...
            Miércoles 7 de octubre se trabajó en la Fulton de Santa Elena entré a las 7:30 salí a las 2:30 no se agarró lonche...
            Jueves 8 de octubre se trabajó en la big ranch entramos a las 7:30 salimos a las 4 p.m menos media hora de lunch...
            Viernes 9 de octubre centró a las 7:30 se salió a las a las 4 de la tarde menos media hora de lonche...
        """.trimIndent()

        val parsed = WorkLogParser.parseParagraphToEntries(sampleParagraph)

        assertEquals(5, parsed.size)
        assertEquals("5 de oct.", parsed[0].dateDisplay)
        assertEquals("6 de oct.", parsed[1].dateDisplay)
        assertEquals("7 de oct.", parsed[2].dateDisplay)
        assertEquals("8 de oct.", parsed[3].dateDisplay)
        assertEquals("9 de oct.", parsed[4].dateDisplay)

        // Verify period title range
        val periodTitle = "${parsed.first().dateDisplay} – ${parsed.last().dateDisplay}"
        assertEquals("5 de oct. – 9 de oct.", periodTitle)
    }
}
