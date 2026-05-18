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
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orthodox.charity.audio.AppAudioController
import com.orthodox.charity.payment.AppPaymentResult
import com.orthodox.charity.payment.PaymentGateway
import com.orthodox.charity.payment.SkyTechPaymentGateway
import com.orthodox.charity.settings.AppSettingsStorage
import com.orthodox.charity.settings.DonationSettings
import java.math.BigDecimal
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.delay

private val BgMain = Color(0xFFF5F3F1)
private val GoldDark = Color(0xFF8A6A30)
private val BorderGold = Color(0xFFD0B98C)
private val TextMain = Color(0xFF3D3326)
private val MutedWarm = Color(0xFF8D7C66)

private val ActionBrush = Brush.horizontalGradient(
    listOf(
        Color(0xFF9D7C3D),
        Color(0xFFB99653)
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
                    posLauncher.launch(paymentGateway.buildPaymentIntent(this@MainActivity, amount))
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
private fun AnimatedCrossGlow(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "cross_glow_breathe")

    val glowAlpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cross_glow_alpha"
    )

    val glowScale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cross_glow_scale"
    )

    val glowOffsetY by transition.animateFloat(
        initialValue = 0f,
        targetValue = -3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3800,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cross_glow_offset_y"
    )

    Image(
        painter = painterResource(id = R.drawable.cross_glow_background),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.graphicsLayer {
            alpha = glowAlpha
            scaleX = glowScale
            scaleY = glowScale
            translationY = glowOffsetY
        }
    )
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
            text = "ПРАВОСЛАВНАЯ\nБЛАГОТВОРИТЕЛЬНОСТЬ",
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = CormorantFontFamily,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = GoldDark
            )
        )
        Spacer(modifier = Modifier.height(6.dp))

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
            color = GoldDark,
            fontSize = 13.sp
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
            .width(232.dp)
            .height(24.dp)
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
            .width(160.dp)
            .height(14.dp)
            .graphicsLayer {
                alpha = 0.92f
            }
    )
}

@Composable
fun CrossPanel(modifier: Modifier = Modifier, onCrossTripleTap: () -> Unit) {
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                var tapCount = 0
                var lastTapTs = 0L

                detectTapGestures(
                    onTap = {
                        val now = SystemClock.elapsedRealtime()

                        tapCount = if (now - lastTapTs > SETTINGS_TRIPLE_TAP_TIMEOUT_MS) {
                            1
                        } else {
                            tapCount + 1
                        }

                        lastTapTs = now

                        if (tapCount >= 3) {
                            tapCount = 0
                            onCrossTripleTap()
                        }
                    }
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(72.dp))

            ShimmeringCross(
                modifier = Modifier.size(width = 98.dp, height = 160.dp)
            )

            Spacer(modifier = Modifier.height(56.dp))

            Text(
                text = "«Блажен, кто думает\nо бедном и нищем»",
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontFamily = AlegreyaFontFamily,
                    fontStyle = FontStyle.Italic,
                    color = GoldDark,
                    fontSize = 12.sp,
                    lineHeight = 15.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "— Псалом 40:1",
                style = TextStyle(
                    fontFamily = AlegreyaFontFamily,
                    fontSize = 12.sp,
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
    Box(modifier = Modifier.fillMaxSize().clickable(onClick = onDismiss)) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 20.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF8F2E6))
                .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(12.dp))
                .clickable(onClick = {})
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "НАСТРОЙКИ",
                style = TextStyle(
                    fontFamily = AlegreyaFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = GoldDark,
                    fontSize = 16.sp
                ),
                modifier = Modifier.clickable(onClick = onOpenSettings)
            )
        }
    }
}

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
            DonationCarouselItem("СВОЯ СУММА", "Введите любую сумму", null, "ВНЕСТИ ЛЕПТУ", DonationCardType.Custom),
            DonationCarouselItem("ПОМОЩЬ ХРАМУ", "Нужды церкви", formatAmountGroups(settings.templeAmount) + " ₽", "ПОЖЕРТВОВАТЬ", DonationCardType.Temple),
            DonationCarouselItem("ДЕТСКИЙ ПРИЮТ", "Забота о сиротах и детях", formatAmountGroups(settings.orphanageAmount) + " ₽", "ПОЖЕРТВОВАТЬ", DonationCardType.Orphanage)
        )
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    LaunchedEffect(pagerState, paymentInProgress, pages.size) {
        while (true) {
            delay(60_000)
            if (!paymentInProgress && pages.isNotEmpty()) {
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % pages.size)
            }
        }
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
            val item = pages[page]
            DonationCarouselCard(
                item = item,
                customAmount = customAmount,
                onAmountChange = onAmountChange,
                paymentInProgress = paymentInProgress,
                onClick = {
                    if (paymentInProgress) return@DonationCarouselCard
                    when (item.type) {
                        DonationCardType.Custom -> parseDonationAmount(customAmount)?.let(onPayment)
                        DonationCardType.Temple -> parseDonationAmount(settings.templeAmount)?.let(onPayment)
                        DonationCardType.Orphanage -> parseDonationAmount(settings.orphanageAmount)?.let(onPayment)
                    }
                }
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        CarouselPageIndicator(pageCount = pages.size, currentPage = pagerState.currentPage)
    }
}


