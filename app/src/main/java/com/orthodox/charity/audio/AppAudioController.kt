package com.orthodox.charity.audio

import android.content.Context
import android.media.MediaPlayer
import com.orthodox.charity.R

class AppAudioController {
    private var mainLoopPlayer: MediaPlayer? = null
    private var paymentResultPlayer: MediaPlayer? = null

    fun prepareMainLoop(context: Context) {
        if (mainLoopPlayer != null) return
        mainLoopPlayer = MediaPlayer.create(context, R.raw.main_loop)?.apply {
            isLooping = true
            setVolume(MAIN_LOOP_VOLUME, MAIN_LOOP_VOLUME)
        }
    }

    fun startMainLoop() {
        val player = mainLoopPlayer ?: return
        safeRun { if (!player.isPlaying) player.start() }
    }

    fun pauseMainLoop() {
        val player = mainLoopPlayer ?: return
        safeRun { if (player.isPlaying) player.pause() }
    }

    fun playPaymentResult(context: Context, restartMainLoopAfterCompletion: Boolean) {
        pauseMainLoop()
        safeRun { paymentResultPlayer?.release() }
        paymentResultPlayer = null

        paymentResultPlayer = MediaPlayer.create(context, R.raw.payment_result)?.apply {
            isLooping = false
            setVolume(PAYMENT_RESULT_VOLUME, PAYMENT_RESULT_VOLUME)
            setOnCompletionListener { completed ->
                safeRun { completed.release() }
                if (paymentResultPlayer === completed) {
                    paymentResultPlayer = null
                }
                if (restartMainLoopAfterCompletion) {
                    startMainLoop()
                }
            }
            safeRun { start() }
        }
    }

    fun pausePaymentResult() {
        val player = paymentResultPlayer ?: return
        safeRun { if (player.isPlaying) player.pause() }
    }

    fun release() {
        safeRun { paymentResultPlayer?.release() }
        paymentResultPlayer = null
        safeRun { mainLoopPlayer?.release() }
        mainLoopPlayer = null
    }

    private inline fun safeRun(action: () -> Unit) {
        try {
            action()
        } catch (_: IllegalStateException) {
        }
    }

    private companion object {
        const val MAIN_LOOP_VOLUME = 0.35f
        const val PAYMENT_RESULT_VOLUME = 0.75f
    }
}
