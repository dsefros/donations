package com.orthodox.charity.payment

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.skytech.smartskyposlib.TransactionParams
import com.skytech.smartskyposlib.TransactionResult
import com.skytech.smartskyposlib.ui.PaymentActivity
import com.skytech.smartskyposlib.ui.SkyPaymentActivityV2
import java.math.BigDecimal

class SkyTechPaymentGateway : PaymentGateway {

    // buildPaymentIntent is kept for legacy Activity-based fallback.
    // The main card-presenting flow uses SmartSkyPosHeadlessClient to keep our UI visible.
    override fun buildPaymentIntent(context: Context, amount: BigDecimal): Intent =
        Intent(context, SkyPaymentActivityV2::class.java).apply {
            putExtra(PaymentActivity.PARAMS_KEY, TransactionParams(amount))
            putExtra(PaymentActivity.TYPE_KEY, PaymentActivity.TYPE_PAYMENT)
        }

    override fun mapTransactionResult(data: Intent?): AppPaymentResult {
        val tx = readTransactionResult(data)
        if (tx == null) {
            Log.w(TAG, "Payment result is null")
            return AppPaymentResult.Error
        }

        val approved = tx.isApproved == true || (tx.code == 0 && tx.rc == "00")
        Log.d(TAG, "Payment mapped: code=${tx.code}, rc=${tx.rc}, approved=$approved")

        return if (approved) AppPaymentResult.Success else AppPaymentResult.Declined
    }

    private fun readTransactionResult(data: Intent?): TransactionResult? {
        if (data == null) return null

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            data.getParcelableExtra(PaymentActivity.RESULT_KEY, TransactionResult::class.java)
        } else {
            @Suppress("DEPRECATION")
            data.getParcelableExtra(PaymentActivity.RESULT_KEY)
        }
    }

    private companion object {
        const val TAG = "SkyTechPaymentGateway"
    }
}