@Composable
fun ButtonsPanel(
    modifier: Modifier = Modifier,
    customAmount: String,
    onAmountChange: (String) -> Unit,
    onPayment: (BigDecimal) -> Unit,
    paymentInProgress: Boolean,
    settings: DonationSettings
) {
    Column(
        modifier = modifier.padding(top = 2.dp)
    ) {
        DonationActionCard(
            title = "СВОЯ СУММА",
            description = "Введите любую сумму",
            actionLabel = "ВНЕСТИ ЛЕПТУ",
            showCardDivider = false,
            clickWholeCard = false,
            enabled = !paymentInProgress,
            onClick = {
                val parsed = parseDonationAmount(customAmount)
                if (!paymentInProgress && parsed != null) {
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

        Spacer(modifier = Modifier.height(8.dp))

        DonationActionCard(
            title = "ПОМОЩЬ ХРАМУ",
            description = "Нужды церкви",
            actionLabel = "ПОЖЕРТВОВАТЬ",
            showCardDivider = true,
            clickWholeCard = true,
            enabled = !paymentInProgress,
            onClick = {
                if (!paymentInProgress) {
                    parseDonationAmount(settings.templeAmount)?.let(onPayment)
                }
            },
            content = {
                Text(
                    text = formatAmountGroups(settings.templeAmount) + " ₽",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        fontFamily = TimesNewRomanFontFamily,
                        fontSize = 30.sp,
                        color = TextMain,
                        fontWeight = FontWeight.Normal
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        DonationActionCard(
            title = "ДЕТСКИЙ ПРИЮТ",
            description = "Забота о сиротах и детях",
            actionLabel = "ПОЖЕРТВОВАТЬ",
            showCardDivider = true,
            clickWholeCard = true,
            enabled = !paymentInProgress,
            onClick = {
                if (!paymentInProgress) {
                    parseDonationAmount(settings.orphanageAmount)?.let(onPayment)
                }
            },
            content = {
                Text(
                    text = formatAmountGroups(settings.orphanageAmount) + " ₽",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        fontFamily = TimesNewRomanFontFamily,
                        fontSize = 30.sp,
                        color = TextMain,
                        fontWeight = FontWeight.Normal
                    )
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
    showCardDivider: Boolean = false,
    clickWholeCard: Boolean,
    enabled: Boolean = true,
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
            enabled = enabled,
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
            .height(108.dp)
            .scale(scale)
            .clip(RoundedCornerShape(11.dp))
            .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(11.dp))
            .then(cardClickModifier)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
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

                if (showCardDivider) {
                    Spacer(modifier = Modifier.height(3.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CardOrnamentDivider(
                            modifier = Modifier
                                .width(120.dp)
                                .height(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(1.dp))
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    content()
                }
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
                .width(140.dp)
                .fillMaxHeight()
                .padding(top = 4.dp, end = 4.dp, bottom = 4.dp)
                .scale(actionScale)
                .clip(RoundedCornerShape(10.dp))
                .clickable(
                    enabled = enabled,
                    interactionSource = actionInteraction,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.button_bg),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Text(
                text = actionLabel,
                textAlign = TextAlign.Center,
                maxLines = 2,
                style = TextStyle(
                    fontFamily = AlegreyaFontFamily,
                    fontSize = 14.sp,
                    lineHeight = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
private fun DonationCarouselCard(
    item: DonationCarouselItem,
    customAmount: String,
    onAmountChange: (String) -> Unit,
    paymentInProgress: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .height(282.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF8F5EE))
            .border(BorderStroke(1.dp, BorderGold), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(item.title, style = TextStyle(fontFamily = AlegreyaFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = GoldDark), textAlign = TextAlign.Center)
            Text(item.description, style = TextStyle(fontFamily = AlegreyaFontFamily, fontSize = 16.sp, color = MutedWarm), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            CardOrnamentDivider()
            Spacer(modifier = Modifier.height(10.dp))
            if (item.type == DonationCardType.Custom) {
                AmountInput(value = customAmount, onValueChange = onAmountChange)
            } else {
                Text(
                    text = item.amountText.orEmpty(),
                    style = TextStyle(fontFamily = TimesNewRomanFontFamily, fontSize = 52.sp, color = TextMain)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(enabled = !paymentInProgress, onClick = onClick),
                contentAlignment = Alignment.Center
            ) {
                Image(painter = painterResource(id = R.drawable.button_bg), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Text(item.actionLabel, style = TextStyle(fontFamily = AlegreyaFontFamily, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 17.sp))
            }
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
        fontSize = 30.sp,
        fontWeight = FontWeight.Normal,
        color = TextMain,
        textAlign = TextAlign.Center
    )

    Box(
        modifier = Modifier
            .width(172.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(7.dp))
            .border(
                BorderStroke(1.dp, BorderGold.copy(alpha = 0.75f)),
                RoundedCornerShape(7.dp)
            )
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
                    onValueChange(normalizeAmountInput(input))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = AmountThousandsVisualTransformation(),
                textStyle = amountTextStyle.copy(textAlign = TextAlign.Center),
                modifier = Modifier.weight(1f),
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
            message = "Пожертвование не было списано. Попробуйте ещё раз",
            buttonLabel = "ЗАКРЫТЬ"
        )

        AppPaymentResult.Error -> DialogUi(
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

            AppOrnamentDivider(
                modifier = Modifier
                    .width(190.dp)
                    .height(18.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = dialogUi.title,
                style = TextStyle(
                    fontFamily = CormorantFontFamily,
                    fontSize = 16.sp,
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
                    fontSize = 14.sp,
                    color = TextMain
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(8.dp))
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
                        color = Color.White,
                        fontFamily = AlegreyaFontFamily,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold
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
