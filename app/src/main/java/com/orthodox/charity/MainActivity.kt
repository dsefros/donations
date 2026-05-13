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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
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

private val BgMain = Color(0xFFF5F3F1)
private val Gold = Color(0xFFAE8843)
private val GoldDark = Color(0xFF8A6A30)
private val BorderGold = Color(0xFFD0B98C)
private val TextMain = Color(0xFF3D3326)
private val MutedWarm = Color(0xFF8D7C66)
private val FooterBar = Color(0xFF9A7B3E)
private val ActionBrush = Brush.horizontalGradient(listOf(Color(0xFF9D7C3D), Color(0xFFB99653)))

private val AlegreyaFontFamily = FontFamily(
    Font(R.font.alegreya_regular, FontWeight.Normal),
    Font(R.font.alegreya_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.alegreya_medium, FontWeight.Medium),
    Font(R.font.alegreya_semibold, FontWeight.SemiBold),
    Font(R.font.alegreya_bold, FontWeight.Bold)
)


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
                    .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CrossPanel(modifier = Modifier.width(130.dp).fillMaxHeight())
                ButtonsPanel(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    customAmount = customAmountValue,
                    onAmountChange = onCustomAmountChange,
                    onPayment = onPayment
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
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ПРАВОСЛАВНАЯ БЛАГОТВОРИТЕЛЬНОСТЬ",
            style = TextStyle(
                fontFamily = AlegreyaFontFamily,
                fontSize = 16.sp,
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.SemiBold,
                color = GoldDark
            )
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "Помогите ближнему своему",
            style = TextStyle(
                fontFamily = AlegreyaFontFamily,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                color = MutedWarm
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(76.dp).height(1.dp).background(BorderGold))
            Text(" ✢ ", style = TextStyle(fontSize = 12.sp, color = BorderGold))
            Box(modifier = Modifier.width(76.dp).height(1.dp).background(BorderGold))
        }
    }
}

@Composable
fun CrossPanel(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        TreeBranchesBackground(
            modifier = Modifier
                .fillMaxHeight()
                .width(120.dp)
                .align(Alignment.CenterStart)
        )
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            ShimmeringCross(modifier = Modifier.size(width = 98.dp, height = 160.dp))
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "«Блажен, кто думает\nо бедном и нищем»",
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontFamily = AlegreyaFontFamily,
                    fontStyle = FontStyle.Italic,
                    color = GoldDark,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "— Псалом 40:1",
                style = TextStyle(fontFamily = AlegreyaFontFamily, fontSize = 11.sp, color = MutedWarm)
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TreeBranchesBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val branchColor = Gold.copy(alpha = 0.055f)
        val leafColor = GoldDark.copy(alpha = 0.045f)
        val w = size.width
        val h = size.height
        val branchStroke = Stroke(width = 1.35.dp.toPx(), cap = StrokeCap.Round)

        val mainBranch = Path().apply {
            moveTo(w * 0.06f, h * 0.86f)
            quadraticBezierTo(w * 0.24f, h * 0.70f, w * 0.42f, h * 0.56f)
        }
        drawPath(mainBranch, color = branchColor, style = branchStroke)

        val branchA = Path().apply {
            moveTo(w * 0.16f, h * 0.73f)
            quadraticBezierTo(w * 0.28f, h * 0.60f, w * 0.44f, h * 0.46f)
        }
        drawPath(branchA, color = branchColor, style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round))

        val branchB = Path().apply {
            moveTo(w * 0.26f, h * 0.66f)
            quadraticBezierTo(w * 0.38f, h * 0.53f, w * 0.52f, h * 0.38f)
        }
        drawPath(branchB, color = branchColor, style = Stroke(width = 1.0.dp.toPx(), cap = StrokeCap.Round))

        fun leaf(cx: Float, cy: Float, angle: Float, scale: Float) {
            rotate(degrees = angle, pivot = androidx.compose.ui.geometry.Offset(cx, cy)) {
                val p = Path().apply {
                    moveTo(cx, cy)
                    quadraticBezierTo(cx + 7f * scale, cy - 3f * scale, cx + 2f * scale, cy - 10f * scale)
                    quadraticBezierTo(cx - 3f * scale, cy - 4f * scale, cx, cy)
                    close()
                }
                drawPath(p, color = leafColor)
            }
        }

        leaf(w * 0.42f, h * 0.56f, -10f, 0.8f)
        leaf(w * 0.45f, h * 0.48f, 20f, 0.75f)
        leaf(w * 0.52f, h * 0.39f, 5f, 0.7f)
        leaf(w * 0.34f, h * 0.60f, -20f, 0.65f)
    }
}

@Composable
fun ShimmeringCross(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "cross_breathe")
    val breatheAlpha by transition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    Image(
        painter = painterResource(id = R.drawable.orthodox_cross_custom),
        contentDescription = "Православный крест",
        contentScale = ContentScale.Fit,
        modifier = modifier.graphicsLayer { alpha = breatheAlpha }
    )
}

