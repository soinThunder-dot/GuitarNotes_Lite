// ========================================================
// 檔案：MusicData.kt
// 用途：音樂理論數據引擎
//       負責定義「音符」「指板位置」「正確答案」等核心數據
//       這是整個 App 的「大腦數據庫」，其他檔案都要用到這裡的定義
// 入門提示：Kotlin 的 // 是單行備注，/* */ 是多行備注
// ========================================================
package com.guitartabquiz
// ↑ package 聲明：告訴編譯器這個檔案屬於哪個「命名空間」
// 所有同一 package 的檔案可以互相直接使用對方的 class / function

// --------------------------------------------------------
// 【重要音樂知識】吉他是「移調樂器」
// 樂譜上寫的音比實際發音高 8 度（一個八度 = 12 個半音）
// 例如：樂譜寫 C4（中央 C），吉他實際發出的是 C3
// 所以：midiNotated（譜面 MIDI）= midiActual（實際 MIDI）+ 12
// --------------------------------------------------------

// ========================================================
// 【Enum Class】Clef — 譜號類型
// enum class = 「列舉類別」，用來定義一組固定選項
// 好比交通燈只有紅、黃、綠三個選項，不會有其他值
// ========================================================
enum class Clef {
    TREBLE, // 高音譜號（𝄞）：用於 C4（含）以上的音，五線的最低線是 E4
    BASS    // 低音譜號（𝄢）：用於 B3（含）以下的音，五線的最低線是 G2
}

// ========================================================
// 【Data Class】Note — 代表一個「音符」
// data class = 數據類別，Kotlin 會自動生成 equals / hashCode / toString
// 每個 Note 物件儲存一個音符的完整資訊
// ========================================================
data class Note(
    val name: String,       // 音名，例如 "C4"、"G3"、"B5"
                            // 格式 = 英文字母 + 八度數字
                            // val = 不可修改（唯讀），const in Java

    val midiNotated: Int,   // 樂譜上的 MIDI 編號
                            // MIDI 是國際通用的音高數字系統
                            // 中央 C（C4）= 60，每高一個半音 +1
                            // 例如 D4 = 62，G4 = 67

    val midiActual: Int,    // 吉他實際發出的 MIDI 音高
                            // = midiNotated - 12（低一個八度）
                            // 用於判斷指板上哪個格子的音高匹配

    val clef: Clef          // 這個音應該畫在哪種譜號上（高音 or 低音）
                            // 由 midiNotated 自動決定：
                            // >= 60 (C4) -> TREBLE，< 60 -> BASS
)

// ========================================================
// 【頂層常數】OPEN_STRING_MIDI — 六條弦空弦的實際 MIDI 音高
// 頂層 val = 寫在 class 之外，整個 package 都可以直接使用
// intArrayOf() = 建立一個整數陣列，index 從 0 開始
// ========================================================
val OPEN_STRING_MIDI = intArrayOf(
    64, // 第 1 弦（最細弦 high e）= E4 = MIDI 64
    59, // 第 2 弦（B 弦）= B3 = MIDI 59
    55, // 第 3 弦（G 弦）= G3 = MIDI 55
    50, // 第 4 弦（D 弦）= D3 = MIDI 50
    45, // 第 5 弦（A 弦）= A2 = MIDI 45
    40  // 第 6 弦（最粗弦 low E）= E2 = MIDI 40
)
// 陣列 index 0 = 第 1 弦，index 5 = 第 6 弦
// 使用時：OPEN_STRING_MIDI[string - 1]，因為弦號由 1 開始

// ========================================================
// 【Data Class】TabPosition — 代表指板上一個「格子的位置」
// 例如 TabPosition(string=2, fret=3) = 第 2 弦第 3 格
// ========================================================
data class TabPosition(
    val string: Int, // 弦號：1（最細 high e）到 6（最粗 low E）
    val fret: Int    // 格號：0（空弦/琴枕）到 23
) {
    // ------
    // 【成員函式】midiActual() — 計算這個格子的實際 MIDI 音高
    // 公式：空弦音高 + 格號 = 按下後的音高
    // 每高一格 = 半音 = MIDI +1
    // 例：第 2 弦（B3=59）第 3 格 -> 59+3 = 62 = D4
    // ------
    fun midiActual(): Int = OPEN_STRING_MIDI[string - 1] + fret
    // ↑ fun = 定義函式，「= 運算式」是 Kotlin 的單行函式寫法
    // OPEN_STRING_MIDI[string - 1] 取陣列元素（-1 因為弦號由 1 起）

    // ------
    // 【成員函式】resourceName() — 生成音效檔案的名稱
    // 用於之後播放音效（例如音效檔名 "s2_f3.mp3"）
    // 格式："s{弦號}_f{格號}"，例如 "s1_f0"、"s6_f23"
    // ------
    fun resourceName(): String = "s${string}_f${fret}"
    // ↑ 字串模板（String template）：${變數名} 會自動嵌入變數值
    // 等同 Java 的 "s" + string + "_f" + fret
}

// ========================================================
// 【Object】MusicData — 全局單例數據物件
// object = 單例（Singleton），整個 App 只有一個實體
// 不需要 new / 建構，直接用 MusicData.XXX 存取
// ========================================================
object MusicData {

