package com.gzavagli.m2block.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.gzavagli.m2block.*
import com.gzavagli.m2block.ui.theme.*

@Composable
fun MainGameApp(viewModel: GameViewModel) {
    val state by viewModel.state.collectAsState()
    val colors = getThemeColors(state.theme)

    var showSettings by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgApp),
        contentAlignment = Alignment.Center
    ) {
        when (state.status) {
            GameStatus.HOME -> {
                HomeScreen(
                    state = state,
                    colors = colors,
                    onStartGame = { viewModel.startGame() },
                    onResumeGame = { viewModel.resumeSavedGame() },
                    onSelectTheme = { viewModel.selectTheme(it) },
                    onShowTutorial = { showTutorial = true },
                    onShowSettings = { showSettings = true }
                )
            }
            GameStatus.PLAYING,
            GameStatus.PAUSED,
            GameStatus.GAMEOVER,
            GameStatus.TARGETING_HAMMER,
            GameStatus.TARGETING_VORTEX,
            GameStatus.TARGETING_DOUBLE -> {
                GamePlayScreen(
                    state = state,
                    colors = colors,
                    viewModel = viewModel,
                    onPause = { viewModel.pauseGame() },
                    onShowTutorial = { showTutorial = true },
                    onShowSettings = { showSettings = true }
                )
            }
        }

        if (showSettings) {
            SettingsDialog(
                state = state,
                colors = colors,
                onMuteToggle = { viewModel.setMuted(it) },
                onHapticsToggle = { viewModel.setHapticsEnabled(it) },
                onResetScore = { viewModel.resetGameToHome() },
                onDismiss = { showSettings = false }
            )
        }

        if (showTutorial) {
            TutorialDialog(
                colors = colors,
                onDismiss = { showTutorial = false }
            )
        }
    }
}

