// ===========================================================
// 檔名：SoundManager.kt
// 用途：音效管理器 — 播放吉他音符的 MP3 檔案
//
// 【重要！MP3 檔案存放位置】
// 所有 MP3 音檔必須放在：
//   app/src/main/res/raw/
//
// 【MP3 檔案命名規則】
    //  格式：g{guitar}_s{string}_f{fret}.wav  (其中 guitar = 1-8)// 例如：
    //      g3_s1_f0.wav  = 吉他 3 第 1 弦第 0 格（第 1 弦空弦，高音 E）//   s1_f5.wav  = 第 1 弦第 5 格（A 音）
    //      g5_s1_f5.wav  = 吉他 5 第 1 弦第 5 格（A 音）//
    //      g8_s6_f0.wav  = 吉他 8 第 6 弦第 0 格（第 6 弦空弦，低音 E）// APP 不會崩潰，只會跳過播放並記錄到 Log
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

    private var lockedGuitar: Int = 1   // 預設第 2 把
    fun setGuitar(guitar: Int) {
        lockedGuitar = guitar
    }
    // 目前正在播放的 MediaPlayer 物件（可能為 null）
    private var currentPlayer: MediaPlayer? = null
    private var currentResId: Int? = null

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
            //  隨機選擇 1-2 把吉他
            //val randomGuitar = (1..2).random()  //  從 1 到 2 隨機選一個
            val guitarResourceName = "g${lockedGuitar}_$resourceName"//  加上吉他編號
            // 步驟 1: 查找資源 ID
            // context.resources.getIdentifier() 會在 res/raw/ 目錄中找 resourceName.wav
            val resId = context.resources.getIdentifier(
                guitarResourceName,        // 例如 "s1_f5"
                "raw",               // 資源類型：raw （res/raw/）
                context.packageName  // APP 的套件名
            )
            Log.d("SoundManager", "play() MP resource=$guitarResourceName resId=$resId")
            
            // 步驟 2: 如果 resId == 0 代表找不到檔案
            if (resId == 0) {
                Log.d("SoundManager", "WAV not found: $guitarResourceName  — fallback")
                playFallback()
                return
            }

            // 3. 如果是同一個檔，直接從頭播（最穩）
            if (currentPlayer != null && currentResId == resId) {
                currentPlayer?.let { mp ->
                    try {
                        mp.seekTo(0)
                        mp.start()
                        Log.d("SoundManager", "replay existing player resId=$resId")
                        return
                    } catch (e: Exception) {
                        Log.e("SoundManager", "replay failed, will recreate", e)
                        // 繼續往下，重新 create
                    }
                }
            }

            // 4. 新檔案：釋放舊 player
            currentPlayer?.release()
            currentPlayer = null
            currentResId = null

            // 5. 創建新的 MediaPlayer
            val mp = MediaPlayer.create(context, resId)
            if (mp == null) {
                Log.e("SoundManager", "Error playing $resourceName: ${e.message}", e)
                playFallback()
                return
            }

            currentPlayer = mp
            currentResId = resId

            // 當播完，不用馬上 release，保留給下一次重播
            mp.setOnCompletionListener {
                // 如果你真的想播完就釋放，也可以在這裡 call release()
                // 但為了重播同一音效順，這裡先不 release
            }

            try {
                mp.start()
                Log.d("SoundManager", "start() ok for resId=$resId")
            } catch (e: IllegalStateException) {
                Log.e("SoundManager", "start() failed, try prepare & start", e)
                try {
                    mp.reset()
                    mp.setOnPreparedListener { it.start() }
                    mp.setDataSource(context.resources.openRawResourceFd(resId).fileDescriptor)
                    mp.prepareAsync()
                } catch (e2: Exception) {
                    Log.e("SoundManager", "prepareAsync fallback failed", e2)
                    playFallback()
                }
            }

        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing $resourceName: ${e.message}", e)
            playFallback()
        }
    }

    // 簡單 fallback：給你一顆共用 click 音，用來保證耳朵有東西
    private fun playFallback() {
        try {
            val resId = context.resources.getIdentifier(
                "click_fallback",
                "raw",
                context.packageName
            )
            if (resId == 0) {
                Log.d("SoundManager", "No fallback sound defined (click_fallback.wav)")
                return
            }
            MediaPlayer.create(context, resId)?.apply {
                setOnCompletionListener { release() }
                start()
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error in fallback sound: ${e.message}", e)
        }
    }

    fun stopCurrent() {
        currentPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        currentPlayer = null
        currentResId = null
    }

    fun release() {
        currentPlayer?.release()
        currentPlayer = null
        currentResId = null
    }
}
