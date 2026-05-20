package com.orthodox.charity.settings

import android.content.Context

class AppSettingsStorage(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): DonationSettings {
        return DonationSettings(
            customDefaultAmount = sanitizeAmount(prefs.getString(KEY_CUSTOM_DEFAULT_AMOUNT, "2000"), "2000"),
            templeAmount = sanitizeAmount(prefs.getString(KEY_TEMPLE_AMOUNT, "500"), "500"),
            orphanageAmount = sanitizeAmount(prefs.getString(KEY_ORPHANAGE_AMOUNT, "1000"), "1000"),
            customAmountEnabled = prefs.getBoolean(KEY_CUSTOM_AMOUNT_ENABLED, true),
            templeAmountEnabled = prefs.getBoolean(KEY_TEMPLE_AMOUNT_ENABLED, true),
            orphanageAmountEnabled = prefs.getBoolean(KEY_ORPHANAGE_AMOUNT_ENABLED, true)
        )
    }

    fun save(settings: DonationSettings) {
        prefs.edit()
            .putString(KEY_CUSTOM_DEFAULT_AMOUNT, sanitizeAmount(settings.customDefaultAmount, "2000"))
            .putString(KEY_TEMPLE_AMOUNT, sanitizeAmount(settings.templeAmount, "500"))
            .putString(KEY_ORPHANAGE_AMOUNT, sanitizeAmount(settings.orphanageAmount, "1000"))
            .putBoolean(KEY_CUSTOM_AMOUNT_ENABLED, settings.customAmountEnabled)
            .putBoolean(KEY_TEMPLE_AMOUNT_ENABLED, settings.templeAmountEnabled)
            .putBoolean(KEY_ORPHANAGE_AMOUNT_ENABLED, settings.orphanageAmountEnabled)
            .apply()
    }

    private fun sanitizeAmount(raw: String?, fallback: String): String {
        val digits = raw.orEmpty().filter { it.isDigit() }
        val normalized = digits.trimStart('0')
        return normalized.takeIf { it.isNotBlank() && it != "0" } ?: fallback
    }

    companion object {
        private const val PREFS_NAME = "donation_settings"
        private const val KEY_CUSTOM_DEFAULT_AMOUNT = "custom_default_amount"
        private const val KEY_TEMPLE_AMOUNT = "temple_amount"
        private const val KEY_ORPHANAGE_AMOUNT = "orphanage_amount"
        private const val KEY_CUSTOM_AMOUNT_ENABLED = "custom_amount_enabled"
        private const val KEY_TEMPLE_AMOUNT_ENABLED = "temple_amount_enabled"
        private const val KEY_ORPHANAGE_AMOUNT_ENABLED = "orphanage_amount_enabled"
    }
}
