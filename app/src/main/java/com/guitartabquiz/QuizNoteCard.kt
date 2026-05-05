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
 * After the first tap the fretboard locks, all correct cells show green (HINT).
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

    // Child views we need to reference after creation
    private val staffView   = StaffView(context).apply { notes = listOf(note) }
    private val fretboard   = GuitarTabView(context)
    private val feedbackLbl = TextView(context)

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#12122A"))
        setPadding(0, 0, 0, 8)

        // --- Note name header ---
        val header = TextView(context).apply {
            text = note.name
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#4FC3F7"))
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 2)
        }
        addView(header)

        // --- Staff (single note) ---
        addView(staffView, LayoutParams(LayoutParams.MATCH_PARENT, 0).also { it.weight = 1f })

        // --- Instruction label ---
        val instrLbl = TextView(context).apply {
            text = "Tap the correct fret on the board below:"
            textSize = 11f
            setTextColor(Color.parseColor("#AAAAAA"))
            gravity = Gravity.CENTER
            setPadding(0, 2, 0, 2)
        }
        addView(instrLbl)

        // --- Interactive fretboard ---
        fretboard.onCellTapped = { string, fret ->
            if (!answered) handleAnswer(string, fret)
        }
        addView(fretboard, LayoutParams(LayoutParams.MATCH_PARENT, 0).also { it.weight = 3f })

        // --- Feedback label (hidden until answer) ---
        feedbackLbl.apply {
            textSize = 13f
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
            if (isCorrect) GuitarTabView.CellState.CORRECT
            else GuitarTabView.CellState.WRONG
        )

        // Reveal ALL correct positions as HINT (green), skip the one just tapped
        for (tp in correctTabs) {
            if (tp != tapped) {
                fretboard.setCellState(tp.string, tp.fret, GuitarTabView.CellState.HINT)
            }
        }

        // Update staff note colour
        staffView.setNoteState(0, if (isCorrect) NoteState.CORRECT else NoteState.WRONG)

        // Feedback text
        feedbackLbl.apply {
            visibility = View.VISIBLE
            if (isCorrect) {
                text = "\u2713  Correct!  ${note.name}  (${tapped.string}th string, fret ${tapped.fret})"
                setTextColor(Color.parseColor("#4CAF50"))
            } else {
                val hint = correctTabs.minByOrNull { it.fret } ?: correctTabs.first()
                text = "\u2717  Wrong.  Correct: ${hint.string}th string, fret ${hint.fret} (+ ${correctTabs.size - 1} more)"
                setTextColor(Color.parseColor("#F44336"))
            }
        }

        // Play sound (skippable — silently ignored if file absent)
        try {
            val tp = if (isCorrect) tapped else (correctTabs.minByOrNull { it.fret } ?: tapped)
            soundManager.play(tp)
        } catch (_: Exception) {}

        onAnswered(isCorrect)
    }
}