@Composable
fun ButtonsPanel(
    modifier: Modifier = Modifier,
    customAmount: String,
    onAmountChange: (String) -> Unit,
    onPayment: (BigDecimal) -> Unit
) {
    Column(modifier = modifier.padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DonationActionCard(
            title = "СВОЯ СУММА",
            description = "Введите любую сумму",
            actionLabel = "ВНЕСТИ\nЛЕПТУ",
            clickWholeCard = false,
            onClick = {
                val parsed = customAmount.toBigDecimalOrNull()
                if (parsed != null && parsed > BigDecimal.ZERO) onPayment(parsed)
            },
            content = { AmountInput(value = customAmount, onValueChange = onAmountChange) }
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
                    style = TextStyle(fontFamily = AlegreyaFontFamily, fontSize = 30.sp, color = TextMain, fontWeight = FontWeight.SemiBold)
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
                    text = "1000 ₽",
                    style = TextStyle(fontFamily = AlegreyaFontFamily, fontSize = 30.sp, color = TextMain, fontWeight = FontWeight.SemiBold)
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

    val cardClickModifier = if (clickWholeCard) {
        Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
    } else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .scale(scale)
            .clip(RoundedCornerShape(11.dp))
            .background(Color.White)
            .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(11.dp))
            .then(cardClickModifier)
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = AlegreyaFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = GoldDark
                    )
                )
                Text(
                    text = description,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontFamily = AlegreyaFontFamily, fontSize = 11.sp, color = MutedWarm)
                )
            }
            content()
        }

        val actionInteraction = remember { MutableInteractionSource() }
        val actionPressed by actionInteraction.collectIsPressedAsState()
        val actionScale by animateFloatAsState(if (actionPressed) 0.985f else 1f, label = "action_scale")

        Box(
            modifier = Modifier
                .padding(2.dp)
                .width(110.dp)
                .fillMaxHeight()
                .scale(actionScale)
                .clip(RoundedCornerShape(10.dp))
                .background(ActionBrush)
                .clickable(interactionSource = actionInteraction, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = actionLabel,
                textAlign = TextAlign.Center,
                maxLines = 2,
                style = TextStyle(
                    fontFamily = AlegreyaFontFamily,
                    fontSize = if (actionLabel.contains("\n")) 12.sp else 11.sp,
                    lineHeight = 14.sp,
                    letterSpacing = 0.1.sp,
                    color = Color.White
                )
            )
        }
    }
}

@Composable
fun AmountInput(value: String, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp)
            .clip(RoundedCornerShape(7.dp))
            .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(7.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("₽", style = TextStyle(fontFamily = AlegreyaFontFamily, fontSize = 18.sp, color = GoldDark))
            Spacer(modifier = Modifier.width(6.dp))
            BasicTextField(
                value = value,
                onValueChange = {
                    if (it.all(Char::isDigit) && it.length <= 8) onValueChange(it)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    fontFamily = AlegreyaFontFamily,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text("0", style = TextStyle(fontFamily = AlegreyaFontFamily, fontSize = 24.sp, color = MutedWarm.copy(alpha = 0.45f)))
                    }
                    inner()
                }
            )
        }
    }
}

@Composable
fun AppFooter() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderGold))
        Row(
            modifier = Modifier.fillMaxWidth().background(FooterBar).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("☦", style = TextStyle(color = Color.White, fontFamily = AlegreyaFontFamily, fontSize = 14.sp))
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                "МОЛИТВА",
                style = TextStyle(color = Color.White, fontFamily = AlegreyaFontFamily, fontSize = 12.sp, letterSpacing = 0.8.sp)
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

    val dialogUi = when (result) {
        is PaymentResult.Success -> DialogUi("☩", "ОПЛАТА ПРИНЯТА", "Код ответа: ${result.rc}", "А М И Н Ь")
        is PaymentResult.Declined -> DialogUi("✕", "ОТКЛОНЕНО", "RC: ${result.rc} • Код: ${result.code}", "ЗАКРЫТЬ")
        is PaymentResult.Error -> DialogUi("!", "ОШИБКА", result.reason, "ЗАКРЫТЬ")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.26f))
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() },
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
            Text(dialogUi.icon, style = TextStyle(fontFamily = AlegreyaFontFamily, fontSize = 28.sp, color = GoldDark))
            Spacer(modifier = Modifier.height(6.dp))
            Text(dialogUi.title, style = TextStyle(fontFamily = AlegreyaFontFamily, fontSize = 16.sp, color = GoldDark, fontWeight = FontWeight.SemiBold))
            Spacer(modifier = Modifier.height(6.dp))
            val message = when (result) {
                is PaymentResult.Success -> result.message
                is PaymentResult.Declined -> result.message
                is PaymentResult.Error -> result.reason
            }
            Text(
                text = message,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
                style = TextStyle(fontFamily = AlegreyaFontFamily, fontSize = 14.sp, color = TextMain)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dialogUi.subtitle,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
                style = TextStyle(fontFamily = AlegreyaFontFamily, fontSize = 11.sp, color = MutedWarm)
            )
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
                Text(dialogUi.buttonLabel, style = TextStyle(color = Color.White, fontFamily = AlegreyaFontFamily, fontSize = 13.sp, letterSpacing = 1.sp))
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private data class DialogUi(
    val icon: String,
    val title: String,
    val subtitle: String,
    val buttonLabel: String
)
