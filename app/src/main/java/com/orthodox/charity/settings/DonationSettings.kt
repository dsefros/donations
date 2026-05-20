package com.orthodox.charity.settings

data class DonationSettings(
    val customDefaultAmount: String = "2000",
    val templeAmount: String = "500",
    val orphanageAmount: String = "1000",
    val customAmountEnabled: Boolean = true,
    val templeAmountEnabled: Boolean = true,
    val orphanageAmountEnabled: Boolean = true
)
