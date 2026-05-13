package com.orthodox.charity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skytech.smartskyposlib.TransactionParams
import com.skytech.smartskyposlib.TransactionResult
import com.skytech.smartskyposlib.ui.PaymentActivity
import com.skytech.smartskyposlib.ui.SkyPaymentActivityV2
import java.math.BigDecimal
import kotlinx.coroutines.delay

private val BgMain = Color(0xFFF5F3F1)
private val GoldDark = Color(0xFF8A6A30)
private val BorderGold = Color(0xFFD0B98C)
private val TextMain = Color(0xFF3D3326)
private val MutedWarm = Color(0xFF8D7C66)

private val ActionBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFF8E6B2F),
        Color(0xFFB99047),
        Color(0xFFD3B06A),
        Color(0xFFA17835)
    )
)

private val CardSurface = Color(0xFFFBF7EE)
private val InputSurface = Color(0xFFFFFDF7)

private val AlegreyaFontFamily = FontFamily(
    Font(R.font.alegreya_regular, FontWeight.Normal),
    Font(R.font.alegreya_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.alegreya_medium, FontWeight.Medium),
    Font(R.font.alegreya_semibold, FontWeight.SemiBold),
    Font(R.font.alegreya_bold, FontWeight.Bold)
)

fun buildPaymentIntent(context: Context, amount: BigDecimal): Intent =
    Intent(context, SkyPaymentActivityV2::class.java).apply {
        putExtra(PaymentActivity.PARAMS_KEY, TransactionParams(amount))
        putExtra(PaymentActivity.TYPE_KEY, PaymentActivity.TYPE_PAYMENT)
    }

sealed class PaymentResult {
    object Success : PaymentResult()
    object Declined : PaymentResult()
    object Error : PaymentResult()
}

class MainActivity : ComponentActivity() {

    private val paymentResult = mutableStateOf<PaymentResult?>(null)
    private val customAmount = mutableStateOf("2000")

    private val posLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        handlePaymentResult(result)
    }

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
                onPayment = { amount ->
                    posLauncher.launch(buildPaymentIntent(this@MainActivity, amount))
                },
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
    override fun onBackPressed() = Unit

    private fun readTransactionResult(data: Intent?): TransactionResult? {
        if (data == null) return null

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            data.getParcelableExtra(
                PaymentActivity.RESULT_KEY,
                TransactionResult::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            data.getParcelableExtra(PaymentActivity.RESULT_KEY)
        }
    }

    private fun handlePaymentResult(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            paymentResult.value = PaymentResult.Error
            return
        }

        val tx = readTransactionResult(result.data)

        if (tx == null) {
            paymentResult.value = PaymentResult.Error
            return
        }

        val code = tx.code
        val rc = tx.rc ?: ""

        val approved = code == 0 && rc == "00"

        paymentResult.value = if (approved) {
            PaymentResult.Success
        } else {
            PaymentResult.Declined
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        Image(
            painter = painterResource(id = R.drawable.cross_background),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-118).dp, y = 22.dp)
                .width(352.dp)
                .height(402.dp)
                .graphicsLayer {
                    alpha = 0.38f
                }
        )

        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader()

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CrossPanel(
                    modifier = Modifier
                        .width(130.dp)
                        .fillMaxHeight()
                )

                ButtonsPanel(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    customAmount = customAmountValue,
                    onAmountChange = onCustomAmountChange,
                    onPayment = onPayment
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                AppOrnamentDivider()
            }
        }

        paymentResult?.let {
            PaymentResultDialog(
                result = it,
                onDismiss = onClearResult
            )
        }
    }
}

@Composable
fun AppHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ПРАВОСЛАВНАЯ БЛАГОТВОРИТЕЛЬНОСТЬ",
            style = TextStyle(
                fontFamily = AlegreyaFontFamily,
                fontSize = 16.sp,
                letterSpacing = 2.1.sp,
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
                letterSpacing = 0.8.sp,
                color = MutedWarm
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        AppOrnamentDivider()
    }
}

@Composable
fun AppOrnamentDivider(
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.app_header_divider),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .width(170.dp)
            .height(16.dp)
    )
}

