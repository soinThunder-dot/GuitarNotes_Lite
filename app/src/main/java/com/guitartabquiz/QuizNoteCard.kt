package com.guitartabquiz

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*

/**
 * QuizNoteCard - one quiz card per note.
 *
 * Layout (landscape, vertical stack inside card):
 *   [Note header label]
 *   [StaffView  — single note on staff, turns RED/GREEN after answer]
 *   [GuitarTabView — full interactive 6x24 fretboard]
 *   [Feedback label — shows CORRECT / WRONG + correct position hint]
 *
 * No multiple-choice buttons. User taps directly on the fretboard.
 * After the first tap the fretboard locks; all correct cells show green (HINT).
 * Callback onAnswered(isCorrect) is fired once per card.
 */
class QuizNoteCard(
    context: Context,
    private val note: Note,
    private val soundManager: SoundManager,
    private val onAnswered: (isCorrect: Boolean) -> Unit
) : LinearLayout(context) {

    private val correctTabs: Set<TabPosition> = MusicData.correctTabsForNote(note)
    private var answered = false

    private val staffView   = StaffView(context).apply { notes = listOf(note) }
    private val fretboard   = GuitarTabView(context)
    private val feedbackLbl = TextView(context)

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#12122A"))
        setPadding(0, 0, 0, 8)

        // Note name header
        addView(TextView(context).apply {
            text = note.name
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#4FC3F7"))
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 2)
        })

        // Staff (single note, turns red/green after answer)
        addView(staffView, LayoutParams(LayoutParams.MATCH_PARENT, 0).also { it.weight = 1f })

        // Instruction
        addView(TextView(context).apply {
            text = "Tap the correct fret:"
            textSize = 10f
            setTextColor(Color.parseColor("#AAAAAA"))
            gravity = Gravity.CENTER
            setPadding(0, 2, 0, 2)
        })

        // Interactive fretboard
        fretboard.onCellTapped = { string, fret ->
            if (!answered) handleAnswer(string, fret)
        }
        addView(fretboard, LayoutParams(LayoutParams.MATCH_PARENT, 0).also { it.weight = 3f })

        // Feedback label (hidden until answer)
        feedbackLbl.apply {
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(8, 4, 8, 4)
            visibility = View.GONE
        }
        addView(feedbackLbl)
    }

    private fun handleAnswer(string: Int, fret: Int) {
        answered = true
        fretboard.locked = true

        val tapped = TabPosition(string, fret)
        val isCorrect = correctTabs.contains(tapped)

        // Colour the tapped cell
        fretboard.setCellState(
            string, fret,
            if (isCorrect) GuitarTabView.CellState.CORRECT else GuitarTabView.CellState.WRONG
        )

        // Reveal ALL correct positions as HINT (green)
        for (tp in correctTabs) {
            if (tp != tapped) {
                fretboard.setCellState(tp.string, tp.fret, GuitarTabView.CellState.HINT)
            }
        }

        // Update staff note colour
        staffView.setNoteState(0, if (isCorrect) NoteState.CORRECT else NoteState.WRONG)

        // Feedback text
        feedbackLbl.visibility = View.VISIBLE
        if (isCorrect) {
            feedbackLbl.text = "\u2713  ${note.name}  —  string ${tapped.string}, fret ${tapped.fret}"
            feedbackLbl.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            val hint = correctTabs.minByOrNull { it.fret } ?: correctTabs.first()
            feedbackLbl.text = "\u2717  Wrong  —  e.g. string ${hint.string}, fret ${hint.fret}"
            feedbackLbl.setTextColor(Color.parseColor("#F44336"))
        }

        // Play sound (skippable — silently ignored if MP3 absent)
        try {
            val tp = if (isCorrect) tapped else (correctTabs.minByOrNull { it.fret } ?: tapped)
            soundManager.play(tp.resourceName())
        } catch (_: Exception) {}

        onAnswered(isCorrect)
    }
}
