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

        // ==== 1. 取得 View 寬高，先畫背景 ====
        val w = width.toFloat()  // 目前 StaffView 實際寬度（像素）
        val h = height.toFloat() // 目前 StaffView 實際高度（像素）
        canvas.drawRect(0f, 0f, w, h, bgPaint)  // 整個矩形塗上背景色

        // ==== 2. 左右邊界：五線譜畫多寬 ====
        val staffLeft  = 4f      // 譜表左邊界：離 View 左邊 4px，避免貼邊
        val staffRight = w - 4f  // 譜表右邊界：離 View 右邊 4px

        // ==== 3. 定義我們要支援的音域（以 step 表示）====
        // trebleStep(note) 的定義：
        //   E4 (五線譜底線) step = 0
        //   每升一個字母音（E→F→G→A→B→C→D...）step +1
        // 所以：
        //   E3 = E4 低一個八度 = 7 個 diatonic step → step = -7
        //   E7 = E4 高三個八度 = 21 個 diatonic step → step = +21
        val minStep = -7   // 最低要畫到的 step：E3
        val maxStep = 21   // 最高要畫到的 step：E7
        val stepRange = maxStep - minStep  // step 總數 = 21 - (-7) = 28

        // ==== 4. 上下預留 margin，避免音符貼螢幕上下邊 ====
        val topMargin    = h * 0.05f  // 上方保留 5% 高度給空白（避免 E7 貼到螢幕頂）
        val bottomMargin = h * 0.30f  // 下方保留 30% 高度（要放音名文字、對錯符號等）

        // 可用來畫「E3～E7 這 28 steps」的垂直高度
        val usableHeight = h - topMargin - bottomMargin

        // 每一個「diatonic step」要用多少像素高度：
        //   usableHeight = stepRange * (高度/一個 step)
        //   → (高度/一個 step) = usableHeight / stepRange
        val halfSp = usableHeight / stepRange  // 1 個 step 高度（對應 lineSpacing 的一半）
        val lineSpacing = halfSp * 2f          // 兩個 step = 線與線的距離

        // ==== 5. 決定「E4 底線」要畫在哪個 Y（staffBottom）====
        // 公式：noteY = staffBottom - step * halfSp
        //
        // 我們希望：
        //   E3 (step=-7) 的 Y ≈ h - bottomMargin （也就是畫面底部往上留一點）
        //
        // 代入 step = -7：
        //   h - bottomMargin = staffBottom - (-7) * halfSp
        //                    = staffBottom + 7 * halfSp
        //   staffBottom = h - bottomMargin - 7 * halfSp
        //
        // 這樣：
        //   step = -7  → noteY ≈ h - bottomMargin  （E3 貼近底邊）
        //   step = 21  → noteY ≈ topMargin         （E7 不會超出上邊）
        val staffBottom = h - bottomMargin - 7f * halfSp

        // ==== 6. 畫五線（5 條）====
        //   i = 0 → 底線 E4
        //   i = 1 → 第二線 G4
        //   ...
        //   i = 4 → 第五線 F5
        for (i in 0..4) {
            val y = staffBottom - i * lineSpacing  // 每條線相距 lineSpacing 像素
            canvas.drawLine(staffLeft, y, staffRight, y, staffPaint)
        }

        // ==== 7. 畫高音譜號（𝄞 treble clef）====
        // 用 lineSpacing 來決定 clef 大小，確保隨著 staff 等比例縮放
        clefPaint.textSize = lineSpacing * 2.2f
        // Y 位置：略低於 staffBottom（讓譜號尾巴伸到五線下方，看起來比較像譜面）
        canvas.drawText("\uD834\uDD1E", sta

        // ==== 7b. 畫低音譜號 + 低音五線 (bass clef) ====
        val bassBottom = staffBottom + lineSpacing * 10f  // 低音譜比高音譜低10個線距
        // 畫低音五線（5條）
        for (i in 0..4) {
            val y = bassBottom - i * lineSpacing
            canvas.drawLine(staffLeft, y, staffRight, y, staffPaint)
        }
        // 畫低音譜號 𝄢
        clefPaint.textSize = lineSpacing * 1.6f
        canvas.drawText("\uD834\uDD22", staffLeft - 8f, bassBottom - lineSpacing * 1.5f, clefPaint)ffLeft - 12f, staffBottom + lineSpacing * 0.75f, clefPaint)

        // ==== 8. 如果沒有音符就結束 ====
        if (notes.isEmpty()) return

        // ==== 9. 設定音符水平分佈範圍 ====
        // noteStartX：第一顆音符的 X 起點（讓出 3 個 lineSpacing 的空間給譜號）
        val noteStartX = staffLeft + lineSpacing * 3f
        // noteAreaW：音符可用的總寬度（右邊再留一個 lineSpacing 的 padding）
        val noteAreaW  = staffRight - noteStartX - lineSpacing

        // spacing：多個音符之間的水平距離
        //   - 如果只有 1 顆：放在可用區中間（noteAreaW / 2）
        //   - 如果 >=2 顆：平均分佈在頭尾之間（除以 notes.size - 1）
        val spacing = if (notes.size > 1) {
            noteAreaW / (notes.size - 1)
        } else {
            noteAreaW / 2f
        }

        // ==== 10. 逐顆音符畫上去 ====
        notes.forEachIndexed { idx, note ->
            // 10-1. 先算這顆音的 step（相對 E4 底線）
                        // 檢查音符 >= C4 用高音譜，< C4 用低音譜
            val step = if (note.midiActual >= 60) trebleStep(note) else bassStep(note)

            // 10-2. 把 step 轉成 Y 座標（上面已經算好 halfSp）
                        // 10-2. 把 step 轉成 Y 座標（高音譜或低音譜）
            val currentStaffBottom = if (note.midiActual >= 60) staffBottom else bassBottom
            val noteY = currentStaffBottom - step * halfSp

            // 10-3. 這顆音的 X 座標：
            //   - 只有 1 顆：noteStartX + noteAreaW / 2 （置中）
            //   - 多顆：noteStartX + idx * spacing
            val noteX = if (notes.size == 1) {
                noteStartX + noteAreaW / 2f
            } else {
                noteStartX + idx * spacing
            }

            // 10-4. 音符頭半徑（與 lineSpacing 成比例）
            val r = lineSpacing * 0.40f

            // 10-5. 取得這顆音目前狀態（預設/正確/錯誤），決定顏色
            val state = noteStates.getOrElse(idx) { NoteState.DEFAULT }
            val noteColor = when (state) {
                NoteState.CORRECT -> colorCorrect  // 答對 → 綠色
                NoteState.WRONG   -> colorWrong    // 答錯 → 紅色
                NoteState.DEFAULT -> colorDefault  // 尚未作答 → 金色
            }

            // 10-6. 建立畫音符頭的畫筆（實心填滿）
            val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = noteColor
                style = Paint.Style.FILL
            }

            // 10-7. 建立畫音符尾巴（stem）的畫筆（描邊線）
            val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = noteColor
                style = Paint.Style.STROKE
                strokeWidth = 1.8f
            }

            // 後面：加線、畫音符頭、畫 stem、畫文字、畫 ✓/✗ 的部分
            // 全部都用上面算好的 noteX / noteY / r / lineSpacing / halfSp
            // （這裡就照你原本的程式寫法繼續）

            // 加線（低於或高於五線範圍）
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
                while (s <= step) currentStaffBottom
                                        val ly = currentStaffBottom - s * halfSp
                    canvas.drawLine(noteX - r * 1.6f, ly, noteX + r * 1.6f, ly, ledgerPaint)
                    s += 2
                }
            }

            // 繪符頭
            canvas.drawOval(noteX currentStaffBottom r * 1.1f, noteY - r * 0.75f, noteX + r * 1.1f, noteY + r * 0.75f, notePaint)

            // 繪符尾
            canvas.drawLine(noteX + r, noteY, noteX + r, noteY - lineSpacing * 2.8f, stemPaint)

            // 繪音名
            labelPaint.textSize = lineSpacing * 1f
            labelPaint.color = noteColor
            canvas.drawText(note.name, noteX, currentStaffBottom + lineSpacing * 1.7f, labelPaint)
