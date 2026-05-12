package com.guitartabquiz

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

// 謎面狀態：預設（金色）、正確（綠色）、錯誤（紅色）
enum class NoteState { DEFAULT, CORRECT, WRONG }

// 五線譜 View — 繪製高音譜，顯示最多 8 個音符
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

    private val colorDefault = Color.parseColor("#F0E68C")  // 金色
    private val colorCorrect = Color.parseColor("#4CAF50")  // 綠色
    private val colorWrong   = Color.parseColor("#F44336")  // 紅色

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

    // 計算音符在高音譜上的位置（最低線 E4 = step 0）
    private fun trebleStep(note: Note): Int {
        val noteNames = listOf("C","D","E","F","G","A","B")
        val name = note.name.dropLast(1)
        val octave = note.name.last().digitToInt()
        val noteIdx = noteNames.indexOf(name)
        val stepsFromC0 = octave * 7 + noteIdx
        return stepsFromC0 - 30  // E4 的 stepsFromC0 = 4*7+2 = 30
    }

    // 計算音符在低音譜上的位置（最低線 G2 = step 0）
    private fun bassStep(note: Note): Int {
        val noteNames = listOf("C","D","E","F","G","A","B")
        val name = note.name.dropLast(1)
        val octave = note.name.last().digitToInt()
        val noteIdx = noteNames.indexOf(name)
        val stepsFromC0 = octave * 7 + noteIdx
        return stepsFromC0 - 18  // G2 的 stepsFromC0 = 2*7+4 = 18
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val staffLeft  = 4f
        val staffRight = w - 4f

        val minStep = -7
        val maxStep = 21
        val stepRange = maxStep - minStep
        val topMargin    = h * 0.05f
        val bottomMargin = h * 0.30f
        val usableHeight = h - topMargin - bottomMargin
        val halfSp = usableHeight / stepRange
        val lineSpacing = halfSp * 2f
        val staffBottom = h - bottomMargin - 7f * halfSp

        // ==== 畫高音五線 ====
        for (i in 0..4) {
            val y = staffBottom - i * lineSpacing
            canvas.drawLine(staffLeft, y, staffRight, y, staffPaint)
        }

        // ==== 畫高音譜號 ====
        clefPaint.textSize = lineSpacing * 2.2f
        canvas.drawText("\uD834\uDD1E", staffLeft - 12f, staffBottom + lineSpacing * 0.75f, clefPaint)

        // ==== 畫低音五線 + 低音譜號 ====
        val bassBottom = staffBottom + lineSpacing * 10f
        for (i in 0..4) {
            val y = bassBottom - i * lineSpacing
            canvas.drawLine(staffLeft, y, staffRight, y, staffPaint)
        }
        clefPaint.textSize = lineSpacing * 1.6f
        canvas.drawText("\uD834\uDD22", staffLeft - 8f, bassBottom - lineSpacing * 1.5f, clefPaint)

        if (notes.isEmpty()) return

        val noteStartX = staffLeft + lineSpacing * 3f
        val noteAreaW  = staffRight - noteStartX - lineSpacing
        val spacing = if (notes.size > 1) { noteAreaW / (notes.size - 1) } else { noteAreaW / 2f }

        // ==== 畫音符 ====
        notes.forEachIndexed { idx, note ->
            // 檢查音符 >= C4 用高音譜，< C4 用低音譜
            val step = if (note.midiActual >= 60) trebleStep(note) else bassStep(note)
            val currentStaffBottom = if (note.midiActual >= 60) staffBottom else bassBottom
            val noteY = currentStaffBottom - step * halfSp

            val noteX = if (notes.size == 1) {
                noteStartX + noteAreaW / 2f
            } else {
                noteStartX + idx * spacing
            }

            val r = lineSpacing * 0.40f
            val state = noteStates.getOrElse(idx) { NoteState.DEFAULT }
            val noteColor = when (state) {
                NoteState.CORRECT -> colorCorrect
                NoteState.WRONG   -> colorWrong
                NoteState.DEFAULT -> colorDefault
            }

            val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = noteColor
                style = Paint.Style.FILL
            }

            val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = noteColor
                style = Paint.Style.STROKE
                strokeWidth = 1.8f
            }

            // 加線
            if (step < 0) {
                var s = -2
                while (s >= step) {
                    val ly = currentStaffBottom - s * halfSp
                    canvas.drawLine(noteX - r * 1.6f, ly, noteX + r * 1.6f, ly, ledgerPaint)
                    s -= 2
                }
            }
            if (step > 8) {
                var s = 10
                while (s <= step) {
                    val ly = currentStaffBottom - s * halfSp
                    canvas.drawLine(noteX - r * 1.6f, ly, noteX + r * 1.6f, ly, ledgerPaint)
                    s += 2
                }
            }

            canvas.drawOval(noteX - r * 1.1f, noteY - r * 0.75f, noteX + r * 1.1f, noteY + r * 0.75f, notePaint)
            canvas.drawLine(noteX + r, noteY, noteX + r, noteY - lineSpacing * 2.8f, stemPaint)

            labelPaint.textSize = lineSpacing * 1f
            labelPaint.color = noteColor
            canvas.drawText(note.name, noteX, currentStaffBottom + lineSpacing * 1.7f, labelPaint)

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
            resolveSize(240, heightMeasureSpec)
        )
    }
}
