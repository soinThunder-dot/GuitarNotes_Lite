package com.guitartabquiz

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * StaffView - renders a treble clef staff with up to 4 notes
 * COMPACT version: desiredH = 130px (was 200px)
 * Displays NOTATED pitch (guitar written pitch, sounds 8va lower)
 * Range: C4-B5 on treble clef
 * Staff lines from bottom: E4, G4, B4, D5, F5
 */
class StaffView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val staffPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCCCCC"); strokeWidth = 1.5f; style = Paint.Style.STROKE
    }
    private val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F0E68C"); style = Paint.Style.FILL
    }
    private val noteLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A2E"); textSize = 13f; textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val bgPaint = Paint().apply {
        color = Color.parseColor("#1A1A2E"); style = Paint.Style.FILL
    }
    private val ledgerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCCCCC"); strokeWidth = 1.5f; style = Paint.Style.STROKE
    }
    private val clefPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AAAAAA"); textSize = 60f; textAlign = Paint.Align.LEFT
    }

    var notes: List<Note> = emptyList()
        set(value) { field = value; invalidate() }

    // Steps from E4 (bottom staff line = step 0)
    // E4=0,F4=1,G4=2,A4=3,B4=4,C5=5,D5=6,E5=7,F5=8,G5=9,A5=10,B5=11
    // C4=-2, D4=-1
    private fun noteToStep(note: Note): Int {
        val noteNames = listOf("C","D","E","F","G","A","B")
        val name = note.name.dropLast(1)
        val octave = note.name.last().digitToInt()
        val noteIdx = noteNames.indexOf(name)
        val stepsFromC0 = octave * 7 + noteIdx
        return stepsFromC0 - 30  // E4 = 4*7+2 = 30
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Compact layout — tighter proportions for smaller height
        val staffLeft = 52f
        val staffRight = w - 12f
        val staffBottom = h * 0.72f
        val lineSpacing = h * 0.115f  // slightly tighter spacing

        // 5 staff lines
        for (i in 0..4) {
            val y = staffBottom - i * lineSpacing
            canvas.drawLine(staffLeft, y, staffRight, y, staffPaint)
        }

        // Treble clef
        clefPaint.textSize = lineSpacing * 5.2f
        canvas.drawText("\uD834\uDD1E", staffLeft - 10f, staffBottom + lineSpacing * 0.75f, clefPaint)

        if (notes.isEmpty()) return

        val noteStartX = staffLeft + lineSpacing * 2.8f
        val noteAreaW = staffRight - noteStartX - lineSpacing
        val spacing = if (notes.size > 1) noteAreaW / (notes.size - 1) else noteAreaW / 2

        notes.forEachIndexed { idx, note ->
            val step = noteToStep(note)
            val halfStep = lineSpacing / 2f
            val noteY = staffBottom - step * halfStep
            val noteX = if (notes.size == 1) noteStartX + noteAreaW / 2
                        else noteStartX + idx * spacing
            val r = lineSpacing * 0.38f

            // Ledger lines below staff (C4 step=-2, D4 step=-1)
            if (step <= -1) {
                for (s in step..(-1)) {
                    val ly = staffBottom - s * halfStep
                    canvas.drawLine(noteX - r * 1.6f, ly, noteX + r * 1.6f, ly, ledgerPaint)
                }
            }
            // Ledger lines above staff (G5 step=9+)
            if (step >= 9) {
                for (s in 9..step) {
                    if (s % 2 == 1 || s == step) {
                        val ly = staffBottom - s * halfStep
                        canvas.drawLine(noteX - r * 1.6f, ly, noteX + r * 1.6f, ly, ledgerPaint)
                    }
                }
            }

            // Note head (filled oval)
            canvas.drawOval(noteX - r * 1.1f, noteY - r * 0.75f, noteX + r * 1.1f, noteY + r * 0.75f, notePaint)

            // Note name label below staff
            noteLabelPaint.textSize = lineSpacing * 0.52f
            canvas.drawText(note.name, noteX, staffBottom + lineSpacing * 1.6f, noteLabelPaint)

            // Stem up
            val stemPaint = Paint(staffPaint).apply { strokeWidth = 1.5f; color = Color.parseColor("#F0E68C") }
            canvas.drawLine(noteX + r, noteY, noteX + r, noteY - lineSpacing * 2.8f, stemPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // COMPACT: height reduced from 200 to 130
        setMeasuredDimension(resolveSize(680, widthMeasureSpec), resolveSize(130, heightMeasureSpec))
    }
}
