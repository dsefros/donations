package com.orthodox.charity.payment

import android.content.Context
import android.content.Intent
import java.math.BigDecimal

interface PaymentGateway {
    fun buildPaymentIntent(context: Context, amount: BigDecimal): Intent
    fun mapTransactionResult(data: Intent?): AppPaymentResult
}
