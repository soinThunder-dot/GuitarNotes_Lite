package com.guitartabquiz

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*

/**
 * QuizNoteCard - a card UI for one note's quiz question
 * Shows: note name header + 2x2 grid of GuitarTabView options
 * On selection: shows correct/wrong feedback, plays sound via SoundManager
 */
class QuizNoteCard(
    context: Context,
    val note: Note,
    private val soundManager: SoundManager,
    private val onAnswered: (isCorrect: Boolean) -> Unit
) : LinearLayout(context) {

    private val options: List<Pair<TabPosition, Boolean>> = MusicData.generateOptions(note)
    private var answered = false
    private val tabViews = mutableListOf<GuitarTabView>()

    init {
        orientation = VERTICAL
        setPadding(16, 16, 16, 16)
        setBackgroundColor(Color.parseColor("#16213E"))

        // Note header
        val header = TextView(context).apply {
            text = "Note: ${note.name}  (written for guitar)"
            textSize = 15f
            setTextColor(Color.parseColor("#4FC3F7"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 12)
        }
        addView(header)

        // Instruction
        val instruction = TextView(context).apply {
            text = "Select the correct guitar TAB position:"
            textSize = 12f
            setTextColor(Color.parseColor("#AAAAAA"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 8)
        }
        addView(instruction)

        // 2x2 grid of options
        val grid = GridLayout(context).apply {
            columnCount = 2
            rowCount = 2
        }

        options.forEachIndexed { idx, (tabPos, isCorrect) ->
            val cellLayout = LinearLayout(context).apply {
                orientation = VERTICAL
                setPadding(6, 6, 6, 6)
                setBackgroundColor(Color.parseColor("#0F3460"))
                isClickable = true
                isFocusable = true
            }

            // Tab label above grid
            val label = TextView(context).apply {
                text = tabPos.displayLabel()
                textSize = 11f
                setTextColor(Color.parseColor("#CCCCCC"))
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 4)
            }
            cellLayout.addView(label)

            // GuitarTabView
            val tabView = GuitarTabView(context).apply {
                tabPosition = tabPos
                this.isCorrect = null
            }
            tabViews.add(tabView)
            cellLayout.addView(tabView)

            // Result text (hidden initially)
            val resultText = TextView(context).apply {
                text = ""
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, 4, 0, 0)
            }
            cellLayout.addView(resultText)

            cellLayout.setOnClickListener {
                if (answered) return@setOnClickListener
                answered = true

                // Play sound
                soundManager.play(tabPos.resourceName())

                // Update all tabs with correct/wrong state
                options.forEachIndexed { i, (tp, correct) ->
                    tabViews[i].isCorrect = if (correct) true else {
                        if (i == idx) false else null
                    }
                }

                // Show feedback
                resultText.text = if (isCorrect) "Correct!" else "Wrong"
                resultText.setTextColor(
                    if (isCorrect) Color.parseColor("#4CAF50") else Color.parseColor("#F44336")
                )

                onAnswered(isCorrect)
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(idx % 2, 1f)
                rowSpec = GridLayout.spec(idx / 2)
                setMargins(4, 4, 4, 4)
            }
            grid.addView(cellLayout, params)
        }

        addView(grid)
    }
}
