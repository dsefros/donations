package com.orthodox.charity

import android.app.Activity
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.charity.audio.AppAudioController
import com.orthodox.charity.payment.AppPaymentResult
import com.orthodox.charity.payment.CardPresentingActivity
import com.orthodox.charity.settings.AppSettingsStorage
import com.orthodox.charity.settings.DonationSettings
import java.math.BigDecimal
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import com.orthodox.charity.payment.PaymentGateway
import com.orthodox.charity.payment.SkyTechPaymentGateway

private val BgMain = Color(0xFFF5F3F1)
private val GoldDark = Color(0xFF8A6A30)
private val BorderGold = Color(0xFFD0B98C)
private val TextMain = Color(0xFF3D3326)
private val MutedWarm = Color(0xFF8D7C66)

private val ButtonBorderBrush = Brush.linearGradient(
    colorStops = arrayOf(
        0.0f to Color(0xFFEAD489),
        0.5f to Color(0xFF7A3800),
        1.0f to Color(0xFF523706)
    ),
    start = Offset.Zero,
    end = Offset.Infinite
)

private val ButtonTextBrush = Brush.verticalGradient(
    colorStops = arrayOf(
        0.0f to Color(0xFFFFFFFF),
        1.0f to Color(0xFFDECF7C)
    )
)
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

class MainActivity : ComponentActivity() {
    private val paymentGateway: PaymentGateway = SkyTechPaymentGateway()
    private val audioController = AppAudioController()
    private val paymentResult = mutableStateOf<AppPaymentResult?>(null)
    private lateinit var settingsStorage: AppSettingsStorage
    private val donationSettings = mutableStateOf(DonationSettings())
    private val showSettingsPinDialog = mutableStateOf(false)
    private val showSettingsDialog = mutableStateOf(false)
    private val showSettingsQuickPanel = mutableStateOf(false)
    private val customAmount = mutableStateOf("2000")
    private val paymentInProgress = mutableStateOf(false)

    private val posLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        handlePaymentResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsStorage = AppSettingsStorage(this)
        donationSettings.value = settingsStorage.load()
        customAmount.value = donationSettings.value.customDefaultAmount

        audioController.prepareMainLoop(this)
        audioController.updateSettings(
            mainLoopEnabled = donationSettings.value.mainLoopSoundEnabled,
            paymentResultEnabled = donationSettings.value.paymentResultSoundEnabled,
            mainLoopVolume = donationSettings.value.mainLoopVolume,
            paymentResultVolume = donationSettings.value.paymentResultVolume
        )

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        enableImmersiveMode()

