package com.example.naughty.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import com.example.naughty.ui.editor.EditorBlockParser
import com.example.naughty.ui.editor.InlineReminderParser
import com.example.naughty.ui.theme.NoteTheme
import com.example.naughty.ui.theme.PaperPattern
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

/**
 * Generates clean, publication-grade multi-page A4 PDF documents from notes.
 * Applies print-friendly theme adaptation:
 *  - Light themes preserve their natural paper surface, grid lines, and palette.
 *  - Dark themes adapt to warm paper white with dark slate text while retaining
 *    the theme's typography, paper pattern (grid/dots/lines), and signature accent color.
 */
object NotePdfGenerator {

    const val PAGE_WIDTH = 595   // Standard A4 width in PostScript points (72 DPI)
    const val PAGE_HEIGHT = 842  // Standard A4 height in PostScript points (72 DPI)

    private const val MARGIN_HORIZONTAL = 44f
    private const val MARGIN_TOP = 44f
    private const val MARGIN_BOTTOM = 46f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_HORIZONTAL * 2)

    data class PdfPalette(
        val background: Int,
        val gridLine: Int,
        val textPrimary: Int,
        val textSecondary: Int,
        val textMuted: Int,
        val border: Int,
        val accent: Int,
        val isDarkAdapted: Boolean
    )

    fun resolvePdfPalette(theme: NoteTheme): PdfPalette {
        if (!theme.isDark) {
            return PdfPalette(
                background = theme.surface.toArgb(),
                gridLine = theme.gridLineColor.toArgb(),
                textPrimary = theme.textPrimary.toArgb(),
                textSecondary = theme.textSecondary.toArgb(),
                textMuted = theme.textMuted.toArgb(),
                border = theme.border.toArgb(),
                accent = theme.accent.toArgb(),
                isDarkAdapted = false
            )
        }

        // Dark theme: Adapt to print-friendly light paper surface with dark text
        val originalAccent = theme.accent.toArgb()
        val contrastAccent = ensurePrintContrast(originalAccent)

        return PdfPalette(
            background = 0xFFFCFCFA.toInt(),
            gridLine = 0xFFECEFF3.toInt(),
            textPrimary = 0xFF1E293B.toInt(),
            textSecondary = 0xFF64748B.toInt(),
            textMuted = 0xFF94A3B8.toInt(),
            border = 0xFFE2E8F0.toInt(),
            accent = contrastAccent,
            isDarkAdapted = true
        )
    }

    /**
     * Ensures an accent color has sufficient contrast against white/paper backgrounds.
     * Uses bitwise ARGB math so it runs on JVM unit tests without Android Color mocking.
     * If relative luminance is too high (e.g. bright neon cyan or pale yellow),
     * darkens the color proportionally while preserving its exact hue.
     */
    fun ensurePrintContrast(color: Int): Int {
        val a = (color ushr 24) and 0xFF
        val r = (color ushr 16) and 0xFF
        val g = (color ushr 8) and 0xFF
        val b = color and 0xFF

        val luminance = (0.2126 * (r / 255.0)) + (0.7152 * (g / 255.0)) + (0.0722 * (b / 255.0))
        if (luminance <= 0.60) {
            return color
        }

        val factor = (0.50 / luminance).coerceIn(0.40, 0.78)
        val newR = (r * factor).toInt().coerceIn(0, 255)
        val newG = (g * factor).toInt().coerceIn(0, 255)
        val newB = (b * factor).toInt().coerceIn(0, 255)
        return (a shl 24) or (newR shl 16) or (newG shl 8) or newB
    }

    fun resolveTypeface(fontFamily: FontFamily, isBold: Boolean = false): Typeface {
        val base = when (fontFamily) {
            FontFamily.Monospace -> Typeface.MONOSPACE
            FontFamily.Serif -> Typeface.SERIF
            else -> Typeface.SANS_SERIF
        }
        return if (isBold) Typeface.create(base, Typeface.BOLD) else base
    }

    fun sanitizeFileName(title: String): String {
        val trimmed = title.trim()
        val baseName = if (trimmed.isBlank() ||
            trimmed.equals("Untitled", ignoreCase = true) ||
            trimmed.equals("New thought", ignoreCase = true)
        ) {
            "Naughty_Note"
        } else {
            trimmed.replace(Regex("""[^a-zA-Z0-9_\-\s]"""), "")
                .replace(Regex("""\s+"""), "_")
                .trim('_')
        }
        val safeBase = baseName.ifBlank { "Naughty_Note" }
        return if (safeBase.length > 40) safeBase.substring(0, 40).trimEnd('_') else safeBase
    }

    private sealed class PdfElement {
        data class Heading(val text: String, val level: Int) : PdfElement()
        data class Paragraph(val text: String) : PdfElement()
        data class Checklist(val isChecked: Boolean, val text: String, val reminderLabel: String?) : PdfElement()
        data class Bullet(val text: String) : PdfElement()
        data class Image(val uriString: String) : PdfElement()
        data class VoiceNote(val uriString: String) : PdfElement()
        data class Spacer(val height: Float) : PdfElement()
    }

    private fun parseContent(content: String): List<PdfElement> {
        val elements = mutableListOf<PdfElement>()
        if (content.isBlank()) return elements

        val lines = content.lines()
        val paragraphBuffer = mutableListOf<String>()

        fun flushParagraph() {
            if (paragraphBuffer.isNotEmpty()) {
                val fullText = paragraphBuffer.joinToString("\n").trim()
                if (fullText.isNotEmpty()) {
                    elements.add(PdfElement.Paragraph(fullText))
                }
                paragraphBuffer.clear()
            }
        }

        val headingRegex = Regex("""^(#{1,3})\s+(.*)$""")
        val checklistRegex = Regex("""^\s*-\s*\[([ xX])\](?:\s*(.*))?$""")
        val bulletRegex = Regex("""^\s*[-*•]\s+(.*)$""")
        val imageRegex = Regex("""^!\[.*?\]\((.*?)\)""")
        val voiceRegex = Regex("""^!?\[(?:🎤\s*)?Voice Note.*?\]\((.*?)\)""", RegexOption.IGNORE_CASE)

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                flushParagraph()
                elements.add(PdfElement.Spacer(8f))
                continue
            }

            val imageMatch = imageRegex.matchEntire(trimmed)
            if (imageMatch != null) {
                flushParagraph()
                elements.add(PdfElement.Image(imageMatch.groupValues[1]))
                continue
            }

            val voiceMatch = voiceRegex.matchEntire(trimmed)
            if (voiceMatch != null) {
                flushParagraph()
                elements.add(PdfElement.VoiceNote(voiceMatch.groupValues[1]))
                continue
            }

            val headingMatch = headingRegex.matchEntire(trimmed)
            if (headingMatch != null) {
                flushParagraph()
                val level = headingMatch.groupValues[1].length
                elements.add(PdfElement.Heading(headingMatch.groupValues[2].trim(), level))
                continue
            }

            val checklistMatch = checklistRegex.matchEntire(line)
            if (checklistMatch != null) {
                flushParagraph()
                val isChecked = checklistMatch.groupValues[1].equals("x", ignoreCase = true)
                val rawText = checklistMatch.groupValues[2]
                val reminder = InlineReminderParser.extractFirstReminder(rawText)
                val cleanText = InlineReminderParser.cleanAllReminders(rawText)
                elements.add(
                    PdfElement.Checklist(
                        isChecked = isChecked,
                        text = cleanText,
                        reminderLabel = reminder?.displayText
                    )
                )
                continue
            }

            val bulletMatch = bulletRegex.matchEntire(line)
            if (bulletMatch != null) {
                flushParagraph()
                elements.add(PdfElement.Bullet(bulletMatch.groupValues[1].trim()))
                continue
            }

            paragraphBuffer.add(line)
        }

        flushParagraph()
        return elements
    }

    /**
     * Writes the generated PDF directly to the specified [OutputStream].
     */
    fun generateToStream(
        context: Context,
        title: String,
        content: String,
        theme: NoteTheme,
        outputStream: OutputStream
    ) {
        val palette = resolvePdfPalette(theme)
        val baseTypeface = resolveTypeface(theme.fontFamily, isBold = false)
        val titleTypeface = resolveTypeface(theme.titleFontFamily, isBold = true)

        val elements = parseContent(content)
        val document = PdfDocument()

        try {
            // Measure & Paginate
            val pages = layoutPages(context, title, elements, palette, baseTypeface, titleTypeface)
            val totalPages = pages.size.coerceAtLeast(1)

            for ((pageIndex, pageItems) in pages.withIndex()) {
                val pageNumber = pageIndex + 1
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                // 1. Draw Paper Background & Pattern
                drawBackground(canvas, palette, theme.paperPattern)

                // 2. Draw Page Header
                val contentStartY = if (pageIndex == 0) {
                    drawCoverHeader(canvas, title, theme, palette, titleTypeface, baseTypeface)
                } else {
                    drawRunningHeader(canvas, title, palette, titleTypeface, baseTypeface)
                }

                // 3. Draw Elements on this page
                drawPageElements(context, canvas, pageItems, contentStartY, palette, baseTypeface, titleTypeface)

                // 4. Draw Footer
                drawFooter(canvas, pageNumber, totalPages, palette, baseTypeface)

                document.finishPage(page)
            }

            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }

    /**
     * Generates the PDF into the app's cache directory and returns the [File] ready for sharing.
     */
    fun generateToCacheFile(
        context: Context,
        title: String,
        content: String,
        theme: NoteTheme
    ): File {
        val cacheDir = File(context.cacheDir, "shared_pdfs").apply { mkdirs() }
        val fileName = "${sanitizeFileName(title)}.pdf"
        val pdfFile = File(cacheDir, fileName)
        FileOutputStream(pdfFile).use { out ->
            generateToStream(context, title, content, theme, out)
        }
        return pdfFile
    }

    // ---------------------------------------------------------------------------------------------
    // Page Layout & Pagination Engine
    // ---------------------------------------------------------------------------------------------

    private data class MeasuredItem(
        val element: PdfElement,
        val height: Float,
        val textLayout: StaticLayout? = null,
        val bitmap: Bitmap? = null
    )

    private fun layoutPages(
        context: Context,
        title: String,
        elements: List<PdfElement>,
        palette: PdfPalette,
        baseTypeface: Typeface,
        titleTypeface: Typeface
    ): List<List<MeasuredItem>> {
        val pages = mutableListOf<MutableList<MeasuredItem>>()
        var currentPage = mutableListOf<MeasuredItem>()

        val firstPageHeaderHeight = measureCoverHeaderHeight(title, titleTypeface)
        val runningHeaderHeight = 32f
        val footerHeight = 36f

        var currentY = MARGIN_TOP + firstPageHeaderHeight
        val maxY = PAGE_HEIGHT - MARGIN_BOTTOM - footerHeight

        fun startNewPage() {
            pages.add(currentPage)
            currentPage = mutableListOf()
            currentY = MARGIN_TOP + runningHeaderHeight
        }

        for (element in elements) {
            val measuredItems = measureElement(context, element, palette, baseTypeface, titleTypeface)
            for (item in measuredItems) {
                // If item doesn't fit on current page and we've already drawn something, start a new page
                if (currentY + item.height > maxY && currentPage.isNotEmpty()) {
                    startNewPage()
                }
                currentPage.add(item)
                currentY += item.height
            }
        }

        if (currentPage.isNotEmpty() || pages.isEmpty()) {
            pages.add(currentPage)
        }

        return pages
    }

    private fun measureElement(
        context: Context,
        element: PdfElement,
        palette: PdfPalette,
        baseTypeface: Typeface,
        titleTypeface: Typeface
    ): List<MeasuredItem> {
        return when (element) {
            is PdfElement.Heading -> {
                val (fontSize, typeface, color) = when (element.level) {
                    1 -> Triple(16f, titleTypeface, palette.accent)
                    2 -> Triple(14f, titleTypeface, palette.textPrimary)
                    else -> Triple(12.5f, titleTypeface, palette.textSecondary)
                }
                val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.textSize = fontSize
                    this.typeface = typeface
                    this.color = color
                }
                val layout = createStaticLayout(element.text, paint, CONTENT_WIDTH)
                listOf(MeasuredItem(element, layout.height.toFloat() + 10f, textLayout = layout))
            }

            is PdfElement.Checklist -> {
                val indent = 22f
                val textWidth = CONTENT_WIDTH - indent
                val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.textSize = 10.5f
                    this.typeface = baseTypeface
                    this.color = if (element.isChecked) palette.textSecondary else palette.textPrimary
                    if (element.isChecked) {
                        this.flags = this.flags or Paint.STRIKE_THRU_TEXT_FLAG
                    }
                }
                val displayText = if (!element.reminderLabel.isNullOrBlank()) {
                    "${element.text}  [🔔 ${element.reminderLabel}]"
                } else {
                    element.text
                }
                val layout = createStaticLayout(displayText, paint, textWidth)
                val itemHeight = maxOf(18f, layout.height.toFloat() + 6f)
                listOf(MeasuredItem(element, itemHeight, textLayout = layout))
            }

            is PdfElement.Bullet -> {
                val indent = 18f
                val textWidth = CONTENT_WIDTH - indent
                val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.textSize = 10.5f
                    this.typeface = baseTypeface
                    this.color = palette.textPrimary
                }
                val layout = createStaticLayout(element.text, paint, textWidth)
                val itemHeight = maxOf(16f, layout.height.toFloat() + 5f)
                listOf(MeasuredItem(element, itemHeight, textLayout = layout))
            }

            is PdfElement.Paragraph -> {
                val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.textSize = 10.5f
                    this.typeface = baseTypeface
                    this.color = palette.textPrimary
                }
                val fullLayout = createStaticLayout(element.text, paint, CONTENT_WIDTH)
                val totalHeight = fullLayout.height.toFloat() + 6f

                // If paragraph fits comfortably or is modest, keep it as single item
                if (totalHeight < 400f) {
                    listOf(MeasuredItem(element, totalHeight, textLayout = fullLayout))
                } else {
                    // Split very long paragraphs into smaller paragraph slices so they paginate gracefully
                    splitLongParagraph(element.text, paint, CONTENT_WIDTH)
                }
            }

            is PdfElement.Image -> {
                val bitmap = loadSampledBitmap(context, element.uriString, maxTargetWidth = 900)
                if (bitmap != null) {
                    val scale = min(CONTENT_WIDTH / bitmap.width.toFloat(), 240f / bitmap.height.toFloat()).coerceAtMost(1f)
                    val displayHeight = (bitmap.height * scale) + 12f
                    listOf(MeasuredItem(element, displayHeight, bitmap = bitmap))
                } else {
                    listOf(MeasuredItem(element, 28f)) // Placeholder height
                }
            }

            is PdfElement.VoiceNote -> {
                listOf(MeasuredItem(element, 32f))
            }

            is PdfElement.Spacer -> {
                listOf(MeasuredItem(element, element.height))
            }
        }
    }

    private fun splitLongParagraph(text: String, paint: TextPaint, width: Float): List<MeasuredItem> {
        val items = mutableListOf<MeasuredItem>()
        val paragraphs = text.split("\n")
        for (p in paragraphs) {
            val trimmed = p.trim()
            if (trimmed.isEmpty()) {
                items.add(MeasuredItem(PdfElement.Spacer(6f), 6f))
                continue
            }
            val layout = createStaticLayout(trimmed, paint, width)
            items.add(MeasuredItem(PdfElement.Paragraph(trimmed), layout.height.toFloat() + 6f, textLayout = layout))
        }
        return items.ifEmpty {
            val layout = createStaticLayout(text, paint, width)
            listOf(MeasuredItem(PdfElement.Paragraph(text), layout.height.toFloat() + 6f, textLayout = layout))
        }
    }

    private fun createStaticLayout(text: CharSequence, paint: TextPaint, width: Float): StaticLayout {
        val safeWidth = width.toInt().coerceAtLeast(50)
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, safeWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1.22f)
            .setIncludePad(false)
            .build()
    }

    // ---------------------------------------------------------------------------------------------
    // Drawing & Rendering Routines
    // ---------------------------------------------------------------------------------------------

    private fun drawBackground(canvas: Canvas, palette: PdfPalette, pattern: PaperPattern) {
        canvas.drawColor(palette.background)

        if (pattern == PaperPattern.NONE) return

        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.gridLine
            strokeWidth = 0.55f
            style = Paint.Style.STROKE
        }

        val step = 20f

        when (pattern) {
            PaperPattern.GRID -> {
                var x = MARGIN_HORIZONTAL
                while (x <= PAGE_WIDTH - MARGIN_HORIZONTAL) {
                    canvas.drawLine(x, MARGIN_TOP, x, PAGE_HEIGHT - MARGIN_BOTTOM, gridPaint)
                    x += step
                }
                var y = MARGIN_TOP
                while (y <= PAGE_HEIGHT - MARGIN_BOTTOM) {
                    canvas.drawLine(MARGIN_HORIZONTAL, y, PAGE_WIDTH - MARGIN_HORIZONTAL, y, gridPaint)
                    y += step
                }
            }

            PaperPattern.LINES -> {
                var y = MARGIN_TOP
                while (y <= PAGE_HEIGHT - MARGIN_BOTTOM) {
                    canvas.drawLine(MARGIN_HORIZONTAL, y, PAGE_WIDTH - MARGIN_HORIZONTAL, y, gridPaint)
                    y += step
                }
            }

            PaperPattern.DOTS -> {
                val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette.gridLine
                    style = Paint.Style.FILL
                }
                var x = MARGIN_HORIZONTAL + (step / 2f)
                while (x < PAGE_WIDTH - MARGIN_HORIZONTAL) {
                    var y = MARGIN_TOP + (step / 2f)
                    while (y < PAGE_HEIGHT - MARGIN_BOTTOM) {
                        canvas.drawCircle(x, y, 0.85f, dotPaint)
                        y += step
                    }
                    x += step
                }
            }
        }
    }

    private fun measureCoverHeaderHeight(title: String, titleTypeface: Typeface): Float {
        val displayTitle = title.trim().ifBlank { "Untitled Note" }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 21f
            typeface = titleTypeface
        }
        val layout = createStaticLayout(displayTitle, titlePaint, CONTENT_WIDTH)
        // Accent bar (3.5) + gap (10) + titleLayout.height + gap (6) + subtitle (14) + divider gap (10) + divider (1) + gap (14)
        return 3.5f + 10f + layout.height + 6f + 14f + 10f + 1f + 14f
    }

    private fun drawCoverHeader(
        canvas: Canvas,
        title: String,
        theme: NoteTheme,
        palette: PdfPalette,
        titleTypeface: Typeface,
        baseTypeface: Typeface
    ): Float {
        var y = MARGIN_TOP

        // 1. Signature Accent Header Bar
        val accentBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accent
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(MARGIN_HORIZONTAL, y, PAGE_WIDTH - MARGIN_HORIZONTAL, y + 3.5f),
            1.75f, 1.75f, accentBarPaint
        )
        y += 3.5f + 12f

        // 2. Note Title
        val displayTitle = title.trim().ifBlank { "Untitled Note" }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 21f
            typeface = titleTypeface
            color = palette.textPrimary
        }
        val titleLayout = createStaticLayout(displayTitle, titlePaint, CONTENT_WIDTH)
        canvas.save()
        canvas.translate(MARGIN_HORIZONTAL, y)
        titleLayout.draw(canvas)
        canvas.restore()
        y += titleLayout.height + 6f

        // 3. Metadata Subtitle (Date + Theme Name)
        val dateString = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date())
        val metaString = "$dateString   •   ${theme.name}"
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9.5f
            typeface = baseTypeface
            color = palette.textSecondary
        }
        canvas.drawText(metaString, MARGIN_HORIZONTAL, y + 10f, metaPaint)
        y += 18f

        // 4. Subtle Border Divider Line
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.border
            strokeWidth = 1f
        }
        canvas.drawLine(MARGIN_HORIZONTAL, y, PAGE_WIDTH - MARGIN_HORIZONTAL, y, dividerPaint)
        y += 14f

        return y
    }

    private fun drawRunningHeader(
        canvas: Canvas,
        title: String,
        palette: PdfPalette,
        titleTypeface: Typeface,
        baseTypeface: Typeface
    ): Float {
        var y = MARGIN_TOP

        // Subtle accent hairline
        val hairlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.accent
            strokeWidth = 1.8f
        }
        canvas.drawLine(MARGIN_HORIZONTAL, y, PAGE_WIDTH - MARGIN_HORIZONTAL, y, hairlinePaint)
        y += 8f

        // Running compact title
        val displayTitle = title.trim().ifBlank { "Untitled Note" }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f
            typeface = titleTypeface
            color = palette.textSecondary
        }
        canvas.drawText(displayTitle, MARGIN_HORIZONTAL, y + 8f, headerPaint)
        y += 16f

        return y
    }

    private fun drawPageElements(
        context: Context,
        canvas: Canvas,
        items: List<MeasuredItem>,
        startY: Float,
        palette: PdfPalette,
        baseTypeface: Typeface,
        titleTypeface: Typeface
    ) {
        var cursorY = startY

        for (item in items) {
            when (val elem = item.element) {
                is PdfElement.Heading -> {
                    item.textLayout?.let { layout ->
                        canvas.save()
                        canvas.translate(MARGIN_HORIZONTAL, cursorY + 4f)
                        layout.draw(canvas)
                        canvas.restore()
                    }
                    cursorY += item.height
                }

                is PdfElement.Checklist -> {
                    val boxSize = 11.5f
                    val boxY = cursorY + 1.5f
                    val boxRect = RectF(MARGIN_HORIZONTAL, boxY, MARGIN_HORIZONTAL + boxSize, boxY + boxSize)

                    if (elem.isChecked) {
                        // Filled checkbox in accent color
                        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = palette.accent
                            style = Paint.Style.FILL
                        }
                        canvas.drawRoundRect(boxRect, 2.5f, 2.5f, fillPaint)

                        // White checkmark glyph
                        val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.WHITE
                            strokeWidth = 1.6f
                            style = Paint.Style.STROKE
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                        }
                        val checkPath = Path().apply {
                            moveTo(boxRect.left + 2.5f, boxRect.top + 5.5f)
                            lineTo(boxRect.left + 4.8f, boxRect.top + 8.5f)
                            lineTo(boxRect.left + 9f, boxRect.top + 3f)
                        }
                        canvas.drawPath(checkPath, checkPaint)
                    } else {
                        // Unchecked stroked box
                        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = palette.textSecondary
                            strokeWidth = 1.15f
                            style = Paint.Style.STROKE
                        }
                        canvas.drawRoundRect(boxRect, 2.5f, 2.5f, strokePaint)
                    }

                    // Text
                    item.textLayout?.let { layout ->
                        canvas.save()
                        canvas.translate(MARGIN_HORIZONTAL + 22f, cursorY)
                        layout.draw(canvas)
                        canvas.restore()
                    }
                    cursorY += item.height
                }

                is PdfElement.Bullet -> {
                    // Bullet circle
                    val bulletPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = palette.accent
                        style = Paint.Style.FILL
                    }
                    canvas.drawCircle(MARGIN_HORIZONTAL + 5f, cursorY + 7f, 2.2f, bulletPaint)

                    // Text
                    item.textLayout?.let { layout ->
                        canvas.save()
                        canvas.translate(MARGIN_HORIZONTAL + 18f, cursorY)
                        layout.draw(canvas)
                        canvas.restore()
                    }
                    cursorY += item.height
                }

                is PdfElement.Paragraph -> {
                    item.textLayout?.let { layout ->
                        canvas.save()
                        canvas.translate(MARGIN_HORIZONTAL, cursorY)
                        layout.draw(canvas)
                        canvas.restore()
                    }
                    cursorY += item.height
                }

                is PdfElement.Image -> {
                    val bitmap = item.bitmap
                    if (bitmap != null) {
                        val scale = min(CONTENT_WIDTH / bitmap.width.toFloat(), 240f / bitmap.height.toFloat()).coerceAtMost(1f)
                        val drawWidth = bitmap.width * scale
                        val drawHeight = bitmap.height * scale
                        val dstRect = RectF(MARGIN_HORIZONTAL, cursorY + 2f, MARGIN_HORIZONTAL + drawWidth, cursorY + 2f + drawHeight)

                        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = palette.border
                            strokeWidth = 1f
                            style = Paint.Style.STROKE
                        }
                        canvas.drawBitmap(bitmap, null, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
                        canvas.drawRoundRect(dstRect, 6f, 6f, borderPaint)
                    } else {
                        // Fallback image placeholder pill
                        val phRect = RectF(MARGIN_HORIZONTAL, cursorY, MARGIN_HORIZONTAL + 160f, cursorY + 22f)
                        val phPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = palette.border
                            strokeWidth = 0.8f
                            style = Paint.Style.STROKE
                        }
                        canvas.drawRoundRect(phRect, 4f, 4f, phPaint)
                        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            textSize = 9f
                            typeface = baseTypeface
                            color = palette.textSecondary
                        }
                        canvas.drawText("🖼  Attached Image", MARGIN_HORIZONTAL + 10f, cursorY + 15f, textPaint)
                    }
                    cursorY += item.height
                }

                is PdfElement.VoiceNote -> {
                    // Sleek pill badge for attached voice notes
                    val pillRect = RectF(MARGIN_HORIZONTAL, cursorY + 2f, MARGIN_HORIZONTAL + 180f, cursorY + 24f)
                    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = palette.border
                        style = Paint.Style.STROKE
                        strokeWidth = 0.85f
                    }
                    canvas.drawRoundRect(pillRect, 6f, 6f, bgPaint)
                    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 9.5f
                        typeface = baseTypeface
                        color = palette.textSecondary
                    }
                    canvas.drawText("🎙  [Voice Note Attached]", MARGIN_HORIZONTAL + 10f, cursorY + 17f, labelPaint)
                    cursorY += item.height
                }

                is PdfElement.Spacer -> {
                    cursorY += item.height
                }
            }
        }
    }

    private fun drawFooter(
        canvas: Canvas,
        pageNumber: Int,
        totalPages: Int,
        palette: PdfPalette,
        baseTypeface: Typeface
    ) {
        val y = PAGE_HEIGHT - MARGIN_BOTTOM

        // Subtle divider
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.border
            strokeWidth = 0.75f
        }
        canvas.drawLine(MARGIN_HORIZONTAL, y, PAGE_WIDTH - MARGIN_HORIZONTAL, y, dividerPaint)

        // Left watermark brand label
        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 8.5f
            typeface = Typeface.create(baseTypeface, Typeface.BOLD)
            color = palette.textMuted
        }
        canvas.drawText("NAUGHTY", MARGIN_HORIZONTAL, y + 16f, brandPaint)

        // Right page numbering
        val pageText = "Page $pageNumber of $totalPages"
        val pagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 8.5f
            typeface = baseTypeface
            color = palette.textMuted
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(pageText, PAGE_WIDTH - MARGIN_HORIZONTAL, y + 16f, pagePaint)
    }

    // ---------------------------------------------------------------------------------------------
    // Image Handling Helpers
    // ---------------------------------------------------------------------------------------------

    private fun loadSampledBitmap(context: Context, uriString: String, maxTargetWidth: Int): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            fun openStream(): InputStream? {
                return if (uri.scheme == "file" || uri.scheme == null) {
                    val path = uri.path ?: uriString
                    File(path).inputStream()
                } else {
                    context.contentResolver.openInputStream(uri)
                }
            }

            // Read dimensions only
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openStream()?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null

            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            // Calculate inSampleSize
            var inSampleSize = 1
            var w = options.outWidth
            while (w / 2 >= maxTargetWidth) {
                w /= 2
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            openStream()?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
        } catch (_: Exception) {
            null
        }
    }
}
