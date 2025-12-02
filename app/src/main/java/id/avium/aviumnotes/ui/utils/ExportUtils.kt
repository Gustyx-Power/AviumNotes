package id.avium.aviumnotes.ui.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import id.avium.aviumnotes.R
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportUtils {

    /**
     * Export bitmap to PNG file
     */
    fun exportToPng(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "note_${System.currentTimeMillis()}.png"
    ): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (Scoped Storage)
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AviumNotes")
                }

                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                imageUri?.let { uri ->
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                }
                imageUri
            } else {
                // Android 9 and below
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val aviumDir = File(imagesDir, "AviumNotes")
                if (!aviumDir.exists()) {
                    aviumDir.mkdirs()
                }

                val imageFile = File(aviumDir, fileName)
                FileOutputStream(imageFile).use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }

                // Notify gallery
                val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                intent.data = Uri.fromFile(imageFile)
                context.sendBroadcast(intent)

                Uri.fromFile(imageFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.export_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
            null
        }
    }

    /**
     * Export bitmap to PDF file
     */
    fun exportToPdf(
        context: Context,
        bitmap: Bitmap,
        title: String = "Note",
        fileName: String = "note_${System.currentTimeMillis()}.pdf"
    ): Uri? {
        return try {
            // Create PDF document
            val pdfDocument = PdfDocument()

            // Calculate page size to fit bitmap
            val pageWidth = bitmap.width
            val pageHeight = bitmap.height

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)

            // Draw bitmap on PDF page
            val canvas = page.canvas
            canvas.drawBitmap(bitmap, 0f, 0f, null)

            pdfDocument.finishPage(page)

            // Save PDF
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ (Scoped Storage)
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/AviumNotes")
                }

                val pdfUri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                pdfUri?.let { uri ->
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                }
                pdfUri
            } else {
                // Android 9 and below
                val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val aviumDir = File(documentsDir, "AviumNotes")
                if (!aviumDir.exists()) {
                    aviumDir.mkdirs()
                }

                val pdfFile = File(aviumDir, fileName)
                FileOutputStream(pdfFile).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                Uri.fromFile(pdfFile)
            }

            pdfDocument.close()
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.export_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
            null
        }
    }

    /**
     * Create bitmap from text content
     */
    fun createBitmapFromText(
        text: String,
        title: String,
        backgroundColor: Int,
        width: Int = 1080,
        padding: Int = 80
    ): Bitmap {
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 48f
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 72f
            isFakeBoldText = true
            isAntiAlias = true
        }

        // Calculate required height
        val textWidth = width - (padding * 2)
        val lines = mutableListOf<String>()

        // Add title lines
        val titleWords = title.split(" ")
        var currentLine = ""
        titleWords.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (titlePaint.measureText(testLine) <= textWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)

        val titleLineCount = lines.size
        lines.clear()

        // Add content lines
        val contentLines = text.split("\n")
        contentLines.forEach { line ->
            val words = line.split(" ")
            currentLine = ""
            words.forEach { word ->
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) <= textWidth) {
                    currentLine = testLine
                } else {
                    if (currentLine.isNotEmpty()) lines.add(currentLine)
                    currentLine = word
                }
            }
            if (currentLine.isNotEmpty()) lines.add(currentLine)
        }

        val totalHeight = padding + (titleLineCount * 100) + 60 + (lines.size * 70) + padding

        // Create bitmap
        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(backgroundColor)

        var y = padding + 80f

        // Draw title
        val titleLines2 = mutableListOf<String>()
        currentLine = ""
        titleWords.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (titlePaint.measureText(testLine) <= textWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) titleLines2.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) titleLines2.add(currentLine)

        titleLines2.forEach { line ->
            canvas.drawText(line, padding.toFloat(), y, titlePaint)
            y += 100f
        }

        y += 60f

        // Draw content
        lines.forEach { line ->
            canvas.drawText(line, padding.toFloat(), y, paint)
            y += 70f
        }

        return bitmap
    }

    /**
     * Share file using Android share dialog
     */
    fun shareFile(context: Context, uri: Uri, mimeType: String, title: String? = null) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooserTitle = title ?: context.getString(R.string.share)
            context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.share_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
        }
    }
}

