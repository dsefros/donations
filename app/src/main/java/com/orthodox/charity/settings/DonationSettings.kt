package com.orthodox.charity.settings

data class DonationSettings(
    val customDefaultAmount: String = "2000",
    val templeAmount: String = "500",
    val orphanageAmount: String = "1000"
)
