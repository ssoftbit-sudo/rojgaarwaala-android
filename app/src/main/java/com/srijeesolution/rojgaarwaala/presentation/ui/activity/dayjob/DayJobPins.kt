package com.srijeesolution.rojgaarwaala.presentation.ui.activity.dayjob

import android.content.Context

/** Map pins for 1 Day Job: a building for the work site, a person for a worker. */
object DayJobPins {
    private val pinCache = mutableMapOf<String, com.google.android.gms.maps.model.BitmapDescriptor>()

    /**
     * A round map pin: [colour] disc, dark ring, white [icon] inside; with [label] the
     * name sits under it in the same colour, so a person and their name match.
     */
    fun mapPin(context: Context, colour: Int, icon: Int, sizeDp: Int, label: String? = null): com.google.android.gms.maps.model.BitmapDescriptor =
        pinCache.getOrPut("$colour/$icon/$sizeDp/$label") {
            val size = DayJobUi.dp(context, sizeDp)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
            val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textSize = DayJobUi.dp(context, 13).toFloat()
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                color = colour
                setShadowLayer(DayJobUi.dp(context, 2).toFloat(), 0f, 0f, 0xFF000000.toInt())
            }
            val textWidth = label?.let { textPaint.measureText(it).toInt() + DayJobUi.dp(context, 6) } ?: 0
            val width = maxOf(size, textWidth)
            val height = size + if (label != null) DayJobUi.dp(context, 18) else 0
            val bmp = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            val cx = width / 2f
            paint.color = 0xFF111111.toInt()
            canvas.drawCircle(cx, size / 2f, size / 2f, paint)
            paint.color = colour
            canvas.drawCircle(cx, size / 2f, size / 2f - DayJobUi.dp(context, 3), paint)
            androidx.core.content.ContextCompat.getDrawable(context, icon)!!.mutate().apply {
                val pad = size / 5
                val left = (cx - size / 2f).toInt()
                setBounds(left + pad, pad, left + size - pad, size - pad)
                draw(canvas)
            }
            label?.let {
                textPaint.textAlign = android.graphics.Paint.Align.CENTER
                canvas.drawText(it, cx, size + DayJobUi.dp(context, 14).toFloat(), textPaint)
            }
            com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(bmp)
        }

    /** Pin anchor for [mapPin]: the disc's centre, which sits above any name. */
    fun pinAnchorY(context: Context, sizeDp: Int, label: String?): Float {
        if (label == null) return 0.5f
        val size = DayJobUi.dp(context, sizeDp)
        return (size / 2f) / (size + DayJobUi.dp(context, 18))
    }
}
