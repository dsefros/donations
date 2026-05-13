package com.orthodox.charity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.skytech.smartskyposlib.Constants
import com.skytech.smartskyposlib.TransactionParams
import com.skytech.smartskyposlib.TransactionResult
import java.math.BigDecimal
import kotlin.math.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

// ── Palette ───────────────────────────────────────────────────────────────────
val BgMain      = Color(0xFFF5F0E8)
val BgHeader    = Color(0xFFEDE5D4)
val BgCard      = Color(0xFFFFFFFF)
val Gold        = Color(0xFFC8A84A)
val GoldDark    = Color(0xFF8A6820)
val GoldDeep    = Color(0xFF5A3E10)
val BorderLight = Color(0xFFD4C4A0)
val BorderGold  = Color(0xFFD4C9B0)
val TextPrimary = Color(0xFF3A2E1A)
val TextMuted   = Color(0xFF9A8870)
val TextGold    = Color(0xFFB0986A)

val BtnBrush = Brush.horizontalGradient(listOf(Color(0xFFC8A84A), Color(0xFFAA8830)))

// ── POS intent builder ────────────────────────────────────────────────────────
fun buildPaymentIntent(amount: BigDecimal): Intent =
    Intent("com.skytech.smartskypos.PAYMENT").apply {
        putExtra(Constants.PARAMS_KEY, TransactionParams(amount))
        putExtra(Constants.TYPE_KEY, Constants.TYPE_PAYMENT)
    }

// ── Payment result state ──────────────────────────────────────────────────────
sealed class PaymentResult {
    data class Success(val message: String, val rc: String) : PaymentResult()
    data class Declined(val code: Int, val message: String, val rc: String) : PaymentResult()
    data class Error(val reason: String) : PaymentResult()
}

// ── Activity ──────────────────────────────────────────────────────────────────
class MainActivity : ComponentActivity() {

    private val paymentResult   = mutableStateOf<PaymentResult?>(null)
    private val customAmount    = mutableStateOf("2000")

    private val posLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult -> handlePaymentResult(result) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE          or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN      or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION        or
            View.SYSTEM_UI_FLAG_FULLSCREEN             or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        setContent {
            OrthodoxCharityApp(
                paymentResult        = paymentResult.value,
                onClearResult        = { paymentResult.value = null },
                onPayment            = { amount -> posLauncher.launch(buildPaymentIntent(amount)) },
                onResetCustomAmount  = { customAmount.value = "2000" },
                customAmountValue    = customAmount.value,
                onCustomAmountChange = { customAmount.value = it }
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE          or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN      or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION        or
                View.SYSTEM_UI_FLAG_FULLSCREEN             or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }

    @Deprecated("Disabled")
    override fun onBackPressed() {}

    private fun handlePaymentResult(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            paymentResult.value = PaymentResult.Error("Оплата отменена или терминал недоступен")
            return
        }
        val tx: TransactionResult? = result.data!!
            .getParcelableExtra(Constants.TRANSACTION_RESULT_KEY)
        if (tx == null) {
            paymentResult.value = PaymentResult.Error("Нет данных о транзакции")
            return
        }
        val code    = tx.code
        val message = tx.message ?: ""
        val rc      = tx.rc ?: ""
        paymentResult.value = if (code == 0 && rc == "00") {
            PaymentResult.Success(message = message, rc = rc)
        } else {
            PaymentResult.Declined(code = code, message = message, rc = rc)
        }
        // Reset custom amount field to default after returning from POS
        customAmount.value = "2000"
    }
}

