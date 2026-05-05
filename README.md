# Guitar Tab Quiz — GuitarNotes_Lite
# 吉他 TAB 問答遊戲 — GuitarNotes_Lite

> **English** | **繁體中文**

一個 Android 音樂問答 App：看高音譜表上的 4 個隨機音符（謎面），為每個音符選出正確的 **24格×6弦吉他 TAB 格** 選項。  
A Music Quiz Android app: read a **treble clef staff** showing 4 random notes, then select the correct **24×6 guitar TAB grid** for each note.

---

## 功能 / Features

| 功能 | Feature |
|------|---------|
| **謎面**：高音譜表隨機顯示 4 個自然音（C4–B5） | **Riddle**: Treble clef staff with 4 random natural notes (C4–B5) |
| **4 選 1**：每個音符有 4 個 TAB 格選項（2×2 排列） | **4 MCQ Options** per note: 2×2 grid of `GuitarTabView` options |
| **即時反饋**：選後立即顯示 ✓ 綠色 / ✗ 紅色，並揭示正確答案 | **Instant Feedback**: Green ✓ / Red ✗, correct answer revealed |
| **MP3 播放**：選擇選項時播放對應吉他音色 MP3 | **MP3 Playback**: Plays the guitar note sound on selection |
| **移調處理**：正確處理吉他移調樂器特性（記譜音高一個八度） | **Transposition**: Correctly handles guitar's transposing instrument nature |
| **分數 + 重來**：完成後顯示分數，可點「新一輪」重新開始 | **Score + Restart**: Final score shown, tap "New Round" to restart |

---

## 吉他移調說明 / Guitar Transposition Note

吉他是一種**移調樂器**（Transposing Instrument）。  
Guitar is a **transposing instrument** (移調樂器).

- 五線譜上記譜的 `C4`，在吉他上**實際發出的是 `C3`**（低一個八度）
- Written `C4` on the treble clef **sounds as `C3`** on guitar (one octave lower)
- 本 App 謎面顯示**記譜音**（如樂譜所示），TAB 選項則對應**實際發聲音高**
- This app shows the **written/notated pitch** on staff; TAB options correspond to **actual sounding pitch**

```
記譜音 Written  →  實際發聲 Actual
   C4          →     C3
   G5          →     G4
   B5          →     B4
```

---

## 專案結構 / Project Structure

```
GuitarNotes_Lite/
├── settings.gradle               ← Gradle 設定 / Gradle settings
├── build.gradle                  ← 根 build 檔 / Root build file
├── README.md                     ← 本說明文件 / This file
└── app/
    ├── build.gradle              ← App 模組 build 檔 / App module build
    └── src/main/
        ├── AndroidManifest.xml   ← App 清單 / App manifest
        ├── java/com/guitartabquiz/
        │   ├── MusicData.kt      ← 音樂理論引擎 + 移調處理 / Music theory engine + transposition
        │   ├── GuitarTabView.kt  ← 自繪 View：24格×6弦 TAB 格 / Custom View: 24×6 TAB grid
        │   ├── StaffView.kt      ← 自繪 View：高音譜表 / Custom View: Treble clef staff
        │   ├── QuizNoteCard.kt   ← 問答卡片（4個TAB選項）/ MCQ card with 4 tab options
        │   ├── SoundManager.kt   ← MP3 播放管理 / MP3 playback manager
        │   └── MainActivity.kt   ← 主介面（全程式化UI）/ Main activity (programmatic UI)
        └── res/
            ├── raw/              ← ← ← 放你的 MP3 在這裡 / PUT YOUR MP3 FILES HERE
            ├── drawable/
            │   └── ic_guitar.xml ← 吉他圖示 / Guitar icon
            └── values/
                ├── strings.xml   ← 字串資源 / String resources
                ├── colors.xml    ← 顏色定義 / Color definitions
                └── themes.xml    ← 深色主題 / Dark theme
```

---

## 各檔案說明 / File Descriptions

### `MusicData.kt` — 音樂理論引擎 / Music Theory Engine
- 定義 `Note`（記譜音 + 實際音 MIDI）和 `TabPosition`（弦號 + 格號）資料類別
- Defines `Note` (notated + actual MIDI) and `TabPosition` (string + fret) data classes
- 建立完整的 MIDI → TAB 位置對照表（6弦 × 24格 = 144個位置）
- Builds complete MIDI → TAB position map (6 strings × 24 frets = 144 positions)
- `randomQuizNotes()` 每次隨機抽取 4 個不重複音符
- `randomQuizNotes()` randomly picks 4 unique notes each round
- `generateOptions()` 生成 1 個正確 + 3 個干擾選項，隨機排列
- `generateOptions()` generates 1 correct + 3 distractor options, shuffled

