// ===========================================================
// 檔名：MainActivity.kt
// 用途：主程式畫面 — 吉他音階測驗 APP（橫向螢幕）
// 畫面佈局：
//     標題列 [Title bar] → 不占太多高度
//     五線譜 [StaffView] → 顯示 4 個音符，答題後依次變綠/紅
//     狀態文字 [Status label] → 提示「正在答第 x/4 題」
//     吉他指板 [GuitarTabView] → 6 弦 x 24 格共 144 個可點擊格子
//     反饋文字 [Feedback label] → 顯示對錯
//     分數欄 + 下一回合按鈕 [Score bar + Next Round button]
//           → 完成 4 題後才顯示
// ===========================================================
package com.guitartabquiz

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

/**
 * [Activity Class] MainActivity — 主畫面 Activity（繼承 AppCompatActivity）
 *
 * 這是整個 APP 的入口畫面。
 * Android 中，Activity 代表一個使用者可以看到的單一畫面。
 * onCreate() 會在這個 Activity 剛建立時被 Android 系統自動呼叫，
 * 是我們用來初始化畫面和邏輯的地方。
 */
class MainActivity : AppCompatActivity() {

    // ---------------------------------------------------------
    // [成員變數] 宣告畫面元件和資料
    // 在 Kotlin，lateinit 代表「稍後初始化」，
    // 因為這些元件會在 onCreate() 時才創建
    // ---------------------------------------------------------

    private lateinit var soundManager: SoundManager       // 音效管理器：播放吉他音檔
    private lateinit var rootLayout: LinearLayout          // 根佈局：垂直排列所有元件
    private lateinit var staffView: StaffView              // 五線譜：顯示 4 個音符
    private lateinit var statusLabel: TextView             // 狀態文字：顯示「正在答第幾題」
    private lateinit var fretboard: GuitarTabView          // 吉他指板：可點擊的 6x24 格
    private lateinit var feedbackLabel: TextView           // 反饋文字：對/錯提示
    private lateinit var scoreBar: LinearLayout            // 分數欄：顯示得分和按鈕
    private lateinit var scoreTv: TextView                 // 分數文字：顯示得分

    private val totalPerRound = 4                          // 每回合題數 = 4
    private var currentNotes: List<Note> = emptyList()    // 目前回合的 4 個音符
    private var currentIndex = 0                           // 目前正在答第幾題（0-based）
    private var score = 0                                  // 目前回合得分

