package com.guitartabquiz

// MusicData.kt - Music Theory Engine
// Guitar is a transposing instrument: written pitch sounds 8va lower
// Quiz range: Treble clef C4-B5 (notated), actual sounding C3-B4

// MIDI note number: C4=60, C3=48
data class Note(
    val name: String,
    val midiNotated: Int,
    val midiActual: Int
)

// Guitar string open pitches (actual sounding MIDI)
// String 1=high e: E4=64, 2=B3=59, 3=G3=55, 4=D3=50, 5=A2=45, 6=low E2=40
val OPEN_STRING_MIDI = intArrayOf(64, 59, 55, 50, 45, 40)

data class TabPosition(
    val string: Int,  // 1-6, 1=thinnest high e
    val fret: Int     // 0-23
) {
    fun midiActual(): Int = OPEN_STRING_MIDI[string - 1] + fret
    fun resourceName(): String = "s${string}_f${fret}"
    fun displayLabel(): String {
        val sName = when(string) {
            1 -> "e"; 2 -> "B"; 3 -> "G"
            4 -> "D"; 5 -> "A"; 6 -> "E"; else -> "?"
        }
        return if (fret == 0) "$sName open" else "$sName fret $fret"
    }
}

object MusicData {

    // Natural notes only, notated C4-B5 (MIDI 60-83)
    val ALL_NOTES: List<Note> = buildList {
        val names = listOf("C","D","E","F","G","A","B")
        val offsets = listOf(0,2,4,5,7,9,11)
        for (octave in 4..5) {
            for (i in names.indices) {
                val notated = 60 + (octave - 4) * 12 + offsets[i]
                add(Note("${names[i]}$octave", notated, notated - 12))
            }
        }
    }

    // Map: actualMidi -> list of TabPosition
    val midiToTabs: Map<Int, List<TabPosition>> = buildMap {
        for (s in 1..6) {
            for (f in 0..23) {
                val tp = TabPosition(s, f)
                val m = tp.midiActual()
                val list = getOrPut(m) { mutableListOf() } as MutableList
                list.add(tp)
            }
        }
    }

    fun randomQuizNotes(): List<Note> = ALL_NOTES.shuffled().take(4)

    fun tabsForNote(note: Note): List<TabPosition> =
        midiToTabs[note.midiActual] ?: emptyList()

    // Returns list of (TabPosition, isCorrect) - 1 correct + 3 distractors, shuffled
    fun generateOptions(note: Note): List<Pair<TabPosition, Boolean>> {
        val correctList = tabsForNote(note).ifEmpty {
            val f = note.midiActual - OPEN_STRING_MIDI[0]
            listOf(TabPosition(1, f.coerceIn(0, 23)))
        }
        val correct = correctList.random()

        val distractors = mutableListOf<TabPosition>()
        val usedMidi = mutableSetOf(note.midiActual)
        for (entry in midiToTabs.entries.shuffled()) {
            if (distractors.size >= 3) break
            if (entry.key in usedMidi) continue
            distractors.add(entry.value.random())
            usedMidi.add(entry.key)
        }
        while (distractors.size < 3) distractors.add(TabPosition(1, distractors.size + 2))

        return (listOf(correct to true) + distractors.map { it to false }).shuffled()
    }
}
