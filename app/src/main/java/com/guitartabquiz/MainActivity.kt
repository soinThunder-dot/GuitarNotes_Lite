package com.guitartabquiz

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView

/**
 * MainActivity - Guitar Tab Quiz
 * Flow:
 * 1. Show treble clef staff with 4 random notes (謎面)
 * 2. For each note, show 4 GuitarTabView options (2x2 grid)
 * 3. User selects → immediate correct/wrong feedback + MP3 plays
 * 4. Score shown at end, option to restart
 *
 * Guitar transposition note:
 * Staff shows WRITTEN pitch (e.g. C4 written) which SOUNDS as C3 (one octave lower)
 * The tab options are based on ACTUAL sounding pitch
 */
class MainActivity : AppCompatActivity() {

    private lateinit var soundManager: SoundManager
    private lateinit var scrollView: NestedScrollView
    private lateinit var mainContainer: LinearLayout

    private var currentNotes: List<Note> = emptyList()
    private var score = 0
    private var answeredCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        soundManager = SoundManager(this)

        // Root scroll layout
        scrollView = NestedScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0D0D1A"))
        }

        mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 32)
        }
        scrollView.addView(mainContainer)
        setContentView(scrollView)

        startNewRound()
    }

    private fun startNewRound() {
        mainContainer.removeAllViews()
        score = 0
        answeredCount = 0
        currentNotes = MusicData.randomQuizNotes()

        // App title
        addHeader()

        // Transposition notice
        addTransposeNotice()

        // Staff view showing all 4 notes
        val staffView = StaffView(this).apply {
            notes = currentNotes
        }
        val staffCard = cardWrap(staffView, "謎面 — 高音譜 (記譜音，實際低一個八度)")
        mainContainer.addView(staffCard)

        addSpacing(16)

        // One QuizNoteCard per note
        currentNotes.forEachIndexed { idx, note ->
            val card = QuizNoteCard(this, note, soundManager) { isCorrect ->
                if (isCorrect) score++
                answeredCount++
                if (answeredCount == currentNotes.size) {
                    showResult()
                }
            }
            val wrapper = cardWrap(card, "Question ${idx + 1} / ${currentNotes.size}")
            mainContainer.addView(wrapper)
            addSpacing(12)
        }
    }

    private fun addHeader() {
        val title = TextView(this).apply {
            text = "Guitar Tab Quiz"
            textSize = 22f
            setTextColor(Color.parseColor("#4FC3F7"))
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 4)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        mainContainer.addView(title)
        val sub = TextView(this).apply {
            text = "Read the treble clef → find the correct TAB"
            textSize = 13f
            setTextColor(Color.parseColor("#AAAAAA"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 12)
        }
        mainContainer.addView(sub)
    }

    private fun addTransposeNotice() {
        val notice = TextView(this).apply {
            text = "Note: Guitar is a transposing instrument.\n" +
                    "Written C4 on staff = sounds C3 on guitar (one octave lower).\n" +
                    "Tab positions shown are ACTUAL sounding pitches."
            textSize = 11f
            setTextColor(Color.parseColor("#FFD700"))
            gravity = Gravity.CENTER
            setPadding(12, 8, 12, 12)
            setBackgroundColor(Color.parseColor("#1A1200"))
        }
        mainContainer.addView(notice)
        addSpacing(8)
    }

    private fun showResult() {
        addSpacing(16)

        val resultCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.parseColor("#16213E"))
            gravity = Gravity.CENTER
        }

        val emoji = if (score == currentNotes.size) "Perfect!" else if (score >= currentNotes.size / 2) "Good!" else "Keep Practicing!"
        val scoreText = TextView(this).apply {
            text = "Score: $score / ${currentNotes.size}  —  $emoji"
            textSize = 20f
            setTextColor(when {
                score == currentNotes.size -> Color.parseColor("#4CAF50")
                score >= currentNotes.size / 2 -> Color.parseColor("#FFD700")
                else -> Color.parseColor("#F44336")
            })
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        resultCard.addView(scoreText)

        val restartBtn = Button(this).apply {
            text = "New Round"
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#4FC3F7"))
            setPadding(32, 16, 32, 16)
            setOnClickListener { startNewRound() }
        }
        resultCard.addView(restartBtn)

        mainContainer.addView(resultCard)

        // Scroll to result
        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun cardWrap(child: View, label: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
            setBackgroundColor(Color.parseColor("#16213E"))

            if (label.isNotEmpty()) {
                val lbl = TextView(this@MainActivity).apply {
                    text = label
                    textSize = 12f
                    setTextColor(Color.parseColor("#888888"))
                    setPadding(12, 8, 12, 4)
                }
                addView(lbl)
            }
            addView(child)
        }
    }

    private fun addSpacing(dp: Int) {
        val v = View(this)
        val px = (dp * resources.displayMetrics.density).toInt()
        mainContainer.addView(v, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, px
        ))
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}
