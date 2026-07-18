package com.example.otweak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.VideoView
import android.widget.MediaController
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.zIndex
import androidx.compose.animation.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.otweak.theme.OTweakTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TweakRepository.init(this)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val blackNightTheme by viewModel.blackNightTheme.collectAsState()
            val useSystemThemeColor by viewModel.useSystemThemeColor.collectAsState()

            OTweakTheme(
                themeMode = themeMode,
                blackNightTheme = blackNightTheme,
                dynamicColor = useSystemThemeColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OTweakApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ShizukuManager.checkAvailability()
        ShizukuManager.checkPermission()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OTweakApp(viewModel: MainViewModel = viewModel()) {
    val isShizukuAvailable by viewModel.isShizukuAvailable.collectAsState()
    val hasPermission by viewModel.hasPermission.collectAsState()
    val isGlowEffectEnabled by viewModel.isGlowEffectEnabled.collectAsState()
    val isWidgetTransparencyEnabled by viewModel.isWidgetTransparencyEnabled.collectAsState()
    val isForceRotateDisabled by viewModel.isForceRotateDisabled.collectAsState()
    val clockStyleIndex by viewModel.clockStyleIndex.collectAsState()
    val clockColorHex by viewModel.clockColorHex.collectAsState()
    val clockSubColorHex by viewModel.clockSubColorHex.collectAsState()
    val clockAlpha by viewModel.clockAlpha.collectAsState()
    val isTermsAccepted by viewModel.isTermsAccepted.collectAsState()
    val isTelegramJoined by viewModel.isTelegramJoined.collectAsState()
    val updateAvailable by viewModel.updateAvailable.collectAsState()
    val updateUrl by viewModel.updateUrl.collectAsState()
    
    var showSettingsScreen by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.refreshTweakStatuses()
        }
    }

    if (!isTermsAccepted) {
        AlertDialog(
            onDismissRequest = { /* Cannot dismiss */ },
            title = { Text("Terms & Conditions", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Origin Tweaks modifies deep system settings using Shizuku. " +
                    "Incorrect use may cause system instability or unexpected behavior. " +
                    "By proceeding, you agree that you use this application entirely at your own risk and the developer is not responsible for any damage to your device."
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.acceptTerms() }) {
                    Text("I Accept")
                }
            },
            dismissButton = {
                TextButton(onClick = { (context as? ComponentActivity)?.finish() }) {
                    Text("Decline & Exit")
                }
            },
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
        )
    }

    AnimatedVisibility(
        visible = showSettingsScreen,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
        modifier = Modifier.zIndex(10f)
    ) {
        SettingsScreen(
            viewModel = viewModel,
            onBack = { showSettingsScreen = false },
            onTelegramClick = { url -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Origin Tweaks", fontWeight = FontWeight.Bold) },
                actions = {
                    if (updateAvailable && updateUrl != null) {
                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                            Text("UPDATE", color = MaterialTheme.colorScheme.onError, modifier = Modifier.padding(horizontal = 4.dp).clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl)))
                            })
                        }
                    }
                    if (isTelegramJoined && hasPermission) {
                        IconButton(onClick = { showSettingsScreen = true }) {
                            Text("☰", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            AnimatedContent(
                targetState = Triple(isTelegramJoined, isShizukuAvailable, hasPermission),
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "MainScreenTransition"
            ) { (telegramJoined, shizukuAvail, permissionGranted) ->
                if (!telegramJoined) {
                    TelegramOnboardingScreen(viewModel)
                } else if (!shizukuAvail || !permissionGranted) {
                    ShizukuOnboardingScreen(
                        isShizukuAvailable = shizukuAvail,
                        hasPermission = permissionGranted,
                        onRequestPermission = { viewModel.requestShizukuPermission() }
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        TweakSection("Display Tweaks", Icons.Default.Monitor) {
                            TweakSwitchCard(
                                title = "Glow Effect",
                                description = "Enable the blur/glow effect on supported budget OriginOS devices.",
                                checked = isGlowEffectEnabled,
                                onCheckedChange = { viewModel.setGlowEffect(it) }
                            )
                            TweakSwitchCard(
                                title = "Widget Transparency",
                                description = "Enable transparency for supported widgets on the launcher.",
                                checked = isWidgetTransparencyEnabled,
                                onCheckedChange = { viewModel.setWidgetTransparency(it) }
                            )
                            TweakSwitchCard(
                                title = "Disable Force Rotate Icon",
                                description = "Turn off the small rotation suggestion icon that appears when you tilt your device.",
                                checked = isForceRotateDisabled,
                                onCheckedChange = { viewModel.setForceRotateDisabled(it) }
                            )
                        }

                        TweakSection("Lock Screen Tweaks", Icons.Default.Lock) {
                            ClockCustomizationCard(
                                currentStyleIndex = clockStyleIndex,
                                currentColorHex = clockColorHex,
                                currentSubColorHex = clockSubColorHex,
                                currentAlpha = clockAlpha,
                                onCustomizationChanged = { styleIndex, colorHex, subColorHex, alpha -> 
                                    viewModel.setClockCustomization(styleIndex, colorHex, subColorHex, alpha) { success ->
                                        if (success) {
                                            android.widget.Toast.makeText(context, "Applied! Please lock screen to view.", android.widget.Toast.LENGTH_SHORT).show()
                                        } else {
                                            android.widget.Toast.makeText(context, "Failed to apply customization.", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TelegramOnboardingScreen(viewModel: MainViewModel) {
    val clickedSupport by viewModel.clickedSupport.collectAsState()
    val clickedHub by viewModel.clickedHub.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var loadingSupport by remember { mutableStateOf(false) }
    var loadingHub by remember { mutableStateOf(false) }

    val allClicked = clickedSupport && clickedHub

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Overlapping Images
        Row(horizontalArrangement = Arrangement.spacedBy((-16).dp), modifier = Modifier.padding(bottom = 24.dp)) {
            Image(
                painter = painterResource(id = R.drawable.g1),
                contentDescription = null,
                modifier = Modifier.size(72.dp).clip(androidx.compose.foundation.shape.CircleShape).border(2.dp, MaterialTheme.colorScheme.background, androidx.compose.foundation.shape.CircleShape).zIndex(1f)
            )
            Image(
                painter = painterResource(id = R.drawable.g2),
                contentDescription = null,
                modifier = Modifier.size(72.dp).clip(androidx.compose.foundation.shape.CircleShape).border(2.dp, MaterialTheme.colorScheme.background, androidx.compose.foundation.shape.CircleShape).zIndex(2f)
            )
        }
        Text("Welcome to Origin Tweaks", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "To proceed, please join our 2 Telegram communities for the latest updates and support.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (!clickedSupport && !loadingSupport) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/OOSSupport")))
                    scope.launch {
                        loadingSupport = true
                        kotlinx.coroutines.delay(5000)
                        viewModel.markSupportClicked()
                        loadingSupport = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = if (clickedSupport || loadingSupport) Color.Gray else MaterialTheme.colorScheme.primary)
        ) {
            if (loadingSupport) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verifying...")
            } else {
                Text(if (clickedSupport) "Joined Support Group ✓" else "Join Support Group")
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (!clickedHub && !loadingHub) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/OOSHub")))
                    scope.launch {
                        loadingHub = true
                        kotlinx.coroutines.delay(5000)
                        viewModel.markHubClicked()
                        loadingHub = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = if (clickedHub || loadingHub) Color.Gray else MaterialTheme.colorScheme.primary)
        ) {
            if (loadingHub) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verifying...")
            } else {
                Text(if (clickedHub) "Joined Hub Channel ✓" else "Join Hub Channel")
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = { viewModel.completeTelegramOnboarding() },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = allClicked,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("CONTINUE TO APP", fontWeight = FontWeight.Bold, fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp))
        }
    }
}

@Composable
fun ShizukuOnboardingScreen(isShizukuAvailable: Boolean, hasPermission: Boolean, onRequestPermission: () -> Unit) {
    val context = LocalContext.current
    var askedInstalled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isShizukuAvailable && !askedInstalled) {
            Text("Shizuku Required", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Origin Tweaks uses Shizuku to modify deep system settings without needing root. Have you installed Shizuku?",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { askedInstalled = true }, modifier = Modifier.weight(1f)) {
                    Text("Yes, I have it", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RikkaApps/Shizuku/releases"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("No, get from GitHub", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        } else {
            Text("Start Shizuku", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            var isVideoFullScreen by remember { mutableStateOf(false) }

            if (isVideoFullScreen) {
                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { isVideoFullScreen = false },
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    setVideoURI(Uri.parse("android.resource://${ctx.packageName}/raw/guide"))
                                    
                                    val mediaController = MediaController(ctx)
                                    mediaController.setAnchorView(this)
                                    setMediaController(mediaController)
                                    
                                    setOnPreparedListener { mp -> 
                                        mp.isLooping = true 
                                        mp.setVolume(0f, 0f)
                                        start()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Video Player
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f/9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(Uri.parse("android.resource://${ctx.packageName}/raw/guide"))
                            setOnPreparedListener { mp -> 
                                mp.isLooping = true 
                                mp.setVolume(0f, 0f)
                                start()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                // Transparent overlay to intercept clicks for fullscreen
                Box(modifier = Modifier.fillMaxSize().clickable { isVideoFullScreen = true })
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            if (!isShizukuAvailable) {
                Text(
                    "Please follow the video guide to start Shizuku on your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                        if (intent != null) {
                            context.startActivity(intent)
                        } else {
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/RikkaApps/Shizuku/releases"))
                            context.startActivity(webIntent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Launch Shizuku App")
                }
            } else if (!hasPermission) {
                Text(
                    "Shizuku is running! Now grant permission to Origin Tweaks.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
fun ShizukuWarningCard(message: String, buttonText: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("⚠", style = MaterialTheme.typography.titleLarge)
                Text("Shizuku Required", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onErrorContainer, contentColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(buttonText)
            }
        }
    }
}

@Composable
fun TweakSection(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        content()
    }
}

@Composable
fun TweakSwitchCard(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = checked,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCheckedChange(it)
                }
            )
        }
    }
}

@Composable
fun ClockCustomizationCard(
    currentStyleIndex: Int,
    currentColorHex: String,
    currentSubColorHex: String,
    currentAlpha: Float,
    onCustomizationChanged: (Int, String, String, Float) -> Unit
) {
    var editingMainColor by remember { mutableStateOf(true) }
    var showColorPicker by remember { mutableStateOf(false) }
    
    // Local state to prevent spamming Shizuku commands during slider/wheel dragging
    var tempStyleIndex by remember(currentStyleIndex) { mutableIntStateOf(currentStyleIndex) }
    var tempColorHex by remember(currentColorHex) { mutableStateOf(currentColorHex) }
    var tempSubColorHex by remember(currentSubColorHex) { mutableStateOf(currentSubColorHex) }
    var tempAlpha by remember(currentAlpha) { mutableFloatStateOf(currentAlpha) }
    val haptic = LocalHapticFeedback.current

    if (showColorPicker) {
        val initialColorHex = if (editingMainColor || !(tempStyleIndex == 1 || tempStyleIndex == 2)) tempColorHex else tempSubColorHex
        var dialogColorHex by remember { mutableStateOf(initialColorHex) }

        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text(if (editingMainColor) "Choose Hours Color" else "Choose Minutes Color") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ColorPicker(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                    ) { hex ->
                        dialogColorHex = hex
                    }
                    val parsedDialogColor = try { Color(android.graphics.Color.parseColor("#$dialogColorHex")) } catch (e: Exception) { Color.White }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(32.dp).clip(androidx.compose.foundation.shape.CircleShape).background(parsedDialogColor).border(1.dp, Color.Gray, androidx.compose.foundation.shape.CircleShape))
                        Text("Selected: #$dialogColorHex", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editingMainColor || !(tempStyleIndex == 1 || tempStyleIndex == 2)) {
                        tempColorHex = dialogColorHex
                    } else {
                        tempSubColorHex = dialogColorHex
                    }
                    showColorPicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showColorPicker = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Lock Screen Clock Customization", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            
            // 1. Style Selector
            Text("1. Select Style", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(3) { index ->
                    val isSelected = tempStyleIndex == index
                    val drawableRes = when (index) {
                        0 -> R.drawable.style_1
                        1 -> R.drawable.style_2
                        2 -> R.drawable.style_3
                        else -> R.drawable.style_1
                    }
                    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(targetValue = if (isPressed) 0.92f else 1f, label = "cardScale")
                    
                    Image(
                        painter = painterResource(id = drawableRes),
                        contentDescription = "Style ${index + 1}",
                        modifier = Modifier
                            .height(180.dp)
                            .aspectRatio(0.45f)
                            .scale(scale)
                            .clip(RoundedCornerShape(8.dp))
                            .border(3.dp, borderColor, RoundedCornerShape(8.dp))
                            .clickable(interactionSource = interactionSource, indication = null) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                tempStyleIndex = index
                            }
                    )
                }
            }
            
            // 2. Color Selection
            val hasTwoColors = tempStyleIndex == 1 || tempStyleIndex == 2
            if (hasTwoColors) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilterChip(
                        selected = editingMainColor,
                        onClick = { editingMainColor = true },
                        label = { Text("Hours Color") }
                    )
                    FilterChip(
                        selected = !editingMainColor,
                        onClick = { editingMainColor = false },
                        label = { Text("Minutes Color") }
                    )
                }
            }

            Text("2. Select Color", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            
            Button(
                onClick = { showColorPicker = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Text(if (hasTwoColors) { if (editingMainColor) "Choose Hours Color" else "Choose Minutes Color" } else "Choose Clock Color")
            }
            
            val parsedMainColor = try { Color(android.graphics.Color.parseColor("#$tempColorHex")) } catch (e: Exception) { Color.White }
            val parsedSubColor = try { Color(android.graphics.Color.parseColor("#$tempSubColorHex")) } catch (e: Exception) { Color.White }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape).background(parsedMainColor).border(1.dp, Color.Gray, androidx.compose.foundation.shape.CircleShape))
                    Text("Hours: #$tempColorHex", style = MaterialTheme.typography.bodySmall)
                }
                if (hasTwoColors) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape).background(parsedSubColor).border(1.dp, Color.Gray, androidx.compose.foundation.shape.CircleShape))
                        Text("Mins: #$tempSubColorHex", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // 3. Transparency Slider
            val transparencyPercent = ((1f - tempAlpha) * 100).toInt()
            val currentTransparency = 1f - tempAlpha
            Text("3. Adjust Transparency: $transparencyPercent%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Slider(
                value = currentTransparency,
                onValueChange = { 
                    tempAlpha = 1f - it 
                    // To prevent spamming haptics, only vibrate on discrete steps or we can leave it off for slider value change to avoid lag, but let's add a light one.
                },
                valueRange = 0f..1f,
                onValueChangeFinished = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
            )

            // 4. Apply Button
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCustomizationChanged(tempStyleIndex, tempColorHex, tempSubColorHex, tempAlpha) 
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("APPLY CLOCK TWEAK", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ColorPicker(modifier: Modifier = Modifier, onColorSelected: (String) -> Unit) {
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var value by remember { mutableFloatStateOf(1f) }
    
    LaunchedEffect(hue, saturation, value) {
        val colorInt = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        onColorSelected(String.format("%06x", 0xFFFFFF and colorInt))
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val angle = atan2(change.position.y - center.y, change.position.x - center.x)
                            hue = ((angle * 180f / Math.PI).toFloat() + 360f) % 360f
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val angle = atan2(offset.y - center.y, offset.x - center.x)
                            hue = ((angle * 180f / Math.PI).toFloat() + 360f) % 360f
                        }
                    }
            ) {
                val radius = size.minDimension / 2 - 20f
                val colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                drawCircle(
                    brush = Brush.sweepGradient(colors, center = center),
                    radius = radius,
                    style = Stroke(width = 40f)
                )
                
                val selectorX = center.x + cos(hue * Math.PI / 180f).toFloat() * radius
                val selectorY = center.y + sin(hue * Math.PI / 180f).toFloat() * radius
                drawCircle(
                    color = Color.White,
                    radius = 20f,
                    center = Offset(selectorX, selectorY),
                    style = Stroke(width = 6f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Saturation: ${(saturation * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
        Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..1f, modifier = Modifier.height(24.dp))
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("Brightness: ${(value * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
        Slider(value = value, onValueChange = { value = it }, valueRange = 0f..1f, modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onTelegramClick: (String) -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val blackNightTheme by viewModel.blackNightTheme.collectAsState()
    val useSystemThemeColor by viewModel.useSystemThemeColor.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Appearance",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
            )

            // Theme Setting
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showThemeDialog = true }
                    .padding(16.dp)
            ) {
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = when (themeMode) {
                        1 -> "Light"
                        2 -> "Dark"
                        else -> "Follow system"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Black night theme
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setBlackNightTheme(!blackNightTheme) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Black night theme", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Use the pure black theme if night mode is enabled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = blackNightTheme,
                    onCheckedChange = { viewModel.setBlackNightTheme(it) }
                )
            }

            // Use system theme color
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setUseSystemThemeColor(!useSystemThemeColor) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Use system theme color",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = useSystemThemeColor,
                    onCheckedChange = { viewModel.setUseSystemThemeColor(it) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Support",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            ListItem(
                headlineContent = { Text("OriginOS 6 Hub (Channel)") },
                modifier = Modifier.clickable { onTelegramClick("https://t.me/OOSHub") }
            )

            ListItem(
                headlineContent = { Text("OriginOS 6 Support (Group)") },
                modifier = Modifier.clickable { onTelegramClick("https://t.me/OOSSupport") }
            )

            ListItem(
                headlineContent = { Text("Made by Mr. Exquisite") },
                supportingContent = { Text("Join our community for OriginOS updates and tweaks!") },
                leadingContent = {
                    Image(
                        painter = painterResource(id = R.drawable.mr_exquisite),
                        contentDescription = "Mr. Exquisite Profile",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)
                    )
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("Theme") },
                text = {
                    Column {
                        val options = listOf("Follow system" to 0, "Light" to 1, "Dark" to 2)
                        options.forEach { (label, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setThemeMode(value)
                                        showThemeDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = themeMode == value,
                                    onClick = {
                                        viewModel.setThemeMode(value)
                                        showThemeDialog = false
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showThemeDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