        setContent {
            OrthodoxCharityApp(
                paymentResult = paymentResult.value,
                onClearResult = { paymentResult.value = null },
                onPayment = { amount ->
                    if (paymentInProgress.value) return@OrthodoxCharityApp
                    paymentInProgress.value = true
                    audioController.pauseMainLoop()
                    posLauncher.launch(CardPresentingActivity.createIntent(this@MainActivity, amount))
                },
                customAmountValue = customAmount.value,
                onCustomAmountChange = { customAmount.value = it },
                paymentInProgress = paymentInProgress.value,
                settings = donationSettings.value,
                showSettingsPinDialog = showSettingsPinDialog.value,
                showSettingsDialog = showSettingsDialog.value,
                showSettingsQuickPanel = showSettingsQuickPanel.value,
                onHiddenSettingsTripleTap = {
                    if (!paymentInProgress.value) showSettingsQuickPanel.value = true
                },
                onDismissSettingsQuickPanel = { showSettingsQuickPanel.value = false },
                onOpenSettingsFromQuickPanel = {
                    showSettingsQuickPanel.value = false
                    showSettingsPinDialog.value = true
                },
                onDismissPinDialog = { showSettingsPinDialog.value = false },
                onPinSuccess = {
                    showSettingsPinDialog.value = false
                    showSettingsDialog.value = true
                },
                onDismissSettings = { showSettingsDialog.value = false },
                onSaveSettings = { newSettings ->
                    donationSettings.value = newSettings
                    settingsStorage.save(newSettings)
                    customAmount.value = newSettings.customDefaultAmount
                    audioController.updateSettings(
                        mainLoopEnabled = newSettings.mainLoopSoundEnabled,
                        paymentResultEnabled = newSettings.paymentResultSoundEnabled,
                        mainLoopVolume = newSettings.mainLoopVolume,
                        paymentResultVolume = newSettings.paymentResultVolume
                    )
                    audioController.startMainLoop()
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()

        audioController.startMainLoop()
    }

    override fun onPause() {
        audioController.pauseMainLoop()
        audioController.pausePaymentResult()
        super.onPause()
    }

    override fun onDestroy() {
        audioController.release()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            enableImmersiveMode()
        }
    }

    @Deprecated("Disabled")
    override fun onBackPressed() = Unit

    private fun enableImmersiveMode() {
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

    private fun handlePaymentResult(result: ActivityResult) {
        paymentInProgress.value = false
        if (result.resultCode != Activity.RESULT_OK || result.data == null) {
            paymentResult.value = AppPaymentResult.Error
            customAmount.value = donationSettings.value.customDefaultAmount
            audioController.playPaymentResult(this, restartMainLoopAfterCompletion = true)
            return
        }
        paymentResult.value = paymentGateway.mapTransactionResult(result.data)
        customAmount.value = donationSettings.value.customDefaultAmount
        audioController.playPaymentResult(this, restartMainLoopAfterCompletion = true)
    }
}

@Composable
fun OrthodoxCharityApp(
    paymentResult: AppPaymentResult?,
    onClearResult: () -> Unit,
    onPayment: (BigDecimal) -> Unit,
    customAmountValue: String,
    onCustomAmountChange: (String) -> Unit,
    paymentInProgress: Boolean,
    settings: DonationSettings,
    showSettingsPinDialog: Boolean,
    showSettingsDialog: Boolean,
    showSettingsQuickPanel: Boolean,
    onHiddenSettingsTripleTap: () -> Unit,
    onDismissSettingsQuickPanel: () -> Unit,
    onOpenSettingsFromQuickPanel: () -> Unit,
    onDismissPinDialog: () -> Unit,
    onPinSuccess: () -> Unit,
    onDismissSettings: () -> Unit,
    onSaveSettings: (DonationSettings) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgMain)
    ) {
        Image(
            painter = painterResource(id = R.drawable.my_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppHeader()
            DonationCarouselPanel(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                settings = settings,
                customAmount = customAmountValue,
                onAmountChange = onCustomAmountChange,
                paymentInProgress = paymentInProgress,
                onPayment = onPayment
            )
            PsalmFooter()
            Spacer(modifier = Modifier.height(14.dp))
        }

        HiddenSettingsHotspot(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(96.dp),
            enabled = !paymentInProgress,
            onTripleTap = onHiddenSettingsTripleTap
        )

        if (showSettingsQuickPanel) {
            SettingsQuickPanel(
                onOpenSettings = onOpenSettingsFromQuickPanel,
                onDismiss = onDismissSettingsQuickPanel
            )
        }
        if (showSettingsPinDialog) {
            SettingsPinDialog(
                onSuccess = onPinSuccess,
                onDismiss = onDismissPinDialog
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(
                settings = settings,
                onSave = onSaveSettings,
                onDismiss = onDismissSettings
            )
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
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ПРАВОСЛАВНАЯ БЛАГОТВОРИТЕЛЬНОСТЬ",
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = CormorantFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = GoldDark
            )
        )
        Spacer(modifier = Modifier.height(10.dp))

        AppOrnamentDivider()
    }
}

private enum class DonationCardType { Custom, Temple, Orphanage }
private data class DonationCarouselItem(
    val title: String,
    val description: String,
    val amountText: String?,
    val actionLabel: String,
    val type: DonationCardType
)

@Composable
private fun PsalmFooter() {
    Text(
        text = "«Блажен, кто думает о бедном и нищем» - Псалом 40:1",
        textAlign = TextAlign.Center,
        style = TextStyle(
            fontFamily = AlegreyaFontFamily,
            fontStyle = FontStyle.Italic,
            color = Color(0xFF8F6630),
            fontSize = 16.sp
        )
    )
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
            .width(416.dp)
            .height(53.dp)
    )
}