// --- Home / Splash Screen ---
@Composable
fun HomeScreen(
    state: GameState,
    colors: ThemeColors,
    onStartGame: () -> Unit,
    onResumeGame: () -> Unit,
    onSelectTheme: (GameTheme) -> Unit,
    onShowTutorial: () -> Unit,
    onShowSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo Brand Area
        Column(
            modifier = Modifier.padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LogoTile(value = 16, colors = colors, rotate = -8f)
                LogoTile(value = 128, colors = colors, rotate = 4f, translateUp = 10.dp)
                LogoTile(value = 2048, colors = colors, rotate = 12f)
            }
            Spacer(modifier = Modifier.height(25.dp))
            Text(
                text = "MERGE\nBLOCKS",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary,
                lineHeight = 44.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "DROP & MATCH PUZZLE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary,
                letterSpacing = 2.sp
            )
        }

        // Actions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (state.hasActiveSavedGame) {
                // Resume Game Button
                Button(
                    onClick = onResumeGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.btnBg,
                        contentColor = colors.btnText
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "RESUME GAME", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                // New Game Button
                OutlinedButton(
                    onClick = onStartGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                    border = BorderStroke(1.dp, colors.borderUi)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "New Game")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "NEW GAME", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                // Main Play Button
                Button(
                    onClick = onStartGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.btnBg,
                        contentColor = colors.btnText
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "PLAY NOW", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Subactions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onShowTutorial,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                    border = BorderStroke(1.dp, colors.borderUi)
                ) {
                    Icon(Icons.Default.Info, contentDescription = "How to Play")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TUTORIAL", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onShowSettings,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                    border = BorderStroke(1.dp, colors.borderUi)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SETTINGS", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Theme selector
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SELECT THEME",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .border(1.dp, colors.borderUi, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GameTheme.values().forEach { t ->
                        val active = state.theme == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) colors.btnBg else Color.Transparent)
                                .clickable { onSelectTheme(t) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = t.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) colors.btnText else colors.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Best Score display
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "BEST SCORE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textSecondary,
                    letterSpacing = 2.sp
                )
                Text(
                    text = String.format("%,d", state.highScore),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun LogoTile(value: Int, colors: ThemeColors, rotate: Float, translateUp: Dp = 0.dp) {
    val blockStyle = colors.getBlockColors(value)
    Box(
        modifier = Modifier
            .offset(y = -translateUp)
            .scale(0.9f)
            .size(50.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(blockStyle.background)
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        val formattedValue = formatBlockValue(value)
        Text(
            text = formattedValue,
            fontSize = if (formattedValue.length <= 3) 18.sp else 14.sp,
            fontWeight = FontWeight.Black,
            color = blockStyle.textColor
        )
    }
}

// --- Main Gameplay Screen ---
@Composable
fun GamePlayScreen(
    state: GameState,
    colors: ThemeColors,
    viewModel: GameViewModel,
    onPause: () -> Unit,
    onShowTutorial: () -> Unit,
    onShowSettings: () -> Unit
) {
    val animOffset = remember { Animatable(0f) }
    var slot0Value by remember { mutableIntStateOf(state.currentBlockValue) }
    var slot1Value by remember { mutableIntStateOf(state.currentBlockValue) }
    var slot2Value by remember { mutableIntStateOf(state.nextBlockValue) }

    var lastCurrent by remember { mutableIntStateOf(state.currentBlockValue) }
    var lastNext by remember { mutableIntStateOf(state.nextBlockValue) }

    LaunchedEffect(state.status) {
        if (state.status == GameStatus.HOME) {
            animOffset.snapTo(0f)
        } else if (state.status == GameStatus.PLAYING) {
            slot1Value = state.currentBlockValue
            slot2Value = state.nextBlockValue
            lastCurrent = state.currentBlockValue
            lastNext = state.nextBlockValue
            animOffset.snapTo(114f)
            animOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300, easing = EaseOutQuad)
            )
        }
    }

    LaunchedEffect(state.currentBlockValue, state.nextBlockValue) {
        if (state.status != GameStatus.PLAYING) return@LaunchedEffect

        if (state.currentBlockValue == lastNext && state.nextBlockValue == lastCurrent) {
            // Swap: update values instantly (we can also do a quick pulse animation if desired)
            slot1Value = state.currentBlockValue
            slot2Value = state.nextBlockValue
        } else if (state.currentBlockValue == lastNext) {
            // Drop: slide in from right
            slot0Value = lastCurrent
            slot1Value = state.currentBlockValue
            slot2Value = state.nextBlockValue

            animOffset.snapTo(57f)
            animOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 250, easing = EaseOutQuad)
            )
        } else {
            // Reinitialize
            slot1Value = state.currentBlockValue
            slot2Value = state.nextBlockValue
            animOffset.snapTo(0f)
        }

        lastCurrent = state.currentBlockValue
        lastNext = state.nextBlockValue
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score Box
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.2f))
                        .border(1.dp, colors.borderUi, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("SCORE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                    Text(
                        text = state.score.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Coins Box
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.colorCoin.copy(alpha = 0.08f))
                        .border(1.dp, colors.colorCoin.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🪙", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = state.coins.toString(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.colorCoin,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Pause Settings Button
                IconButton(
                    onClick = onPause,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.2f))
                        .border(1.dp, colors.borderUi, RoundedCornerShape(10.dp))
                ) {
                    Text("⏸", color = colors.textPrimary, fontSize = 16.sp)
                }
            }

            // Grid Board
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .border(1.dp, colors.borderUi, RoundedCornerShape(20.dp))
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val colWidth = maxWidth / 5
                    val rowHeight = maxHeight / 7

                    // Column trigger panels (touch grid columns to drop)
                    Row(modifier = Modifier.fillMaxSize()) {
                        for (col in 0 until 5) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(colWidth)
                                    .clickable {
                                        if (state.status == GameStatus.PLAYING) {
                                            viewModel.dropBlockInColumn(col)
                                        } else {
                                            viewModel.handleColumnTargetClick(col)
                                        }
                                    }
                                    .border(0.5.dp, Color.White.copy(alpha = 0.02f))
                            )
                        }
                    }

                    // Render active Blocks
                    state.grid.forEach { block ->
                        key(block.id) {
                            val blockColors = colors.getBlockColors(block.value)


                            val targetCol = if (block.status == BlockStatus.MERGING && block.targetCol != null) block.targetCol else block.col
                            val targetRow = if (block.status == BlockStatus.MERGING && block.targetRow != null) block.targetRow else block.row

                            // Compose offsets
                            val xOffset by animateDpAsState(
                                targetValue = colWidth * targetCol,
                                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                                label = "x"
                            )
                            val yOffset by animateDpAsState(
                                targetValue = rowHeight * targetRow,
                                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                                label = "y"
                            )

                            // Shrink when merging or pop scale when newly created
                            val scale by animateFloatAsState(
                                targetValue = when (block.status) {
                                    BlockStatus.MERGING -> 0f
                                    BlockStatus.NEW -> 1.15f
                                    else -> 1f
                                },
                                animationSpec = tween(durationMillis = 150),
                                label = "scale"
                            )

                            Box(
                                modifier = Modifier
                                    .absoluteOffset(x = xOffset, y = yOffset)
                                    .size(width = colWidth, height = rowHeight)
                                    .padding(4.dp)
                                    .scale(scale.coerceAtLeast(0f))
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(blockColors.background)
                                    .clickable {
                                        if (state.status in listOf(GameStatus.TARGETING_HAMMER, GameStatus.TARGETING_DOUBLE)) {
                                            viewModel.handleBlockTargetClick(block)
                                        } else {
                                            if (state.status == GameStatus.PLAYING) {
                                                viewModel.dropBlockInColumn(block.col)
                                            } else {
                                                viewModel.handleColumnTargetClick(block.col)
                                            }
                                        }
                                    }
                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                val formattedValue = formatBlockValue(block.value)
                                Text(
                                    text = formattedValue,
                                    fontSize = if (formattedValue.length <= 3) 22.sp else 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = blockColors.textColor
                                )
                            }
                        }
                    }
                }

                // Targeting Overlay (displays message when a power-up targeting mode is active)
                if (state.status in listOf(GameStatus.TARGETING_HAMMER, GameStatus.TARGETING_VORTEX, GameStatus.TARGETING_DOUBLE)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = when (state.status) {
                                    GameStatus.TARGETING_HAMMER -> "🔨 TAP ANY BLOCK TO SMASH IT"
                                    GameStatus.TARGETING_DOUBLE -> "×2 TAP ANY BLOCK TO DOUBLE IT"
                                    GameStatus.TARGETING_VORTEX -> "🌪 TAP ANY COLUMN TO CLEAR IT"
                                    else -> ""
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Button(
                                onClick = { viewModel.cancelTargetingMode() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("CANCEL", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Preview Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.15f))
                    .border(1.dp, colors.borderUi, RoundedCornerShape(16.dp))
                    .padding(8.dp, 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CURRENT BLOCK", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = colors.textSecondary)
                    Text("Tap column to shoot", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                }
                Box {
                    // Static Labels & Background Placeholders
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "DROP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.1f))
                                    .border(1.dp, colors.borderUi.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "NEXT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.1f))
                                    .border(1.dp, colors.borderUi.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            )
                        }
                    }

                    // Sliding blocks overlay
                    val currentOffset = animOffset.value
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = currentOffset.dp)
                            .size(width = 99.dp, height = 42.dp)
                    ) {
                        // Render Slot 0 only when container is animating a drop (offset > 0 and <= 57)
                        if (currentOffset > 0f && currentOffset <= 57f) {
                            val alpha = (currentOffset / 57f).coerceIn(0f, 1f)
                            val scale = 0.8f + 0.2f * alpha
                            val blockColors = colors.getBlockColors(slot0Value)
                            Box(
                                modifier = Modifier
                                    .offset(x = (-57).dp)
                                    .size(42.dp)
                                    .graphicsLayer(alpha = alpha, scaleX = scale, scaleY = scale)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(blockColors.background)
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                val formattedVal = formatBlockValue(slot0Value)
                                Text(
                                    text = formattedVal,
                                    fontSize = if (formattedVal.length <= 3) 14.sp else 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = blockColors.textColor
                                )
                            }
                        }

                        // Slot 1 (DROP)
                        val blockColors1 = colors.getBlockColors(slot1Value)
                        Box(
                            modifier = Modifier
                                .offset(x = 0.dp)
                                .size(42.dp)
                                .graphicsLayer(alpha = 1f, scaleX = 1f, scaleY = 1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(blockColors1.background)
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            val formattedVal1 = formatBlockValue(slot1Value)
                            Text(
                                text = formattedVal1,
                                fontSize = if (formattedVal1.length <= 3) 14.sp else 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = blockColors1.textColor
                            )
                        }

                        // Slot 2 (NEXT)
                        val alpha2 = (1f - (currentOffset / 57f)).coerceIn(0f, 1f)
                        val scale2 = 0.8f + 0.2f * alpha2
                        val blockColors2 = colors.getBlockColors(slot2Value)
                        Box(
                            modifier = Modifier
                                .offset(x = 57.dp)
                                .size(42.dp)
                                .graphicsLayer(alpha = alpha2, scaleX = scale2, scaleY = scale2)
                                .clip(RoundedCornerShape(8.dp))
                                .background(blockColors2.background)
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            val formattedVal2 = formatBlockValue(slot2Value)
                            Text(
                                text = formattedVal2,
                                fontSize = if (formattedVal2.length <= 3) 14.sp else 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = blockColors2.textColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Power-ups Shop Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .border(1.dp, colors.borderUi, RoundedCornerShape(16.dp))
                    .padding(10.dp, 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PowerupButton(icon = "🔨", name = "SMASH", cost = 100, currentCoins = state.coins, modifier = Modifier.weight(1f)) {
                    viewModel.activateHammerMode()
                }
                PowerupButton(icon = "🔄", name = "SWAP", cost = 50, currentCoins = state.coins, modifier = Modifier.weight(1f)) {
                    viewModel.useSwapPowerup()
                }
                PowerupButton(icon = "🌪", name = "VORTEX", cost = 150, currentCoins = state.coins, modifier = Modifier.weight(1f)) {
                    viewModel.activateVortexMode()
                }
                PowerupButton(icon = "×2", name = "DOUBLE", cost = 120, currentCoins = state.coins, modifier = Modifier.weight(1f)) {
                    viewModel.activateDoubleMode()
                }
            }
        }

        // Pause overlay
        if (state.status == GameStatus.PAUSED) {
            Dialog(onDismissRequest = { viewModel.resumeGame() }) {
                Card(
                    modifier = Modifier.padding(24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.bgPanel)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("GAME PAUSED", fontSize = 22.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
                        
                        Button(
                            onClick = { viewModel.resumeGame() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.btnBg, contentColor = colors.btnText)
                        ) {
                            Text("RESUME", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onShowTutorial,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                            border = BorderStroke(1.dp, colors.borderUi)
                        ) {
                            Text("TUTORIAL", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.resetGameToHome()
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                        ) {
                            Text("QUIT TO HOME", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Game Over overlay
        if (state.status == GameStatus.GAMEOVER) {
            Dialog(onDismissRequest = {}) {
                Card(
                    modifier = Modifier.padding(20.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.bgPanel)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            text = "GAME OVER",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Red,
                            textAlign = TextAlign.Center
                        )

                        // Stats Summary
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.2f))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatRow("SCORE", state.score.toString(), colors)
                            StatRow("HIGH SCORE", state.highScore.toString(), colors, isHighScore = true)
                        }

                        // Revive Button
                        Button(
                            onClick = { viewModel.useRevive() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.colorCoin),
                            enabled = state.coins >= 150
                        ) {
                            Text("🪙 REVIVE (-150)", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        // Restart Button
                        Button(
                            onClick = { viewModel.startGame() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.btnBg, contentColor = colors.btnText)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PLAY AGAIN", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetGameToHome() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                            border = BorderStroke(1.dp, colors.borderUi)
                        ) {
                            Text("MAIN MENU", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PreviewSlot(value: Int, tag: String, colors: ThemeColors) {
    val blockColors = colors.getBlockColors(value)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = tag, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(blockColors.background)
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(value.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = blockColors.textColor)
        }
    }
}

@Composable
fun PowerupButton(
    icon: String,
    name: String,
    cost: Int,
    currentCoins: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val enabled = currentCoins >= cost
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(68.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White.copy(alpha = 0.01f),
            contentColor = if (enabled) Color.White else Color.White.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, if (enabled) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f)),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(icon, fontSize = 16.sp)
            Text(name, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🪙", fontSize = 9.sp)
                Text(cost.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String, colors: ThemeColors, isHighScore: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = if (isHighScore) colors.colorCoin else colors.textPrimary,
            fontFamily = FontFamily.Monospace
        )
    }
}

// --- Settings Dialog ---
@Composable
fun SettingsDialog(
    state: GameState,
    colors: ThemeColors,
    onMuteToggle: (Boolean) -> Unit,
    onHapticsToggle: (Boolean) -> Unit,
    onResetScore: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.bgPanel)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "SETTINGS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Settings Switch Row: Sound
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SOUND EFFECTS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Switch(
                        checked = !state.isMuted,
                        onCheckedChange = { onMuteToggle(!it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.btnBg)
                    )
                }

                // Settings Switch Row: Haptics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("HAPTIC VIBRATION", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Switch(
                        checked = state.isHapticsEnabled,
                        onCheckedChange = onHapticsToggle,
                        colors = SwitchDefaults.colors(checkedTrackColor = colors.btnBg)
                    )
                }

                Divider(color = colors.borderUi.copy(alpha = 0.3f))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.btnBg, contentColor = colors.btnText)
                ) {
                    Text("CLOSE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- Tutorial Dialog ---
@Composable
fun TutorialDialog(
    colors: ThemeColors,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.bgPanel)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "HOW TO PLAY",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TutorialStep("1", "Tap columns to drop numbers from the top.", colors)
                    TutorialStep("2", "Matching numbers merge when touching (e.g. 2 + 2 = 4).", colors)
                    TutorialStep("3", "Chaining multiple merges creates combos for massive points!", colors)
                    TutorialStep("4", "Earn gold coins on merges to purchase powerups at the bottom.", colors)
                    TutorialStep("5", "If blocks reach the top row, it's Game Over!", colors)
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.btnBg, contentColor = colors.btnText)
                ) {
                    Text("GOT IT", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TutorialStep(num: String, desc: String, colors: ThemeColors) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(colors.borderUi),
            contentAlignment = Alignment.Center
        ) {
            Text(num, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        }
        Text(desc, fontSize = 12.sp, color = colors.textSecondary, lineHeight = 16.sp)
    }
}

private fun formatBlockValue(value: Int): String {
    return when {
        value >= 1_073_741_824 -> "${value / 1_073_741_824}G"
        value >= 1_048_576 -> "${value / 1_048_576}M"
        value >= 1024 -> "${value / 1024}K"
        else -> value.toString()
    }
}
