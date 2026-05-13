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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skytech.smartskyposlib.Constants
import com.skytech.smartskyposlib.TransactionParams
import com.skytech.smartskyposlib.TransactionResult
import java.math.BigDecimal
import kotlinx.coroutines.delay

private val BgMain = Color.White
private val BgFooter = Color(0xFFF7F3EA)
private val CardBg = Color.White
private val Gold = Color(0xFFB3904B)
private val GoldDark = Color(0xFF8D6A2B)
private val BorderGold = Color(0xFFD7C6A5)
private val MutedWarm = Color(0xFF8C7A63)
private val TextMain = Color(0xFF3E3222)
private val ActionBrush = Brush.horizontalGradient(listOf(Color(0xFF9F7B3A), Color(0xFFBE9B56)))

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
    customAmountValue: String,
    onCustomAmountChange: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(BgMain)) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader()
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 1.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CrossPanel(modifier = Modifier.width(130.dp).fillMaxSize())
                ButtonsPanel(
                    modifier = Modifier.weight(1f),
                    customAmount = customAmountValue,
                    onAmountChange = onCustomAmountChange,
                    onPayment = onPayment
                )
            }
            AppFooter()
        }

        paymentResult?.let {
            PaymentResultDialog(result = it, onDismiss = onClearResult)
        }
    }
}

@Composable
fun AppHeader() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ПРАВОСЛАВНАЯ БЛАГОТВОРИТЕЛЬНОСТЬ",
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp,
                letterSpacing = 1.8.sp,
                color = GoldDark,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Помогите ближнему своему",
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontSize = 13.sp,
                letterSpacing = 1.2.sp,
                color = MutedWarm
            )
        )
        Spacer(modifier = Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Box(modifier = Modifier.width(74.dp).height(1.dp).background(BorderGold))
            Text(" ✢ ", style = TextStyle(color = BorderGold, fontSize = 12.sp))
            Box(modifier = Modifier.width(74.dp).height(1.dp).background(BorderGold))
        }
    }
}

@Composable
fun CrossPanel(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        ShimmeringCross(modifier = Modifier.size(width = 98.dp, height = 160.dp))
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "«Блажен, кто думает\nо бедном и нищем»",
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontSize = 19.sp/2,
                lineHeight = 15.sp,
                color = GoldDark,
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic
            )
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "— Псалом 40:1",
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontSize = 11.sp,
                color = MutedWarm,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
fun ShimmeringCross(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "cross_shimmer")
    val shift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_shift"
    )

    androidx.compose.foundation.Image(
        painter = painterResource(id = R.drawable.orthodox_cross_custom),
        contentDescription = "Православный крест",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val w = size.width
                val h = size.height
                val startX = (shift - 0.45f) * w
                val endX = (shift + 0.45f) * w
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.13f),
                            Color.Transparent
                        ),
                        start = androidx.compose.ui.geometry.Offset(startX, h),
                        end = androidx.compose.ui.geometry.Offset(endX, 0f)
                    ),
                    blendMode = BlendMode.SrcAtop
                )
            }
    )
}