@Composable
fun CardOrnamentDivider(
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.card_divider),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .width(368.dp)
            .height(21.dp)
            .graphicsLayer {
                alpha = 0.92f
            }
    )
}


@Composable
private fun HiddenSettingsHotspot(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onTripleTap: () -> Unit
) {
    Box(
        modifier = modifier.pointerInput(enabled) {
            var tapCount = 0
            var lastTapTs = 0L
            detectTapGestures(onTap = {
                if (!enabled) return@detectTapGestures
                val now = SystemClock.elapsedRealtime()
                tapCount = if (now - lastTapTs > SETTINGS_TRIPLE_TAP_TIMEOUT_MS) 1 else tapCount + 1
                lastTapTs = now
                if (tapCount >= 3) {
                    tapCount = 0
                    onTripleTap()
                }
            })
        }
    )
}

@Composable
private fun SettingsQuickPanel(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.18f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.BottomStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp, bottom = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.96f))
                .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(12.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {}
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        BorderStroke(1.dp, BorderGold.copy(alpha = 0.85f)),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "НАСТРОЙКИ",
                    style = TextStyle(
                        fontFamily = AlegreyaFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = GoldDark,
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DonationCarouselPanel(
    modifier: Modifier = Modifier,
    settings: DonationSettings,
    customAmount: String,
    onAmountChange: (String) -> Unit,
    paymentInProgress: Boolean,
    onPayment: (BigDecimal) -> Unit
) {
    val pages = remember(settings) {
        listOf(
            DonationCarouselItem(
                title = "СВОЯ СУММА",
                description = "Введите любую сумму",
                amountText = null,
                actionLabel = "ВНЕСТИ ЛЕПТУ",
                type = DonationCardType.Custom
            ),
            DonationCarouselItem(
                title = "ПОМОЩЬ ХРАМУ",
                description = "Нужды церкви",
                amountText = formatAmountGroups(settings.templeAmount) + " ₽",
                actionLabel = "ПОЖЕРТВОВАТЬ",
                type = DonationCardType.Temple
            ),
            DonationCarouselItem(
                title = "ДЕТСКИЙ ПРИЮТ",
                description = "Забота о сиротах и детях",
                amountText = formatAmountGroups(settings.orphanageAmount) + " ₽",
                actionLabel = "ПОЖЕРТВОВАТЬ",
                type = DonationCardType.Orphanage
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })

    LaunchedEffect(pagerState, paymentInProgress, pages.size) {
        while (true) {
            delay(30_000)

            if (!paymentInProgress && pages.isNotEmpty()) {
                val nextPage = (pagerState.currentPage + 1) % pages.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(277.dp)
        ) { page ->
            val item = pages[page]

            DonationCarouselCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(277.dp),
                item = item,
                customAmount = customAmount,
                onAmountChange = onAmountChange,
                paymentInProgress = paymentInProgress,
                onClick = {
                    if (paymentInProgress) return@DonationCarouselCard

                    when (item.type) {
                        DonationCardType.Custom -> {
                            parseDonationAmount(customAmount)?.let(onPayment)
                        }

                        DonationCardType.Temple -> {
                            parseDonationAmount(settings.templeAmount)?.let(onPayment)
                        }

                        DonationCardType.Orphanage -> {
                            parseDonationAmount(settings.orphanageAmount)?.let(onPayment)
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        CarouselPageIndicator(
            pageCount = pages.size,
            currentPage = pagerState.currentPage
        )
    }
}



@Composable
private fun DonationCarouselCard(
    modifier: Modifier = Modifier,
    item: DonationCarouselItem,
    customAmount: String,
    onAmountChange: (String) -> Unit,
    paymentInProgress: Boolean,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(20.dp)
    val buttonShape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .shadow(
                elevation = 10.dp,
                shape = cardShape,
                clip = false
            )
            .clip(cardShape)
            .background(Color.White)
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    color = BorderGold.copy(alpha = 0.75f)
                ),
                shape = cardShape
            )
            .padding(start = 8.dp, top = 16.dp, end = 8.dp, bottom = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = item.title,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontFamily = CormorantFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = GoldDark
                )
            )

            Text(
                text = item.description,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontFamily = AlegreyaFontFamily,
                    fontSize = 20.sp,
                    color = Color.Black.copy(alpha = 0.60f)
                )
            )

            Spacer(modifier = Modifier.height(5.dp))

            CardOrnamentDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(21.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (item.type == DonationCardType.Custom) {
                    AmountInput(
                        value = customAmount,
                        onValueChange = onAmountChange
                    )
                } else {
                    Text(
                        text = item.amountText.orEmpty(),
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            fontFamily = TimesNewRomanFontFamily,
                            fontSize = 40.sp,
                            color = TextMain,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = buttonShape,
                    clip = false
                )
                .clip(buttonShape)
                .clickable(
                    enabled = !paymentInProgress,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.button_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = if (paymentInProgress) 0.65f else 1f
                    }
            )

            Text(
                text = item.actionLabel,
                textAlign = TextAlign.Center,
                maxLines = 1,
                style = TextStyle(
                    fontFamily = CormorantFontFamily,
                    fontWeight = FontWeight.Bold,
                    brush = ButtonTextBrush,
                    fontSize = 20.sp,
                    lineHeight = 20.sp,
                    letterSpacing = 0.8.sp,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.50f),
                        offset = Offset(x = 0f, y = 1f),
                        blurRadius = 34f
                    )
                )
            )
        }
    }
}

@Composable
private fun CarouselPageIndicator(
    pageCount: Int,
    currentPage: Int
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (index == currentPage) GoldDark else BorderGold.copy(alpha = 0.45f))
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
        fontFamily = TimesNewRomanFontFamily,
        fontSize = 40.sp,
        fontWeight = FontWeight.Normal,
        color = TextMain,
        textAlign = TextAlign.Center
    )

    val visibleText = formatAmountGroups(value).ifEmpty { "0" }

    val inputWidth = (visibleText.length * 24).dp.coerceIn(
        minimumValue = 28.dp,
        maximumValue = 170.dp
    )

    Box(
        modifier = Modifier
            .width(368.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                BorderStroke(1.dp, BorderGold.copy(alpha = 0.75f)),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            BasicTextField(
                value = value,
                onValueChange = { input ->
                    onValueChange(normalizeAmountInput(input))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = AmountThousandsVisualTransformation(),
                textStyle = amountTextStyle.copy(textAlign = TextAlign.Center),
                modifier = Modifier.width(inputWidth),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = "0",
                                style = amountTextStyle.copy(
                                    color = MutedWarm.copy(alpha = 0.45f),
                                    textAlign = TextAlign.Center
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

private fun formatAmountGroups(rawDigits: String): String {
    if (rawDigits.isEmpty()) return ""

    return buildString {
        rawDigits.forEachIndexed { index, char ->
            append(char)
            val remaining = rawDigits.length - index - 1
            if (remaining > 0 && remaining % 3 == 0) {
                append(' ')
            }
        }
    }
}

private fun normalizeAmountInput(input: String): String {
    val digitsOnly = input.filter { it.isDigit() }
    val normalized = digitsOnly.trimStart('0').ifEmpty { if (digitsOnly.isEmpty()) "" else "0" }
    return normalized.take(8)
}

private fun parseDonationAmount(input: String): BigDecimal? {
    val normalized = normalizeAmountInput(input)
    val parsed = normalized.toBigDecimalOrNull() ?: return null
    return parsed.takeIf { it > BigDecimal.ZERO }
}

private class AmountThousandsVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val formatted = formatAmountGroups(raw)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, raw.length)

                if (safeOffset == 0) return 0

                var digitsSeen = 0

                formatted.forEachIndexed { index, char ->
                    if (char.isDigit()) {
                        digitsSeen++

                        if (digitsSeen == safeOffset) {
                            return index + 1
                        }
                    }
                }

                return formatted.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safeOffset = offset.coerceIn(0, formatted.length)
                return formatted
                    .take(safeOffset)
                    .count { it.isDigit() }
                    .coerceIn(0, raw.length)
            }
        }

        return TransformedText(
            text = AnnotatedString(formatted),
            offsetMapping = offsetMapping
        )
    }
}

