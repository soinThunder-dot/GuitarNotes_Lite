// ========================================================
// 檔案：GuitarTabView.kt
// 用途：繪製 6 弦 × 24 格互動指板（144 個可點擊格子）
//       玩家點擊格子後，透過 callback 通知 MainActivity
//       支持 5 種格子狀態：IDLE / SELECTED / CORRECT / WRONG / HINT
// ========================================================
package com.guitartabquiz

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

// ========================================================
// 【Custom View】GuitarTabView — 互動吉他指板
// 繼承 Android View，自定義繪製逻輯
// @JvmOverloads 讓 Kotlin 自動生成多個 Java 建構函式（兼容 XML 布局）
// ========================================================
class GuitarTabView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null  // XML 屬性（可選）
) : View(context, attrs) {

    // ----------------------------------------------------
    // 【companion object】— 靜態常數（類似 Java 的 static）
    // 所有 GuitarTabView 實體共用這些值
    // ----------------------------------------------------
    companion object {
        const val NUM_STRINGS = 6   // 6 條弦
        const val NUM_FRETS = 24    // 24 個格（0..23）
    }

    // ========================================================
// 【Enum Class】CellState — 格子狀態
// 每個格子可以有不同的顯示狀態，用不同顏色區分
// ========================================================
enum class CellState {
    IDLE,      // 預設狀態：深色背景，未選中
    SELECTED,  // 已選中：玩家點擊這格，显示藍色高亮
    CORRECT,   // 正確答案：這格是正解，綠色
    WRONG,     // 錯誤：玩家點錯格子，紅色閃現
    HINT       // 提示：顯示所有正確格子（當玩家答對或答錯後）
}



    // ----------------------------------------------------
    // 【Callback】— 當格子被點擊時呼叫此函式
    // MainActivity 會設定這個 callback，接收 (string, fret) 參數
    // var 表示可以後續修改，? 表示可為 null
    // ((Int, Int) -> Unit) 是函式類型：接收兩個 Int，無回傳值
    // ----------------------------------------------------
    var onCellTapped: ((string: Int, fret: Int) -> Unit)? = null

    // ----------------------------------------------------
    // 【鎖定狀態】— 當玩家答題後，鎖定指板不再接受點擊
    // ----------------------------------------------------
    var locked = false

    // ----------------------------------------------------
    // 【格子狀態陣列】— 儲存每個格子的狀態
    // cellState[string-1][fret] — string 由 1 起，陣列 index 由 0 起
    // Array(6) { Array(24) { ... } } 建立 6×24 的二維陣列
    // ----------------------------------------------------
    private val cellState = Array(NUM_STRINGS) { Array(NUM_FRETS) { CellState.IDLE } }

    // ----------------------------------------------------
    // 【顏色定義】— 指板各部份的顏色
    // Color.parseColor("#RRGGBB") 將 16 進制顏色碼轉為 Android Color
    // ----------------------------------------------------
    private val colBg = Color.parseColor("#0D0D1A")       // 背景：深藍灰
    private val colString = Color.parseColor("#888888")   // 弦線：灰色
    private val colFret = Color.parseColor("#444466")     // 品絲：深灰
    private val colNut = Color.parseColor("#AAAAAA")      // 琴枕（第 0 格）：淺灰
    private val colIdle = Color.parseColor("#1A1A2E")     // IDLE 格子：深色
    private val colSelected = Color.parseColor("#1565C0") // SELECTED：藍色
    private val colCorrect = Color.parseColor("#2E7D32")  // CORRECT：綠色
    private val colWrong = Color.parseColor("#B71C1C")    // WRONG：紅色
    private val colHint = Color.parseColor("#1B5E20")     // HINT：深綠
    private val colText = Color.parseColor("#CCCCCC")     // 文字：淺灰
    private val colTextBright = Color.parseColor("#FFFFFF") // 高亮文字：白色
    private val colDotMarker = Color.parseColor("#555577") // 品位點：淡藍灰

    // ----------------------------------------------------
    // 【品位標記】— 吉他指板上的小圓點，幫助定位品格
    // setOf() 建立不可變集合（不重複）
    // ----------------------------------------------------
    private val singleDotFrets = setOf(3, 5, 7, 9, 15, 17, 19, 21)  // 單點品位
    private val doubleDotFrets = setOf(12)  // 雙點品位（第 12 格）

    // ----------------------------------------------------
    // 【Paint 物件】— Android 繪圖筆刷，用於 canvas.draw...() 方法
    // Paint.ANTI_ALIAS_FLAG = 開啟反鋸齒（邊緣平滑）
    // apply { } 是 Kotlin 的 scope function，用來初始化物件屬性
    // ----------------------------------------------------
    private val bgPaint = Paint().apply { 
        color = colBg
        style = Paint.Style.FILL  // 填充樣式
    }
    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
        style = Paint.Style.FILL 
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
        style = Paint.Style.STROKE  // 描邊樣式
        strokeWidth = 1f 
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { 
        textAlign = Paint.Align.CENTER  // 文字置中
        typeface = Typeface.DEFAULT_BOLD  // 粗體
    }

        // ====================================================
    // 【公開方法】setCellState() — 設定某個格子的狀態
    // MainActivity 呼叫此方法來更新格子外觀（例如變綠、變紅）
    // invalidate() = 要求 View 重新繪製，會觸發 onDraw()
    // ====================================================
    fun setCellState(string: Int, fret: Int, state: CellState) {
        if (string in 1..NUM_STRINGS && fret in 0 until NUM_FRETS) {
            cellState[string - 1][fret] = state
            invalidate()  // 重繪
        }
    }

    // ====================================================
    // 【公開方法】resetAllCells() — 重設所有格子為 IDLE
    // 用於開始新一輪遊戲
    // ====================================================
    fun resetAllCells() {
        for (s in 0 until NUM_STRINGS)
            for (f in 0 until NUM_FRETS)
                cellState[s][f] = CellState.IDLE
        locked = false
        invalidate()
    }

    // ====================================================
    // 【內部變數】— 格子寬高，由 onSizeChanged() 計算
    // ====================================================
    private var cellW = 0f
    private var cellH = 0f
    private var boardLeft = 0f
    private var boardTop = 0f

    // ====================================================
    // 【生命周期回調】onSizeChanged() — View 尺寸改變時呼叫
    // 當 View 第一次顯示或螢幕旋轉時，重新計算布局
    // ====================================================
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val labelW = w * 0.06f  // 左邊欄：弦名標籤
        boardLeft = labelW
        boardTop = h * 0.06f    // 上邊欄：品格數字
        cellW = (w - labelW) / NUM_FRETS.toFloat()
        cellH = (h - boardTop) / NUM_STRINGS.toFloat()
    }

        // ========================================================
    // 【核心方法】onDraw(canvas) — 繪製整個指板
    // 每次 invalidate() 會觸發此方法
    // Canvas = Android 的畫布，可以在上面畫圖形、線條、文字等
    // ========================================================
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        // 畫背景
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        if (cellW == 0f || cellH == 0f) return  // 尺寸未計算，跳過

        // --- 繪品格數字標籤（頂部一行）---
        textPaint.color = colText
        textPaint.textSize = boardTop * 0.65f
        for (f in 0 until NUM_FRETS) {
            val cx = boardLeft + f * cellW + cellW / 2f
            val label = if (f == 0) "O" else f.toString()  // 第 0 格顯示 "O"（空弦）
            canvas.drawText(label, cx, boardTop * 0.85f, textPaint)
        }

        // --- 繪弦名標籤（左邊一列）---
        val stringNames = listOf("e", "B", "G", "D", "A", "E")  // 弦 1..6
        textPaint.textSize = cellH * 0.42f
        for (s in 0 until NUM_STRINGS) {
            val cy = boardTop + s * cellH + cellH / 2f
            textPaint.color = colString
            canvas.drawText(stringNames[s], boardLeft * 0.5f, cy + textPaint.textSize * 0.35f, textPaint)
        }

        // --- 繪 6×24 = 144 個格子 ---
        for (s in 0 until NUM_STRINGS) {
            for (f in 0 until NUM_FRETS) {
                val left = boardLeft + f * cellW
                val top = boardTop + s * cellH
                val right = left + cellW
                val bottom = top + cellH

                // 格子填充顏色（根據狀態）
                cellPaint.color = when (cellState[s][f]) {
                    CellState.IDLE -> colIdle
                    CellState.SELECTED -> colSelected
                    CellState.CORRECT -> colCorrect
                    CellState.WRONG -> colWrong
                    CellState.HINT -> colHint
                }
                canvas.drawRect(left + 1f, top + 1f, right - 1f, bottom - 1f, cellPaint)

                // 品位點（在弦 3-4 之間畫小圓點）
                if (s == 2 || s == 3) {
                    val cx = left + cellW / 2f
                    val cy = top + cellH / 2f
                    when (f) {
                        in singleDotFrets -> {
                            cellPaint.color = colDotMarker
                            canvas.drawCircle(cx, cy, cellH * 0.18f, cellPaint)
                        }
                        in doubleDotFrets -> {  // 第 12 格畫雙點
                            cellPaint.color = colDotMarker
                            if (s == 2) canvas.drawCircle(cx, cy + cellH * 0.25f, cellH * 0.18f, cellPaint)
                            else canvas.drawCircle(cx, cy - cellH * 0.25f, cellH * 0.18f, cellPaint)
                        }
                    }
                }

                // 繪弦線（水平線，粗細不同）
                val stringY = top + cellH / 2f
                val stringThickness = when (s) {
                    0 -> 0.8f; 1 -> 1.0f; 2 -> 1.3f
                    3 -> 1.7f; 4 -> 2.1f; else -> 2.6f
                }
                linePaint.color = colString
                linePaint.strokeWidth = stringThickness
                canvas.drawLine(left, stringY, right, stringY, linePaint)

                // 繪品絲（垂直線）
                if (f == 0) {  // 第 0 格 = 琴枕，粗線
                    linePaint.color = colNut
                    linePaint.strokeWidth = 3.5f
                    canvas.drawLine(left + 1f, top, left + 1f, bottom, linePaint)
                } else {
                    linePaint.color = colFret
                    linePaint.strokeWidth = 1f
                    canvas.drawLine(right, top, right, bottom, linePaint)
                }

                // 格子內文字（非 IDLE 狀態才顯示）
                if (cellState[s][f] != CellState.IDLE) {
                    textPaint.color = colTextBright
                    textPaint.textSize = cellH * 0.38f
                    val label = when (cellState[s][f]) {
                        CellState.CORRECT, CellState.HINT -> "\u2713"  // ✓ = 勾
                        CellState.WRONG -> "\u2717"  // ✗ = 叉
                        CellState.SELECTED -> "?"
                        else -> ""
                    }
                    val cx = left + cellW / 2f
                    val cy = top + cellH / 2f + textPaint.textSize * 0.35f
                    canvas.drawText(label, cx, cy, textPaint)
                }
            }
        }

        // 外框
        linePaint.color = colFret
        linePaint.strokeWidth = 1.5f
        canvas.drawRect(boardLeft, boardTop, boardLeft + NUM_FRETS * cellW, boardTop + NUM_STRINGS * cellH, linePaint)
    }

        // ========================================================
    // 【觸摸事件處理】onTouchEvent() — 處理玩家點擊指板
    // 當玩家點擊螢幕時，Android 會呼叫此方法
    // ========================================================
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 只處理 ACTION_DOWN（手指按下），忽略 MOVE / UP
        if (event.action != MotionEvent.ACTION_DOWN) return true

        // 如果指板已鎖定，不接受點擊
        if (locked) return true

        val x = event.x  // 點擊的 X 坐標
        val y = event.y  // 點擊的 Y 坐標

        // 判斷是否點在指板範圍內
        if (x < boardLeft || y < boardTop) return true

        // 計算被點擊的格子
        val fret = ((x - boardLeft) / cellW).toInt().coerceIn(0, NUM_FRETS - 1)
        val sIndex = ((y - boardTop) / cellH).toInt().coerceIn(0, NUM_STRINGS - 1)
        val string = sIndex + 1  // 轉為 1-based

        // 呼叫 callback，通知 MainActivity
        onCellTapped?.invoke(string, fret)
        // ?. 是 Kotlin 的 safe call：如果 onCellTapped 是 null，不呼叫
        // invoke() = 執行函式

        return true  // 表示事件已處理
    }

    // ========================================================
    // 【尺寸測量】onMeasure() — 決定 View 的大小
    // Android 在布局時會呼叫此方法來詢問 View 想要多大
    // ========================================================
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        // 高度 = 寬度 × 0.28（保持適當的指板比例）
        val desiredH = (w * 0.28f).toInt().coerceAtLeast(160)
        setMeasuredDimension(
            resolveSize(w, widthMeasureSpec),
            resolveSize(desiredH, heightMeasureSpec)
        )
        // resolveSize() 是 Android 的工具方法，處理父容器的限制
    }
}
