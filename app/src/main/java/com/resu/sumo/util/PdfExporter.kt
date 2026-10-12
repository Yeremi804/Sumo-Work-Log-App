package com.resu.sumo.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.resu.sumo.data.WeeklyLog
import com.resu.sumo.data.WorkEntry
import com.resu.sumo.ui.AppLanguage
import com.resu.sumo.ui.LanguageManager
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    fun generatePdfReport(
        context: Context,
        weeklyLog: WeeklyLog,
        language: AppLanguage = AppLanguage.SPANISH,
        useColor: Boolean = true,
        customFileName: String? = null
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 portrait in points
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }

        var y = 30f
        val startX = 30f
        val pageWidth = 535f

        val docMainTitle = LanguageManager.getString("doc_main_title", language)
        val docSubTitle = LanguageManager.getString("doc_subtitle", language)

        // Color definitions
        val primaryHeaderColor = if (useColor) Color.parseColor("#1E3A8A") else Color.BLACK
        val secondaryHeaderColor = if (useColor) Color.parseColor("#0284C7") else Color.DKGRAY
        val accentBgColor = if (useColor) Color.parseColor("#DBEAFE") else Color.parseColor("#F1F5F9")
        val highlightBgColor = if (useColor) Color.parseColor("#D1FAE5") else Color.parseColor("#E2E8F0")
        val altRowBgColor = if (useColor) Color.parseColor("#F8FAFC") else Color.WHITE

        // Title Header Banner
        if (useColor) {
            paint.style = Paint.Style.FILL
            paint.color = primaryHeaderColor
            canvas.drawRect(startX, y, startX + pageWidth, y + 45f, paint)

            paint.color = Color.WHITE
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 13f
            canvas.drawText(docMainTitle, startX + 12f, y + 22f, paint)

            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(docSubTitle, startX + 12f, y + 36f, paint)
            y += 55f
        } else {
            paint.color = Color.BLACK
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 14f
            canvas.drawText(docMainTitle, startX, y + 15f, paint)
            y += 22f

            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(docSubTitle, startX, y + 10f, paint)
            y += 25f
        }

        // Worker Name & Period Line
        paint.color = Color.BLACK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        val workerTitle = LanguageManager.getString("worker_name_label", language)
        val periodTitle = LanguageManager.getString("period_label", language)
        canvas.drawText("$workerTitle ${weeklyLog.workerName}", startX, y, paint)
        canvas.drawText("$periodTitle ${weeklyLog.periodTitle}", startX + 320f, y, paint)
        y += 18f

        // Metrics Summary Boxes
        val boxWidth = pageWidth / 3f
        val boxHeight = 36f

        paint.style = Paint.Style.FILL
        // Box 1
        paint.color = accentBgColor
        canvas.drawRect(startX, y, startX + boxWidth - 4f, y + boxHeight, paint)
        // Box 2
        canvas.drawRect(startX + boxWidth + 2f, y, startX + (boxWidth * 2) - 2f, y + boxHeight, paint)
        // Box 3 (Net Hours)
        paint.color = highlightBgColor
        canvas.drawRect(startX + (boxWidth * 2) + 4f, y, startX + pageWidth, y + boxHeight, paint)

        // Borders
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = if (useColor) secondaryHeaderColor else Color.GRAY
        canvas.drawRect(startX, y, startX + boxWidth - 4f, y + boxHeight, paint)
        canvas.drawRect(startX + boxWidth + 2f, y, startX + (boxWidth * 2) - 2f, y + boxHeight, paint)
        canvas.drawRect(startX + (boxWidth * 2) + 4f, y, startX + pageWidth, y + boxHeight, paint)

        val labelPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.DKGRAY
        }

        val valPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = if (useColor) primaryHeaderColor else Color.BLACK
        }

        canvas.drawText(LanguageManager.getString("total_property_time", language), startX + 6f, y + 12f, labelPaint)
        canvas.drawText(LanguageManager.getString("lunch_deducted", language), startX + boxWidth + 8f, y + 12f, labelPaint)
        canvas.drawText(LanguageManager.getString("total_hours_worked", language), startX + (boxWidth * 2) + 10f, y + 12f, labelPaint)

        canvas.drawText(weeklyLog.totalPropertyFormatted, startX + 6f, y + 28f, valPaint)
        canvas.drawText(weeklyLog.totalLunchFormatted, startX + boxWidth + 8f, y + 28f, valPaint)
        canvas.drawText(weeklyLog.totalNetFormatted, startX + (boxWidth * 2) + 10f, y + 28f, valPaint)

        y += boxHeight + 12f

        // Policy Note
        val notePaint = Paint().apply {
            isAntiAlias = true
            textSize = 7.5f
            color = Color.DKGRAY
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        canvas.drawText(LanguageManager.getString("policy_note", language), startX, y, notePaint)
        y += 14f

        // Table Header
        paint.style = Paint.Style.FILL
        paint.color = secondaryHeaderColor
        canvas.drawRect(startX, y, startX + pageWidth, y + 18f, paint)

        val headerPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.WHITE
        }

        val col1 = startX + 6f   // Date
        val col2 = startX + 70f  // Location
        val col3 = startX + 155f // Schedule
        val col4 = startX + 240f // Lunch
        val col5 = startX + 290f // Hours
        val col6 = startX + 340f // Description

        val headerY = y + 12f
        canvas.drawText(LanguageManager.getString("header_date", language), col1, headerY, headerPaint)
        canvas.drawText(LanguageManager.getString("header_location", language), col2, headerY, headerPaint)
        canvas.drawText(LanguageManager.getString("header_time", language), col3, headerY, headerPaint)
        canvas.drawText(LanguageManager.getString("header_lunch", language), col4, headerY, headerPaint)
        canvas.drawText(LanguageManager.getString("header_hours", language), col5, headerY, headerPaint)
        canvas.drawText(LanguageManager.getString("header_description", language), col6, headerY, headerPaint)

        y += 18f

        // Table Rows
        val rowPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8f
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val borderPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
            color = Color.LTGRAY
        }

        val maxDescWidth = (startX + pageWidth) - col6 - 6f
        val sortedEntries = weeklyLog.entries.sortedBy { it.timestamp }

        for ((index, entry) in sortedEntries.withIndex()) {
            val descLines = wrapTextToLines(entry.description, rowPaint, maxDescWidth)
            val rowHeight = (descLines.size * 11f + 10f).coerceAtLeast(22f)

            // Alternating Row Background
            if (index % 2 == 1) {
                paint.style = Paint.Style.FILL
                paint.color = altRowBgColor
                canvas.drawRect(startX, y, startX + pageWidth, y + rowHeight, paint)
            }

            canvas.drawRect(startX, y, startX + pageWidth, y + rowHeight, borderPaint)

            val rY = y + 14f
            canvas.drawText(entry.dateDisplay, col1, rY, rowPaint)
            canvas.drawText(entry.location, col2, rY, rowPaint)
            canvas.drawText("${entry.clockInTime} – ${entry.clockOutTime}", col3, rY, rowPaint)
            canvas.drawText(entry.lunchFormatted, col4, rY, rowPaint)
            canvas.drawText(WorkEntry.formatMinutesToHoursMinutes(entry.calculatedNetMinutes), col5, rY, rowPaint)

            // Multi-line full paragraph description rendering!
            var lineY = rY
            for (line in descLines) {
                canvas.drawText(line, col6, lineY, rowPaint)
                lineY += 11f
            }

            y += rowHeight
            if (y > 780f) break
        }

        pdfDocument.finishPage(page)

        return try {
            val sanitizedWorker = weeklyLog.workerName.replace(" ", "_").replace("[^a-zA-Z0-9_]".toRegex(), "")
            val sanitizedPeriod = weeklyLog.periodTitle.replace(" ", "_").replace("[^a-zA-Z0-9_]".toRegex(), "")
            val defaultName = if (sanitizedWorker.isNotEmpty()) "${sanitizedWorker}_${sanitizedPeriod}" else "Registro_Horas"

            val baseName = if (!customFileName.isNullOrBlank()) customFileName.trim() else defaultName
            val finalFileName = if (baseName.endsWith(".pdf", ignoreCase = true)) baseName else "$baseName.pdf"

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, finalFileName)
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()
            file
        } catch (e: Exception) {
            pdfDocument.close()
            null
        }
    }

    fun openPdfFile(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.fromFile(file), "application/pdf")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Open PDF"))
            } catch (ex: Exception) {
                // Ignore
            }
        }
    }

    private fun wrapTextToLines(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(candidate) <= maxWidth) {
                currentLine = candidate
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        return if (lines.isEmpty()) listOf(text) else lines
    }

    fun shareReportText(context: Context, weeklyLog: WeeklyLog) {
        try {
            val text = generateCsvReport(weeklyLog)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Registro de Horas - ${weeklyLog.workerName}")
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir Reporte / Share Report"))
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun generateCsvReport(weeklyLog: WeeklyLog): String {
        val sb = StringBuilder()
        sb.append("${LanguageManager.getString("doc_main_title", AppLanguage.SPANISH)}\n")
        sb.append("Trabajador / Worker: ${weeklyLog.workerName}\n")
        sb.append("Período / Period: ${weeklyLog.periodTitle}\n")
        sb.append("Tiempo Total Propiedad: ${weeklyLog.totalPropertyFormatted}\n")
        sb.append("Lonche Descontado: ${weeklyLog.totalLunchFormatted}\n")
        sb.append("Total Horas Trabajadas: ${weeklyLog.totalNetFormatted}\n\n")

        sb.append("Fecha, Ubicación, Horario, Lonche, Horas, Descripción\n")
        for (entry in weeklyLog.entries.sortedBy { it.timestamp }) {
            sb.append("${entry.dateDisplay}, ")
            sb.append("${entry.location}, ")
            sb.append("${entry.clockInTime} – ${entry.clockOutTime}, ")
            sb.append("${entry.lunchFormatted}, ")
            sb.append("${entry.netFormatted}, ")
            sb.append("${entry.description}\n")
        }
        return sb.toString()
    }
}