// ── Root ──────────────────────────────────────────────────────────────────────
@Composable
fun OrthodoxCharityApp(
    paymentResult       : PaymentResult?,
    onClearResult       : () -> Unit,
    onPayment           : (BigDecimal) -> Unit,
    onResetCustomAmount : () -> Unit,
    customAmountValue   : String = "2000",
    onCustomAmountChange: (String) -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize().background(BgMain)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader()
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CrossPanel(modifier = Modifier.width(170.dp).fillMaxHeight())
                ButtonsPanel(
                    modifier      = Modifier.weight(1f).fillMaxHeight(),
                    onPayment     = onPayment,
                    customAmount  = customAmountValue,
                    onAmountChange = onCustomAmountChange
                )
            }
            AppFooter()
        }
        paymentResult?.let {
            PaymentResultDialog(result = it, onDismiss = onClearResult)
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────
@Composable
fun AppHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgHeader)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "ПРАВОСЛАВНАЯ БЛАГОТВОРИТЕЛЬНОСТЬ",
            style = TextStyle(color = GoldDark, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp, textAlign = TextAlign.Center)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "Помогите ближнему своему",
            style = TextStyle(color = TextMuted, fontSize = 10.sp, letterSpacing = 1.sp)
        )
    }
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(BorderGold))
}

// ── Footer ────────────────────────────────────────────────────────────────────
@Composable
fun AppFooter() {
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(BorderGold))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgHeader)
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "С БОГОМ  ·  ВО СЛАВУ ГОСПОДНЮ",
            style = TextStyle(color = TextGold, fontSize = 8.sp, letterSpacing = 2.sp)
        )
    }
}

// ── Left panel — cross + verse ────────────────────────────────────────────────
@Composable
fun CrossPanel(modifier: Modifier = Modifier) {
    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier         = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            OrthodoxCross(modifier = Modifier.size(width = 100.dp, height = 148.dp))
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Text(
                "«Блажен, кто думает\nо бедном и нищем»",
                style = TextStyle(color = TextMuted, fontSize = 10.sp, fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center, lineHeight = 15.sp)
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "— Псалом 40:1",
                style = TextStyle(color = TextGold, fontSize = 9.sp, textAlign = TextAlign.Center)
            )
        }
    }
}

// ── Orthodox cross ────────────────────────────────────────────────────────────
@Composable
fun OrthodoxCross(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "cross")
    val haloScale by inf.animateFloat(1f, 1.15f,
        infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "halo")

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(130.dp).scale(haloScale)) {
            drawCircle(brush = Brush.radialGradient(listOf(Color(0x22C8A84A), Color.Transparent)))
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val shaftW = w * 0.14f
            val shaftX = (w - shaftW) / 2f
            val rx = 2.5.dp.toPx()

            val goldV = Brush.verticalGradient(listOf(Color(0xFFC8A84A), Color(0xFFA07828), Color(0xFF7A5818)), 0f, h)
            val goldH = Brush.horizontalGradient(listOf(Color(0xFF7A5818), Color(0xFFC8A84A), Color(0xFF7A5818)), 0f, w)
            val hiV   = Brush.verticalGradient(listOf(Color(0x20FFFFFF), Color.Transparent), 0f, h * 0.35f)

            // Vertical shaft
            drawRoundRect(goldV, Offset(shaftX, h * 0.04f), Size(shaftW, h * 0.90f), CornerRadius(rx))

            // Titlo (top small bar) — shorter
            drawRoundRect(goldH, Offset(w * 0.30f, h * 0.12f), Size(w * 0.40f, h * 0.062f), CornerRadius(rx))

            // Main crossbar — shorter
            drawRoundRect(goldH, Offset(w * 0.18f, h * 0.30f), Size(w * 0.64f, h * 0.090f), CornerRadius(rx))

            // Suppedaneum — higher position, shorter length, left HIGH right LOW
            val footH = h * 0.055f
            drawPath(Path().apply {
                moveTo(w * 0.28f, h * 0.580f)
                lineTo(w * 0.72f, h * 0.700f)
                lineTo(w * 0.72f, h * 0.700f + footH)
                lineTo(w * 0.28f, h * 0.580f + footH)
                close()
            }, goldH)

            // Highlight on shaft
            drawRoundRect(hiV, Offset(shaftX, h * 0.04f), Size(shaftW * 0.30f, h * 0.45f), CornerRadius(rx))
        }
    }
}