### `GuitarTabView.kt` — TAB 格 View / TAB Grid View
- Canvas 自繪 24格 × 6弦吉他指板格線圖
- Canvas-drawn 24-fret × 6-string guitar fretboard grid
- 顯示內嵌圓點（第3、5、7、9、12格等）
- Shows inlay dots (frets 3, 5, 7, 9, 12, etc.)
- 高亮顯示選中的弦/格位置（未答：藍色，正確：綠色，錯誤：紅色）
- Highlights selected string/fret (unanswered: blue, correct: green, wrong: red)

### `StaffView.kt` — 高音譜表 View / Staff View
- Canvas 自繪五線譜（高音譜號）
- Canvas-drawn five-line treble clef staff
- 顯示加線（C4、D4 在譜表下方；G5、A5、B5 在上方）
- Handles ledger lines (C4, D4 below staff; G5, A5, B5 above)
- 每個音符顯示符頭、符桿及音名標籤
- Shows note head, stem, and note name label per note

### `SoundManager.kt` — 聲音管理 / Sound Manager
- 從 `res/raw/` 讀取 MP3，格式：`s{弦號}_f{格號}.mp3`
- Reads MP3 from `res/raw/`, format: `s{string}_f{fret}.mp3`
- 檔案不存在時靜默跳過，不會 crash
- Silently skips missing files, no crash

### `MainActivity.kt` — 主介面 / Main Activity
- 全程式化 UI（無 XML 佈局）
- Fully programmatic UI (no XML layouts)
- 流程：顯示謎面 → 4 張問答卡 → 全部作答後顯示分數
- Flow: Show riddle staff → 4 quiz cards → show score when all answered

---

## MP3 檔案需求 / MP3 Files Required

將 MP3 檔案放入 `app/src/main/res/raw/`，命名格式：  
Place MP3 files in `app/src/main/res/raw/` using this naming convention:

```
s{弦號}_f{格號}.mp3
s{string}_f{fret}.mp3
```

### 弦號對照 / String Reference

| 弦號 String | 弦名 Name | 空弦音（實際）Actual Open Pitch | MIDI |
|-------------|-----------|-------------------------------|------|
| 1 | 高 e 弦 high e | E4 | 64 |
| 2 | B 弦 | B3 | 59 |
| 3 | G 弦 | G3 | 55 |
| 4 | D 弦 | D3 | 50 |
| 5 | A 弦 | A2 | 45 |
| 6 | 低 E 弦 low E | E2 | 40 |

### 命名範例 / Naming Examples

| 檔名 File | 弦 String | 格 Fret | 實際音 Actual Pitch |
|-----------|-----------|---------|-------------------|
| `s1_f0.mp3` | e（高）high e | 空弦 open | E4 |
| `s1_f5.mp3` | e | 第 5 格 5th | A4 |
| `s2_f0.mp3` | B | 空弦 open | B3 |
| `s3_f2.mp3` | G | 第 2 格 2nd | A3 |
| `s4_f0.mp3` | D | 空弦 open | D3 |
| `s5_f2.mp3` | A | 第 2 格 2nd | B2 |
| `s6_f0.mp3` | E（低）low E | 空弦 open | E2 |

### 所需數量 / Required Count

問答範圍：記譜音 C4–B5（實際 C3–B4），約需 **70–80 個 MP3 檔案**。  
Quiz range covers written C4–B5 (actual C3–B4), approximately **70–80 MP3 files** needed.

若某檔案不存在，播放靜默跳過，Quiz 不會 crash。  
If a file is missing, playback silently skips — the quiz will **not crash**.

---

## 自動生成 MP3（Python + FluidSynth）/ Auto-generate MP3s

用 FluidSynth + 吉他音色 SoundFont 自動生成所有音符的 MP3：  
Use FluidSynth with a guitar SoundFont to auto-generate all note MP3s:

