package com.guitartabquiz

// MusicData.kt - Music Theory Engine
// Guitar is a transposing instrument: written pitch sounds 8va lower
// Quiz range: E2-E6 (notated). C4+ shown on TREBLE clef, B3 and lower on BASS clef.
// Boundary: midiNotated >= 60 (C4) -> TREBLE; midiNotated < 60 (B3 and below) -> BASS

enum class Clef { TREBLE, BASS }

data class Note(
    val name: String,
    val midiNotated: Int,
    val midiActual: Int,
    val clef: Clef
)

// Guitar string open pitches (actual sounding MIDI)
// String 1=high e: E4=64, 2=B3=59, 3=G3=55, 4=D3=50, 5=A2=45, 6=low E2=40
val OPEN_STRING_MIDI = intArrayOf(64, 59, 55, 50, 45, 40)

data class TabPosition(
    val string: Int, // 1-6, 1=thinnest high e
    val fret: Int    // 0-23
) {
    fun midiActual(): Int = OPEN_STRING_MIDI[string - 1] + fret
    fun resourceName(): String = "s${string}_f${fret}"
}

object MusicData {
    // Natural notes E2-E6 (notated)
    // Treble clef: C4 (midiNotated=60) and above
    // Bass clef:   B3 (midiNotated=59) and below
    val ALL_NOTES: List<Note> = buildList {
        val noteNames = listOf("C","D","E","F","G","A","B")
        val semitoneOffsets = listOf(0,2,4,5,7,9,11)
        // Build E2 to E6 inclusive
        // E2 notated = MIDI 52, E6 notated = MIDI 88
        for (octave in 2..6) {
            for (i in noteNames.indices) {
                val notated = 60 + (octave - 4) * 12 + semitoneOffsets[i]
                // E2 notated = 52, skip notes below E2 (notated < 52)
                if (notated < 52) continue
                // E6 notated = 88, skip notes above E6 (notated > 88)
                if (notated > 88) continue
                val clef = if (notated >= 60) Clef.TREBLE else Clef.BASS
                add(Note("${noteNames[i]}$octave", notated, notated - 12, clef))
            }
        }
    }

    // Map: actualMidi -> list of TabPosition that produce that pitch
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

    fun randomQuizNotes(count: Int = 4): List<Note> = ALL_NOTES.shuffled().take(count)

    // All TabPositions that are CORRECT for this note
    fun correctTabsForNote(note: Note): Set<TabPosition> =
        (midiToTabs[note.midiActual] ?: emptyList()).toSet()
}