// ── Right panel — buttons ─────────────────────────────────────────────────────
@Composable
fun ButtonsPanel(
    modifier       : Modifier = Modifier,
    onPayment      : (BigDecimal) -> Unit,
    customAmount   : String = "2000",
    onAmountChange : (String) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier            = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        CustomAmountButton(
            amount         = customAmount,
            onAmountChange = { value ->
                // Allow only digits and one decimal point, max 8 chars
                if (value.all { it.isDigit() || it == '.' } && value.length <= 8) {
                    onAmountChange(value)
                }
            },
            focusRequester = focusRequester,
            onPay          = {
                val parsed = customAmount.toBigDecimalOrNull()
                if (parsed != null && parsed > BigDecimal.ZERO) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onPayment(parsed)
                }
            }
        )
        DonateButton("ПОМОЩЬ ХРАМУ",  "Восстановление и нужды церкви", "500 ₽") {
            focusManager.clearFocus()
            onPayment(BigDecimal("500.00"))
        }
        DonateButton("ДЕТСКИЙ ПРИЮТ", "Забота о сиротах и детях", "1 000 ₽") {
            focusManager.clearFocus()
            onPayment(BigDecimal("1000.00"))
        }
    }
}

// ── Donate button ─────────────────────────────────────────────────────────────
// Gold left bar done via outer Box + padding — no drawBehind needed
@Composable
fun DonateButton(cause: String, desc: String, amount: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "scale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isPressed) Color(0xFFFDF8F0) else BgCard,
        label       = "bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isPressed) Gold else BorderLight,
        label       = "border"
    )

    // Outer box provides the gold left bar (4dp wide)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .background(color = Gold, shape = RoundedCornerShape(8.dp))
            .padding(start = 4.dp)                           // exposes 4dp gold on left
    ) {
        // Inner box is the white card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = bgColor, shape = RoundedCornerShape(
                    topStart = 0.dp, bottomStart = 0.dp, topEnd = 8.dp, bottomEnd = 8.dp
                ))
                .border(
                    BorderStroke(1.dp, borderColor),
                    RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                )
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .padding(start = 14.dp, end = 14.dp, top = 13.dp, bottom = 13.dp)
        ) {
            Column {
                Text(cause, style = TextStyle(color = GoldDark, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp))
                Spacer(Modifier.height(3.dp))
                Text(desc, style = TextStyle(color = TextMuted, fontSize = 10.sp))
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(amount, style = TextStyle(color = GoldDeep, fontSize = 28.sp,
                        fontWeight = FontWeight.Bold, lineHeight = 30.sp))
                    Box(
                        modifier = Modifier
                            .background(BtnBrush, RoundedCornerShape(4.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("ПОЖЕРТВОВАТЬ", style = TextStyle(color = Color.White,
                            fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                    }
                }
            }
        }
    }
}

// ── Custom amount button (3rd slot) ──────────────────────────────────────────
@Composable
fun CustomAmountButton(
    amount         : String,
    onAmountChange : (String) -> Unit,
    focusRequester : FocusRequester,
    onPay          : () -> Unit
) {
    val isValid = amount.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Gold, shape = RoundedCornerShape(8.dp))
            .padding(start = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = BgCard, shape = RoundedCornerShape(
                    topStart = 0.dp, bottomStart = 0.dp, topEnd = 8.dp, bottomEnd = 8.dp
                ))
                .border(BorderStroke(1.dp, BorderLight), RoundedCornerShape(
                    topStart = 0.dp, bottomStart = 0.dp, topEnd = 8.dp, bottomEnd = 8.dp
                ))
                .padding(start = 14.dp, end = 14.dp, top = 13.dp, bottom = 13.dp)
        ) {
            Column {
                Text("СВОЯ СУММА", style = TextStyle(color = GoldDark, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp))
                Spacer(Modifier.height(3.dp))
                Text("Введите любую сумму", style = TextStyle(color = TextMuted, fontSize = 10.sp))
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    // Numeric input field
                    OutlinedTextField(
                        value         = amount,
                        onValueChange = onAmountChange,
                        modifier      = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),

                        textStyle = TextStyle(color = GoldDeep, fontSize = 24.sp,
                            fontWeight = FontWeight.Bold),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { onPay() }
                        ),
                        singleLine = true,
                        suffix     = {
                            Text(" ₽", style = TextStyle(color = GoldDeep, fontSize = 20.sp,
                                fontWeight = FontWeight.Bold))
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Gold,
                            unfocusedBorderColor = BorderLight,
                            cursorColor          = Gold
                        )
                    )
                    Spacer(Modifier.width(10.dp))
                    // Pay button — only active when amount is valid
                    Box(
                        modifier = Modifier
                            .background(
                                if (isValid) BtnBrush
                                else Brush.horizontalGradient(listOf(BorderLight, BorderLight)),
                                RoundedCornerShape(4.dp)
                            )
                            .clickable(enabled = isValid) { onPay() }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("ВНЕСТИ ЛЕПТУ", style = TextStyle(
                            color      = if (isValid) Color.White else TextMuted,
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ))
                    }
                }
            }
        }
    }
}

