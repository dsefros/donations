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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skytech.smartskyposlib.Constants
import com.skytech.smartskyposlib.TransactionParams
import com.skytech.smartskyposlib.TransactionResult
import java.math.BigDecimal
import kotlinx.coroutines.delay

private val Ivory = Color(0xFFFCFAF4)
private val HeaderFooter = Color(0xFFF3EBDD)
private val CardWhite = Color(0xFFFFFDF9)
private val Gold = Color(0xFFA7833A)
private val GoldLight = Color(0xFFC3A15B)
private val GoldDark = Color(0xFF76551E)
private val TextPrimary = Color(0xFF2E2A24)
private val TextMuted = Color(0xFF746C62)
private val ThinLine = Color(0xFFD8C8A8)
private val CardBorder = Color(0xCCA7833A)

private val ActionBrush = Brush.linearGradient(listOf(GoldLight, Gold, GoldDark))

fun buildPaymentIntent(amount: BigDecimal): Intent =
    Intent("com.skytech.smartskypos.PAYMENT").apply {
        putExtra(Constants.PARAMS_KEY, TransactionParams(amount))
        putExtra(Constants.TYPE_KEY, Constants.TYPE_PAYMENT)
    }

sealed class PaymentResult {
    data class Success(val message: String, val rc: String) : PaymentResult()
    data class Declined(val code: Int, val message: String, val rc: String) : PaymentResult()
    data class Error(val reason: String) : PaymentResult()
}

class MainActivity : ComponentActivity() {

    private val paymentResult = mutableStateOf<PaymentResult?>(null)
    private val customAmount = mutableStateOf("2000")

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
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        setContent {
            OrthodoxCharityApp(
                paymentResult = paymentResult.value,
                onClearResult = { paymentResult.value = null },
                onPayment = { amount -> posLauncher.launch(buildPaymentIntent(amount)) },
                customAmountValue = customAmount.value,
                onCustomAmountChange = { customAmount.value = it }
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
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
        val tx: TransactionResult? = result.data!!.getParcelableExtra(Constants.TRANSACTION_RESULT_KEY)
        if (tx == null) {
            paymentResult.value = PaymentResult.Error("Нет данных о транзакции")
            return
        }
        val code = tx.code
        val message = tx.message ?: ""
        val rc = tx.rc ?: ""
        paymentResult.value = if (code == 0 && rc == "00") {
            PaymentResult.Success(message = message, rc = rc)
        } else {
            PaymentResult.Declined(code = code, message = message, rc = rc)
        }
        customAmount.value = "2000"
    }
}

@Composable
fun OrthodoxCharityApp(
    paymentResult: PaymentResult?,
    onClearResult: () -> Unit,
    onPayment: (BigDecimal) -> Unit,
    customAmountValue: String = "2000",
    onCustomAmountChange: (String) -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize().background(Ivory)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader()
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 24.dp, top = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CrossPanel(modifier = Modifier.width(176.dp).fillMaxHeight())
                ButtonsPanel(
                    modifier = Modifier.weight(1f),
                    onPayment = onPayment,
                    customAmount = customAmountValue,
                    onAmountChange = onCustomAmountChange
                )
            }
            AppFooter()
        }
        paymentResult?.let { PaymentResultDialog(result = it, onDismiss = onClearResult) }
    }
}

@Composable
fun AppHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(HeaderFooter)
            .padding(top = 8.dp, start = 12.dp, end = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "ПРАВОСЛАВНАЯ БЛАГОТВОРИТЕЛЬНОСТЬ",
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                letterSpacing = 2.4.sp,
                color = GoldDark,
                textAlign = TextAlign.Center
            )
        )
        Text(
            "Помогите ближнему своему",
            style = TextStyle(fontSize = 12.sp, letterSpacing = 3.2.sp, color = TextMuted)
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Box(Modifier.width(90.dp).height(1.dp).background(ThinLine))
            Text("  ✢  ", color = Color(0xFFB79B64), fontSize = 14.sp)
            Box(Modifier.width(90.dp).height(1.dp).background(ThinLine))
        }
    }
}

