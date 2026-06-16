package com.gzavagli.m2block

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gzavagli.m2block.utils.AudioSynthManager
import com.gzavagli.m2block.utils.HapticManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.log2
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("m2block_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    val audioSynthManager = AudioSynthManager()
    val hapticManager = HapticManager(application)

    // Controls lock to prevent user inputs during animations
    internal var isAnimationLocked = false
    private var currentBlockAge = 0

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        val highScore = sharedPrefs.getInt("high_score", 0)
        val coins = sharedPrefs.getInt("coins", 200)
        val isMuted = sharedPrefs.getBoolean("muted", false)
        val isHaptics = sharedPrefs.getBoolean("haptics", true)
        val themeOrdinal = sharedPrefs.getInt("theme", GameTheme.CYBERPUNK.ordinal)
        val hasSavedGame = sharedPrefs.getBoolean("has_saved_game", false)
        
        audioSynthManager.isMuted = isMuted
        hapticManager.isEnabled = isHaptics

        _state.value = _state.value.copy(
            highScore = highScore,
            coins = coins,
            isMuted = isMuted,
            isHapticsEnabled = isHaptics,
            theme = GameTheme.values().getOrElse(themeOrdinal) { GameTheme.CYBERPUNK },
            hasActiveSavedGame = hasSavedGame
        )
    }

    private fun saveHighScore(score: Int) {
        if (score > _state.value.highScore) {
            _state.value = _state.value.copy(highScore = score)
            sharedPrefs.edit().putInt("high_score", score).apply()
        }
    }

    private fun saveCoins(coins: Int) {
        _state.value = _state.value.copy(coins = coins)
        sharedPrefs.edit().putInt("coins", coins).apply()
    }

    fun setMuted(muted: Boolean) {
        _state.value = _state.value.copy(isMuted = muted)
        audioSynthManager.isMuted = muted
        sharedPrefs.edit().putBoolean("muted", muted).apply()
    }

    fun setHapticsEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(isHapticsEnabled = enabled)
        hapticManager.isEnabled = enabled
        sharedPrefs.edit().putBoolean("haptics", enabled).apply()
    }

    fun selectTheme(theme: GameTheme) {
        _state.value = _state.value.copy(theme = theme)
        sharedPrefs.edit().putInt("theme", theme.ordinal).apply()
    }

    fun resetGameToHome() {
        _state.value = _state.value.copy(
            grid = emptyList(),
            score = 0,
            combo = 1,
            status = GameStatus.HOME,
            hasActiveSavedGame = false
        )
        isAnimationLocked = false
        clearSavedGameState()
    }

    fun startGame() {
        val startVal = getSpawnValue(2)
        val nextVal = getSpawnValue(startVal)
        _state.value = _state.value.copy(
            grid = emptyList(),
            score = 0,
            combo = 1,
            currentBlockValue = startVal,
            nextBlockValue = nextVal,
            status = GameStatus.PLAYING,
            hasActiveSavedGame = true
        )
        isAnimationLocked = false
        saveGameState()
    }

    fun pauseGame() {
        if (_state.value.status == GameStatus.PLAYING) {
            _state.value = _state.value.copy(status = GameStatus.PAUSED)
            saveGameState()
        }
    }

    fun resumeGame() {
        if (_state.value.status == GameStatus.PAUSED) {
            _state.value = _state.value.copy(status = GameStatus.PLAYING)
            saveGameState()
        }
    }

    // --- Core Game Loop Trigger ---
    fun dropBlockInColumn(col: Int) {
        if (isAnimationLocked || _state.value.status != GameStatus.PLAYING) return

        // 1. Calculate landing row
        var landingRow = getLowestEmptyRow(col)
        if (landingRow < 0) {
            val topmostBlock = _state.value.grid
                .filter { it.col == col && it.status != BlockStatus.MERGING }
                .maxByOrNull { it.row }
            if (topmostBlock != null && topmostBlock.value == _state.value.currentBlockValue) {
                landingRow = topmostBlock.row
            } else {
                // Column is full and cannot merge - check if game over
                checkGameOver()
                return
            }
        }

        // Calculate oldMinSpawn before we change currentBlockValue and grid
        val oldMaxBlock = _state.value.grid.maxOfOrNull { it.value } ?: 2
        val oldMinSpawn = getMinSpawnValue(oldMaxBlock)

        isAnimationLocked = true
        audioSynthManager.playDrop()

        val droppedValue = _state.value.currentBlockValue
        spawnNextBlock()

        viewModelScope.launch {
            // 2. Add block at bottom row (row 7, off-screen)
            currentBlockAge++
            val newBlock = BlockItem(
                row = 7,
                col = col,
                value = droppedValue,
                status = BlockStatus.DROPPING,
                targetRow = landingRow,
                age = currentBlockAge
            )

            val currentGrid = _state.value.grid.toMutableList()
            currentGrid.add(newBlock)
            _state.value = _state.value.copy(grid = currentGrid)

            // Yield to let the initial frame render at row 7
            delay(20)

            // Now set the target row to landingRow. This triggers the sliding up animation!
            val updatedGrid = _state.value.grid.map {
                if (it.id == newBlock.id) it.copy(row = landingRow) else it
            }
            _state.value = _state.value.copy(grid = updatedGrid)

            // Check if this drop will result in any merges
            val willMerge = findMergesOnBoard().isNotEmpty()
            val dropDelay = if (willMerge) 130 else 180
            delay(dropDelay.toLong())

            // 3. Settle block
            audioSynthManager.playLand()
            hapticManager.vibrateLand()

            val settledGrid = _state.value.grid.map {
                if (it.id == newBlock.id) it.copy(status = BlockStatus.IDLE) else it
            }
            _state.value = _state.value.copy(grid = settledGrid)

            // 4. Run Merging Loop
            runMergeAndGravityCascade(col, landingRow, oldMinSpawn)
        }
    }

    private fun getLowestEmptyRow(col: Int): Int {
        val occupiedRows = _state.value.grid
            .filter { it.col == col && it.status != BlockStatus.MERGING }
            .map { it.row }
        for (r in 0..6) {
            if (r !in occupiedRows) return r
        }
        return -1
    }

    // Recursive cascade handling merges and gravity falls
    internal suspend fun runMergeAndGravityCascade(landingCol: Int, landingRow: Int, oldMinSpawn: Int) {
        var comboCount = 1
        var needsCheck = true

        while (needsCheck) {
            val merges = findMergesOnBoard()
            if (merges.isEmpty()) {
                needsCheck = false
                break
            }

            // Animate Merges (mark blocks for merging and set target offsets)
            animateMerges(merges)
            delay(150) // wait for merge animation to complete

            // Apply Merge math (sum values, add scores, give coins)
            resolveMerges(merges, comboCount)
            comboCount++

            // Apply Gravity (float down any blocks suspended in air)
            val gravityApplied = applyGravity()
            if (gravityApplied) {
                delay(180) // wait for gravity drop animation
            } else {
                delay(150) // wait for pop animation to finish
            }
            resetBlockStatuses()
        }

        // Cascade finished. Now check if the phase progressed!
        val newMaxBlock = _state.value.grid.maxOfOrNull { it.value } ?: 2
        val newMinSpawn = getMinSpawnValue(newMaxBlock)

        if (newMinSpawn > oldMinSpawn) {
            // Remove under-valued blocks!
            val blocksRemoved = removeBlocksLessThan(newMinSpawn)
            if (blocksRemoved) {
                // Wait for any pop/removal animations if we want, or just wait a bit
                delay(200)

                // Since blocks were removed, we must apply gravity to let blocks fall
                val gravityApplied = applyGravity()
                if (gravityApplied) {
                    delay(180)
                }
                resetBlockStatuses()

                // Removing blocks and dropping them might trigger new merges!
                // Recursively run cascade for any new merges
                runMergeAndGravityCascade(landingCol, landingRow, oldMinSpawn)
                return
            }
        }

        // Cascade finished, unlock input and check game over
        isAnimationLocked = false
        checkGameOver()
        if (_state.value.status != GameStatus.GAMEOVER) {
            saveGameState()
        }
    }

    internal data class MergePair(val sourceBlockId: String, val targetBlockId: String)

    internal fun findMergesOnBoard(): List<MergePair> {
        val grid = _state.value.grid.filter { it.status != BlockStatus.MERGING }
        val merges = mutableListOf<MergePair>()
        val sourceIds = mutableSetOf<String>()
        val targetIds = mutableSetOf<String>()

        // Scan the board top-down to prioritize merging upwards
        for (row in 0..6) {
            for (col in 0 until 5) {
                val block = grid.find { it.row == row && it.col == col } ?: continue
                if (block.id in sourceIds) continue

                // Check neighbors: Up, Down, Left, Right
                val neighbors = listOf(
                    grid.find { it.row == row - 1 && it.col == col }, // Up
                    grid.find { it.row == row && it.col == col + 1 }, // Right
                    grid.find { it.row == row && it.col == col - 1 }, // Left
                    grid.find { it.row == row + 1 && it.col == col }  // Down
                ).filterNotNull()

                val overlapping = grid.filter { it.id != block.id && it.row == row && it.col == col && it.id !in sourceIds }
                val match = overlapping.firstOrNull { it.value == block.value } ?: neighbors.firstOrNull { it.value == block.value && it.id !in sourceIds }
                if (match != null) {
                    // Merge source into target (prefer target as the newer block with higher age)
                    val (source, target) = if (match.age >= block.age) {
                        Pair(block, match)
                    } else {
                        Pair(match, block)
                    }

                    if (source.id !in targetIds && target.id !in sourceIds) {
                        merges.add(MergePair(source.id, target.id))
                        sourceIds.add(source.id)
                        targetIds.add(target.id)
                    }
                }
            }
        }
        return merges
    }

    private fun animateMerges(merges: List<MergePair>) {
        val currentGrid = _state.value.grid.toMutableList()
        merges.forEach { merge ->
            val sourceIdx = currentGrid.indexOfFirst { it.id == merge.sourceBlockId }
            val targetIdx = currentGrid.indexOfFirst { it.id == merge.targetBlockId }
            if (sourceIdx != -1 && targetIdx != -1) {
                val source = currentGrid[sourceIdx]
                val target = currentGrid[targetIdx]
                currentGrid[sourceIdx] = source.copy(
                    status = BlockStatus.MERGING,
                    targetRow = target.row,
                    targetCol = target.col
                )
            }
        }
        _state.value = _state.value.copy(grid = currentGrid)
    }

    internal fun resolveMerges(merges: List<MergePair>, combo: Int) {
        val currentGrid = _state.value.grid.toMutableList()
        var totalGain = 0
        var coinGain = 0

        val mergesByTarget = merges.groupBy { it.targetBlockId }
        mergesByTarget.forEach { (targetId, pairs) ->
            val targetIdx = currentGrid.indexOfFirst { it.id == targetId }
            if (targetIdx != -1) {
                val target = currentGrid[targetIdx]
                val totalBlocks = pairs.size + 1
                val multiplier = Math.pow(2.0, (totalBlocks - 1).toDouble()).toInt()
                val newValue = target.value * multiplier

                currentBlockAge++
                currentGrid[targetIdx] = target.copy(
                    value = newValue,
                    status = BlockStatus.NEW,
                    age = currentBlockAge
                )

                totalGain += newValue
                coinGain += (log2(newValue.toDouble()).toInt()).coerceAtLeast(1)
            }

            pairs.forEach { pair ->
                val sourceIdx = currentGrid.indexOfFirst { it.id == pair.sourceBlockId }
                if (sourceIdx != -1) {
                    currentGrid.removeAt(sourceIdx)
                }
            }
        }

        audioSynthManager.playMerge(combo)
        hapticManager.vibrateMerge()

        val newScore = _state.value.score + totalGain
        saveHighScore(newScore)
        saveCoins(_state.value.coins + coinGain)
        
        _state.value = _state.value.copy(
            grid = currentGrid,
            score = newScore,
            combo = combo
        )
    }

    private fun applyGravity(): Boolean {
        var movedAny = false
        val currentGrid = _state.value.grid.toMutableList()

        for (col in 0 until 5) {
            var highestEmptyRow = 0
            for (row in 0..6) {
                val blockIdx = currentGrid.indexOfFirst { it.row == row && it.col == col && it.status != BlockStatus.MERGING }
                if (blockIdx != -1) {
                    val block = currentGrid[blockIdx]
                    if (row > highestEmptyRow) {
                        currentGrid[blockIdx] = block.copy(
                            row = highestEmptyRow,
                            status = BlockStatus.DROPPING,
                            targetRow = highestEmptyRow
                        )
                        movedAny = true
                    }
                    highestEmptyRow++
                }
            }
        }

        if (movedAny) {
            _state.value = _state.value.copy(grid = currentGrid)
        }
        return movedAny
    }

    private fun resetBlockStatuses() {
        val grid = _state.value.grid.map { it.copy(status = BlockStatus.IDLE) }
        _state.value = _state.value.copy(grid = grid)
    }

    private fun spawnNextBlock() {
        val maxVal = _state.value.grid.maxOfOrNull { it.value } ?: 2
        val nextVal = getSpawnValue(maxVal)
        _state.value = _state.value.copy(
            currentBlockValue = _state.value.nextBlockValue,
            nextBlockValue = nextVal
        )
    }

    private fun checkGameOver() {
        // Game over if any column is full down to row 6 (bottom), and we cannot perform any more merges
        val row6OccupiedCount = _state.value.grid.filter { it.row == 6 && it.status != BlockStatus.MERGING }.size
        if (row6OccupiedCount >= 5 && findMergesOnBoard().isEmpty()) {
            _state.value = _state.value.copy(
                status = GameStatus.GAMEOVER,
                hasActiveSavedGame = false
            )
            audioSynthManager.playGameOver()
            hapticManager.vibrateGameOver()
            clearSavedGameState()
        }
    }

    // --- Progressive Random Block Spawn ---
    private fun getSpawnValue(maxBlock: Int): Int {
        val rand = Random.nextInt(100)
        val p = log2(maxBlock.toDouble()).toInt()
        
        return if (p < 7) {
            val exponent = if (rand < 40) 1 else if (rand < 75) 2 else if (rand < 90) 3 else 4
            Math.pow(2.0, exponent.toDouble()).toInt()
        } else {
            val minExp = 1 + (p - 7) / 2
            val offset = if (rand < 35) 0 else if (rand < 65) 1 else if (rand < 85) 2 else if (rand < 95) 3 else 4
            Math.pow(2.0, (minExp + offset).toDouble()).toInt()
        }
    }

    // --- Power-up Actions & Cost Checks ---
    fun activateHammerMode() {
        if (_state.value.coins >= 100 && !isAnimationLocked) {
            _state.value = _state.value.copy(status = GameStatus.TARGETING_HAMMER)
        }
    }

    fun activateVortexMode() {
        if (_state.value.coins >= 150 && !isAnimationLocked) {
            _state.value = _state.value.copy(status = GameStatus.TARGETING_VORTEX)
        }
    }

    fun activateDoubleMode() {
        if (_state.value.coins >= 120 && !isAnimationLocked) {
            _state.value = _state.value.copy(status = GameStatus.TARGETING_DOUBLE)
        }
    }

    fun useSwapPowerup() {
        if (_state.value.coins >= 50 && !isAnimationLocked) {
            audioSynthManager.playPowerup()
            hapticManager.vibratePowerup()
            val temp = _state.value.currentBlockValue
            _state.value = _state.value.copy(
                currentBlockValue = _state.value.nextBlockValue,
                nextBlockValue = temp
            )
            saveCoins(_state.value.coins - 50)
            saveGameState()
        }
    }

    fun cancelTargetingMode() {
        if (_state.value.status in listOf(GameStatus.TARGETING_HAMMER, GameStatus.TARGETING_VORTEX, GameStatus.TARGETING_DOUBLE)) {
            _state.value = _state.value.copy(status = GameStatus.PLAYING)
        }
    }

    fun handleBlockTargetClick(block: BlockItem) {
        val currentStatus = _state.value.status
        if (isAnimationLocked) return

        if (currentStatus == GameStatus.TARGETING_HAMMER) {
            isAnimationLocked = true
            saveCoins(_state.value.coins - 100)
            audioSynthManager.playPowerup()
            hapticManager.vibratePowerup()

            val oldMaxBlock = _state.value.grid.maxOfOrNull { it.value } ?: 2
            val oldMinSpawn = getMinSpawnValue(oldMaxBlock)

            viewModelScope.launch {
                val currentGrid = _state.value.grid.filter { it.id != block.id }
                _state.value = _state.value.copy(grid = currentGrid, status = GameStatus.PLAYING)
                
                applyGravity()
                delay(180)
                resetBlockStatuses()
                runMergeAndGravityCascade(block.col, block.row, oldMinSpawn)
            }
        } else if (currentStatus == GameStatus.TARGETING_DOUBLE) {
            isAnimationLocked = true
            saveCoins(_state.value.coins - 120)
            audioSynthManager.playPowerup()
            hapticManager.vibratePowerup()

            val oldMaxBlock = _state.value.grid.maxOfOrNull { it.value } ?: 2
            val oldMinSpawn = getMinSpawnValue(oldMaxBlock)

            viewModelScope.launch {
                val currentGrid = _state.value.grid.map {
                    if (it.id == block.id) {
                        currentBlockAge++
                        it.copy(value = it.value * 2, status = BlockStatus.NEW, age = currentBlockAge)
                    } else it
                }
                _state.value = _state.value.copy(grid = currentGrid, status = GameStatus.PLAYING)
                
                delay(150)
                resetBlockStatuses()
                runMergeAndGravityCascade(block.col, block.row, oldMinSpawn)
            }
        }
    }

    fun handleColumnTargetClick(col: Int) {
        if (_state.value.status == GameStatus.TARGETING_VORTEX && !isAnimationLocked) {
            isAnimationLocked = true
            saveCoins(_state.value.coins - 150)
            audioSynthManager.playPowerup()
            hapticManager.vibratePowerup()

            val oldMaxBlock = _state.value.grid.maxOfOrNull { it.value } ?: 2
            val oldMinSpawn = getMinSpawnValue(oldMaxBlock)

            viewModelScope.launch {
                val currentGrid = _state.value.grid.filter { it.col != col }
                _state.value = _state.value.copy(grid = currentGrid, status = GameStatus.PLAYING)
                
                applyGravity()
                delay(180)
                resetBlockStatuses()
                
                // Sweep through adjacent columns for potential merges
                runMergeAndGravityCascade(col, 6, oldMinSpawn)
            }
        }
    }

    fun useRevive() {
        if (_state.value.coins >= 150 && _state.value.status == GameStatus.GAMEOVER) {
            saveCoins(_state.value.coins - 150)
            audioSynthManager.playPowerup()
            hapticManager.vibratePowerup()
            
            // Delete the bottom two rows (row 5 and 6)
            val currentGrid = _state.value.grid.filter { it.row < 5 }
            _state.value = _state.value.copy(grid = currentGrid, status = GameStatus.PLAYING)
            
            val oldMaxBlock = currentGrid.maxOfOrNull { it.value } ?: 2
            val oldMinSpawn = getMinSpawnValue(oldMaxBlock)

            viewModelScope.launch {
                isAnimationLocked = true
                applyGravity()
                delay(180)
                resetBlockStatuses()
                runMergeAndGravityCascade(2, 6, oldMinSpawn)
            }
        }
    }

    internal fun getMinSpawnValue(maxBlock: Int): Int {
        val p = log2(maxBlock.toDouble()).toInt()
        val minExp = if (p < 7) 1 else 1 + (p - 7) / 2
        return Math.pow(2.0, minExp.toDouble()).toInt()
    }

    internal suspend fun removeBlocksLessThan(threshold: Int): Boolean {
        val currentGrid = _state.value.grid
        val blocksToRemove = currentGrid.filter { it.value < threshold }
        if (blocksToRemove.isEmpty()) return false

        val animatingGrid = currentGrid.map {
            if (it.value < threshold) {
                it.copy(status = BlockStatus.MERGING, targetRow = it.row, targetCol = it.col)
            } else {
                it
            }
        }
        _state.value = _state.value.copy(grid = animatingGrid)

        delay(150)

        val cleanedGrid = _state.value.grid.filter { it.value >= threshold }
        _state.value = _state.value.copy(grid = cleanedGrid)

        return true
    }

    // --- State Persistence Helpers ---
    private fun serializeGrid(grid: List<BlockItem>): String {
        return grid.joinToString(";") { block ->
            "${block.id}:${block.row}:${block.col}:${block.value}:${block.age}"
        }
    }

    private fun deserializeGrid(gridStr: String?): List<BlockItem> {
        if (gridStr.isNullOrEmpty()) return emptyList()
        return try {
            gridStr.split(";").map { item ->
                val parts = item.split(":")
                BlockItem(
                    id = parts[0],
                    row = parts[1].toInt(),
                    col = parts[2].toInt(),
                    value = parts[3].toInt(),
                    status = BlockStatus.IDLE,
                    age = parts[4].toInt()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveGameState() {
        val gridStr = serializeGrid(_state.value.grid)
        sharedPrefs.edit()
            .putBoolean("has_saved_game", true)
            .putString("saved_grid", gridStr)
            .putInt("saved_current_value", _state.value.currentBlockValue)
            .putInt("saved_next_value", _state.value.nextBlockValue)
            .putInt("saved_score", _state.value.score)
            .putInt("saved_combo", _state.value.combo)
            .putString("saved_status", _state.value.status.name)
            .apply()
    }

    private fun clearSavedGameState() {
        sharedPrefs.edit()
            .putBoolean("has_saved_game", false)
            .remove("saved_grid")
            .remove("saved_current_value")
            .remove("saved_next_value")
            .remove("saved_score")
            .remove("saved_combo")
            .remove("saved_status")
            .apply()
    }

    fun resumeSavedGame() {
        if (!sharedPrefs.getBoolean("has_saved_game", false)) return

        val gridStr = sharedPrefs.getString("saved_grid", "") ?: ""
        val grid = deserializeGrid(gridStr)
        val currentVal = sharedPrefs.getInt("saved_current_value", 2)
        val nextVal = sharedPrefs.getInt("saved_next_value", 4)
        val score = sharedPrefs.getInt("saved_score", 0)
        val combo = sharedPrefs.getInt("saved_combo", 1)
        val statusStr = sharedPrefs.getString("saved_status", GameStatus.PLAYING.name) ?: GameStatus.PLAYING.name
        val status = try {
            GameStatus.valueOf(statusStr)
        } catch (e: Exception) {
            GameStatus.PLAYING
        }

        _state.value = _state.value.copy(
            grid = grid,
            currentBlockValue = currentVal,
            nextBlockValue = nextVal,
            score = score,
            combo = combo,
            status = if (status == GameStatus.PAUSED) GameStatus.PAUSED else GameStatus.PLAYING,
            hasActiveSavedGame = true
        )
        currentBlockAge = grid.maxOfOrNull { it.age } ?: 0
        isAnimationLocked = false
    }

    internal fun setupGridForTesting(grid: List<BlockItem>, age: Int) {
        _state.value = _state.value.copy(grid = grid)
        currentBlockAge = age
    }

    internal fun setupStateForTesting(
        grid: List<BlockItem>,
        currentVal: Int,
        nextVal: Int,
        score: Int,
        combo: Int,
        status: GameStatus
    ) {
        _state.value = _state.value.copy(
            grid = grid,
            currentBlockValue = currentVal,
            nextBlockValue = nextVal,
            score = score,
            combo = combo,
            status = status
        )
    }
}