// ── Payment result dialog ─────────────────────────────────────────────────────
@Composable
fun PaymentResultDialog(result: PaymentResult, onDismiss: () -> Unit) {
    // Auto-dismiss after 10 seconds
    LaunchedEffect(result) {
        delay(10_000L)
        onDismiss()
    }

    val scale by animateFloatAsState(1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "dialogScale")

    val icon     : String
    val title    : String
    val message  : String
    val detail   : String
    val btnColor : Color
    val btnLabel : String
    val iconColor: Color
    val topBar   : Color

    when (result) {
        is PaymentResult.Success -> {
            icon      = "☩"
            title     = "ОПЛАТА ПРИНЯТА"
            message   = result.message
            detail    = "Код ответа: ${result.rc}"
            btnColor  = Gold
            btnLabel  = "А М И Н Ь"
            iconColor = Gold
            topBar    = Gold
        }
        is PaymentResult.Declined -> {
            icon      = "✕"
            title     = "ОТКЛОНЕНО"
            message   = result.message
            detail    = "RC: ${result.rc}  •  Код: ${result.code}"
            btnColor  = Color(0xFFB03030)
            btnLabel  = "ЗАКРЫТЬ"
            iconColor = Color(0xFFB03030)
            topBar    = Color(0xFFB03030)
        }
        is PaymentResult.Error -> {
            icon      = "!"
            title     = "ОШИБКА"
            message   = result.reason
            detail    = ""
            btnColor  = GoldDark
            btnLabel  = "ЗАКРЫТЬ"
            iconColor = GoldDark
            topBar    = GoldDark
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF0F5F0E8))
            .clickable(indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        // Dialog card — top color bar via outer Box + padding
        Box(
            modifier = Modifier
                .scale(scale)
                .widthIn(max = 260.dp)
                .background(color = topBar, shape = RoundedCornerShape(10.dp))
                .padding(top = 4.dp)                         // exposes 4dp color bar on top
                .clickable(indication = null,
                    interactionSource = remember { MutableInteractionSource() }) {}
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = BgCard, shape = RoundedCornerShape(
                        topStart = 0.dp, topEnd = 0.dp, bottomStart = 10.dp, bottomEnd = 10.dp
                    ))
                    .border(BorderStroke(1.dp, BorderLight), RoundedCornerShape(
                        topStart = 0.dp, topEnd = 0.dp, bottomStart = 10.dp, bottomEnd = 10.dp
                    ))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(icon, style = TextStyle(color = iconColor, fontSize = 28.sp))
                Spacer(Modifier.height(10.dp))
                Text(title, style = TextStyle(color = iconColor, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp))
                Spacer(Modifier.height(8.dp))
                Text(message, style = TextStyle(color = TextPrimary, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
                if (detail.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(detail, style = TextStyle(color = TextMuted, fontSize = 10.sp,
                        textAlign = TextAlign.Center, letterSpacing = 0.5.sp))
                }
                Spacer(Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = btnColor, shape = RoundedCornerShape(4.dp))
                        .clickable { onDismiss() }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(btnLabel, style = TextStyle(color = Color.White, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 2.sp))
                }
            }
        }
    }
}
