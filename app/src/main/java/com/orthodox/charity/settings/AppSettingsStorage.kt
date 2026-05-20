package com.orthodox.charity.settings

import android.content.Context

class AppSettingsStorage(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): DonationSettings {
        return DonationSettings(
            customDefaultAmount = prefs.getString(KEY_CUSTOM_DEFAULT_AMOUNT, "2000") ?: "2000",
            templeAmount = prefs.getString(KEY_TEMPLE_AMOUNT, "500") ?: "500",
            orphanageAmount = prefs.getString(KEY_ORPHANAGE_AMOUNT, "1000") ?: "1000"
        )
    }

    fun save(settings: DonationSettings) {
        prefs.edit()
            .putString(KEY_CUSTOM_DEFAULT_AMOUNT, settings.customDefaultAmount)
            .putString(KEY_TEMPLE_AMOUNT, settings.templeAmount)
            .putString(KEY_ORPHANAGE_AMOUNT, settings.orphanageAmount)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "donation_settings"
        private const val KEY_CUSTOM_DEFAULT_AMOUNT = "custom_default_amount"
        private const val KEY_TEMPLE_AMOUNT = "temple_amount"
        private const val KEY_ORPHANAGE_AMOUNT = "orphanage_amount"
    }
}
