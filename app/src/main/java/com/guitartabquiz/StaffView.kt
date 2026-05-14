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
        color = Color.parseColor("#CCCCCC"); strokeWidth = 1.8f;
        style = Paint.Style.STROKE
    }
    private val bgPaint = Paint().apply {
        color = Color.parseColor("#1A1A2E"); style = Paint.Style.FILL
    }
    private val ledgerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCCCCC"); strokeWidth = 1.8f;
        style = Paint.Style.STROKE
    }
    private val clefPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#AAAAAA"); textAlign = Paint.Align.LEFT
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A1A2E"); textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val colorDefault = Color.parseColor("#F0E68C")  // 金色
    private val colorDefault2 = Color.parseColor("#42A5F5")  // 藍色
    private val colorCorrect = Color.parseColor("#4CAF50")  // 綠色
    private val colorWrong   = Color.parseColor("#F44336")  // 紅色

    var notes: List<Note> = emptyList()
        set(value) { field = value; noteStates =
            MutableList(value.size) { NoteState.DEFAULT }; invalidate() }

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
        val w = width.toFloat()
        val h = height.toFloat()
                // ===== 全域縮放設定 =====
        val scaleFactor = 1.5f   // 想放多大就改這裡，例如 1.2 / 1.5 / 2.0
        canvas.save()            // 先存起原本狀態
        val originX = -2f          //         // 1) 把原點移到「畫面右邊中間」最右邊
        val originY = h / 1.5f     // 垂直中心
        canvas.translate(originX, originY)        
        canvas.scale(scaleFactor, scaleFactor)    // 2) 以這個原點為中心做縮放
        canvas.translate(-originX, -originY)      // 3) 再把原點移回去左上（因為計算都是以左上寫的）
        // ==== 從這裡開始，全部沿用你原來的畫法 ====
        canvas.drawRect(0f, 0f, w, h, bgPaint)
                // ==== 2. 左右邊界：五線譜畫多寬 ====
        val staffLeft = 6f
        val staffRight = w - 4f
                // ==== 3. 定義我們要支援的音域（以 step 表示）====
                // trebleStep(note) 的定義：
                //   E4 (高五線譜底線) step = 0
                //   每升一個字母音（E→F→G→A→B→C→D...）step +1
                // 所以：
                //   E3 = E4 低一個八度 = 7 個 diatonic step  → step = -7
                //   E7 = E4 高三個八度 = 21 個 diatonic step → step = +21
                // 最低要畫到的 step：E2
                // 最高要畫到的 step：E7
                // step 總數 = 21 - (-14) = 35
        val minStep = -14
        val maxStep = 21
        val stepRange = maxStep - minStep
                // ==== 4. 上下預留 margin，避免音符貼螢幕上下邊 ====
                // 上方保留 5% 高度給空白（避免 E7 貼到螢幕頂）
                // 下方保留 30% 高度（要放音名文字、對錯符號等）
        val topMargin    = h * 0.05f
        val bottomMargin = h * 0.30f
                // 可用來畫「E3～E7 這 28 steps」的垂直高度
        val usableHeight = h - topMargin - bottomMargin
                // 每一個「diatonic step」要用多少像素高度：
                //   usableHeight = stepRange * (高度/一個 step)
                //   → (高度/一個 step) = usableHeight / stepRange
        // ==== 5. 決定「E4 底線」要畫在哪個 Y（staffBottom）====
        // 公式：noteY = staffBottom - step * halfSp
        // 我們希望：E3 (step=-7) 的 Y ≈ h - bottomMargin   （也就是畫面底部往上留一點）
        // 代入 step = -7：
        //   h - bottomMargin = staffBottom - (-7) * halfSp
        //                    = staffBottom + 7 * halfSp
        //   staffBottom = h - bottomMargin - 7 * halfSp
        //   step = -7 → noteY ≈ h - bottomMargin   （E3 貼近底邊）
        //   step = 21 → noteY ≈ topMargin          （E7 不會超出上邊）
        val halfSp = usableHeight / stepRange   // 1 個 step 高度（對應 lineSpacing 的一半）
        val lineSpacing = halfSp * 2f   // 兩個 step = 線與線的距離
        val staffBottom = h - bottomMargin - 7f * halfSp

        // ==== 畫高音五線 ====
        for (i in 0..4) {
            val y = staffBottom - i * lineSpacing   // 每條線相距 lineSpacing 像素
            canvas.drawLine(staffLeft, y, staffRight, y, staffPaint)
        }

        // ==== 畫高音譜號 ====
        clefPaint.textSize = lineSpacing * 2.2f
        canvas.drawText("\uD834\uDD1E", staffLeft - 8f,
            staffBottom + lineSpacing * 0.75f, clefPaint)

        // ==== 畫低音五線 + 低音譜號（間隔正好 1 條看不見的線）====
        val bassBottom = staffBottom + lineSpacing * 6f
        for (i in 0..4) {
            val y = bassBottom - i * lineSpacing
            canvas.drawLine(staffLeft, y, staffRight, y, staffPaint)
        }
        clefPaint.textSize = lineSpacing * 1.6f
        canvas.drawText("\uD834\uDD22", staffLeft - 8f,
            bassBottom - lineSpacing * 0.5f, clefPaint)

        // ============================================
        // ======= 從這裡開始：只負責畫音符區塊 =======
        // ============================================

        // ★★★ 如果沒有音符資料，直接結束，不畫任何音符 ★★★
        if (notes.isEmpty()) return

        // ==== 9. 設定音符在水平方向的分佈（兩組共用） ====
        // noteStartX：所有音符「最左邊」的起點 X，前面三格留給譜號
        val noteStartX = staffLeft + lineSpacing * 3f
        // noteAreaW：音符可以使用的水平總寬度（右邊再留一格 padding）
        val noteAreaW  = staffRight - noteStartX - lineSpacing
        // spacing：音符之間的間距
        //   - 只有一顆 → 放在中間
        //   - 多顆 → 均勻鋪開
        val spacing = if (notes.size > 1) {
            (noteAreaW / (notes.size - 1)) * 0.6f   // 這裡乘以 0.6 → note 更靠近
        } else {
            (noteAreaW / 2f) * 0.6f
        }

        // ==== 第二組音符的「整排 Y 位移」 ====
        // 注意：這個只會影響 2nd set（下面 forEach 傳 yOffset 的地方）
        //      1st set 一律用 yOffset = 0f，絕對不會被影響
        // 想要兩排靠更近 / 更遠，就改這個數字
        val secondSetOffsetY = lineSpacing * 4f   // ★★★ 第二組整排往下移 4 個 lineSpacing ★★★

        val isTreble = note.midiActual >= 60   // ★ 確認這行在 drawOneNote 之前
        // ==========================================================
        // 把「畫一顆音符」寫成一個函式，1st / 2nd 兩組共同呼叫
        // 千萬注意：
        //   1. 這個函式只吃「顏色 / 位置 / 狀態」，不改 noteStates 裡的值
        // ==========================================================
        fun drawOneNote(
            canvas: Canvas,
            noteX: Float,                 // 這顆音符的 X 座標（兩組都事先算好再丟進來）
            step: Int,                    // 這顆音符相對於該譜表底線的「音階 step」
            staffBottomForThisNote: Float,// 這顆音符所屬譜表的「底線 Y」（高音或低音）
            state: NoteState,             // 這顆的狀態：DEFAULT / CORRECT / WRONG
            noteColor: Int,               // 這顆要用什麼顏色畫（呼叫者決定）
            label: String,                // 要顯示在下面的文字（音名或別的）
            yOffset: Float,                // 額外 Y 位移（1st = 0；2nd = secondSetOffsetY）
            isTreble: Boolean        // ★★★ 新增：告訴這個函式是高音還是低音 ★★★

        ) {
            // 半徑跟 lineSpacing 綁在一起，放大縮小螢幕時會跟著變
            val r = lineSpacing * 0.45f

            // =======================
            // 1. 畫加線（高於/低於五線）
            // =======================
            // ★★★ 注意：這邊用的是 step / staffBottomForThisNote / yOffset，
            //      所以 1st / 2nd 的「加線位置」跟音符位置完全一致，不會飄掉
            if (!isTreble && step < 0) {// 1. 高音譜「上方」出界（step > 8 = 高於 F5）
                var s = -2
                while (s >= step) {
                    val ly = staffBottomForThisNote - s * halfSp + yOffset
                    canvas.drawLine(
                        noteX - r * 1.6f,ly,noteX + r * 1.6f,ly,ledgerPaint
                    )
                    s -= 2
                }
            }
            if (isTreble && step > 8) {// 2. 低音譜「下方」出界（step < 0 = 低於 G2）
                var s = 10
                while (s <= step) {
                    val ly = staffBottomForThisNote - s * halfSp + yOffset
                    canvas.drawLine(
                        noteX - r * 1.6f,ly,noteX + r * 1.6f,ly,ledgerPaint
                    )
                    s += 2
                }
            }
            // 3. 剛好是 C4， step = -2（E4 底線下兩格）
            if (isTreble && step == -2) { //    用 isTreble 判斷是哪組，midY 就是那條線的 Y
                val ly = staffBottomForThisNote + 2 * halfSp + yOffset
                canvas.drawLine(noteX - r * 1.6f, ly, noteX + r * 1.6f, ly, ledgerPaint)
            }

            // =======================
            // 2. 算出這顆音符真正要畫的 Y
            // =======================
            // staffBottomForThisNote：高音或低音譜底線
            // step：這顆音相對底線往上幾個 diatonic step
            // yOffset：整排再往上/下平移一段
            val noteY = staffBottomForThisNote - step * halfSp + yOffset

            // =======================
            // 3. 準備畫筆（符頭 + 符尾）
            // =======================
            val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = noteColor
                style = Paint.Style.FILL
            }
            val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = noteColor
                style = Paint.Style.STROKE
                strokeWidth = 1.8f
            }

            // =======================
            // 4. 畫符頭（橢圓）
            // =======================
            canvas.drawOval(
                noteX - r * 1.1f,
                noteY - r * 0.75f,
                noteX + r * 1.1f,
                noteY + r * 0.75f,
                notePaint
            )

            // =======================
            // 5. 畫符尾（直線）
            // =======================
            canvas.drawLine(
                noteX + r,
                noteY,
                noteX + r,
                noteY - lineSpacing * 2.8f,
                stemPaint
            )

            // =======================
            // 6. 畫音名文字（note name）
            // =======================
            // ★★★ labelPaint 是整個 View 共用的畫筆，
            //     這裡每顆會重新設定 textSize / color
            labelPaint.textSize = lineSpacing * 1.5f
            labelPaint.color = noteColor
            labelPaint.textAlign = Paint.Align.LEFT   // 從左邊開始畫，貼在 note 右邊
            val textX = noteX + r * 3f              // 符頭再右邊一點
            val textY = noteY + lineSpacing * 0.2f    // 稍微往下 0.2 格，視覺居中

            canvas.drawText(
                label,
                textX,
                textY,
                labelPaint
            )

            // =======================
            // 7. 畫對錯符號（✓ / ✗）
            // =======================
            // ★★★ 重點：這裡只看 state，完全不改 state ★★★
            if (state != NoteState.DEFAULT) {
                val badge = if (state == NoteState.CORRECT) "✓" else "✗"
                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = noteColor
                    textSize = lineSpacing * 0.9f
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                }
                canvas.drawText(
                    badge,
                    noteX,
                    noteY - lineSpacing * 3.2f,
                    badgePaint
                )
            }
        }

        // ==========================================================
        // =============== 第一組音符（原始那排） ====================
        // ==========================================================
        // ★★★ 顏色規則：
        //     DEFAULT → 金色 (colorDefault)
        //     CORRECT → 綠色 (colorCorrect)
        //     WRONG   → 紅色 (colorWrong)
        // ★★★ 千萬不要把 DEFAULT 改成 colorDefault2，不然整排會變藍
        notes.forEachIndexed { idx, note ->
            // 1. 根據 midiActual 決定這顆音要畫在高音譜或低音譜
            val step = if (note.midiActual >= 60) trebleStep(note) else bassStep(note)
            val currentStaffBottom = if (note.midiActual >= 60) staffBottom else bassBottom

            // 2. 水平方向位置：所有 set 共用同一個 noteX，才會對齊
            val noteX = if (notes.size == 1) {
                noteStartX + noteAreaW / 2f
            } else {
                noteStartX + idx * spacing
            }

            // 3. 讀取這顆音目前的狀態（如果 notes 跟 noteStates 長度不同，就給 DEFAULT）
            val state = noteStates.getOrElse(idx) { NoteState.DEFAULT }

            // 4. 第一組顏色 mapping（預設金色）
            val noteColor = when (state) {
                NoteState.CORRECT -> colorCorrect   // 對 → 綠
                NoteState.WRONG   -> colorWrong     // 錯 → 紅
                NoteState.DEFAULT -> colorDefault   // 未答 → 金色（只在第一組用）
            }

            // 5. 呼叫通用畫 note 函式，yOffset = 0f 表示「原本那排」
            drawOneNote(
                canvas = canvas,
                noteX = noteX,
                step = step,
                staffBottomForThisNote = currentStaffBottom,
                state = state,
                noteColor = noteColor,
                label = note.name,  // 第一組顯示原本音名
                yOffset = 0f,      // ★★★ 第一組絕對不要改這個（0 就是原始高度）
                isTreble = isTreble 
            )
        }

        // ==========================================================
        // =============== 第二組音符（偏移＋藍色） ==================
        // ==========================================================
        // ★★★ 顏色規則：
        //     DEFAULT → 藍色 (colorDefault2)
        //     CORRECT → 綠色 (colorCorrect)
        //     WRONG   → 紅色 (colorWrong)
        // ★★★ 注意：這裡只改「看起來的顏色」，完全不改 noteStates 的值
        notes.forEachIndexed { idx, note ->
            // 1. 一樣用原始 note 的 midiActual 決定高音/低音譜
            val step = if (note.midiActual >= 60) trebleStep(note) else bassStep(note)
            val currentStaffBottom = if (note.midiActual >= 60) staffBottom else bassBottom
                    val isTreble = note.midiActual >= 60

            // 2. 水平位置跟第一組完全相同，這樣兩組會垂直對齊
            val noteX = if (notes.size == 1) {
                noteStartX + noteAreaW / 2f
            } else {
                noteStartX + idx * spacing
            }

            // 3. 狀態一樣從 noteStates 讀，同一顆 idx 共享狀態
            val state = noteStates.getOrElse(idx) { NoteState.DEFAULT }

            // 4. 第二組顏色 mapping（預設藍色）
            val noteColor = when (state) {
                NoteState.CORRECT -> colorCorrect   // 對 → 跟第一組一樣綠
                NoteState.WRONG   -> colorWrong     // 錯 → 跟第一組一樣紅
                NoteState.DEFAULT -> colorDefault2  // 未答 → 這排才用藍色
            }
            
            // ★★★ 這一行就是你要的：同一顆 note，但 octave-1 ★★★
            val pitch = note.name.dropLast(1)               // "D4" → "D"
            val octave = note.name.last().digitToInt()      // 4
            val labelB = pitch + (octave - 1).toString()     // "D3"
            // 5. 呼叫通用畫 note 函式，這次 yOffset = secondSetOffsetY
            drawOneNote(
                canvas = canvas,
                noteX = noteX,
                step = step,
                staffBottomForThisNote = currentStaffBottom,
                state = state,
                noteColor = noteColor,
                label = labelB,           // 你要改成別的字（例如「+8」）也在這裡改
                yOffset = secondSetOffsetY,// ★★★ 第二組就是靠這個整排往下移
                isTreble = isTreble 
            )
        }

        // ============================================
        // ======= 到這裡為止：音符區塊結束 ===========
        // ============================================
        




        
        // 右上角模式文字
        val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = lineSpacing * 1.6f
            textAlign = Paint.Align.LEFT//RIGHT
        }
        //val baseX = w - 120f        // 先算整串文字的基準座標
        //val baseY = topMargin + lineSpacing * 9f
        val textPiano = "piano & pitch"        // 先測量各段文字寬度
        val textMid   = " <> "
        val textGuitar= "guitar(+8)"
        val widthPiano  = cornerPaint.measureText(textPiano)
        val widthMid    = cornerPaint.measureText(textMid)
        val widthGuitar = cornerPaint.measureText(textGuitar)
                
        // 你要的基準位置（可以繼續調這兩個數字讓整串往中間靠一點）
        val baseX = staffLeft
        // 五線譜高度 = 4 個 lineSpacing（5 條線之間有 4 個距離）
        val baseY = staffBottom + 20f + lineSpacing * 4f

        // 整串總長，用來推回每一段的起點
        val totalWidth = widthPiano + widthMid + widthGuitar
        cornerPaint.color = Color.parseColor("#42A5F5")   // bright blue
        //canvas.drawText(textPiano, baseX - totalWidth + widthPiano, baseY, cornerPaint)
        canvas.drawText(textPiano, baseX , baseY, cornerPaint )
        cornerPaint.color = Color.WHITE  //白色
        //canvas.drawText(textMid, baseX - totalWidth + widthPiano + widthMid, baseY, cornerPaint)
        canvas.drawText(textMid, baseX + widthPiano , baseY, cornerPaint )
        cornerPaint.color = Color.parseColor("#FFD600")   // yellow
        //canvas.drawText(textGuitar, baseX, baseY, cornerPaint)
        canvas.drawText(textGuitar, baseX + widthPiano + widthMid, baseY, cornerPaint )


        
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize(800, widthMeasureSpec),
            resolveSize(240, heightMeasureSpec)
        )
    }
}
