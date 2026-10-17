package com.srijeesolution.rojgaarwaala.utils.resume

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Picture
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import com.srijeesolution.rojgaarwaala.utils.resume.ResumeCopy.Text
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Draws the resume as a single A4 page.
 *
 * Done on the phone rather than on the server because Android lays out Hindi
 * (Devanagari) correctly, joined letters and all, while the usual server PDF
 * libraries break it. A long resume is drawn a little smaller until it fits.
 */
object ResumePdfWriter {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 36f

    /** Tried in order; the first size whose content fits above the declaration wins. */
    private val SCALES = listOf(1f, 0.94f, 0.88f, 0.82f, 0.77f, 0.72f, 0.68f)

    private val AMBER = Color.parseColor("#FFBB02")
    private val ACCENT_TEXT = Color.parseColor("#8A5F00")
    private val DARK = Color.parseColor("#1A1A1A")
    private val BODY = Color.parseColor("#333333")
    private val MUTED = Color.parseColor("#666666")
    private val HAIRLINE = Color.parseColor("#D9D9D9")
    private val ROW_LINE = Color.parseColor("#ECECEC")
    private val SOFT = Color.parseColor("#FFF3CC")
    private val SOFT_TEXT = Color.parseColor("#5A4300")
    private val PHOTO_EMPTY = Color.parseColor("#E3E3E3")