@Composable
fun PaymentResultDialog(
    result: AppPaymentResult,
    onDismiss: () -> Unit
) {
    LaunchedEffect(result) {
        delay(10_000)
        onDismiss()
    }

    val dialogUi = when (result) {
        AppPaymentResult.Success -> DialogUi(
            icon = "☩",
            title = "ПОЖЕРТВОВАНИЕ ПРИНЯТО",
            message = "Спасибо за ваш вклад",
            buttonLabel = "ЗАКРЫТЬ"
        )

        AppPaymentResult.Declined -> DialogUi(
            icon = "✕",
            title = "ПОЖЕРТВОВАНИЕ НЕ ПРИНЯТО",
            message = "Попробуйте ещё раз",
            buttonLabel = "ЗАКРЫТЬ"
        )

        AppPaymentResult.Error -> DialogUi(
            icon = "!",
            title = "ОШИБКА",
            message = "Попробуйте ещё раз",
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
                .widthIn(max = 380.dp)
                .height(275.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(12.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            AppOrnamentDivider(
                modifier = Modifier
                    .width(296.dp)
                    .height(38.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = dialogUi.title,
                style = TextStyle(
                    fontFamily = CormorantFontFamily,
                    fontSize = 20.sp,
                    color = GoldDark,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            DialogStatusIcon(result = result)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = dialogUi.message,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
                style = TextStyle(
                    fontFamily = AlegreyaFontFamily,
                    fontSize = 16.sp,
                    color = TextMain
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.dialog_close_button_bg),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )

                Text(
                    text = dialogUi.buttonLabel,
                    style = TextStyle(
                        brush = ButtonTextBrush,
                        fontFamily = CormorantFontFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold

                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DialogStatusIcon(
    result: AppPaymentResult
) {
    val iconRes = when (result) {
        AppPaymentResult.Success -> R.drawable.dialog_checked
        AppPaymentResult.Declined -> R.drawable.dialog_cancel
        AppPaymentResult.Error -> R.drawable.dialog_cancel
    }

    Image(
        painter = painterResource(id = iconRes),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(64.dp)
    )
}

private data class DialogUi(
    val icon: String,
    val title: String,
    val message: String,
    val buttonLabel: String
)


private const val SETTINGS_PIN = "1234"
private const val SETTINGS_TRIPLE_TAP_TIMEOUT_MS = 1000L

@Composable
fun SettingsPinDialog(onSuccess: () -> Unit, onDismiss: () -> Unit) {
    val pin = remember { mutableStateOf("") }
    val error = remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(300.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(12.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {}
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("ВВЕДИТЕ PIN", style = TextStyle(fontFamily = CormorantFontFamily, fontSize = 20.sp, color = GoldDark))
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, BorderGold.copy(alpha = 0.75f)), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                BasicTextField(value = pin.value, onValueChange = {
                    val v = it.filter(Char::isDigit).take(4)
                    pin.value = v
                    error.value = null
                    if (v.length == 4) {
                        if (v == SETTINGS_PIN) onSuccess() else { error.value = "Неверный пароль"; pin.value = "" }
                    }
                },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = TextStyle(fontFamily = AlegreyaFontFamily, fontSize = 24.sp, textAlign = TextAlign.Center, color = TextMain),
                    modifier = Modifier.fillMaxWidth())
            }
            if (error.value != null) Text(error.value!!, color = Color.Red, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(8.dp))
                    .clickable { onDismiss() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("ЗАКРЫТЬ", style = TextStyle(fontFamily = AlegreyaFontFamily, color = GoldDark, fontWeight = FontWeight.SemiBold))
            }
        }
    }
}

@Composable
fun SettingsDialog(settings: DonationSettings, onSave: (DonationSettings) -> Unit, onDismiss: () -> Unit) {
    val custom = remember { mutableStateOf(settings.customDefaultAmount) }
    val temple = remember { mutableStateOf(settings.templeAmount) }
    val orphan = remember { mutableStateOf(settings.orphanageAmount) }
    val mainEnabled = remember { mutableStateOf(settings.mainLoopSoundEnabled) }
    val payEnabled = remember { mutableStateOf(settings.paymentResultSoundEnabled) }
    val mainVol = remember { mutableStateOf(settings.mainLoopVolume) }
    val payVol = remember { mutableStateOf(settings.paymentResultVolume) }
    val error = remember { mutableStateOf<String?>(null) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(12.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {}
                .padding(14.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("НАСТРОЙКИ", style = TextStyle(fontFamily = CormorantFontFamily, fontSize = 20.sp, color = GoldDark), modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(10.dp))
            SettingsAmountField("Своя сумма по умолчанию", custom.value) { custom.value = normalizeAmountInput(it) }
            SettingsAmountField("Помощь храму", temple.value) { temple.value = normalizeAmountInput(it) }
            SettingsAmountField("Детский приют", orphan.value) { orphan.value = normalizeAmountInput(it) }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Фоновый звук", modifier = Modifier.weight(1f)); Switch(checked = mainEnabled.value, onCheckedChange = { mainEnabled.value = it }) }
            Text("Громкость фонового звука: ${(mainVol.value * 100).toInt()}%")
            Slider(value = mainVol.value, onValueChange = { mainVol.value = it }, valueRange = 0f..1f)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Звук результата оплаты", modifier = Modifier.weight(1f)); Switch(checked = payEnabled.value, onCheckedChange = { payEnabled.value = it }) }
            Text("Громкость результата оплаты: ${(payVol.value * 100).toInt()}%")
            Slider(value = payVol.value, onValueChange = { payVol.value = it }, valueRange = 0f..1f)
            Spacer(modifier = Modifier.height(8.dp))
            error.value?.let { Text(it, color = Color.Red, fontSize = 13.sp) }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(8.dp))
                        .clickable {
                    val c = normalizeAmountInput(custom.value)
                    val t = normalizeAmountInput(temple.value)
                    val o = normalizeAmountInput(orphan.value)
                    if (parseDonationAmount(c) == null || parseDonationAmount(t) == null || parseDonationAmount(o) == null) {
                        error.value = "Суммы должны быть больше 0"
                    } else {
                        onSave(DonationSettings(c, t, o, mainEnabled.value, payEnabled.value, mainVol.value, payVol.value)); onDismiss()
                    }
                }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("СОХРАНИТЬ", style = TextStyle(fontFamily = AlegreyaFontFamily, color = GoldDark, fontWeight = FontWeight.SemiBold))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(8.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("ЗАКРЫТЬ", style = TextStyle(fontFamily = AlegreyaFontFamily, color = GoldDark, fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

@Composable
private fun SettingsAmountField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Text(label, style = TextStyle(fontFamily = AlegreyaFontFamily, color = TextMain, fontSize = 14.sp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp)
            .clip(RoundedCornerShape(7.dp))
            .border(BorderStroke(1.dp, BorderGold.copy(alpha = 0.75f)), RoundedCornerShape(7.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = TextStyle(fontFamily = AlegreyaFontFamily, color = TextMain, fontSize = 16.sp)
        )
    }
}
