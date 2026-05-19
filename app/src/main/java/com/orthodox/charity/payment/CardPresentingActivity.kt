package com.orthodox.charity.payment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.charity.R
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode

private val GoldDark = Color(0xFF8A6A30)
private val TextMain = Color(0xFF3D3326)

private val AlegreyaFontFamily = FontFamily(
    Font(R.font.alegreya_regular, FontWeight.Normal),
    Font(R.font.alegreya_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.alegreya_medium, FontWeight.Medium),
    Font(R.font.alegreya_semibold, FontWeight.SemiBold),
    Font(R.font.alegreya_bold, FontWeight.Bold)
)

private val TimesNewRomanFontFamily = FontFamily(
    Font(R.font.times_new_roman_regular, FontWeight.Normal)
)

private val CormorantFontFamily = FontFamily(
    Font(R.font.cormorant_bold, FontWeight.Bold)
)

private enum class CardPresentingUiState {
    Preparing,
    WaitingForCard,
    ReturningResult
}

class CardPresentingActivity : ComponentActivity() {

    companion object {
        const val EXTRA_AMOUNT = "extra_amount"
        private const val KEY_PAYMENT_STARTED = "payment_started"
        private const val TAG = "CardPresentingActivity"

        fun createIntent(context: Context, amount: BigDecimal): Intent =
            Intent(context, CardPresentingActivity::class.java).apply {
                putExtra(EXTRA_AMOUNT, amount.toPlainString())
            }
    }

    private val paymentGateway: PaymentGateway = SkyTechPaymentGateway()
    private val uiState = mutableStateOf(CardPresentingUiState.Preparing)
    private var paymentStarted = false

    private val posLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        uiState.value = CardPresentingUiState.ReturningResult
        // Forward original SmartSkyPos result unchanged so MainActivity can reuse SkyTechPaymentGateway.mapTransactionResult(...).
        setResult(result.resultCode, result.data)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        paymentStarted = savedInstanceState?.getBoolean(KEY_PAYMENT_STARTED, false) ?: false

        val amountRaw = intent.getStringExtra(EXTRA_AMOUNT)
        val amount = amountRaw?.toBigDecimalOrNull()
        if (amount == null || amount <= BigDecimal.ZERO) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        enableImmersiveMode()

        setContent {
            BackHandler(enabled = true) { }
            CardPresentingScreen(
                amountText = formatDonationAmount(amount),
                uiState = uiState.value
            )
            LaunchedEffect(Unit) {
                delay(500)
                uiState.value = CardPresentingUiState.WaitingForCard
                startSmartSkyPaymentOnce(amount)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        enableImmersiveMode()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enableImmersiveMode()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_PAYMENT_STARTED, paymentStarted)
        super.onSaveInstanceState(outState)
    }

    private fun startSmartSkyPaymentOnce(amount: BigDecimal) {
        if (paymentStarted) return
        paymentStarted = true
        // TODO: Investigate SmartSkyPosLib for a headless/card-presenting API.
        // Current implementation shows our branded pre-payment screen, then delegates to SkyPaymentActivityV2.
        // Full replacement of SSP card UI requires SDK support or a safe transparent Activity theme.
        try {
            posLauncher.launch(paymentGateway.buildPaymentIntent(this, amount))
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to start SmartSky payment", t)
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun enableImmersiveMode() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
    }
}

@Composable
private fun CardPresentingScreen(amountText: String, uiState: CardPresentingUiState) {
    val bottomText = when (uiState) {
        CardPresentingUiState.Preparing -> "Подготовка оплаты"
        CardPresentingUiState.WaitingForCard -> "Приложите карту"
        CardPresentingUiState.ReturningResult -> "Завершаем операцию"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.my_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CardPresentingHeader()
            CardPresentingAmount(amountText)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                PaymentTargetRings()
                DecorativeBankCard()
            }
            Text(
                text = bottomText,
                fontFamily = AlegreyaFontFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 22.sp,
                color = Color(0xFF8F6630),
                modifier = Modifier.padding(bottom = 26.dp)
            )
        }
    }
}

@Composable
fun CardPresentingHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_header_divider),
            contentDescription = null,
            modifier = Modifier
                .width(416.dp)
                .height(53.dp)
        )
        Text(
            text = "ПОЖЕРТВОВАНИЕ",
            fontFamily = CormorantFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = GoldDark,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
fun CardPresentingAmount(amountText: String) {
    Text(
        text = amountText,
        fontFamily = TimesNewRomanFontFamily,
        fontSize = 42.sp,
        color = TextMain,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun PaymentTargetRings() {
    val transition = rememberInfiniteTransition(label = "rings")
    val scale = transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(animation = tween(1800), repeatMode = RepeatMode.Reverse),
        label = "ringScale"
    )
    val alpha = transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(animation = tween(1800), repeatMode = RepeatMode.Reverse),
        label = "ringAlpha"
    )

    Canvas(
        modifier = Modifier
            .size(270.dp)
            .scale(scale.value)
    ) {
        val center = center
        val baseColor = Color(0xFFB58A3B)
        drawCircle(color = baseColor.copy(alpha = 0.20f * alpha.value), radius = size.minDimension * 0.46f, center = center)
        drawCircle(color = baseColor.copy(alpha = 0.28f * alpha.value), radius = size.minDimension * 0.34f, center = center)
        drawCircle(color = baseColor.copy(alpha = 0.35f * alpha.value), radius = size.minDimension * 0.22f, center = center)
    }
}

@Composable
fun DecorativeBankCard() {
    Box(
        modifier = Modifier
            .size(width = 230.dp, height = 130.dp)
            .graphicsLayer {
                rotationZ = 14f
                translationX = 95f
                translationY = 25f
            }
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF4E4E4E), Color(0xFF2E2E2E), Color(0xFF1D1D1D))
                )
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFB7B7B7).copy(alpha = 0.6f))
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(2.dp)
                    .background(Color(0xFFE1E1E1).copy(alpha = 0.35f))
            )
            Text(
                text = "•••• 8724",
                color = Color(0xFFF5F5F5),
                fontFamily = TimesNewRomanFontFamily,
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

private fun formatDonationAmount(amount: BigDecimal): String {
    // UI shows whole rubles; payment amount itself is still passed to SmartSkyPos unchanged.
    val rubles = amount.setScale(0, RoundingMode.DOWN).toPlainString()
    val grouped = rubles.reversed().chunked(3).joinToString(" ").reversed()
    return "$grouped ₽"
}
