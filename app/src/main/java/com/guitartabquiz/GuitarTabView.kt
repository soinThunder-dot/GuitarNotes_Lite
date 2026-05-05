package com.guitartabquiz

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * GuitarTabView - Interactive 6-string x 24-fret fretboard (144 tappable cells).
 *
 * Layout: string 1 (high e) = top row, string 6 (low E) = bottom row.
 *         fret 0 (open/nut) = leftmost column, fret 23 = rightmost.
 *
 * States per cell:
 *   IDLE     - default dark cell
 *   SELECTED - user tapped this cell (blue highlight)
 *   CORRECT  - this cell is a correct answer (green)
 *   WRONG    - user tapped wrong cell (red flash)
 *   HINT     - highlight all correct cells after wrong/right answer
 *
 * Callback: onCellTapped(string: Int, fret: Int) invoked on tap.
 * Quiz mode: set targetNote to lock the fretboard after answer.
 */
class GuitarTabView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    companion object {
        const val NUM_STRINGS = 6
        const val NUM_FRETS   = 24  // frets 0..23
    }

    // Callback: (string 1-6, fret 0-23)
    var onCellTapped: ((string: Int, fret: Int) -> Unit)? = null

    // Locked = no more tapping allowed (after answer)
    var locked = false

    // Per-cell state: cellState[string-1][fret] (string index 0=high e)
    enum class CellState { IDLE, SELECTED, CORRECT, WRONG, HINT }
    private val cellState = Array(NUM_STRINGS) { Array(NUM_FRETS) { CellState.IDLE } }

    // Colors
    private val colBg         = Color.parseColor("#0D0D1A")
    private val colString     = Color.parseColor("#888888")
    private val colFret       = Color.parseColor("#444466")
    private val colNut        = Color.parseColor("#AAAAAA")
    private val colIdle       = Color.parseColor("#1A1A2E")
    private val colSelected   = Color.parseColor("#1565C0")
    private val colCorrect    = Color.parseColor("#2E7D32")
    private val colWrong      = Color.parseColor("#B71C1C")
    private val colHint       = Color.parseColor("#1B5E20")
    private val colText       = Color.parseColor("#CCCCCC")
    private val colTextBright = Color.parseColor("#FFFFFF")
    private val colDotMarker  = Color.parseColor("#555577")

    // Fret marker positions (single dot)
    private val singleDotFrets = setOf(3, 5, 7, 9, 15, 17, 19, 21)
    // Double dot
    private val doubleDotFrets = setOf(12)

    private val bgPaint   = Paint().apply { color = colBg; style = Paint.Style.FILL }
    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }

    fun setCellState(string: Int, fret: Int, state: CellState) {
        if (string in 1..NUM_STRINGS && fret in 0 until NUM_FRETS) {
            cellState[string - 1][fret] = state
            invalidate()
        }
    }

    fun resetAllCells() {
        for (s in 0 until NUM_STRINGS)
            for (f in 0 until NUM_FRETS)
                cellState[s][f] = CellState.IDLE
        locked = false
        invalidate()
    }

    // Precomputed cell rects — calculated once in onSizeChanged
    private var cellW = 0f
    private var cellH = 0f
    private var boardLeft  = 0f
    private var boardTop   = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val labelW = w * 0.06f   // left label column for string names
        boardLeft = labelW
        boardTop  = h * 0.06f   // top label row for fret numbers
        cellW = (w - labelW) / NUM_FRETS.toFloat()
        cellH = (h - boardTop) / NUM_STRINGS.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        if (cellW == 0f || cellH == 0f) return

        // --- Fret number labels (top row) ---
        textPaint.color = colText
        textPaint.textSize = boardTop * 0.65f
        for (f in 0 until NUM_FRETS) {
            val cx = boardLeft + f * cellW + cellW / 2f
            val label = if (f == 0) "O" else f.toString()
            canvas.drawText(label, cx, boardTop * 0.85f, textPaint)
        }

        // --- String name labels (left column) ---
        val stringNames = listOf("e","B","G","D","A","E")  // string 1..6
        textPaint.textSize = cellH * 0.42f
        for (s in 0 until NUM_STRINGS) {
            val cy = boardTop + s * cellH + cellH / 2f
            textPaint.color = colString
            canvas.drawText(stringNames[s], boardLeft * 0.5f, cy + textPaint.textSize * 0.35f, textPaint)
        }

        // --- Cells ---
        for (s in 0 until NUM_STRINGS) {
            for (f in 0 until NUM_FRETS) {
                val left   = boardLeft + f * cellW
                val top    = boardTop + s * cellH
                val right  = left + cellW
                val bottom = top + cellH

                // Cell fill
                cellPaint.color = when (cellState[s][f]) {
                    CellState.IDLE     -> colIdle
                    CellState.SELECTED -> colSelected
                    CellState.CORRECT  -> colCorrect
                    CellState.WRONG    -> colWrong
                    CellState.HINT     -> colHint
                }
                canvas.drawRect(left + 1f, top + 1f, right - 1f, bottom - 1f, cellPaint)

                // Fret dot markers (only in middle string gap)
                if (s == 2 || s == 3) {  // between strings 3 and 4
                    val cx = left + cellW / 2f
                    val cy = top + cellH / 2f
                    when (f) {
                        in singleDotFrets -> {
                            cellPaint.color = colDotMarker
                            canvas.drawCircle(cx, cy, cellH * 0.18f, cellPaint)
                        }
                        in doubleDotFrets -> {
                            if (s == 2) {
                                cellPaint.color = colDotMarker
                                canvas.drawCircle(cx, cy + cellH * 0.25f, cellH * 0.18f, cellPaint)
                            } else {
                                cellPaint.color = colDotMarker
                                canvas.drawCircle(cx, cy - cellH * 0.25f, cellH * 0.18f, cellPaint)
                            }
                        }
                    }
                }

                // String line (horizontal center of cell)
                val stringY = top + cellH / 2f
                val stringThickness = when (s) { // string 1=thin, 6=thick
                    0 -> 0.8f; 1 -> 1.0f; 2 -> 1.3f
                    3 -> 1.7f; 4 -> 2.1f; else -> 2.6f
                }
                linePaint.color = colString
                linePaint.strokeWidth = stringThickness
                canvas.drawLine(left, stringY, right, stringY, linePaint)

                // Fret wire (vertical right edge, skip nut)
                if (f == 0) {
                    // Nut — thick left edge
                    linePaint.color = colNut
                    linePaint.strokeWidth = 3.5f
                    canvas.drawLine(left + 1f, top, left + 1f, bottom, linePaint)
                } else {
                    linePaint.color = colFret
                    linePaint.strokeWidth = 1f
                    canvas.drawLine(right, top, right, bottom, linePaint)
                }

                // Cell label text for non-IDLE states
                if (cellState[s][f] != CellState.IDLE) {
                    textPaint.color = colTextBright
                    textPaint.textSize = cellH * 0.38f
                    val label = when (cellState[s][f]) {
                        CellState.CORRECT, CellState.HINT -> "\u2713"
                        CellState.WRONG    -> "\u2717"
                        CellState.SELECTED -> "?"
                        else -> ""
                    }
                    val cx = left + cellW / 2f
                    val cy = top + cellH / 2f + textPaint.textSize * 0.35f
                    canvas.drawText(label, cx, cy, textPaint)
                }
            }
        }

        // Outer border
        linePaint.color = colFret
        linePaint.strokeWidth = 1.5f
        canvas.drawRect(boardLeft, boardTop, boardLeft + NUM_FRETS * cellW, boardTop + NUM_STRINGS * cellH, linePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return true
        if (locked) return true

        val x = event.x
        val y = event.y
        if (x < boardLeft || y < boardTop) return true

        val fret   = ((x - boardLeft) / cellW).toInt().coerceIn(0, NUM_FRETS - 1)
        val sIndex = ((y - boardTop) / cellH).toInt().coerceIn(0, NUM_STRINGS - 1)
        val string = sIndex + 1  // 1-based

        onCellTapped?.invoke(string, fret)
        return true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Prefer full width; height = width * (6/24) * ratio for readability
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val desiredH = (w * 0.28f).toInt().coerceAtLeast(160)
        setMeasuredDimension(
            resolveSize(w, widthMeasureSpec),
            resolveSize(desiredH, heightMeasureSpec)
        )
    }
}