@Composable
fun AppFooter() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(ThinLine))
    Row(
        modifier = Modifier.fillMaxWidth().height(40.dp).background(HeaderFooter).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("☩", color = Gold, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text("МОЛИТВА", color = GoldDark, fontSize = 20.sp, fontFamily = FontFamily.Serif)
        Spacer(Modifier.width(10.dp))
        Text(
            "Господи, помилуй. Господи, прости. Господи, благослови.",
            color = TextMuted.copy(alpha = 0.75f),
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

@Composable
fun CrossPanel(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            BotanicalBackground(Modifier.fillMaxSize().padding(top = 10.dp))
            OrthodoxCross(modifier = Modifier.size(width = 110.dp, height = 160.dp))
        }
        Text(
            "«Блажен, кто думает\nо бедном и нищем»",
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                color = GoldDark,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )
        )
        Text(
            "— Псалом 40:1",
            style = TextStyle(fontFamily = FontFamily.Serif, color = TextMuted, fontSize = 11.sp)
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun BotanicalBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stem = TextMuted.copy(alpha = 0.10f)
        repeat(5) { idx ->
            val x = size.width * (0.18f + idx * 0.16f)
            drawLine(stem, Offset(x, size.height * 0.18f), Offset(x - 22f, size.height * 0.82f), strokeWidth = 2f)
            drawCircle(stem, radius = 8f, center = Offset(x + 12f, size.height * (0.32f + idx * 0.05f)))
            drawCircle(stem, radius = 7f, center = Offset(x - 12f, size.height * (0.48f + idx * 0.04f)))
        }
    }
}

@Composable
fun OrthodoxCross(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val brush = Brush.verticalGradient(listOf(GoldLight, Gold, GoldDark))
        val shaftW = w * 0.18f
        val shaftX = (w - shaftW) / 2f
        drawRoundRect(brush, Offset(shaftX, h * 0.08f), Size(shaftW, h * 0.86f), CornerRadius(4f, 4f))
        drawRoundRect(brush, Offset(w * 0.30f, h * 0.14f), Size(w * 0.40f, h * 0.08f), CornerRadius(4f, 4f))
        drawRoundRect(brush, Offset(w * 0.10f, h * 0.34f), Size(w * 0.80f, h * 0.11f), CornerRadius(4f, 4f))
        drawPath(Path().apply {
            moveTo(w * 0.16f, h * 0.63f)
            lineTo(w * 0.84f, h * 0.76f)
            lineTo(w * 0.84f, h * 0.82f)
            lineTo(w * 0.16f, h * 0.69f)
            close()
        }, brush)
    }
}

@Composable
fun ButtonsPanel(
    modifier: Modifier = Modifier,
    onPayment: (BigDecimal) -> Unit,
    customAmount: String = "2000",
    onAmountChange: (String) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DonationActionCard(
            title = "СВОЯ СУММА",
            description = "Введите любую сумму",
            actionLabel = "ВНЕСТИ\nЛЕПТУ",
            onClick = {
                val parsed = customAmount.toBigDecimalOrNull()
                if (parsed != null && parsed > BigDecimal.ZERO) onPayment(parsed)
            },
            content = {
                AmountInput(amount = customAmount, onAmountChange = onAmountChange)
            }
        )
        DonationActionCard(
            title = "ПОМОЩЬ ХРАМУ",
            description = "Восстановление и нужды церкви",
            actionLabel = "ПОЖЕРТВОВАТЬ",
            onClick = { onPayment(BigDecimal("500.00")) },
            content = {
                Text("500 ₽", style = TextStyle(fontFamily = FontFamily.Serif, color = TextPrimary, fontSize = 32.sp))
            }
        )
        DonationActionCard(
            title = "ДЕТСКИЙ ПРИЮТ",
            description = "Забота о сиротах и детях",
            actionLabel = "ПОЖЕРТВОВАТЬ",
            onClick = { onPayment(BigDecimal("1000.00")) },
            content = {
                Text("1 000 ₽", style = TextStyle(fontFamily = FontFamily.Serif, color = TextPrimary, fontSize = 32.sp))
            }
        )
    }
}

