package com.orthodox.charity.settings

import android.content.Context

class AppSettingsStorage(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): DonationSettings {
        return DonationSettings(
            customDefaultAmount = prefs.getString(KEY_CUSTOM_DEFAULT_AMOUNT, "2000")?.filter { it.isDigit() }
                ?.ifEmpty { "2000" } ?: "2000",
            templeAmount = prefs.getString(KEY_TEMPLE_AMOUNT, "500")?.filter { it.isDigit() }
                ?.ifEmpty { "500" } ?: "500",
            orphanageAmount = prefs.getString(KEY_ORPHANAGE_AMOUNT, "1000")?.filter { it.isDigit() }
                ?.ifEmpty { "1000" } ?: "1000",
            mainLoopSoundEnabled = prefs.getBoolean(KEY_MAIN_LOOP_SOUND_ENABLED, true),
            paymentResultSoundEnabled = prefs.getBoolean(KEY_PAYMENT_RESULT_SOUND_ENABLED, true),
            mainLoopVolume = prefs.getFloat(KEY_MAIN_LOOP_VOLUME, 0.35f).coerceIn(0f, 1f),
            paymentResultVolume = prefs.getFloat(KEY_PAYMENT_RESULT_VOLUME, 0.75f).coerceIn(0f, 1f)
        )
    }

    fun save(settings: DonationSettings) {
        prefs.edit()
            .putString(KEY_CUSTOM_DEFAULT_AMOUNT, settings.customDefaultAmount.filter { it.isDigit() })
            .putString(KEY_TEMPLE_AMOUNT, settings.templeAmount.filter { it.isDigit() })
            .putString(KEY_ORPHANAGE_AMOUNT, settings.orphanageAmount.filter { it.isDigit() })
            .putBoolean(KEY_MAIN_LOOP_SOUND_ENABLED, settings.mainLoopSoundEnabled)
            .putBoolean(KEY_PAYMENT_RESULT_SOUND_ENABLED, settings.paymentResultSoundEnabled)
            .putFloat(KEY_MAIN_LOOP_VOLUME, settings.mainLoopVolume.coerceIn(0f, 1f))
            .putFloat(KEY_PAYMENT_RESULT_VOLUME, settings.paymentResultVolume.coerceIn(0f, 1f))
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "donation_settings"
        const val KEY_CUSTOM_DEFAULT_AMOUNT = "custom_default_amount"
        const val KEY_TEMPLE_AMOUNT = "temple_amount"
        const val KEY_ORPHANAGE_AMOUNT = "orphanage_amount"
        const val KEY_MAIN_LOOP_SOUND_ENABLED = "main_loop_sound_enabled"
        const val KEY_PAYMENT_RESULT_SOUND_ENABLED = "payment_result_sound_enabled"
        const val KEY_MAIN_LOOP_VOLUME = "main_loop_volume"
        const val KEY_PAYMENT_RESULT_VOLUME = "payment_result_volume"
    }
}
