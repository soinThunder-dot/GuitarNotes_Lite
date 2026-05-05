package com.guitartabquiz

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * StaffView - renders a treble clef staff with up to 4 notes.
 * Each note can be coloured: DEFAULT (gold), CORRECT (green), WRONG (red).
 * Used in landscape quiz: shown at top of screen as the "puzzle" (謎面).
 */
enum class NoteState { DEFAULT, CORRECT, WRONG }

class StaffView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val staffPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCCCCC"); strokeWidth = 1.8f; style = Paint.Style.STROKE
    }
    private val bgPaint = Paint().apply {
        color = Color.parseColor("#1A1A2E"); style = Paint.Style.FILL
    }
    private val ledgerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCCCCC"); strokeWidth = 1.8f; style = Paint.Style.STROKE
    }
    private val clefPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AAAAAA"); textAlign = Paint.Align.LEFT
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A2E"); textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    // Note colors per state
    private val colorDefault = Color.parseColor("#F0E68C")   // gold
    private val colorCorrect = Color.parseColor("#4CAF50")   // green
    private val colorWrong   = Color.parseColor("#F44336")   // red

    var notes: List<Note> = emptyList()
        set(value) { field = value; noteStates = MutableList(value.size) { NoteState.DEFAULT }; invalidate() }

    var noteStates: MutableList<NoteState> = mutableListOf()
        set(value) { field = value; invalidate() }

    fun setNoteState(index: Int, state: NoteState) {
        if (index in noteStates.indices) {
            noteStates[index] = state
            invalidate()
        }
    }

    // Steps from E4 (bottom staff line = step 0)
    // E4=0,F4=1,G4=2,A4=3,B4=4,C5=5,D5=6,E5=7,F5=8,G5=9,A5=10,B5=11
    // C4=-2, D4=-1
    private fun noteToStep(note: Note): Int {
        val noteNames = listOf("C","D","E","F","G","A","B")
        val name = note.name.dropLast(1)
        val octave = note.name.last().digitToInt()
        val noteIdx = noteNames.indexOf(name)
        val stepsFromC0 = octave * 7 + noteIdx
        return stepsFromC0 - 30   // E4 = 4*7+2 = 30
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val staffLeft    = 56f
        val staffRight   = w - 14f
        val staffBottom  = h * 0.70f
        val lineSpacing  = h * 0.12f

        // 5 staff lines
        for (i in 0..4) {
            val y = staffBottom - i * lineSpacing
            canvas.drawLine(staffLeft, y, staffRight, y, staffPaint)
        }

        // Treble clef
        clefPaint.textSize = lineSpacing * 5.2f
        canvas.drawText("\uD834\uDD1E", staffLeft - 12f, staffBottom + lineSpacing * 0.75f, clefPaint)

        if (notes.isEmpty()) return

        val noteStartX = staffLeft + lineSpacing * 3f
        val noteAreaW  = staffRight - noteStartX - lineSpacing
        val spacing    = if (notes.size > 1) noteAreaW / (notes.size - 1) else noteAreaW / 2f

        notes.forEachIndexed { idx, note ->
            val step   = noteToStep(note)
            val halfSp = lineSpacing / 2f
            val noteY  = staffBottom - step * halfSp
            val noteX  = if (notes.size == 1) noteStartX + noteAreaW / 2f
                         else noteStartX + idx * spacing
            val r      = lineSpacing * 0.40f

            val state  = noteStates.getOrElse(idx) { NoteState.DEFAULT }
            val noteColor = when (state) {
                NoteState.CORRECT -> colorCorrect
                NoteState.WRONG   -> colorWrong
                NoteState.DEFAULT -> colorDefault
            }

            val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = noteColor; style = Paint.Style.FILL
            }
            val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = noteColor; style = Paint.Style.STROKE; strokeWidth = 1.8f
            }

            // Ledger lines below staff (C4=-2, D4=-1)
            if (step <= -1) {
                for (s in step..-1) {
                    if (s % 2 == 0 || s == step) {
                        val ly = staffBottom - s * halfSp
                        canvas.drawLine(noteX - r * 1.6f, ly, noteX + r * 1.6f, ly, ledgerPaint)
                    }
                }
            }
            // Ledger lines above staff (above G5 step=9)
            if (step >= 9) {
                for (s in 9..step) {
                    if (s % 2 == 1 || s == step) {
                        val ly = staffBottom - s * halfSp
                        canvas.drawLine(noteX - r * 1.6f, ly, noteX + r * 1.6f, ly, ledgerPaint)
                    }
                }
            }

            // Note head
            canvas.drawOval(noteX - r * 1.1f, noteY - r * 0.75f,
                            noteX + r * 1.1f, noteY + r * 0.75f, notePaint)

            // Stem up
            canvas.drawLine(noteX + r, noteY, noteX + r, noteY - lineSpacing * 2.8f, stemPaint)

            // Note name label below staff
            labelPaint.textSize = lineSpacing * 0.52f
            labelPaint.color = noteColor
            canvas.drawText(note.name, noteX, staffBottom + lineSpacing * 1.7f, labelPaint)

            // State badge: tick or cross
            if (state != NoteState.DEFAULT) {
                val badge = if (state == NoteState.CORRECT) "\u2713" else "\u2717"
                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = noteColor; textSize = lineSpacing * 0.9f; textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                }
                canvas.drawText(badge, noteX, noteY - lineSpacing * 3.2f, badgePaint)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(800, widthMeasureSpec),
            resolveSize(150, heightMeasureSpec)
        )
    }
}