    // --------------------------------------------------------
    // 【屬性】ALL_NOTES — 所有可能出現的謎面音符（E2 到 E6）
    // List<Note> = Note 物件的列表（不可修改）
    // buildList { } = Kotlin 的列表建構器，在 { } 內用 add() 加入元素
    // --------------------------------------------------------
    val ALL_NOTES: List<Note> = buildList {

        // 音名列表（自然音，不含升降號）
        val noteNames = listOf("C", "D", "E", "F", "G", "A", "B")
        // ↑ listOf() = 建立唯讀列表

        // 每個音名距離同八度 C 的半音數
        // C=0, D=2, E=4, F=5, G=7, A=9, B=11
        val semitoneOffsets = listOf(0, 2, 4, 5, 7, 9, 11)

        // 外層迴圈：八度 2 到 6（E2 最低，E6 最高）
        // for (變數 in 範圍) = Kotlin 的 for 迴圈
        // 2..6 = 閉區間，包含 2 和 6
        for (octave in 2..6) {

            // 內層迴圈：遍歷 7 個音名（indices = 0..6）
            for (i in noteNames.indices) {

                // 計算這個音的樂譜 MIDI 值
                // 公式：以 C4（MIDI 60）為基準，向上推算
                // C4=60, C5=72, C3=48, C2=36 ...
                // octave 4 時：60 + 0*12 + offset = 60..71
                // octave 5 時：60 + 1*12 + offset = 72..83
                val notated = 60 + (octave - 4) * 12 + semitoneOffsets[i]

                // 過濾：只保留 E2（MIDI 52）到 E6（MIDI 88）的音
                // E2: octave=2, E的offset=4 -> 60+(2-4)*12+4 = 60-24+4 = 40+4... = 52
                // continue = 跳過這次迴圈，繼續下一個
                if (notated < 52) continue  // 低於 E2，跳過
                if (notated > 88) continue  // 高於 E6，跳過

                // 決定譜號：C4（MIDI 60）以上用高音譜，以下用低音譜
                val clef = if (notated >= 60) Clef.TREBLE else Clef.BASS
                // ↑ Kotlin 的 if 可以作為「表達式」直接回傳值（不必寫 return）

                // 加入 Note 物件到列表
                // midiActual = notated - 12（吉他移調）
                add(Note("${noteNames[i]}$octave", notated, notated - 12, clef))
                // 例：noteNames[2]="E", octave=4 -> name="E4", notated=64, actual=52, TREBLE
            }
        }
        // 最終 ALL_NOTES 共有 29 個音符（E2 到 E6 的自然音）
    }

    // --------------------------------------------------------
    // 【屬性】midiToTabs — 「實際 MIDI 音高」對應「所有能彈出此音的格子」
    // Map<Int, List<TabPosition>> = 鍵值對：MIDI數字 -> 格子列表
    // buildMap { } = Kotlin 的 Map 建構器
    // 這個 Map 在 App 啟動時計算一次，之後直接查表，非常快速
    // --------------------------------------------------------
    val midiToTabs: Map<Int, List<TabPosition>> = buildMap {

        // 掃描所有 6 弦 × 24 格 = 144 個格子
        for (s in 1..6) {           // s = 弦號 1..6
            for (f in 0..23) {      // f = 格號 0..23

                val tp = TabPosition(s, f)  // 建立格子物件
                val m = tp.midiActual()     // 計算此格的實際音高

                // getOrPut(key) { 預設值 }
                // = 如果 Map 已有此 key，回傳現有列表
                // = 如果沒有，建立新的空列表並存入
                // as MutableList = 強制轉型為可修改列表
                val list = getOrPut(m) { mutableListOf() } as MutableList
                list.add(tp)  // 把這個格子加入對應音高的列表
            }
        }
        // 完成後，例如 midiToTabs[52] 會包含所有發出 E3 實際音的格子
        // E3實際MIDI=52: 第6弦12格(40+12)、第5弦7格(45+7)、第4弦2格(50+2) 等
    }

    // --------------------------------------------------------
    // 【函式】randomQuizNotes() — 隨機選出本輪謎面的 4 個音符
    // count: Int = 4 是預設參數，呼叫時可以不傳，預設選 4 個
    // shuffled() = 把列表隨機打亂，回傳新列表（不修改原列表）
    // take(count) = 從打亂後的列表取前 count 個
    // --------------------------------------------------------
    fun randomQuizNotes(count: Int = 4): List<Note> = ALL_NOTES.shuffled().take(count)
    // 每次呼叫都會從 29 個音符裡隨機選 4 個，確保每輪不重複且多樣化

    // --------------------------------------------------------
    // 【函式】correctTabsForNote() — 取得某個音符所有正確的格子
    // 因為吉他同一音高可在多個位置彈奏，所以返回「集合」(Set)
    // Set = 不重複的集合，適合用來「判斷某格是否在正確答案裡」
    // ?: emptyList() = Elvis 運算子：如果左邊是 null，用右邊的值
    // --------------------------------------------------------
    fun correctTabsForNote(note: Note): Set<TabPosition> =
        (midiToTabs[note.midiActual] ?: emptyList()).toSet()
    // 例：note = E3（midiActual=52）
    // -> 查 midiToTabs[52] -> 得到所有發 E3 音的 TabPosition 列表
    // -> 轉成 Set -> 之後用 set.contains(tabPosition) 快速判斷對錯
}
