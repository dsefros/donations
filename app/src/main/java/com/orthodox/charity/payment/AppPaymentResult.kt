package com.orthodox.charity.payment

sealed interface AppPaymentResult {
    data object Success : AppPaymentResult
    data object Declined : AppPaymentResult
    data object Error : AppPaymentResult
}
