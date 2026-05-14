package com.orthodox.charity.audio

import android.content.Context
import android.media.MediaPlayer
import com.orthodox.charity.R

class AppAudioController {
    private var mainLoopPlayer: MediaPlayer? = null
    private var paymentResultPlayer: MediaPlayer? = null

    private var mainLoopEnabled: Boolean = true
    private var paymentResultEnabled: Boolean = true
    private var mainLoopVolume: Float = 0.35f
    private var paymentResultVolume: Float = 0.75f

    fun updateSettings(
        mainLoopEnabled: Boolean,
        paymentResultEnabled: Boolean,
        mainLoopVolume: Float,
        paymentResultVolume: Float
    ) {
        this.mainLoopEnabled = mainLoopEnabled
        this.paymentResultEnabled = paymentResultEnabled
        this.mainLoopVolume = mainLoopVolume.coerceIn(0f, 1f)
        this.paymentResultVolume = paymentResultVolume.coerceIn(0f, 1f)

        safeRun { mainLoopPlayer?.setVolume(this.mainLoopVolume, this.mainLoopVolume) }
        safeRun { paymentResultPlayer?.setVolume(this.paymentResultVolume, this.paymentResultVolume) }

        if (!this.mainLoopEnabled) {
            pauseMainLoop()
        }
    }

    fun prepareMainLoop(context: Context) {
        if (mainLoopPlayer != null) return
        mainLoopPlayer = MediaPlayer.create(context, R.raw.main_loop)?.apply {
            isLooping = true
            setVolume(mainLoopVolume, mainLoopVolume)
        }
    }

    fun startMainLoop() {
        if (!mainLoopEnabled) return
        if (isPaymentResultPlaying()) return
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

        if (!paymentResultEnabled) {
            if (restartMainLoopAfterCompletion) {
                startMainLoop()
            }
            return
        }

        val player = MediaPlayer.create(context, R.raw.payment_result)
        if (player == null) {
            if (restartMainLoopAfterCompletion) startMainLoop()
            return
        }

        paymentResultPlayer = player.apply {
            isLooping = false
            setVolume(paymentResultVolume, paymentResultVolume)
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

    private fun isPaymentResultPlaying(): Boolean {
        val player = paymentResultPlayer ?: return false
        return try {
            player.isPlaying
        } catch (_: IllegalStateException) {
            false
        }
    }

    private inline fun safeRun(action: () -> Unit) {
        try {
            action()
        } catch (_: IllegalStateException) {
        }
    }
}
