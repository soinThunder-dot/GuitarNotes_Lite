package com.guitartabquiz

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.roundToInt

/**
 * StaffView - renders a treble clef staff with up to 4 notes
 * Displays NOTATED pitch (guitar written pitch, sounds 8va lower)
 * Range: C4-B5 on treble clef
 * Staff lines from bottom: E4, G4, B4, D5, F5
 * Spaces:        F4, A4, C5, E5
 * Ledger lines below: D4(1 below), C4(1 below + ledger)
 * Ledger lines above: G5(1 above), A5(1 above), B5(1 above + ledger)
 */
class StaffView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val staffPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCCCCC"); strokeWidth = 2f; style = Paint.Style.STROKE
    }
    private val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F0E68C"); style = Paint.Style.FILL
    }
    private val noteLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A2E"); textSize = 16f; textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val bgPaint = Paint().apply {
        color = Color.parseColor("#1A1A2E"); style = Paint.Style.FILL
    }
    private val ledgerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCCCCC"); strokeWidth = 2f; style = Paint.Style.STROKE
    }
    private val clefPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AAAAAA"); textSize = 80f; textAlign = Paint.Align.LEFT
    }

    var notes: List<Note> = emptyList()
        set(value) { field = value; invalidate() }

    // Treble clef staff: 5 lines
    // Position mapping: step from bottom line (E4)
    // E4=0, F4=1, G4=2, A4=3, B4=4, C5=5, D5=6, E5=7, F5=8, G5=9, A5=10, B5=11
    // C4 = -2 (two steps below E4)
    // D4 = -1
    private fun noteToStep(note: Note): Int {
        // notated pitch: C4=60
        val noteNames = listOf("C","D","E","F","G","A","B")
        val name = note.name.dropLast(1)
        val octave = note.name.last().digitToInt()
        val noteIdx = noteNames.indexOf(name)
        // Steps from C0
        val stepsFromC0 = octave * 7 + noteIdx
        // E4 is step = 4*7+2 = 30
        return stepsFromC0 - 30
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val staffLeft = 64f
        val staffRight = w - 20f
        val staffBottom = h * 0.75f
        val lineSpacing = h * 0.10f  // space between staff lines

        // Draw 5 staff lines
        for (i in 0..4) {
            val y = staffBottom - i * lineSpacing
            canvas.drawLine(staffLeft, y, staffRight, y, staffPaint)
        }

        // Draw treble clef symbol (text approximation)
        clefPaint.textSize = lineSpacing * 5.5f
        canvas.drawText("\uD834\uDD1E", staffLeft - 8f, staffBottom + lineSpacing * 0.8f, clefPaint)

        if (notes.isEmpty()) return

        // Place notes evenly across staff width
        val noteStartX = staffLeft + lineSpacing * 3f
        val noteAreaW = staffRight - noteStartX - lineSpacing
        val spacing = if (notes.size > 1) noteAreaW / (notes.size - 1) else noteAreaW / 2

        notes.forEachIndexed { idx, note ->
            val step = noteToStep(note)
            // Y position: step 0 = bottom line (E4) = staffBottom
            // Each step = lineSpacing / 2
            val halfStep = lineSpacing / 2f
            val noteY = staffBottom - step * halfStep

            val noteX = if (notes.size == 1)
                noteStartX + noteAreaW / 2
            else
                noteStartX + idx * spacing

            val r = lineSpacing * 0.4f

            // Draw ledger lines if needed
            // Below staff: step <= -1 (D4 or lower)
            if (step <= -1) {
                // draw ledger lines from step -2 up to step -1 (every even step = line)
                for (s in step..(-1)) {
                    if (s % 2 == 0 || s == -1) { // C4 (step -2) and D4 (step -1)
                        val ly = staffBottom - s * halfStep
                        canvas.drawLine(noteX - r * 1.5f, ly, noteX + r * 1.5f, ly, ledgerPaint)
                    }
                }
            }
            // Above staff: step >= 9 (G5+)
            if (step >= 9) {
                for (s in 9..step) {
                    if (s % 2 == 1 || s == step) {
                        val ly = staffBottom - s * halfStep
                        canvas.drawLine(noteX - r * 1.5f, ly, noteX + r * 1.5f, ly, ledgerPaint)
                    }
                }
            }

            // Draw note head (oval)
            canvas.drawOval(noteX - r * 1.1f, noteY - r * 0.8f, noteX + r * 1.1f, noteY + r * 0.8f, notePaint)

            // Note name label below
            noteLabelPaint.textSize = lineSpacing * 0.55f
            canvas.drawText(note.name, noteX, staffBottom + lineSpacing * 1.8f, noteLabelPaint)

            // Stem up
            val stemPaint = Paint(staffPaint).apply { strokeWidth = 2f; color = Color.parseColor("#F0E68C") }
            canvas.drawLine(noteX + r, noteY, noteX + r, noteY - lineSpacing * 3f, stemPaint)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(resolveSize(680, widthMeasureSpec), resolveSize(200, heightMeasureSpec))
    }
}
