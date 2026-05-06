package com.guitartabquiz

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

// 謎面狀態：預設（金色）、正確（綠色）、錯誤（紅色）
enum class NoteState { DEFAULT, CORRECT, WRONG }

// 五線譜 View — 繪製高音譜，顯示最多 4 個音符
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        val staffLeft = 56f
        val staffRight = w - 14f
        val staffBottom = h * 0.85f  // 五線譜最底線位置（從 0.70 改成 0.85 以給高音符更多上方空間）        val lineSpacing = h * 0.12f
                val lineSpacing = h * 0.12f  // 線間距

        // 繪 5 條線
        for (i in 0..4) {
            val y = staffBottom - i * lineSpacing
            canvas.drawLine(staffLeft, y, staffRight, y, staffPaint)
        }

        // 繪高音譜號 𝄞
        clefPaint.textSize = lineSpacing * 5.2f
        canvas.drawText("\uD834\uDD1E", staffLeft - 12f, staffBottom + lineSpacing * 0.75f, clefPaint)

        if (notes.isEmpty()) return

        val noteStartX = staffLeft + lineSpacing * 3f
        val noteAreaW = staffRight - noteStartX - lineSpacing
        val spacing = if (notes.size > 1) noteAreaW / (notes.size - 1) else noteAreaW / 2f

        notes.forEachIndexed { idx, note ->
            val step = trebleStep(note)
            val halfSp = lineSpacing / 2f
            val noteY = staffBottom - step * halfSp
            val noteX = if (notes.size == 1) noteStartX + noteAreaW / 2f else noteStartX + idx * spacing
            val r = lineSpacing * 0.40f

            val state = noteStates.getOrElse(idx) { NoteState.DEFAULT }
            val noteColor = when (state) {
                NoteState.CORRECT -> colorCorrect
                NoteState.WRONG   -> colorWrong
                NoteState.DEFAULT -> colorDefault
            }
            val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = noteColor; style = Paint.Style.FILL }
            val stemPaint  = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = noteColor; style = Paint.Style.STROKE; strokeWidth = 1.8f }

            // 加線（低於或高於五線範圍）
            if (step < 0) {
                var s = -2
                while (s >= step) {
                    val ly = staffBottom - s * halfSp
                    canvas.drawLine(noteX - r * 1.6f, ly, noteX + r * 1.6f, ly, ledgerPaint)
                    s -= 2
                }
            }
            if (step > 8) {
                var s = 10
                while (s <= step) {
                    val ly = staffBottom - s * halfSp
                    canvas.drawLine(noteX - r * 1.6f, ly, noteX + r * 1.6f, ly, ledgerPaint)
                    s += 2
                }
            }

            // 繪符頭
            canvas.drawOval(noteX - r * 1.1f, noteY - r * 0.75f, noteX + r * 1.1f, noteY + r * 0.75f, notePaint)

            // 繪符尾
            canvas.drawLine(noteX + r, noteY, noteX + r, noteY - lineSpacing * 2.8f, stemPaint)

            // 繪音名
            labelPaint.textSize = lineSpacing * 0.52f
            labelPaint.color = noteColor
            canvas.drawText(note.name, noteX, staffBottom + lineSpacing * 1.7f, labelPaint)

            // 繪對錯記號
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