```python
import subprocess, os

# 各弦空弦 MIDI（實際發聲）/ Open string MIDI (actual sounding)
# e=64, B=59, G=55, D=50, A=45, E=40
open_midi = [64, 59, 55, 50, 45, 40]
os.makedirs("raw", exist_ok=True)

for s, base in enumerate(open_midi, 1):
    for f in range(24):  # 0=空弦, 1-23=各格
        midi = base + f
        fname = f"raw/s{s}_f{f}.mp3"
        # 需要吉他音色 SoundFont 檔 guitar.sf2
        # Requires guitar.sf2 SoundFont file
        cmd = f'echo "noteon 0 {midi} 100\nsleep 2\nnoteoff 0 {midi}\n" | ' \
              f'fluidsynth -ni guitar.sf2 -g 1.0 -F {fname} --sample-rate 44100 /dev/stdin'
        os.system(cmd)
        print(f"生成 / Generated: {fname}  (MIDI {midi})")
```

推薦免費吉他 SoundFont：`GeneralUser GS` 或 `SGM-v2.01`  
Recommended free guitar SoundFonts: `GeneralUser GS` or `SGM-v2.01`

---

## 編譯與安裝 / Build & Install

```bash
# 1. Clone 專案 / Clone the repo
git clone https://github.com/soinThunder-dot/GuitarNotes_Lite.git
cd GuitarNotes_Lite

# 2. 放入 MP3 / Add your MP3 files
cp your_mp3s/*.mp3 app/src/main/res/raw/

# 3. 編譯 Debug APK / Build debug APK
./gradlew assembleDebug

# APK 位置 / APK location:
# app/build/outputs/apk/debug/app-debug.apk

# 4. 安裝到裝置 / Install to device
adb install app/build/outputs/apk/debug/app-debug.apk
```

或用 **Android Studio** 開啟後直接 Run / Or open in **Android Studio** and press Run.

### 系統需求 / Requirements

| 項目 | 需求 |
|------|------|
| Android SDK | minSdk 26 (Android 8.0+) |
| Kotlin | 1.9.10 |
| AGP (Android Gradle Plugin) | 8.2.0 |
| 依賴 Dependencies | AndroidX AppCompat, Material Components, ConstraintLayout |

---

## 遊戲流程 / Game Flow

```
啟動 App / Launch App
        ↓
隨機抽取 4 個音符 / Random 4 notes drawn
        ↓
謎面：高音譜表顯示 4 個音 / Staff riddle: 4 notes shown on treble clef
        ↓
問題 1–4 / Question 1–4:
  每題顯示 4 個 24×6 TAB 格選項 / Each shows 4 × 24×6 TAB grid options
  點選 → 播放 MP3 + 顯示對錯 / Tap → Play MP3 + Show correct/wrong
        ↓
顯示最終分數 / Show final score
        ↓
點「新一輪」重新開始 / Tap "New Round" to restart
```

---

## 顏色主題 / Color Theme

| 用途 Purpose | 顏色 Color | HEX |
|-------------|-----------|-----|
| 背景 Background | 深海藍 Deep navy | `#0D0D1A` |
| 卡片 Card | 深藍 Dark blue | `#16213E` |
| 選項格 Option | 海軍藍 Navy | `#0F3460` |
| 音符 Note head | 金黃 Gold | `#F0E68C` |
| TAB 高亮 TAB highlight | 天藍 Sky blue | `#4FC3F7` |
| 正確 Correct | 綠色 Green | `#4CAF50` |
| 錯誤 Wrong | 紅色 Red | `#F44336` |
| 強調 Accent | 金色 Gold | `#FFD700` |

---

## 技術細節 / Technical Notes

- **全程式化 UI**：無 XML 佈局檔，所有 View 在 Kotlin 程式碼中動態建立
- **Programmatic UI**: No XML layout files; all Views created dynamically in Kotlin
- **自訂 Canvas 繪圖**：TAB 格和五線譜均用 `onDraw()` 手繪
- **Custom Canvas drawing**: Both TAB grid and staff drawn via `onDraw()`
- **移調邏輯**：`MusicData.kt` 中明確區分 `midiNotated`（記譜）和 `midiActual`（實際發聲）
- **Transposition logic**: `MusicData.kt` explicitly separates `midiNotated` (written) and `midiActual` (sounding)
- **容錯播放**：`SoundManager` 使用 `getIdentifier()` 動態查找資源，找不到時靜默跳過
- **Fault-tolerant playback**: `SoundManager` uses `getIdentifier()` to find resources at runtime, skips silently if absent

---

## 授權 / License

MIT License — 自由使用、修改、分發 / Free to use, modify, and distribute.

---

*以 Kotlin 開發，自訂 Canvas 繪圖，全程式化 UI，無 XML 佈局*  
*Built with Kotlin · Custom Canvas drawing · Programmatic UI · No XML layouts*