    /** Writes the PDF to [out]. Always one page. */
    fun write(data: ResumeData, photo: Bitmap?, out: File) {
        val footerTop = PAGE_H - MARGIN - footerHeight(data)
        val scale = SCALES.firstOrNull { k ->
            val picture = Picture()
            val bottom = body(picture.beginRecording(PAGE_W, PAGE_H), data, photo, k)
            picture.endRecording()
            bottom <= footerTop - 10
        } ?: SCALES.last()

        val doc = PdfDocument()
        try {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)
            canvas.save()
            // Anything still too long at the smallest size is cut above the declaration.
            canvas.clipRect(0f, 0f, PAGE_W.toFloat(), footerTop - 4)
            body(canvas, data, photo, scale)
            canvas.restore()
            footer(canvas, data, footerTop)
            doc.finishPage(page)
            out.parentFile?.mkdirs()
            out.outputStream().use { doc.writeTo(it) }
        } finally {
            doc.close()
        }
    }

    // ---------------------------------------------------------------- body

    /** Draws everything above the declaration at size [k] and returns the y below it. */
    private fun body(c: Canvas, d: ResumeData, photo: Bitmap?, k: Float): Float {
        val lang = d.language
        val left = MARGIN
        val width = (PAGE_W - 2 * MARGIN).toInt()

        // Header: name, wanted work and contact, photo on the right.
        val photoW = 84f * k
        val photoH = 102f * k
        val photoRect = RectF(PAGE_W - MARGIN - photoW, MARGIN, PAGE_W - MARGIN, MARGIN + photoH)
        drawPhoto(c, photo, photoRect)

        val headW = (width - photoW - 16).toInt()
        var y = MARGIN - 2
        y = text(c, d.name, paint(22f * k, DARK, bold = true), left, y, headW)
        if (d.roles.isNotEmpty()) {
            y = text(c, d.roles.joinToString(" • "), paint(12f * k, ACCENT_TEXT, bold = true), left, y + 3 * k, headW)
        }
        y += 6 * k
        val contact = paint(10f * k, BODY)
        y = labelled(c, Text.MOBILE.of(lang), d.mobile, contact, left, y, headW)
        if (d.email.isNotBlank()) y = labelled(c, Text.EMAIL.of(lang), d.email, contact, left, y + 1, headW)
        y = labelled(c, Text.CURRENT_ADDRESS.of(lang), place(d), contact, left, y + 1, headW)

        y = max(y, photoRect.bottom) + 10 * k
        c.drawRect(left, y, left + width, y + 2f, fill(AMBER))
        y += 4f

        // Personal details.
        y = heading(c, Text.PERSONAL.of(lang), lang, left, y, width, k)
        val rows = listOfNotNull(
            d.dob.takeIf { it.isNotBlank() }?.let { dob ->
                val age = ResumeRules.ageOn(dob)?.let { " ($it ${Text.YEARS.of(lang)})" }.orEmpty()
                Text.DOB.of(lang) to ResumeCopy.date(dob, lang) + age
            },
            d.jobLocations.takeIf { it.isNotEmpty() }?.let { list ->
                Text.JOB_LOCATION.of(lang) to list.joinToString(", ") { ResumeCopy.label(it, lang) }
            },
        )
        y = keyValues(c, rows, left, y, width, k)

        // Work experience.
        y = heading(c, Text.EXPERIENCE.of(lang), lang, left, y, width, k)
        if (d.fresher || d.experiences.isEmpty()) {
            y = text(c, Text.FRESHER.of(lang), paint(10.5f * k, BODY), left, y, width)
        } else {
            val role = paint(11f * k, DARK, bold = true)
            val years = paint(10f * k, MUTED)
            val company = paint(10f * k, BODY)
            d.experiences.forEachIndexed { index, e ->
                if (index > 0) y += 5 * k
                val duration = ResumeCopy.duration(e.years, lang)
                val durationW = years.measureText(duration)
                val roleBottom = text(c, e.role, role, left, y, (width - durationW - 12).toInt())
                c.drawText(duration, left + width - durationW, y - years.ascent(), years)
                y = roleBottom
                if (e.company.isNotBlank()) y = text(c, e.company, company, left, y + 1, width)
            }
        }

        // Education, as a small table.
        if (d.education.isNotEmpty()) {
            y = heading(c, Text.EDUCATION.of(lang), lang, left, y, width, k)
            y = educationTable(c, d, left, y, width, k)
        }

        if (d.skills.isNotEmpty()) {
            y = heading(c, Text.SKILLS.of(lang), lang, left, y, width, k)
            y = tags(c, d.skills.map { ResumeCopy.label(it, lang) }, left, y, width, k)
        }

        val extra = listOfNotNull(
            d.languages.takeIf { it.isNotEmpty() }?.let { l -> Text.LANGUAGES.of(lang) to l.joinToString(", ") { ResumeCopy.label(it, lang) } },
            d.licences.takeIf { it.isNotEmpty() }?.let { l -> Text.LICENCES.of(lang) to l.joinToString(", ") { ResumeCopy.label(it, lang) } },
        )
        if (extra.isNotEmpty()) {
            y = heading(c, Text.LANGUAGES_LICENCE.of(lang), lang, left, y, width, k)
            y = keyValues(c, extra, left, y, width, k)
        }
        return y
    }

    private fun place(d: ResumeData) = listOf(d.address, d.district).filter { it.isNotBlank() }.joinToString(", ")

    // ---------------------------------------------------------------- footer

    private fun footerHeight(d: ResumeData): Float {
        val body = layout(Text.DECLARATION.of(d.language), paint(10f, MUTED), (PAGE_W - 2 * MARGIN).toInt())
        return 22f + body.height + 50f
    }

    /** Declaration, date, place and signature, always at the foot of the page. */
    private fun footer(c: Canvas, d: ResumeData, top: Float) {
        val lang = d.language
        val left = MARGIN
        val width = (PAGE_W - 2 * MARGIN).toInt()
        var y = heading(c, Text.DECLARATION_TITLE.of(lang), lang, left, top - 8, width, 1f)
        y = text(c, Text.DECLARATION.of(lang), paint(10f, MUTED), left, y, width)

        val body = paint(10f, BODY)
        val line1 = y + 30
        val line2 = line1 + 15
        c.drawText("${Text.DATE.of(lang)}: ______________", left, line1, body)
        c.drawText("${Text.PLACE.of(lang)}: ${d.district}", left, line2, body)

        val signW = 150f
        val signX = left + width - signW
        c.drawRect(signX, line1 - 2, signX + signW, line1 - 1.2f, fill(DARK))
        val name = paint(10.5f, DARK, bold = true)
        val nameText = TextUtils.ellipsize(d.name, name, signW, TextUtils.TruncateAt.END).toString()
        c.drawText(nameText, signX + (signW - name.measureText(nameText)) / 2, line2, name)
    }

    // ---------------------------------------------------------------- pieces

    /** Amber tick, title and a hairline running to the right edge. Returns y below it. */
    private fun heading(c: Canvas, title: String, lang: ResumeLanguage, x: Float, top: Float, width: Int, k: Float): Float {
        val y = top + 12 * k
        val p = paint(11f * k, DARK, bold = true).apply {
            if (lang == ResumeLanguage.ENGLISH) letterSpacing = 0.06f
        }
        val label = if (lang == ResumeLanguage.ENGLISH) title.uppercase() else title
        val baseline = y - p.ascent()
        val midY = baseline + (p.ascent() + p.descent()) / 2
        c.drawRect(x, midY - 6 * k, x + 3f, midY + 6 * k, fill(AMBER))
        c.drawText(label, x + 9f, baseline, p)
        val lineStart = x + 9f + p.measureText(label) + 8f
        if (lineStart < x + width) c.drawRect(lineStart, midY, x + width, midY + 0.6f, fill(HAIRLINE))
        return baseline + p.descent() + 5 * k
    }

    /** "Label: value" with the label in a darker weight. */
    private fun labelled(c: Canvas, label: String, value: String, p: TextPaint, x: Float, y: Float, width: Int): Float {
        if (value.isBlank()) return y
        val bold = TextPaint(p).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = DARK }
        val head = "$label: "
        val headW = bold.measureText(head)
        c.drawText(head, x, y - p.ascent(), bold)
        return text(c, value, p, x + headW, y, (width - headW).toInt())
    }

    /** Two columns: muted labels on the left, values on the right. */
    private fun keyValues(c: Canvas, rows: List<Pair<String, String>>, x: Float, top: Float, width: Int, k: Float): Float {
        if (rows.isEmpty()) return top
        val key = paint(10f * k, MUTED)
        val value = paint(10.5f * k, BODY)
        val keyW = rows.maxOf { key.measureText(it.first) } + 14 * k
        var y = top
        rows.forEach { (label, text) ->
            c.drawText(label, x, y - key.ascent() + 0.5f, key)
            y = text(c, text, value, x + keyW, y, (width - keyW).toInt()) + 3 * k
        }
        return y
    }

    private fun educationTable(c: Canvas, d: ResumeData, x: Float, top: Float, width: Int, k: Float): Float {
        val lang = d.language
        val cols = floatArrayOf(0.3f, 0.55f, 0.15f).map { it * width }
        val pad = 6f * k
        val head = paint(10f * k, SOFT_TEXT, bold = true)
        val cell = paint(10.5f * k, BODY)

        fun row(values: List<String>, p: TextPaint, yTop: Float, shaded: Boolean): Float {
            val layouts = values.mapIndexed { i, v -> layout(v, p, (cols[i] - 2 * pad).toInt()) }
            val h = layouts.maxOf { it.height } + 2 * pad * 0.7f
            if (shaded) c.drawRect(x, yTop, x + width, yTop + h, fill(SOFT))
            var cx = x
            layouts.forEachIndexed { i, l ->
                draw(c, l, cx + pad, yTop + pad * 0.7f)
                cx += cols[i]
            }
            c.drawRect(x, yTop + h - 0.6f, x + width, yTop + h, fill(ROW_LINE))
            return yTop + h
        }

        var y = row(
            listOf(Text.QUALIFICATION.of(lang), Text.INSTITUTE.of(lang), Text.YEAR_COLUMN.of(lang)),
            head, top, shaded = true,
        )
        d.education.forEach { e ->
            y = row(
                listOf(ResumeCopy.label(e.level, lang), e.institute.ifBlank { "—" }, if (e.year > 0) e.year.toString() else "—"),
                cell, y, shaded = false,
            )
        }
        return y + 2 * k
    }

    private fun tags(c: Canvas, tags: List<String>, x: Float, top: Float, width: Int, k: Float): Float {
        val tp = paint(10f * k, SOFT_TEXT)
        val padH = 8f * k
        val chipH = 18f * k
        val gap = 5f * k
        var cx = x
        var y = top
        for (tag in tags) {
            val label = TextUtils.ellipsize(tag, tp, width - 2 * padH, TextUtils.TruncateAt.END).toString()
            val w = tp.measureText(label) + 2 * padH
            if (cx + w > x + width && cx > x) {
                cx = x
                y += chipH + gap
            }
            val rect = RectF(cx, y, cx + w, y + chipH)
            c.drawRoundRect(rect, chipH / 2, chipH / 2, fill(SOFT))
            c.drawText(label, cx + padH, rect.centerY() - (tp.descent() + tp.ascent()) / 2, tp)
            cx += w + gap
        }
        return y + chipH + 2 * k
    }

    private fun drawPhoto(canvas: Canvas, photo: Bitmap?, dest: RectF) {
        val frame = RectF(dest).apply { inset(-1.5f, -1.5f) }
        canvas.drawRoundRect(frame, 6f, 6f, fill(AMBER))
        canvas.save()
        canvas.clipPath(Path().apply { addRoundRect(dest, 5f, 5f, Path.Direction.CW) })
        if (photo == null) {
            canvas.drawRect(dest, fill(PHOTO_EMPTY))
        } else {
            // Centre-crop: fill the box, keep the middle of the photo.
            val scale = max(dest.width() / photo.width, dest.height() / photo.height)
            val srcW = min(photo.width, (dest.width() / scale).toInt())
            val srcH = min(photo.height, (dest.height() / scale).toInt())
            val left = (photo.width - srcW) / 2
            val top = (photo.height - srcH) / 2
            canvas.drawBitmap(photo, Rect(left, top, left + srcW, top + srcH), dest, Paint(Paint.FILTER_BITMAP_FLAG))
        }
        canvas.restore()
    }

    private fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }

    private fun paint(size: Float, color: Int, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
    }

    private fun layout(text: CharSequence, paint: TextPaint, width: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, max(1, width))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.1f)
            .setIncludePad(false)
            .build()

    private fun draw(canvas: Canvas, layout: StaticLayout, x: Float, y: Float) {
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    /** Draws wrapped text and returns the y just below it. */
    private fun text(canvas: Canvas, text: String, paint: TextPaint, x: Float, y: Float, width: Int): Float {
        if (text.isBlank()) return y
        val l = layout(text, paint, width)
        draw(canvas, l, x, y)
        return y + l.height
    }
}