@Composable
fun ButtonsPanel(
    modifier: Modifier = Modifier,
    customAmount: String,
    onAmountChange: (String) -> Unit,
    onPayment: (BigDecimal) -> Unit
) {
    Column(
        modifier = modifier.padding(top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        DonationActionCard(
            title = "СВОЯ СУММА",
            description = "Введите любую сумму",
            actionLabel = "ВНЕСТИ\nЛЕПТУ",
            clickWholeCard = false,
            onClick = {
                val parsed = customAmount.toBigDecimalOrNull()
                if (parsed != null && parsed > BigDecimal.ZERO) onPayment(parsed)
            },
            content = {
                AmountInput(value = customAmount, onValueChange = onAmountChange)
            }
        )

        DonationActionCard(
            title = "ПОМОЩЬ ХРАМУ",
            description = "Восстановление и нужды церкви",
            actionLabel = "ПОЖЕРТВОВАТЬ",
            clickWholeCard = true,
            onClick = { onPayment(BigDecimal("500.00")) },
            content = {
                Text(
                    text = "500 ₽",
                    style = TextStyle(fontFamily = FontFamily.Serif, color = TextMain, fontSize = 38.sp / 1.3f, fontWeight = FontWeight.SemiBold)
                )
            }
        )

        DonationActionCard(
            title = "ДЕТСКИЙ ПРИЮТ",
            description = "Забота о сиротах и детях",
            actionLabel = "ПОЖЕРТВОВАТЬ",
            clickWholeCard = true,
            onClick = { onPayment(BigDecimal("1000.00")) },
            content = {
                Text(
                    text = "1 000 ₽",
                    style = TextStyle(fontFamily = FontFamily.Serif, color = TextMain, fontSize = 38.sp / 1.3f, fontWeight = FontWeight.SemiBold)
                )
            }
        )
    }
}

@Composable
fun DonationActionCard(
    title: String,
    description: String,
    actionLabel: String,
    clickWholeCard: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = spring(stiffness = 700f),
        label = "card_scale"
    )

    val rounded = RoundedCornerShape(12.dp)
    val cardClick = if (clickWholeCard) {
        Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .scale(scale)
            .clip(rounded)
            .background(CardBg)
            .border(BorderStroke(1.dp, BorderGold), rounded)
            .then(cardClick)
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(start = 10.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 16.sp,
                        color = GoldDark,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = description,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontSize = 11.sp, color = MutedWarm, fontFamily = FontFamily.Serif)
                )
            }
            content()
        }

        val actionInteraction = remember { MutableInteractionSource() }
        val actionPressed by actionInteraction.collectIsPressedAsState()
        val actionScale by animateFloatAsState(if (actionPressed) 0.98f else 1f, label = "action_scale")

        Box(
            modifier = Modifier
                .padding(2.dp)
                .width(100.dp)
                .fillMaxSize()
                .scale(actionScale)
                .clip(RoundedCornerShape(10.dp))
                .background(ActionBrush)
                .clickable(interactionSource = actionInteraction, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = actionLabel,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    color = Color.White,
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    letterSpacing = 0.2.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
fun AmountInput(value: String, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 42.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(7.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "₽",
                style = TextStyle(color = GoldDark, fontFamily = FontFamily.Serif, fontSize = 18.sp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            BasicTextField(
                value = value,
                onValueChange = {
                    if (it.all(Char::isDigit) && it.length <= 8) onValueChange(it)
                    if (it.isEmpty()) onValueChange(it)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    color = TextMain,
                    fontFamily = FontFamily.Serif,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            text = "0",
                            style = TextStyle(color = MutedWarm.copy(alpha = 0.45f), fontSize = 24.sp, fontFamily = FontFamily.Serif)
                        )
                    }
                    inner()
                }
            )
        }
    }
}

@Composable
fun AppFooter() {
    Column(modifier = Modifier.fillMaxWidth().background(BgFooter)) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderGold))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("☦", style = TextStyle(color = GoldDark, fontSize = 14.sp, fontFamily = FontFamily.Serif))
            Spacer(modifier = Modifier.width(6.dp))
            Text("МОЛИТВА", style = TextStyle(color = GoldDark, fontSize = 11.sp, letterSpacing = 1.sp, fontFamily = FontFamily.Serif))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Господи, помилуй. Господи, прости. Господи, благослови.",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(color = MutedWarm, fontSize = 10.sp, fontFamily = FontFamily.Serif)
            )
        }
    }
}

@Composable
fun PaymentResultDialog(result: PaymentResult, onDismiss: () -> Unit) {
    LaunchedEffect(result) {
        delay(10_000)
        onDismiss()
    }

    val (icon, title, subtitle, btnLabel) = when (result) {
        is PaymentResult.Success -> Quad("☩", "ОПЛАТА ПРИНЯТА", "Код ответа: ${result.rc}", "А М И Н Ь")
        is PaymentResult.Declined -> Quad("✕", "ОТКЛОНЕНО", "RC: ${result.rc} • Код: ${result.code}", "ЗАКРЫТЬ")
        is PaymentResult.Error -> Quad("!", "ОШИБКА", result.reason, "ЗАКРЫТЬ")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.26f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(12.dp))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(18.dp))
            Text(icon, style = TextStyle(color = GoldDark, fontSize = 28.sp, fontFamily = FontFamily.Serif))
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, style = TextStyle(color = GoldDark, fontSize = 16.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold))
            Spacer(modifier = Modifier.height(6.dp))
            val message = when (result) {
                is PaymentResult.Success -> result.message
                is PaymentResult.Declined -> result.message
                is PaymentResult.Error -> result.reason
            }
            Text(message, textAlign = TextAlign.Center, style = TextStyle(color = TextMain, fontSize = 14.sp, fontFamily = FontFamily.Serif), modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, textAlign = TextAlign.Center, style = TextStyle(color = MutedWarm, fontSize = 11.sp, fontFamily = FontFamily.Serif), modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ActionBrush)
                    .clickable { onDismiss() }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(btnLabel, style = TextStyle(color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Serif, letterSpacing = 1.sp, fontWeight = FontWeight.Medium))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

data class Quad(val a: String, val b: String, val c: String, val d: String)
