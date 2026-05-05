# Guitar Tab Quiz — GuitarNotes_Lite

A Music Quiz Android app: read a **treble clef staff** showing 4 random notes, then select the correct **24×6 guitar TAB grid** for each note.

## Features

- **謎面 (Riddle)**: Treble clef staff showing 4 random natural notes (C4–B5)
- **4 MCQ Options** per note: 2×2 grid of `GuitarTabView` — each shows a full 24-fret × 6-string TAB grid with one position highlighted
- **Instant Feedback**: Green ✓ / Red ✗ after selection, correct answer revealed
- **MP3 Playback**: Plays the guitar note sound when you select an option
- **Guitar Transposition**: Correctly handles guitar as a transposing instrument (written pitch sounds 8va lower)
- **Score + Restart**: Final score shown, tap "New Round" for new random notes

## Guitar Transposition Note

Guitar is a **transposing instrument** (移調樂器).  
Written `C4` on the treble clef **sounds as `C3`** on guitar (one octave lower).  
TAB positions in this quiz are based on the **actual sounding pitch**.

## Project Structure

```
GuitarNotes_Lite/
├── settings.gradle
├── build.gradle
├── README.md
└── app/
    ├── build.gradle
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/guitartabquiz/
        │   ├── MusicData.kt        ← Music theory engine + transposition
        │   ├── GuitarTabView.kt    ← Custom View: 24×6 TAB grid
        │   ├── StaffView.kt        ← Custom View: Treble clef staff
        │   ├── QuizNoteCard.kt     ← MCQ card with 4 tab options
        │   ├── SoundManager.kt     ← MP3 playback from res/raw/
        │   └── MainActivity.kt     ← Main quiz activity (programmatic UI)
        └── res/
            ├── raw/                ← PUT YOUR MP3 FILES HERE
            ├── drawable/ic_guitar.xml
            └── values/ (strings, colors, themes)
```

## MP3 Files Required

Place MP3 files in `app/src/main/res/raw/` using this naming convention:

```
s{string}_f{fret}.mp3
```

- **String**: 1 = high e, 2 = B, 3 = G, 4 = D, 5 = A, 6 = low E
- **Fret**: 0 = open string, 1–23 = fret number

### Examples

| File | String | Fret | Actual Pitch |
|------|--------|------|-------------|
| `s1_f0.mp3` | e (high) | open | E4 |
| `s1_f5.mp3` | e | 5th | A4 |
| `s2_f0.mp3` | B | open | B3 |
| `s3_f2.mp3` | G | 2nd | A3 |
| `s4_f0.mp3` | D | open | D3 |
| `s6_f0.mp3` | E (low) | open | E2 |

### Quiz Range

Quiz covers written pitches C4–B5 (actual C3–B4). Required MP3s cover approximately **70–80 files**.

If an MP3 file is missing, playback silently skips — the quiz will not crash.

### Auto-generate MP3s with FluidSynth (Python)

```python
import subprocess, os

open_midi = [64, 59, 55, 50, 45, 40]  # e B G D A E
os.makedirs("raw", exist_ok=True)

for s, base in enumerate(open_midi, 1):
    for f in range(24):
        midi = base + f
        fname = f"raw/s{s}_f{f}.mp3"
        # Use FluidSynth with a guitar soundfont
        subprocess.run([
            "fluidsynth", "-ni", "guitar.sf2",
            "-g", "1.0", "-F", fname,
            "--sample-rate", "44100"
        ] + [f"noteon 0 {midi} 100", "sleep 2", f"noteoff 0 {midi}"])
        print(f"Generated {fname} (MIDI {midi})")
```

## Build & Run

```bash
# Open in Android Studio
# OR build from command line:
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

**Requirements**: Android SDK 26+, Kotlin 1.9, Material Components

---
*Built with Kotlin, programmatic UI (no XML layouts), custom Canvas drawing*