@Composable
fun AmountInput(amount: String, onAmountChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .width(140.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(BorderStroke(1.dp, Color(0xFFD6C9B5)), RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("₽", color = TextMuted, fontSize = 20.sp)
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = amount,
            onValueChange = { value ->
                if (value.all { it.isDigit() } && value.length <= 10) onAmountChange(value)
            },
            singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontSize = 24.sp, fontFamily = FontFamily.Serif),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions.Default,
            cursorBrush = SolidColor(GoldDark),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun DonationActionCard(
    title: String,
    description: String,
    actionLabel: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, label = "cardScale")
    val border by animateColorAsState(if (pressed) GoldDark else CardBorder, label = "cardBorder")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(99.dp)
            .scale(scale)
            .border(BorderStroke(1.dp, border), RoundedCornerShape(9.dp))
            .clip(RoundedCornerShape(9.dp))
            .background(CardWhite)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 14.dp, end = 12.dp, top = 11.dp, bottom = 9.dp)
        ) {
            Text(title, style = TextStyle(fontFamily = FontFamily.Serif, color = GoldDark, fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
            Text(description, style = TextStyle(color = TextMuted, fontSize = 12.sp), maxLines = 1)
            Spacer(Modifier.weight(1f))
            content()
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(CardBorder))
        Box(
            modifier = Modifier.width(116.dp).fillMaxHeight().background(ActionBrush),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(actionLabel, style = TextStyle(color = Color.White, fontFamily = FontFamily.Serif, fontSize = 13.sp, textAlign = TextAlign.Center))
                Text("→", style = TextStyle(color = Color.White, fontSize = 25.sp))
            }
        }
    }
}

@Composable
fun PaymentResultDialog(result: PaymentResult, onDismiss: () -> Unit) {
    LaunchedEffect(result) {
        delay(10_000L)
        onDismiss()
    }

    val scale by animateFloatAsState(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "dialogScale")

    val icon: String
    val title: String
    val message: String
    val detail: String
    val btnColor: Color
    val iconColor: Color

    when (result) {
        is PaymentResult.Success -> {
            icon = "☩"; title = "ОПЛАТА ПРИНЯТА"; message = result.message; detail = "Код ответа: ${result.rc}"; btnColor = Gold; iconColor = GoldDark
        }

        is PaymentResult.Declined -> {
            icon = "✕"; title = "ОТКЛОНЕНО"; message = result.message; detail = "RC: ${result.rc} • Код: ${result.code}"; btnColor = Color(0xFFB23A3A); iconColor = Color(0xFFB23A3A)
        }

        is PaymentResult.Error -> {
            icon = "!"; title = "ОШИБКА"; message = result.reason; detail = ""; btnColor = GoldDark; iconColor = GoldDark
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xD9FCFAF4)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.scale(scale).widthIn(max = 290.dp).background(CardWhite, RoundedCornerShape(10.dp))
                .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(10.dp)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, color = iconColor, fontSize = 30.sp)
            Text(title, color = iconColor, fontSize = 13.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(message, color = TextPrimary, fontSize = 18.sp, textAlign = TextAlign.Center)
            if (detail.isNotEmpty()) Text(detail, color = TextMuted, fontSize = 11.sp)
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxWidth().background(btnColor, RoundedCornerShape(5.dp)).clickable { onDismiss() }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) { Text("ЗАКРЫТЬ", color = Color.White, fontWeight = FontWeight.SemiBold) }
        }
    }
}
