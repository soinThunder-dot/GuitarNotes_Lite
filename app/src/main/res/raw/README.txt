Guitar Note Audio Files — res/raw/

Naming convention: s{string}_f{fret}.mp3
  string: 1-6  (1=high e, 6=low E)
  fret:   0-23 (0=open)

Examples:
  s1_f0.mp3  = String 1, open (high e, E4 notated / E3 sounding)
  s1_f5.mp3  = String 1, fret 5 (A4 notated / A3 sounding)
  s6_f0.mp3  = String 6, open (low E, E2 notated / E1 sounding)

Place all .mp3 files in this folder.
SoundManager.kt loads them by R.raw resource ID at runtime.
Missing files are silently skipped (no crash).