    // ---------------------------------------------------------
    // [Activity 生命週期] onCreate() — Activity 建立時執行
    // ---------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)        // 呼叫父類別的 onCreate
        soundManager = SoundManager(this)         // 初始化音效管理器
        buildLayout()                              // 建立畫面佈局
        startNewRound()                            // 開始第一回合（抽 4 個音符）
    }

    // ---------------------------------------------------------
    // [建立畫面] buildLayout() — 創建所有 UI 元件並排版
    // ---------------------------------------------------------
    private fun buildLayout() {
        // 根佈局：垂直方向排列（LinearLayout.VERTICAL）
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D0D1A"))  // 深藍黑背景
        }
        setContentView(rootLayout)  // 將根佈局設為 Activity 的畫面

        // --- 五線譜（單一 StaffView，顯示全部 4 個音符）---
        // 使用 layout weight = 2，佔據較少的高度
        staffView = StaffView(this)
        staffView.scaleX = 0.5f  // 水平縮小到 50%
        staffView.scaleY = 0.5f  // 垂直縮小到 50%
        rootLayout.addView(staffView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,   // 寬度填滿
            0,                                         // 高度 0（使用 weight）
            3f  // weight = 3 （增加以顯示更多五線譜空間
                        ))

        // --- 狀態文字（顯示「正在答第 x/4 題」）---
        statusLabel = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#FFD700"))  // 金色
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 4)
            setBackgroundColor(Color.parseColor("#0D0D1A"))
        }
        rootLayout.addView(statusLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        // --- 吉他指板（ONE shared fretboard，6 弦 x 24 格）---
        // 使用 layout weight = 5，佔據最多的高度
        fretboard = GuitarTabView(this)
        fretboard.onCellTapped = { string, fret ->
            handleAnswer(string, fret)  // 當使用者點擊格子時，呼叫處理答案的函式
        }
        rootLayout.addView(fretboard, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            2f  // weight = 2 （減少以給五線譜更多空間）            
                        ))
        // --- 反饋文字（顯示對/錯和提示）---
        feedbackLabel = TextView(this).apply {
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 4)
            text = ""
        }
        rootLayout.addView(feedbackLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        // --- 分數欄（包含分數文字 + 下一回合按鈕）---
        // 預設隱藏（visibility = View.GONE），4 題答完才顯示
        scoreBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#16213E"))
            setPadding(16, 8, 16, 8)
            visibility = View.GONE  // 初始隱藏
        }

        scoreTv = TextView(this).apply {
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 24, 0)
        }

        val nextBtn = Button(this).apply {
            text = "Next Round ➔"
            textSize = 14f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1565C0"))
            setPadding(24, 8, 24, 8)
            setOnClickListener {
                startNewRound()  // 點擊按鈕後開始新回合
            }
        }

        scoreBar.addView(scoreTv)
        scoreBar.addView(nextBtn)
        rootLayout.addView(scoreBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
    }

    // ---------------------------------------------------------
    // [開始新回合] startNewRound() — 重置並抽 4 個音符
    // ---------------------------------------------------------
    private fun startNewRound() {
        // 1. 從 MusicData 抽 4 個隨機音符（E3-E7 範圍）
        currentNotes = MusicData.randomQuizNotes(totalPerRound)
        currentIndex = 0  // 重置為第 0 題
        score = 0         // 重置得分

        // 2. 將 4 個音符顯示在五線譜上（全部預設為金色）
        staffView.notes = currentNotes

        // 3. 隱藏分數欄
        scoreBar.visibility = View.GONE

        // 4. 清空反饋文字
        feedbackLabel.text = ""

        // 5. 清空指板所有格子的狀態
        fretboard.resetAllCells()

        // 6. 顯示第一題的提示
        showCurrentQuestion()
    }

    // ---------------------------------------------------------
    // [顯示當前題目] showCurrentQuestion() — 更新狀態文字
    // ---------------------------------------------------------
    private fun showCurrentQuestion() {
        if (currentIndex >= totalPerRound) return

        val note = currentNotes[currentIndex]
        // 顯示「Note 1 / 4 — Find: C4」
        statusLabel.text = "Note ${currentIndex + 1} / $totalPerRound — Find: ${note.name}"

        // 清空反饋文字
        feedbackLabel.text = ""

        // 重置指板格子狀態（讓使用者可以點擊）
        fretboard.resetAllCells()
    }

    // ---------------------------------------------------------
    // [處理答案] handleAnswer() — 使用者點擊指板格子時執行
    // ---------------------------------------------------------
    private fun handleAnswer(string: Int, fret: Int) {
        // 1. 如果已經答完所有題目，或指板已鎖定，則忽略點擊
        if (currentIndex >= totalPerRound) return
        if (fretboard.locked) return

        // 2. 鎖定指板，防止使用者連續點擊多個格子
        fretboard.locked = true

        // 3. 取得當前題目的音符和正確答案
        val note = currentNotes[currentIndex]
        val correctTabs = MusicData.correctTabsForNote(note)
        val tapped = TabPosition(string, fret)
        val isCorrect = correctTabs.contains(tapped)

        // 4. 如果正確，加分
        if (isCorrect) score++

        // 5. 將使用者點擊的格子標記為對/錯
        fretboard.setCellState(
            string, fret,
            if (isCorrect) GuitarTabView.CellState.CORRECT else GuitarTabView.CellState.WRONG
        )

        // 6. 顯示其他正確答案格子（HINT）
        for (tp in correctTabs) {
            if (tp != tapped) fretboard.setCellState(tp.string, tp.fret, GuitarTabView.CellState.HINT)
        }

        // 7. 將五線譜上的音符標記為對/錯
        staffView.setNoteState(currentIndex, if (isCorrect) NoteState.CORRECT else NoteState.WRONG)


        // 8. 播放音效（播放正確答案的音）
        val tp = if (isCorrect) tapped else (correctTabs.minByOrNull { it.fret } ?: tapped)
        val soundDebug = try {
            soundManager.play(tp.resourceName())
        } catch (e: Exception) {
            "💥 ERROR: ${e.message}"
        }
        //soundManager.play(tp.resourceName())
        
        // 9. 顯示反饋文字
        if (isCorrect) {
            feedbackLabel.text = "\u2713 Correct! ${note.name} — string $string, fret $fret"
            feedbackLabel.setTextColor(Color.parseColor("#4CAF50"))  // 綠色
        } else {
            val hint = correctTabs.minByOrNull { it.fret } ?: correctTabs.first()
            feedbackLabel.text = "\u2717 Wrong. ${note.name} — e.g. string ${hint.string}, fret ${hint.fret}\n$soundDebug"
            feedbackLabel.setTextColor(Color.parseColor("#F44336"))  // 紅色
        }

        

        // 10. 移動到下一題
        currentIndex++

        if (currentIndex >= totalPerRound) {
            // 如果所有題目答完，顯示分數
            statusLabel.text = "Round complete!"
            showRoundComplete()
        } else {
            // 否則，延遲 1.2 秒後顯示下一題
            fretboard.postDelayed({
                showCurrentQuestion()
            }, 1200)
        }
    }

    // ---------------------------------------------------------
    // [顯示回合完成] showRoundComplete() — 顯示得分和按鈕
    // ---------------------------------------------------------
    private fun showRoundComplete() {
        // 根據得分顯示不同的文字
        val emoji = when {
            score == totalPerRound -> "Perfect! 🎉"
            score >= totalPerRound * 3 / 4 -> "Great!"
            score >= totalPerRound / 2 -> "Good!"
            else -> "Keep Practicing!"
        }

        // 設定分數文字
        scoreTv.text = "Score: $score / $totalPerRound — $emoji"
        scoreTv.setTextColor(when {
            score == totalPerRound -> Color.parseColor("#4CAF50")  // 綠色
            score >= totalPerRound / 2 -> Color.parseColor("#FFD700")  // 金色
            else -> Color.parseColor("#F44336")  // 紅色
        })

        // 顯示分數欄
        scoreBar.visibility = View.VISIBLE
    }

    // ---------------------------------------------------------
    // [Activity 生命週期] onDestroy() — Activity 銷毀時執行
    // ---------------------------------------------------------
    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()  // 釋放音效資源
    }
}