@Composable
fun CrossPanel(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            ShimmeringCross(
                modifier = Modifier.size(width = 102.dp, height = 168.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "«Блажен, кто думает\nо бедном и нищем»",
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontFamily = AlegreyaFontFamily,
                    fontStyle = FontStyle.Italic,
                    color = GoldDark,
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "— Псалом 40:1",
                style = TextStyle(
                    fontFamily = AlegreyaFontFamily,
                    fontSize = 10.5.sp,
                    color = MutedWarm
                )
            )

            Spacer(modifier = Modifier.weight(1f))
        }
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
        modifier = modifier.graphicsLayer {
            alpha = breatheAlpha
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
        modifier = modifier.padding(top = 2.dp)
    ) {
        DonationActionCard(
            title = "СВОЯ СУММА",
            description = "Введите любую сумму",
            actionLabel = "ВНЕСТИ ЛЕПТУ",
            clickWholeCard = false,
            onClick = {
                val parsed = customAmount.toBigDecimalOrNull()
                if (parsed != null && parsed > BigDecimal.ZERO) {
                    onPayment(parsed)
                }
            },
            content = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AmountInput(
                        value = customAmount,
                        onValueChange = onAmountChange
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        DonationActionCard(
            title = "ПОМОЩЬ ХРАМУ",
            description = "Восстановление и нужды церкви",
            actionLabel = "ПОЖЕРТВОВАТЬ",
            clickWholeCard = true,
            onClick = { onPayment(BigDecimal("500.00")) },
            content = {
                AmountWithDivider(amountText = "500 ₽")
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        DonationActionCard(
            title = "ДЕТСКИЙ ПРИЮТ",
            description = "Забота о сиротах и детях",
            actionLabel = "ПОЖЕРТВОВАТЬ",
            clickWholeCard = true,
            onClick = { onPayment(BigDecimal("1000.00")) },
            content = {
                AmountWithDivider(amountText = "1000 ₽")
            }
        )
    }
}

@Composable
private fun AmountWithDivider(amountText: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(BorderGold.copy(alpha = 0.55f))
            )
            Text(
                text = "✦",
                modifier = Modifier.padding(horizontal = 6.dp),
                style = TextStyle(
                    fontFamily = AlegreyaFontFamily,
                    fontSize = 10.sp,
                    color = BorderGold
                )
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(BorderGold.copy(alpha = 0.55f))
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = amountText,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = AlegreyaFontFamily,
                fontSize = 30.sp,
                color = TextMain,
                fontWeight = FontWeight.SemiBold
            )
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
        Modifier.clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .scale(scale)
            .clip(RoundedCornerShape(11.dp))
            .graphicsLayer {
                shadowElevation = 8.dp.toPx()
                shape = RoundedCornerShape(12.dp)
                clip = true
            }
            .background(CardSurface)
            .border(BorderStroke(1.dp, BorderGold.copy(alpha = 0.85f)), RoundedCornerShape(12.dp))
            .then(cardClickModifier)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 10.dp, top = 8.dp, end = 10.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    style = TextStyle(
                        fontFamily = AlegreyaFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = GoldDark
                    )
                )

                Text(
                    text = description,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        fontFamily = AlegreyaFontFamily,
                        fontSize = 12.sp,
                        color = MutedWarm
                    )
                )
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                content()
            }
        }

        val actionInteraction = remember { MutableInteractionSource() }
        val actionPressed by actionInteraction.collectIsPressedAsState()

        val actionScale by animateFloatAsState(
            targetValue = if (actionPressed) 0.985f else 1f,
            label = "action_scale"
        )

        Box(
            modifier = Modifier
                .width(120.dp)
                .fillMaxHeight()
                .scale(actionScale)
                 .clip(RoundedCornerShape(10.dp))
                .background(ActionBrush)
                .border(BorderStroke(1.dp, Color(0xFFF1DEB5).copy(alpha = 0.65f)), RoundedCornerShape(10.dp))
                .clickable(
                    interactionSource = actionInteraction,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✦\n$actionLabel\n✦",
                textAlign = TextAlign.Center,
                maxLines = 4,
                style = TextStyle(
                    fontFamily = AlegreyaFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 15.sp,
                    letterSpacing = 0.1.sp,
                    color = Color.White
                )
            )
        }
    }
}

@Composable
fun AmountInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    val amountTextStyle = TextStyle(
        fontFamily = AlegreyaFontFamily,
        fontSize = 30.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextMain,
        textAlign = TextAlign.Center
    )

    Box(
        modifier = Modifier
             .width(180.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                BorderStroke(1.dp, BorderGold.copy(alpha = 0.9f)),
                RoundedCornerShape(8.dp)
            )
            .background(InputSurface)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            BasicTextField(
                value = value,
                onValueChange = { input ->
                    if (input.all(Char::isDigit) && input.length <= 8) {
                        onValueChange(input)
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = amountTextStyle,
                modifier = Modifier.width(76.dp),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = "0",
                                style = amountTextStyle.copy(
                                    color = MutedWarm.copy(alpha = 0.45f)
                                )
                            )
                        }

                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "₽",
                style = amountTextStyle
            )
        }
    }
}

@Composable
fun PaymentResultDialog(
    result: PaymentResult,
    onDismiss: () -> Unit
) {
    LaunchedEffect(result) {
        delay(10_000)
        onDismiss()
    }

    val dialogUi = when (result) {
        is PaymentResult.Success -> DialogUi(
            icon = "☩",
            title = "ОПЛАТА ПРИНЯТА",
            message = "Спасибо за ваше пожертвование",
            buttonLabel = "А М И Н Ь"
        )

        is PaymentResult.Declined -> DialogUi(
            icon = "✕",
            title = "ОПЛАТА НЕ ПРОШЛА",
            message = "Пожертвование не было списано. Попробуйте ещё раз",
            buttonLabel = "ЗАКРЫТЬ"
        )

        is PaymentResult.Error -> DialogUi(
            icon = "!",
            title = "ОШИБКА",
            message = "Не удалось выполнить оплату. Попробуйте ещё раз",
            buttonLabel = "ЗАКРЫТЬ"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.26f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(12.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = dialogUi.icon,
                style = TextStyle(
                    fontFamily = AlegreyaFontFamily,
                    fontSize = 28.sp,
                    color = GoldDark
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = dialogUi.title,
                style = TextStyle(
                    fontFamily = AlegreyaFontFamily,
                    fontSize = 16.sp,
                    color = GoldDark,
                    fontWeight = FontWeight.SemiBold
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = dialogUi.message,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
                style = TextStyle(
                    fontFamily = AlegreyaFontFamily,
                    fontSize = 14.sp,
                    color = TextMain
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

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
                Text(
                    text = dialogUi.buttonLabel,
                    style = TextStyle(
                        color = Color.White,
                        fontFamily = AlegreyaFontFamily,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private data class DialogUi(
    val icon: String,
    val title: String,
    val message: String,
    val buttonLabel: String
)
