// ===========================================================
// 檔名：QuizNoteCard.kt
// 用途：單一音符測驗卡片（此檔案目前未使用）
// 說明：原本設計為每個音符一張卡片，包含五線譜 + 指板
//      但目前架構改為 MainActivity 統一管理 4 個音符
// ===========================================================
package com.guitartabquiz

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*

/**
 * [未使用的類別] QuizNoteCard - 單一音符測驗卡片
 * 
 * 這個類別原本設計用來為每個音符創建一張獨立的測驗卡片。
 * 目前的 APP 架構已改為使用 MainActivity 統一管理，不再使用此類別。
 * 保留此檔案僅供參考。
 */
class QuizNoteCard(
    context: Context,
    private val note: Note,                        // 此卡片要測驗的音符
    private val soundManager: SoundManager,        // 音效管理器
    private val onAnswered: (isCorrect: Boolean) -> Unit  // 答題後的回調函式
) : LinearLayout(context) {

    // 正確答案的所有指板位置
    private val correctTabs: Set<TabPosition> = MusicData.correctTabsForNote(note)
    
    // 是否已經答過此題
    private var answered = false

    // UI 元件
    private val staffView = StaffView(context).apply { notes = listOf(note) }
    private val fretboard = GuitarTabView(context)
    private val feedbackLbl = TextView(context)

    init {
        // 設定卡片佈局為垂直排列
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#12122A"))
        setPadding(0, 0, 0, 8)

        // --- 音符名稱標題 ---
        addView(TextView(context).apply {
            text = note.name
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#4FC3F7"))
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 2)
        })

        // --- 五線譜（顯示單一音符）---
        addView(staffView, LayoutParams(LayoutParams.MATCH_PARENT, 0).also { it.weight = 1f })

        // --- 提示文字 ---
        addView(TextView(context).apply {
            text = "Tap the correct fret:"
            textSize = 10f
            setTextColor(Color.parseColor("#AAAAAA"))
            gravity = Gravity.CENTER
            setPadding(0, 2, 0, 2)
        })

        // --- 互動式指板 ---
        fretboard.onCellTapped = { string, fret ->
            if (!answered) handleAnswer(string, fret)
        }
        addView(fretboard, LayoutParams(LayoutParams.MATCH_PARENT, 0).also { it.weight = 3f })

        // --- 反饋標籤（答題前隱藏）---
        feedbackLbl.apply {
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(8, 4, 8, 4)
            visibility = View.GONE
        }
        addView(feedbackLbl)
    }

    /**
     * 處理使用者點擊指板格子的答案
     */
    private fun handleAnswer(string: Int, fret: Int) {
        answered = true
        fretboard.locked = true

        val tapped = TabPosition(string, fret)
        val isCorrect = correctTabs.contains(tapped)

        // 標記點擊的格子為對/錯
        fretboard.setCellState(
            string, fret,
            if (isCorrect) GuitarTabView.CellState.CORRECT else GuitarTabView.CellState.WRONG
        )

        // 顯示所有正確位置為 HINT（綠色）
        for (tp in correctTabs) {
            if (tp != tapped) {
                fretboard.setCellState(tp.string, tp.fret, GuitarTabView.CellState.HINT)
            }
        }

        // 更新五線譜音符顏色
        staffView.setNoteState(0, if (isCorrect) NoteState.CORRECT else NoteState.WRONG)

        // 顯示反饋文字
        feedbackLbl.visibility = View.VISIBLE
        if (isCorrect) {
            feedbackLbl.text = "\u2713 ${note.name} — string ${tapped.string}, fret ${tapped.fret}"
            feedbackLbl.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            val hint = correctTabs.minByOrNull { it.fret } ?: correctTabs.first()
            feedbackLbl.text = "\u2717 Wrong — e.g. string ${hint.string}, fret ${hint.fret}"
            feedbackLbl.setTextColor(Color.parseColor("#F44336"))
        }

        // 播放音效（如果 MP3 不存在則靜默忽略）
        try {
            val tp = if (isCorrect) tapped else (correctTabs.minByOrNull { it.fret } ?: tapped)
            soundManager.play(tp.resourceName())
        } catch (_: Exception) {}

        // 觸發回調函式
        onAnswered(isCorrect)
    }
}
