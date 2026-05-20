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
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.skytech.smartskyposlib.State
import com.skytech.smartskyposlib.TransactionParams
import com.skytech.smartskyposlib.ui.PaymentActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.remember
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.offset

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
    PinEntering,
    UseChip,
    PresentAgain,
    UseOtherInterface,
    UseMagReader,
    Processing,
    ReturningResult
}

class CardPresentingActivity : ComponentActivity() {

    companion object {
        const val EXTRA_AMOUNT = "extra_amount"
        private const val TAG = "CardPresentingActivity"
        private const val START_PAYMENT_DELAY_MS = 900L

        fun createIntent(context: Context, amount: BigDecimal): Intent =
            Intent(context, CardPresentingActivity::class.java).apply {
                putExtra(EXTRA_AMOUNT, amount.toPlainString())
            }
    }

    private val uiState = mutableStateOf(CardPresentingUiState.Preparing)
    private var paymentStarted = false
    private var paymentCompleted = false
    private var paymentJob: Job? = null
    private var headlessClient: SmartSkyPosHeadlessClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // CardPresentingActivity is a short-lived bridge screen; restarting the payment after process death
        // is safer than restoring a half-started external payment flow.

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
            LaunchedEffect(amount) {
                startHeadlessPaymentOnce(amount)
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
    private fun startHeadlessPaymentOnce(amount: BigDecimal) {
        if (paymentStarted) return
        if (isFinishing || isDestroyed) return

        paymentStarted = true
        headlessClient = SmartSkyPosHeadlessClient(applicationContext)

        paymentJob = lifecycleScope.launch {
            try {
                delay(START_PAYMENT_DELAY_MS)
                uiState.value = CardPresentingUiState.WaitingForCard

                val result = headlessClient!!.payment(
                    params = TransactionParams(amount),
                    onStateChanged = { stateCode, message ->
                        runOnUiThread {
                            updateUiStateFromSspState(stateCode, message)
                        }
                    }
                )

                uiState.value = CardPresentingUiState.ReturningResult
                paymentCompleted = true
                setResult(Activity.RESULT_OK, Intent().putExtra(PaymentActivity.RESULT_KEY, result))
                if (!isFinishing) finish()
            } catch (t: Throwable) {
                Log.e(TAG, "Headless SmartSky payment failed", t)
                setResult(Activity.RESULT_CANCELED)
                if (!isFinishing) finish()
            } finally {
                headlessClient?.close()
                headlessClient = null
            }
        }
    }

    private fun updateUiStateFromSspState(stateCode: Int, message: String?) {
        // message is intentionally ignored to avoid displaying raw SDK/vendor strings.

        uiState.value = when (stateCode) {
            State.CARD_READING.code,
            State.QR_AND_CARD_READING.code -> CardPresentingUiState.WaitingForCard

            State.PIN_CODE_ENTERING.code -> CardPresentingUiState.PinEntering
            State.USE_CHIP_READER.code -> CardPresentingUiState.UseChip
            State.PRESENT_CARD_AGAIN.code -> CardPresentingUiState.PresentAgain
            State.USE_OTHER_INTERFACE.code -> CardPresentingUiState.UseOtherInterface
            State.USE_MAG_READER.code -> CardPresentingUiState.UseMagReader

            State.CONNECTING.code,
            State.DATA_EXCHANGE.code -> CardPresentingUiState.Processing

            else -> CardPresentingUiState.WaitingForCard
        }
    }


    override fun onDestroy() {
        paymentJob?.cancel()
        if (!paymentCompleted) {
            headlessClient?.cancelCardReading()
        }
        headlessClient?.close()
        headlessClient = null
        super.onDestroy()
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
        CardPresentingUiState.PinEntering -> "Введите PIN на терминале"
        CardPresentingUiState.UseChip -> "Вставьте карту"
        CardPresentingUiState.PresentAgain -> "Приложите карту повторно"
        CardPresentingUiState.UseOtherInterface -> "Используйте другой способ чтения"
        CardPresentingUiState.UseMagReader -> "Проведите карту"
        CardPresentingUiState.Processing -> "Связь с банком"
        CardPresentingUiState.ReturningResult -> "Завершаем операцию"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.card_presenting_background),
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
                    .fillMaxWidth()
                    .offset(y = (-24).dp),
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
private fun CardPresentingHeader() {
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
private fun CardPresentingAmount(amountText: String) {
    Text(
        text = amountText,
        fontFamily = TimesNewRomanFontFamily,
        fontSize = 42.sp,
        color = TextMain,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun PaymentTargetRings() {
    val transition = rememberInfiniteTransition(label = "payment_target_ripple")

    val waveProgress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2600,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_progress"
    )

    val centerGlow = transition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1700,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "center_glow"
    )

    Canvas(
        modifier = Modifier.size(270.dp)
    ) {
        val baseColor = Color(0xFFB58A3B)
        val maxRadius = size.minDimension * 0.47f
        val minRadius = size.minDimension * 0.12f

        // Мягкое центральное свечение
        drawCircle(
            color = baseColor.copy(alpha = 0.10f),
            radius = size.minDimension * 0.24f * centerGlow.value,
            center = center
        )

        drawCircle(
            color = baseColor.copy(alpha = 0.16f),
            radius = size.minDimension * 0.145f * centerGlow.value,
            center = center
        )

        // Три расходящиеся волны с разной фазой
        repeat(3) { index ->
            val phase = (waveProgress.value + index * 0.33f) % 1f

            val radius = minRadius + (maxRadius - minRadius) * phase
            val alpha = (1f - phase).coerceIn(0f, 1f)

            drawCircle(
                color = baseColor.copy(alpha = 0.32f * alpha),
                radius = radius,
                center = center,
                style = Stroke(
                    width = 2.2.dp.toPx()
                )
            )

            drawCircle(
                color = baseColor.copy(alpha = 0.10f * alpha),
                radius = radius + 8.dp.toPx(),
                center = center,
                style = Stroke(
                    width = 5.dp.toPx()
                )
            )
        }

        // Тонкое статичное внутреннее кольцо
        drawCircle(
            color = baseColor.copy(alpha = 0.34f),
            radius = size.minDimension * 0.18f,
            center = center,
            style = Stroke(width = 1.4.dp.toPx())
        )
    }
}

@Composable
private fun DecorativeBankCard() {
    val cardVisible = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(120)
        cardVisible.value = true
    }

    val cardX = animateFloatAsState(
        targetValue = if (cardVisible.value) 180f else 220f,
        animationSpec = tween(
            durationMillis = 950,
            easing = FastOutSlowInEasing
        ),
        label = "card_x"
    )

    val cardY = animateFloatAsState(
        targetValue = if (cardVisible.value) -25f else -125f,
        animationSpec = tween(
            durationMillis = 950,
            easing = FastOutSlowInEasing
        ),
        label = "card_y"
    )

    val cardRotation = animateFloatAsState(
        targetValue = if (cardVisible.value) 14f else 18f,
        animationSpec = tween(
            durationMillis = 950,
            easing = FastOutSlowInEasing
        ),
        label = "card_rotation"
    )

    val cardAlpha = animateFloatAsState(
        targetValue = if (cardVisible.value) 1f else 0f,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        ),
        label = "card_alpha"
    )

    val cardScale = animateFloatAsState(
        targetValue = if (cardVisible.value) 1f else 0.96f,
        animationSpec = tween(
            durationMillis = 950,
            easing = FastOutSlowInEasing
        ),
        label = "card_scale"
    )

    val attachTransition = rememberInfiniteTransition(label = "card_attach_cycle")

    val attachProgress = attachTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3600

                // Пауза после появления карты
                0f at 0 using FastOutSlowInEasing
                0f at 900 using FastOutSlowInEasing

                // Карта плавно идет к экрану
                1f at 1650 using FastOutSlowInEasing

                // Небольшая фиксация — как будто карта приложена
                1f at 1950 using FastOutSlowInEasing

                // Карта поднимается обратно
                0f at 2750 using FastOutSlowInEasing

                // Пауза перед следующим циклом
                0f at 3600 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "card_attach_progress"
    )

    val attachEnabled = if (cardAlpha.value >= 0.98f) 1f else 0f
    val effectiveAttach = attachProgress.value * attachEnabled

    Image(
        painter = painterResource(id = R.drawable.card_presenting_custom),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .size(width = 230.dp, height = 146.dp)
            .graphicsLayer {
                translationX = cardX.value + (-44f * effectiveAttach)
                translationY = cardY.value + (42f * effectiveAttach)
                rotationZ = cardRotation.value + (-5f * effectiveAttach)

                val pressScale = 1f - (0.035f * effectiveAttach)
                scaleX = cardScale.value * pressScale
                scaleY = cardScale.value * pressScale

                alpha = cardAlpha.value
            }
    )
}

private fun formatDonationAmount(amount: BigDecimal): String {
    // UI shows whole rubles; payment amount itself is still passed to SmartSkyPos unchanged.
    val rubles = amount.setScale(0, RoundingMode.DOWN).toPlainString()
    val grouped = rubles.reversed().chunked(3).joinToString(" ").reversed()
    return "$grouped ₽"
}
