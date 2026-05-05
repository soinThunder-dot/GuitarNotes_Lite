package com.guitartabquiz

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

/**
 * SoundManager - manages MP3 playback from res/raw/
 * File naming convention: s{string}_f{fret}.mp3
 * e.g. s1_f0.mp3 = string 1, fret 0 (open high e)
 * If file not found, silently skips (no crash)
 */
class SoundManager(private val context: Context) {

    private var currentPlayer: MediaPlayer? = null

    /**
     * Play a note by its resource name (e.g. "s1_f5")
     * Looks up R.raw.s1_f5
     */
    fun play(resourceName: String) {
        try {
            val resId = context.resources.getIdentifier(
                resourceName, "raw", context.packageName
            )
            if (resId == 0) {
                Log.d("SoundManager", "MP3 not found: $resourceName — skipping")
                return
            }
            stopCurrent()
            currentPlayer = MediaPlayer.create(context, resId)?.apply {
                setOnCompletionListener { release() }
                start()
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing $resourceName: ${e.message}")
        }
    }

    fun stopCurrent() {
        currentPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        currentPlayer = null
    }

    fun release() {
        stopCurrent()
    }
}
