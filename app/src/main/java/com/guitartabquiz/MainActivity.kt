package com.guitartabquiz

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

/**
 * MainActivity - Guitar Tab Quiz (Landscape)
 *
 * Screen layout (vertical stack, landscape):
 *
 *   [Title bar]
 *   [StaffView  - 1 staff showing ALL 4 notes; each turns GREEN/RED as answered]
 *   [Status label - "Answering note 2/4: F4"]
 *   [GuitarTabView - ONE shared 6x24 fretboard (144 cells)]
 *   [Feedback label]
 *   [Score bar + Next Round button  (visible only after all 4 answered)]
 *
 * Flow:
 *   1. 4 random notes drawn, shown on single staff.
 *   2. User answers note 1 by tapping fretboard.
 *      -> Correct cell turns GREEN; wrong cell RED + correct cells shown (HINT).
 *      -> Staff note 1 turns GREEN or RED.
 *   3. Fretboard resets, moves on to note 2, etc.
 *   4. After note 4 answered -> score bar + "Next Round" button appear.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var soundManager: SoundManager
    private lateinit var rootLayout: LinearLayout
    private lateinit var staffView: StaffView
    private lateinit var statusLabel: TextView
    private lateinit var fretboard: GuitarTabView
    private lateinit var feedbackLabel: TextView
    private lateinit var scoreBar: LinearLayout
    private lateinit var scoreTv: TextView

    private val totalPerRound = 4
    private var currentNotes: List<Note> = emptyList()
    private var currentIndex = 0   // which note we are currently asking (0-based)
    private var score = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        soundManager = SoundManager(this)
        buildLayout()
        startNewRound()
    }

    // ---------------------------------------------------------------
    // Build the permanent view hierarchy once
    // ---------------------------------------------------------------
    private fun buildLayout() {
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D0D1A"))
        }
        setContentView(rootLayout)

        // --- Title bar ---
        val titleBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#16213E"))
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 6, 16, 6)
        }
        titleBar.addView(TextView(this).apply {
            text = "Guitar Tab Quiz  —  謎面"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#4FC3F7"))
        })
        titleBar.addView(TextView(this).apply {
            text = "  |  Read the staff → tap the correct fret"
            textSize = 11f
            setTextColor(Color.parseColor("#777777"))
        })
        rootLayout.addView(titleBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        // --- Single StaffView (4 notes) ---
        staffView = StaffView(this)
        rootLayout.addView(staffView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 2f
        ))

        // --- Status label ---
        statusLabel = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#FFD700"))
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 4)
            setBackgroundColor(Color.parseColor("#0D0D1A"))
        }
        rootLayout.addView(statusLabel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        // --- ONE shared fretboard ---
        fretboard = GuitarTabView(this)
        fretboard.onCellTapped = { string, fret -> handleAnswer(string, fret) }
        rootLayout.addView(fretboard, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 5f
        ))

        // --- Feedback label ---
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

        // --- Score bar (hidden until round complete) ---
        scoreBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#16213E"))
            setPadding(16, 8, 16, 8)
            visibility = View.GONE
        }
        scoreTv = TextView(this).apply {
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 24, 0)
        }
        val nextBtn = Button(this).apply {
            text = "Next Round  ➔"
            textSize = 14f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1565C0"))
            setPadding(24, 8, 24, 8)
            setOnClickListener { startNewRound() }
        }
        scoreBar.addView(scoreTv)
        scoreBar.addView(nextBtn)
        rootLayout.addView(scoreBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))
    }

    // ---------------------------------------------------------------
    // Start / reset a new round of 4 notes
    // ---------------------------------------------------------------
    private fun startNewRound() {
        currentNotes = MusicData.randomQuizNotes(totalPerRound)
        currentIndex = 0
        score = 0

        // Show all 4 notes on staff, all DEFAULT (gold)
        staffView.notes = currentNotes

        scoreBar.visibility = View.GONE
        feedbackLabel.text = ""
        fretboard.resetAllCells()

        showCurrentQuestion()
    }

    // ---------------------------------------------------------------
    // Update status label to reflect current question
    // ---------------------------------------------------------------
    private fun showCurrentQuestion() {
        if (currentIndex >= totalPerRound) return
        val note = currentNotes[currentIndex]
        statusLabel.text = "Note ${currentIndex + 1} / $totalPerRound  —  Find: ${note.name}"
        feedbackLabel.text = ""
        fretboard.resetAllCells()
    }

    // ---------------------------------------------------------------
    // Handle a fretboard tap
    // ---------------------------------------------------------------
    private fun handleAnswer(string: Int, fret: Int) {
        if (currentIndex >= totalPerRound) return
        if (fretboard.locked) return

        fretboard.locked = true
        val note = currentNotes[currentIndex]
        val correctTabs = MusicData.correctTabsForNote(note)
        val tapped = TabPosition(string, fret)
        val isCorrect = correctTabs.contains(tapped)

        if (isCorrect) score++

        // Colour the tapped cell
        fretboard.setCellState(
            string, fret,
            if (isCorrect) GuitarTabView.CellState.CORRECT else GuitarTabView.CellState.WRONG
        )
        // Show all other correct cells as HINT
        for (tp in correctTabs) {
            if (tp != tapped) fretboard.setCellState(tp.string, tp.fret, GuitarTabView.CellState.HINT)
        }

        // Colour this note on the staff
        staffView.setNoteState(currentIndex, if (isCorrect) NoteState.CORRECT else NoteState.WRONG)

        // Feedback text
        if (isCorrect) {
            feedbackLabel.text = "\u2713  Correct!  ${note.name}  —  string $string, fret $fret"
            feedbackLabel.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            val hint = correctTabs.minByOrNull { it.fret } ?: correctTabs.first()
            feedbackLabel.text = "\u2717  Wrong.  ${note.name}  —  e.g. string ${hint.string}, fret ${hint.fret}"
            feedbackLabel.setTextColor(Color.parseColor("#F44336"))
        }

        // Play sound
        try {
            val tp = if (isCorrect) tapped else (correctTabs.minByOrNull { it.fret } ?: tapped)
            soundManager.play(tp.resourceName())
        } catch (_: Exception) {}

        currentIndex++

        if (currentIndex >= totalPerRound) {
            // Round complete
            statusLabel.text = "Round complete!"
            showRoundComplete()
        } else {
            // Pause briefly then advance to next note
            fretboard.postDelayed({
                showCurrentQuestion()
            }, 1200)
        }
    }

    // ---------------------------------------------------------------
    // Show final score
    // ---------------------------------------------------------------
    private fun showRoundComplete() {
        val emoji = when {
            score == totalPerRound -> "Perfect!  🎉"
            score >= totalPerRound * 3 / 4 -> "Great!"
            score >= totalPerRound / 2 -> "Good!"
            else -> "Keep Practicing!"
        }
        scoreTv.text = "Score: $score / $totalPerRound  —  $emoji"
        scoreTv.setTextColor(when {
            score == totalPerRound -> Color.parseColor("#4CAF50")
            score >= totalPerRound / 2 -> Color.parseColor("#FFD700")
            else -> Color.parseColor("#F44336")
        })
        scoreBar.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}
