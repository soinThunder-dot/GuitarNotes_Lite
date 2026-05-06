// ===========================================================
// 檔名：SoundManager.kt
// 用途：音效管理器 — 播放吉他音符的 MP3 檔案
//
// 【重要！MP3 檔案存放位置】
// 所有 MP3 音檔必須放在：
//   app/src/main/res/raw/
//
// 【MP3 檔案命名規則】
// 格式：s{string}_f{fret}.wav
// 例如：
//   s1_f0.wav  = 第 1 弦第 0 格（第 1 弦空弦，高音 E）
//   s1_f5.wav  = 第 1 弦第 5 格（A 音）
//   s6_f0.wav  = 第 6 弦第 0 格（第 6 弦空弦，低音 E）
//
// 【如果 MP3 不存在】
// APP 不會崩潰，只會跳過播放並記錄到 Log
// ===========================================================
package com.guitartabquiz

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

/**
 * [Music Player] SoundManager — 音效管理器
 * 
 * 負責從 res/raw/ 目錄播放 MP3 檔案。
 * 每個 MP3 檔案代表一個吉他音符（特定弦和格數）。
 * 
 * 使用 MediaPlayer 播放音檔，播放完成後自動釋放資源。
 */
class SoundManager(private val context: Context) {

    // 目前正在播放的 MediaPlayer 物件（可能為 null）
    private var currentPlayer: MediaPlayer? = null

    /**
     * 播放指定的音檔
     * 
     * @param resourceName 資源名稱，例如 "s1_f5" 代表 s1_f5.wav
     * 
     * 步驟：
     * 1. 利用 resourceName 查找 R.raw.{resourceName}
     * 2. 如果找不到 MP3 檔案，記錄到 Log 並跳過
     * 3. 停止並釋放上一個正在播放的音檔
     * 4. 創建新的 MediaPlayer 並開始播放
     * 5. 播放完成後自動釋放資源
     */
    fun play(resourceName: String) {
        try {
            // 步驟 1: 查找資源 ID
            // context.resources.getIdentifier() 會在 res/raw/ 目錄中找 resourceName.wav
            val resId = context.resources.getIdentifier(
                resourceName,        // 例如 "s1_f5"
                "raw",               // 資源類型：raw （res/raw/）
                context.packageName  // APP 的套件名
            )
            
            // 步驟 2: 如果 resId == 0 代表找不到檔案
            if (resId == 0) {
                Log.d("SoundManager", "MP3 not found: $resourceName — skipping")
                return  // 跳過，不播放
            }

            // 步驟 3: 停止上一個音檔
            stopCurrent()

            // 步驟 4: 創建新的 MediaPlayer 並開始播放
            currentPlayer = MediaPlayer.create(context, resId)?.apply {
                // 播放完成後的監聽器：釋放資源
                setOnCompletionListener { release() }
                // 開始播放
                start()
            }
        } catch (e: Exception) {
            // 如果發生錯誤，記錄到 Log 但不崩潰
            Log.e("SoundManager", "Error playing $resourceName: ${e.message}")
        }
    }

    /**
     * 停止目前正在播放的音檔
     */
    fun stopCurrent() {
        currentPlayer?.apply {
            if (isPlaying) stop()  // 如果正在播放，先停止
            release()               // 釋放 MediaPlayer 資源
        }
        currentPlayer = null
    }

    /**
     * 釋放所有資源（當 Activity 銷毀時呼叫）
     */
    fun release() {
        stopCurrent()
    }
}
