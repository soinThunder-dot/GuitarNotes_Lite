package com.guitartabquiz

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * GuitarTabView - draws a 24-fret x 6-string guitar tab grid
 * Highlights a specific TabPosition (string + fret)
 * Strings: top=1(high e), bottom=6(low E)
 * Frets: columns 0(open/nut) to 23
 */
class GuitarTabView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        const val NUM_STRINGS = 6
        const val NUM_FRETS = 24
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCCCCC"); strokeWidth = 1.5f; style = Paint.Style.STROKE
    }
    private val nutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFFF"); strokeWidth = 5f; style = Paint.Style.STROKE
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4FC3F7"); style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 20f; textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val sidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#888888"); textSize = 13f; textAlign = Paint.Align.CENTER
    }
    private val bgPaint = Paint().apply {
        color = Color.parseColor("#1A1A2E"); style = Paint.Style.FILL
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#334466"); style = Paint.Style.FILL
    }

    var tabPosition: TabPosition? = null
        set(value) { field = value; invalidate() }
    var isCorrect: Boolean? = null
        set(value) { field = value; invalidate() }

    private val STRING_NAMES = listOf("e","B","G","D","A","E")

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val padL = 32f; val padR = 8f; val padT = 22f; val padB = 18f
        val gridW = w - padL - padR; val gridH = h - padT - padB
        val fretW = gridW / NUM_FRETS
        val strH = gridH / (NUM_STRINGS - 1)

        // Fret numbers top
        for (f in listOf(3,5,7,9,12,15,17,19,21)) {
            canvas.drawText("$f", padL + (f - 0.5f) * fretW, padT - 5f, sidePaint)
        }
        // String names left
        for (s in 0 until NUM_STRINGS) {
            canvas.drawText(STRING_NAMES[s], padL - 18f, padT + s * strH + 5f, sidePaint)
        }
        // Strings horizontal
        for (s in 0 until NUM_STRINGS) {
            val y = padT + s * strH
            canvas.drawLine(padL, y, padL + gridW, y, linePaint)
        }
        // Frets vertical
        for (f in 0..NUM_FRETS) {
            val x = padL + f * fretW
            canvas.drawLine(x, padT, x, padT + gridH, if (f == 0) nutPaint else linePaint)
        }
        // Inlay dots
        for (f in listOf(3,5,7,9,15,17,19,21)) {
            canvas.drawCircle(padL + (f - 0.5f) * fretW, padT + gridH/2, fretW * 0.22f, dotPaint)
        }
        // Double dot at 12
        val x12 = padL + 11.5f * fretW
        canvas.drawCircle(x12, padT + strH, fretW * 0.22f, dotPaint)
        canvas.drawCircle(x12, padT + gridH - strH, fretW * 0.22f, dotPaint)

        // Highlight selected position
        tabPosition?.let { tp ->
            val sIdx = tp.string - 1
            val y = padT + sIdx * strH
            highlightPaint.color = when (isCorrect) {
                true -> Color.parseColor("#4CAF50")
                false -> Color.parseColor("#F44336")
                null -> Color.parseColor("#4FC3F7")
            }
            if (tp.fret == 0) {
                val p = Paint(highlightPaint).apply { style = Paint.Style.STROKE; strokeWidth = 3f }
                canvas.drawCircle(padL - 14f, y, fretW * 0.35f, p)
            } else {
                val x = padL + (tp.fret - 0.5f) * fretW
                val r = minOf(strH, fretW) * 0.4f
                canvas.drawCircle(x, y, r, highlightPaint)
                labelPaint.textSize = r * 1.1f
                canvas.drawText("${tp.fret}", x, y + r * 0.38f, labelPaint)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(resolveSize(620, widthMeasureSpec), resolveSize(140, heightMeasureSpec))
    }
}
