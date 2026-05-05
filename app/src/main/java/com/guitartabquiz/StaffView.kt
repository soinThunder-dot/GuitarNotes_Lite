package com.guitartabquiz

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * StaffView - renders either a TREBLE or BASS clef staff for each note.
 * Each note can be coloured: DEFAULT (gold), CORRECT (green), WRONG (red).
 * Clef per note is determined by Note.clef (from MusicData).
 * TREBLE: bottom line = E4, step reference = E4 (step 0)
 * BASS:   bottom line = G2, step reference = G2 (step 0)
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

    private val colorDefault = Color.parseColor("#F0E68C")  // gold
    private val colorCorrect = Color.parseColor("#4CAF50")  // green
    private val colorWrong   = Color.parseColor("#F44336")  // red

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

    /**
     * TREBLE step: bottom staff line = E4 = step 0
     * Lines: E4(0), G4(2), B4(4), D5(6), F5(8)
     * Each step = half a line spacing
     * Reference: stepsFromC0 for E4 = 4*7+2 = 30
     */
    private fun trebleStep(note: Note): Int {
        val noteNames = listOf("C","D","E","F","G","A","B")
        val name = note.name.dropLast(1)
        val octave = note.name.last().digitToInt()
        val noteIdx = noteNames.indexOf(name)
        val stepsFromC0 = octave * 7 + noteIdx
        return stepsFromC0 - 30  // E4 = 30
    }

    /**
     * BASS step: bottom staff line = G2 = step 0
     * Lines: G2(0), B2(2), D3(4), F3(6), A3(8)
     * Reference: stepsFromC0 for G2 = 2*7+4 = 18
     */
    private fun bassStep(note: Note): Int {
        val noteNames = listOf("C","D","E","F","G","A","B")
        val name = note.name.dropLast(1)
        val octave = note.name.last().digitToInt()
        val noteIdx = noteNames.indexOf(name)
        val stepsFromC0 = octave * 7 + noteIdx
        return stepsFromC0 - 18  // G2 = 18
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        if (notes.isEmpty()) return

        val lineSpacing = h * 0.12f
        val halfSp = lineSpacing / 2f
        val staffLeft = 56f
        val staffRight = w - 14f
        val noteStartX = staffLeft + lineSpacing * 3f
        val noteAreaW = staffRight - noteStartX - lineSpacing
        val spacing = if (notes.size > 1) noteAreaW / (notes.size - 1) else noteAreaW / 2f

        notes.forEachIndexed { idx, note ->
            // Each note may have its own clef -> its own staffBottom Y
            // Space 4 mini-staves evenly; each mini-staff is drawn independently
            val isTreble = note.clef == Clef.TREBLE
            val step = if (isTreble) trebleStep(note) else bassStep(note)

            // staffBottom: centre the staff vertically in the view
            // For treble: centre at 65% height; for bass: same (staff is 5-line universal)
            val staffBottom = h * 0.70f

            // 5 staff lines
            for (i in 0..4) {
                val y = staffBottom - i * lineSpacing
                canvas.drawLine(staffLeft, y, staffRight, y, staffPaint)
            }

            // Draw clef symbol only for the first note (avoid overdraw on shared staff)
            if (idx == 0) {
                if (isTreble) {
                    // Treble clef (U+1D11E)
                    clefPaint.textSize = lineSpacing * 5.2f
                    canvas.drawText("\uD834\uDD1E", staffLeft - 12f, staffBottom + lineSpacing * 0.75f, clefPaint)
                } else {
                    // Bass clef (U+1D122)
                    clefPaint.textSize = lineSpacing * 2.8f
                    canvas.drawText("\uD834\uDD22", staffLeft - 8f, staffBottom - lineSpacing * 1.5f, clefPaint)
                }
            }

            val noteX = if (notes.size == 1) noteStartX + noteAreaW / 2f else noteStartX + idx * spacing
            val noteY = staffBottom - step * halfSp
            val r = lineSpacing * 0.40f

            val state = noteStates.getOrElse(idx) { NoteState.DEFAULT }
            val noteColor = when (state) {
                NoteState.CORRECT -> colorCorrect
                NoteState.WRONG   -> colorWrong
                NoteState.DEFAULT -> colorDefault
            }
            val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = noteColor; style = Paint.Style.FILL }
            val stemPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = noteColor; style = Paint.Style.STROKE; strokeWidth = 1.8f }

            // Ledger lines BELOW bottom line (step < 0)
            if (step < 0) {
                var s = -2
                while (s >= step) {
                    val ly = staffBottom - s * halfSp
                    canvas.drawLine(noteX - r * 1.6f, ly, noteX + r * 1.6f, ly, ledgerPaint)
                    s -= 2
                }
            }

            // Ledger lines ABOVE top line (step > 8)
            if (step > 8) {
                var s = 10
                while (s <= step) {
                    val ly = staffBottom - s * halfSp
                    canvas.drawLine(noteX - r * 1.6f, ly, noteX + r * 1.6f, ly, ledgerPaint)
                    s += 2
                }
            }

            // Note head
            canvas.drawOval(noteX - r * 1.1f, noteY - r * 0.75f, noteX + r * 1.1f, noteY + r * 0.75f, notePaint)

            // Stem
            canvas.drawLine(noteX + r, noteY, noteX + r, noteY - lineSpacing * 2.8f, stemPaint)

            // Note name label
            labelPaint.textSize = lineSpacing * 0.52f
            labelPaint.color = noteColor
            canvas.drawText(note.name, noteX, staffBottom + lineSpacing * 1.7f, labelPaint)

            // State badge
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
