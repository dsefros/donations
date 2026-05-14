package com.orthodox.charity.settings

data class DonationSettings(
    val customDefaultAmount: String = "2000",
    val templeAmount: String = "500",
    val orphanageAmount: String = "1000",
    val mainLoopSoundEnabled: Boolean = true,
    val paymentResultSoundEnabled: Boolean = true,
    val mainLoopVolume: Float = 0.35f,
    val paymentResultVolume: Float = 0.75f
)
