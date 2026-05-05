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
 * Screen layout (landscape, horizontal scroll):
 *
 *  [Title bar]
 *  [HorizontalScrollView]
 *    [4 x QuizNoteCard side by side]
 *  [Score bar + Next Round button (appears after all 4 answered)]
 *
 * Each QuizNoteCard shows:
 *   - Single note on treble staff (turns RED/GREEN after answer)
 *   - Full interactive 6x24 fretboard to tap
 *   - Feedback text
 *
 * After all 4 answered → score shown + "Next Round" button.
 * Tap Next Round → new set of 4 random notes.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var soundManager: SoundManager
    private lateinit var rootLayout: LinearLayout
    private lateinit var scoreBar: LinearLayout
    private lateinit var scoreTv: TextView
    private lateinit var nextBtn: Button

    private var currentNotes: List<Note> = emptyList()
    private var score = 0
    private var answeredCount = 0
    private val totalPerRound = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        soundManager = SoundManager(this)

        // Full-screen dark background
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D0D1A"))
        }
        setContentView(rootLayout)

        startNewRound()
    }

    private fun startNewRound() {
        rootLayout.removeAllViews()
        score = 0
        answeredCount = 0
        currentNotes = MusicData.randomQuizNotes(totalPerRound)

        // --- Top title bar ---
        val titleBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#16213E"))
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 8, 16, 8)
        }
        val titleTv = TextView(this).apply {
            text = "Guitar Tab Quiz  —  謎面"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#4FC3F7"))
        }
        val subtitleTv = TextView(this).apply {
            text = "  |  Written pitch (sounds 8va lower)  |  Tap the correct fret"
            textSize = 11f
            setTextColor(Color.parseColor("#888888"))
        }
        titleBar.addView(titleTv)
        titleBar.addView(subtitleTv)
        rootLayout.addView(titleBar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        // --- Horizontal scroll view for 4 cards side by side ---
        val hScroll = HorizontalScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0D0D1A"))
            isFillViewport = true
        }
        val cardsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        currentNotes.forEachIndexed { idx, note ->
            val card = QuizNoteCard(this, note, soundManager) { isCorrect ->
                if (isCorrect) score++
                answeredCount++
                if (answeredCount == totalPerRound) showRoundComplete()
            }

            // Each card takes equal width = 1/4 screen
            val cardParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            cardParams.setMargins(4, 4, 4, 4)
            cardsRow.addView(card, cardParams)

            // Divider between cards
            if (idx < currentNotes.size - 1) {
                val div = View(this).apply { setBackgroundColor(Color.parseColor("#2A2A4A")) }
                cardsRow.addView(div, LinearLayout.LayoutParams(2, LinearLayout.LayoutParams.MATCH_PARENT))
            }
        }

        hScroll.addView(cardsRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ))

        rootLayout.addView(hScroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
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
        nextBtn = Button(this).apply {
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

    private fun showRoundComplete() {
        val emoji = when {
            score == totalPerRound -> "Perfect!  🎉"
            score >= totalPerRound * 3 / 4 -> "Great!"
            score >= totalPerRound / 2 -> "Good!"
            else -> "Keep Practicing!"
        }
        scoreTv.text = "Round Score: $score / $totalPerRound  —  $emoji"
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
