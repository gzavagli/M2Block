package com.gzavagli.m2block

import android.app.Application
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.gzavagli.m2block.ui.theme.getThemeColors
import com.gzavagli.m2block.ui.theme.getBlockColors


class MockSharedPreferences : SharedPreferences {
    val map = mutableMapOf<String, Any>(
        "muted" to true,
        "haptics" to false
    )

    override fun getAll(): Map<String, *> = map
    override fun getString(key: String, defValue: String?): String? = map[key] as? String ?: defValue
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? = map[key] as? Set<String> ?: defValues
    override fun getInt(key: String, defValue: Int): Int = map[key] as? Int ?: defValue
    override fun getLong(key: String, defValue: Long): Long = map[key] as? Long ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = map[key] as? Float ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
    override fun contains(key: String): Boolean = map.containsKey(key)
    override fun edit(): SharedPreferences.Editor = Editor(this)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    class Editor(private val prefs: MockSharedPreferences) : SharedPreferences.Editor {
        private val tempMap = mutableMapOf<String, Any>()

        override fun putString(key: String, value: String?): SharedPreferences.Editor { value?.let { tempMap[key] = it }; return this }
        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor { values?.let { tempMap[key] = it }; return this }
        override fun putInt(key: String, value: Int): SharedPreferences.Editor { tempMap[key] = value; return this }
        override fun putLong(key: String, value: Long): SharedPreferences.Editor { tempMap[key] = value; return this }
        override fun putFloat(key: String, value: Float): SharedPreferences.Editor { tempMap[key] = value; return this }
        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor { tempMap[key] = value; return this }
        override fun remove(key: String): SharedPreferences.Editor { tempMap.remove(key); return this }
        override fun clear(): SharedPreferences.Editor { tempMap.clear(); return this }
        override fun commit(): Boolean { prefs.map.putAll(tempMap); return true }
        override fun apply() { prefs.map.putAll(tempMap) }
    }
}

class MockApplication(private val prefs: SharedPreferences) : Application() {
    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
}

class GameEngineTest {

    @Test
    fun testInitialState() {
        val prefs = MockSharedPreferences()
        // Put some starting preferences
        prefs.edit().putInt("high_score", 5000).putInt("coins", 350).apply()

        val app = MockApplication(prefs)
        val viewModel = GameViewModel(app)

        val state = viewModel.state.value
        assertEquals(5000, state.highScore)
        assertEquals(350, state.coins)
        assertEquals(GameTheme.CYBERPUNK, state.theme)
        assertEquals(GameStatus.HOME, state.status)
    }

    @Test
    fun testSettingsToggles() {
        val prefs = MockSharedPreferences()
        val app = MockApplication(prefs)
        val viewModel = GameViewModel(app)

        viewModel.setMuted(true)
        assertTrue(viewModel.state.value.isMuted)
        assertEquals(true, prefs.getBoolean("muted", false))

        viewModel.setHapticsEnabled(false)
        assertEquals(false, viewModel.state.value.isHapticsEnabled)
        assertEquals(false, prefs.getBoolean("haptics", true))
    }

    @Test
    fun testGameStart() {
        val prefs = MockSharedPreferences()
        val app = MockApplication(prefs)
        val viewModel = GameViewModel(app)

        viewModel.startGame()
        val state = viewModel.state.value
        assertEquals(GameStatus.PLAYING, state.status)
        assertEquals(0, state.score)
        assertTrue(state.grid.isEmpty())
        assertTrue(state.currentBlockValue in listOf(2, 4, 8, 16))
        assertTrue(state.nextBlockValue in listOf(2, 4, 8, 16))
    }

    @Test
    fun testMergingGoesToNewerBlock() {
        val prefs = MockSharedPreferences()
        val app = MockApplication(prefs)
        val viewModel = GameViewModel(app)

        // Scenario 1: block1 (older, age=10) at (2,2), block2 (newer, age=12) at (2,3)
        // Expected: source = block1 (older), target = block2 (newer)
        val block1 = BlockItem(row = 2, col = 2, value = 4, age = 10)
        val block2 = BlockItem(row = 2, col = 3, value = 4, age = 12)
        viewModel.setupGridForTesting(listOf(block1, block2), 12)

        val merges = viewModel.findMergesOnBoard()
        assertEquals(1, merges.size)
        assertEquals(block1.id, merges[0].sourceBlockId)
        assertEquals(block2.id, merges[0].targetBlockId)

        // Scenario 2: block3 (newer, age=15) at (2,2), block4 (older, age=11) at (2,3)
        // Expected: source = block4 (older), target = block3 (newer)
        val block3 = BlockItem(row = 2, col = 2, value = 4, age = 15)
        val block4 = BlockItem(row = 2, col = 3, value = 4, age = 11)
        viewModel.setupGridForTesting(listOf(block3, block4), 15)

        val merges2 = viewModel.findMergesOnBoard()
        assertEquals(1, merges2.size)
        assertEquals(block4.id, merges2[0].sourceBlockId)
        assertEquals(block3.id, merges2[0].targetBlockId)
    }

    @Test
    fun testThreeBlocksMergeScore() {
        val prefs = MockSharedPreferences()
        val app = MockApplication(prefs)
        val viewModel = GameViewModel(app)

        // target block (value = 4) at (2,2)
        val target = BlockItem(id = "target", row = 2, col = 2, value = 4, age = 15)
        // source 1 (value = 4) at (2,1)
        val source1 = BlockItem(id = "source1", row = 2, col = 1, value = 4, age = 10)
        // source 2 (value = 4) at (2,3)
        val source2 = BlockItem(id = "source2", row = 2, col = 3, value = 4, age = 11)

        viewModel.setupGridForTesting(listOf(target, source1, source2), 15)

        val merges = listOf(
            GameViewModel.MergePair(source1.id, target.id),
            GameViewModel.MergePair(source2.id, target.id)
        )

        viewModel.resolveMerges(merges, 1)

        val grid = viewModel.state.value.grid
        assertEquals(1, grid.size)
        assertEquals(16, grid[0].value)
        assertEquals(16, viewModel.state.value.score)

        // Scenario 2: target block (value = 2) at (2,2)
        val target2 = BlockItem(id = "target2", row = 2, col = 2, value = 2, age = 15)
        val source3 = BlockItem(id = "source3", row = 2, col = 1, value = 2, age = 10)
        val source4 = BlockItem(id = "source4", row = 2, col = 3, value = 2, age = 11)

        viewModel.setupGridForTesting(listOf(target2, source3, source4), 15)
        val merges2 = listOf(
            GameViewModel.MergePair(source3.id, target2.id),
            GameViewModel.MergePair(source4.id, target2.id)
        )
        // Reset score
        viewModel.startGame() // StartGame resets score and grid
        viewModel.setupGridForTesting(listOf(target2, source3, source4), 15)

        viewModel.resolveMerges(merges2, 1)

        val grid2 = viewModel.state.value.grid
        assertEquals(1, grid2.size)
        assertEquals(8, grid2[0].value)
        assertEquals(8, viewModel.state.value.score)

        // Scenario 3: 4 blocks of value 2 merge
        // target (value = 2) at (2,2)
        val target3 = BlockItem(id = "target3", row = 2, col = 2, value = 2, age = 15)
        val s5 = BlockItem(id = "s5", row = 2, col = 1, value = 2, age = 10)
        val s6 = BlockItem(id = "s6", row = 2, col = 3, value = 2, age = 11)
        val s7 = BlockItem(id = "s7", row = 1, col = 2, value = 2, age = 12)

        viewModel.startGame()
        viewModel.setupGridForTesting(listOf(target3, s5, s6, s7), 15)
        val merges3 = listOf(
            GameViewModel.MergePair(s5.id, target3.id),
            GameViewModel.MergePair(s6.id, target3.id),
            GameViewModel.MergePair(s7.id, target3.id)
        )

        viewModel.resolveMerges(merges3, 1)

        val grid3 = viewModel.state.value.grid
        assertEquals(1, grid3.size)
        assertEquals(16, grid3[0].value) // 2^4 = 16
        assertEquals(16, viewModel.state.value.score)
    }

    @Test
    fun testPhaseProgressionRemovesLowValueBlocks() = kotlinx.coroutines.runBlocking {
        val prefs = MockSharedPreferences()
        val app = MockApplication(prefs)
        val viewModel = GameViewModel(app)

        val block1 = BlockItem(id = "b1", row = 2, col = 2, value = 2, age = 10)
        val block2 = BlockItem(id = "b2", row = 3, col = 2, value = 4, age = 12)

        viewModel.setupGridForTesting(listOf(block1, block2), 12)

        val removed = viewModel.removeBlocksLessThan(4)
        assertTrue(removed)

        val grid = viewModel.state.value.grid
        assertEquals(1, grid.size)
        assertEquals("b2", grid[0].id)
        assertEquals(4, grid[0].value)
    }

    @Test
    fun testMergeDoesNotAdvancePreviewTwice() = kotlinx.coroutines.runBlocking {
        val prefs = MockSharedPreferences()
        val app = MockApplication(prefs)
        val viewModel = GameViewModel(app)

        viewModel.startGame()
        val state1 = viewModel.state.value
        val curVal = state1.currentBlockValue
        val nextVal = state1.nextBlockValue

        // Now place two identical blocks that can merge, e.g. value = 4
        val block1 = BlockItem(row = 2, col = 2, value = 4, age = 10)
        val block2 = BlockItem(row = 2, col = 3, value = 4, age = 12)
        viewModel.setupGridForTesting(listOf(block1, block2), 12)

        // Call cascade
        val oldMax = viewModel.state.value.grid.maxOfOrNull { it.value } ?: 2
        viewModel.runMergeAndGravityCascade(2, 3, viewModel.getMinSpawnValue(oldMax))

        // Under the new design, runMergeAndGravityCascade does NOT advance the preview,
        // so currentBlockValue remains curVal.
        assertEquals(curVal, viewModel.state.value.currentBlockValue)
    }

    @Test
    fun testMergeWithPhaseProgressionDoesNotAdvancePreviewTwice() = kotlinx.coroutines.runBlocking {
        val prefs = MockSharedPreferences()
        val app = MockApplication(prefs)
        val viewModel = GameViewModel(app)

        viewModel.startGame()
        val state1 = viewModel.state.value
        val curVal = state1.currentBlockValue
        val nextVal = state1.nextBlockValue

        // To trigger phase progression (newMinSpawn > oldMinSpawn):
        // oldMaxBlock = 256 -> oldMinSpawn = 2
        // newMaxBlock = 512 -> newMinSpawn = 4
        // We need:
        // - block1 (256) at (2,2)
        // - block2 (256) at (2,3)
        // - block3 (2) at (1,1) -> this will be removed by phase progression
        val block1 = BlockItem(row = 2, col = 2, value = 256, age = 10)
        val block2 = BlockItem(row = 2, col = 3, value = 256, age = 12)
        val block3 = BlockItem(row = 1, col = 1, value = 2, age = 5)
        viewModel.setupGridForTesting(listOf(block1, block2, block3), 12)

        // Call cascade
        val oldMax = viewModel.state.value.grid.maxOfOrNull { it.value } ?: 2
        viewModel.runMergeAndGravityCascade(2, 3, viewModel.getMinSpawnValue(oldMax))

        // The phase progression should have removed block3 (2)
        // And the preview should NOT have advanced (currentBlockValue remains curVal)
        assertEquals(curVal, viewModel.state.value.currentBlockValue)
    }

    @Test
    fun testDropBlockInColumnAdvancesPreview() {
        val prefs = MockSharedPreferences()
        val app = MockApplication(prefs)
        val viewModel = GameViewModel(app)

        viewModel.startGame()
        val state1 = viewModel.state.value
        val curVal = state1.currentBlockValue
        val nextVal = state1.nextBlockValue

        // Drop block
        try {
            viewModel.dropBlockInColumn(2)
        } catch (e: Exception) {
            // If viewModelScope launch throws due to missing Main dispatcher,
            // we catch it, but spawnNextBlock should have already run synchronously.
        }

        // Preview should have advanced immediately to nextVal
        assertEquals(nextVal, viewModel.state.value.currentBlockValue)
    }

    @Test
    fun testStartGameSavesState() {
        val prefs = MockSharedPreferences()
        val app = MockApplication(prefs)
        val viewModel = GameViewModel(app)

        viewModel.startGame()
        assertTrue(prefs.getBoolean("has_saved_game", false))
        assertTrue(viewModel.state.value.hasActiveSavedGame)
        assertEquals(viewModel.state.value.score, prefs.getInt("saved_score", -1))
        assertEquals(viewModel.state.value.currentBlockValue, prefs.getInt("saved_current_value", -1))
        assertEquals(viewModel.state.value.nextBlockValue, prefs.getInt("saved_next_value", -1))
    }

    @Test
    fun testResetGameClearsState() {
        val prefs = MockSharedPreferences()
        val app = MockApplication(prefs)
        val viewModel = GameViewModel(app)

        viewModel.startGame()
        assertTrue(prefs.getBoolean("has_saved_game", false))

        viewModel.resetGameToHome()
        assertFalse(prefs.getBoolean("has_saved_game", true))
        assertFalse(viewModel.state.value.hasActiveSavedGame)
    }

    @Test
    fun testResumeSavedGameRestoresCorrectly() {
        val prefs = MockSharedPreferences()
        val app = MockApplication(prefs)
        
        // Let's prepopulate prefs with a saved game
        prefs.edit()
            .putBoolean("has_saved_game", true)
            .putString("saved_grid", "b1:2:2:4:10;b2:2:3:8:15")
            .putInt("saved_current_value", 16)
            .putInt("saved_next_value", 32)
            .putInt("saved_score", 120)
            .putInt("saved_combo", 3)
            .putString("saved_status", GameStatus.PLAYING.name)
            .apply()

        val viewModel = GameViewModel(app) // will loadPreferences() -> hasActiveSavedGame = true
        assertTrue(viewModel.state.value.hasActiveSavedGame)
        assertEquals(GameStatus.HOME, viewModel.state.value.status) // initially HOME until resumed

        viewModel.resumeSavedGame()
        val state = viewModel.state.value
        assertEquals(GameStatus.PLAYING, state.status)
        assertEquals(120, state.score)
        assertEquals(3, state.combo)
        assertEquals(16, state.currentBlockValue)
        assertEquals(32, state.nextBlockValue)
        assertEquals(2, state.grid.size)

        val b1 = state.grid.find { it.id == "b1" }
        val b2 = state.grid.find { it.id == "b2" }
        org.junit.Assert.assertNotNull(b1)
        org.junit.Assert.assertNotNull(b2)
        assertEquals(2, b1!!.row)
        assertEquals(2, b1.col)
        assertEquals(4, b1.value)
        assertEquals(10, b1.age)

        assertEquals(2, b2!!.row)
        assertEquals(3, b2.col)
        assertEquals(8, b2.value)
        assertEquals(15, b2.age)
    }

    @Test
    fun testCascadeWithPowerupTriggersPhaseProgression() = kotlinx.coroutines.runBlocking {
        val prefs = MockSharedPreferences()
        val app = MockApplication(prefs)
        val viewModel = GameViewModel(app)

        val block1 = BlockItem(id = "b1", row = 2, col = 2, value = 256, age = 10)
        val block2 = BlockItem(id = "b2", row = 1, col = 1, value = 2, age = 5)
        viewModel.setupGridForTesting(listOf(block1, block2), 10)

        val oldMax = viewModel.state.value.grid.maxOfOrNull { it.value } ?: 2
        val oldMinSpawn = viewModel.getMinSpawnValue(oldMax)

        val doubledGrid = viewModel.state.value.grid.map {
            if (it.id == "b1") it.copy(value = 512) else it
        }
        viewModel.setupGridForTesting(doubledGrid, 11)

        viewModel.runMergeAndGravityCascade(2, 2, oldMinSpawn)

        val grid = viewModel.state.value.grid
        assertEquals(1, grid.size)
        assertEquals("b1", grid[0].id)
        assertEquals(512, grid[0].value)
    }

    @Test
    fun testBlockColorRotation() {
        val themeColors = getThemeColors(GameTheme.CYBERPUNK)
        val colors2 = themeColors.getBlockColors(2)
        val colors2048 = themeColors.getBlockColors(2048)

        val colors4096 = themeColors.getBlockColors(4096)
        assertEquals(colors2, colors4096)

        val colors8192 = themeColors.getBlockColors(8192)
        val colors4 = themeColors.getBlockColors(4)
        assertEquals(colors4, colors8192)
    }

    @Test
    fun testOverlappingBlocksMerge() = kotlinx.coroutines.runBlocking {
        val prefs = MockSharedPreferences()
        val app = MockApplication(prefs)
        val viewModel = GameViewModel(app)

        val block0 = BlockItem(id = "b0", row = 0, col = 2, value = 4, age = 1)
        val block1 = BlockItem(id = "b1", row = 1, col = 2, value = 4, age = 2)
        val block2 = BlockItem(id = "b2", row = 2, col = 2, value = 4, age = 3)
        val block3 = BlockItem(id = "b3", row = 3, col = 2, value = 4, age = 4)
        val block4 = BlockItem(id = "b4", row = 4, col = 2, value = 4, age = 5)
        val block5 = BlockItem(id = "b5", row = 5, col = 2, value = 4, age = 6)
        val block6 = BlockItem(id = "b6", row = 6, col = 2, value = 8, age = 7)
        val block7 = BlockItem(id = "b7", row = 6, col = 2, value = 8, age = 8, status = BlockStatus.DROPPING)

        viewModel.setupGridForTesting(listOf(block0, block1, block2, block3, block4, block5, block6, block7), 8)

        val oldMax = 8
        val oldMinSpawn = viewModel.getMinSpawnValue(oldMax)

        viewModel.runMergeAndGravityCascade(2, 6, oldMinSpawn)

        val grid = viewModel.state.value.grid
        assertEquals(3, grid.size)

        val targetBlock3 = grid.find { it.id == "b3" }
        val targetBlock5 = grid.find { it.id == "b5" }
        val targetBlock7 = grid.find { it.id == "b7" }

        org.junit.Assert.assertNotNull(targetBlock3)
        assertEquals(0, targetBlock3!!.row)
        assertEquals(16, targetBlock3.value)

        org.junit.Assert.assertNotNull(targetBlock5)
        assertEquals(1, targetBlock5!!.row)
        assertEquals(8, targetBlock5.value)

        org.junit.Assert.assertNotNull(targetBlock7)
        assertEquals(2, targetBlock7!!.row)
        assertEquals(16, targetBlock7.value)
    }

    @Test
    fun testDropBlockInFullColumnAllowsMatchingAndRejectsNonMatching() {
        val prefs = MockSharedPreferences()
        val app = MockApplication(prefs)
        val viewModel = GameViewModel(app)

        val blocks = listOf(
            BlockItem(row = 0, col = 2, value = 4, age = 1),
            BlockItem(row = 1, col = 2, value = 4, age = 2),
            BlockItem(row = 2, col = 2, value = 4, age = 3),
            BlockItem(row = 3, col = 2, value = 4, age = 4),
            BlockItem(row = 4, col = 2, value = 4, age = 5),
            BlockItem(row = 5, col = 2, value = 4, age = 6),
            BlockItem(row = 6, col = 2, value = 8, age = 7)
        )

        // 1. Try with NON-matching currentBlockValue = 4
        viewModel.setupStateForTesting(
            grid = blocks,
            currentVal = 4,
            nextVal = 16,
            score = 0,
            combo = 1,
            status = GameStatus.PLAYING
        )

        try {
            viewModel.dropBlockInColumn(2)
        } catch (e: Exception) {}

        assertFalse(viewModel.isAnimationLocked)

        // 2. Try with MATCHING currentBlockValue = 8
        viewModel.setupStateForTesting(
            grid = blocks,
            currentVal = 8,
            nextVal = 16,
            score = 0,
            combo = 1,
            status = GameStatus.PLAYING
        )

        try {
            viewModel.dropBlockInColumn(2)
        } catch (e: Exception) {}

        assertTrue(viewModel.isAnimationLocked)
    }

    @Test
    fun testMysteryPhaseTriggerEventually() {
        val prefs = MockSharedPreferences()
        val app = MockApplication(prefs)
        val viewModel = GameViewModel(app)
        viewModel.startGame()

        var triggered = false
        // Simulate drops
        for (i in 0 until 100) {
            viewModel.isAnimationLocked = false // bypass animation lock for drop simulation
            try {
                viewModel.dropBlockInColumn(0)
            } catch (e: Exception) {}
            if (viewModel.state.value.isNextBlockHidden) {
                triggered = true
                assertTrue(viewModel.state.value.hiddenMovesRemaining in 3..5)
                break
            }
        }
        assertTrue("Mystery phase should be triggered eventually over 100 drops", triggered)
    }

    @Test
    fun testMysteryPhaseDecrement() {
        val prefs = MockSharedPreferences()
        val app = MockApplication(prefs)
        val viewModel = GameViewModel(app)
        viewModel.startGame()

        // Force active mystery phase
        viewModel.setupStateForTesting(
            grid = emptyList(),
            currentVal = 2,
            nextVal = 4,
            score = 0,
            combo = 1,
            status = GameStatus.PLAYING,
            isNextBlockHidden = true,
            hiddenMovesRemaining = 3
        )

        // Drop 1
        viewModel.isAnimationLocked = false
        try {
            viewModel.dropBlockInColumn(0)
        } catch (e: Exception) {}
        assertTrue(viewModel.state.value.isNextBlockHidden)
        assertEquals(2, viewModel.state.value.hiddenMovesRemaining)

        // Drop 2
        viewModel.isAnimationLocked = false
        try {
            viewModel.dropBlockInColumn(0)
        } catch (e: Exception) {}
        assertTrue(viewModel.state.value.isNextBlockHidden)
        assertEquals(1, viewModel.state.value.hiddenMovesRemaining)

        // Drop 3
        viewModel.isAnimationLocked = false
        try {
            viewModel.dropBlockInColumn(0)
        } catch (e: Exception) {}
        assertFalse(viewModel.state.value.isNextBlockHidden)
        assertEquals(0, viewModel.state.value.hiddenMovesRemaining)
    }

    @Test
    fun testMysteryPhasePersistence() {
        val prefs = MockSharedPreferences()
        val app = MockApplication(prefs)
        val viewModel = GameViewModel(app)
        viewModel.startGame()

        // Set state to active mystery phase
        viewModel.setupStateForTesting(
            grid = emptyList(),
            currentVal = 2,
            nextVal = 4,
            score = 100,
            combo = 1,
            status = GameStatus.PLAYING,
            isNextBlockHidden = true,
            hiddenMovesRemaining = 4
        )

        // Trigger save via pause
        viewModel.pauseGame()

        // Load in new ViewModel
        val viewModel2 = GameViewModel(app)
        viewModel2.resumeSavedGame()

        assertTrue(viewModel2.state.value.isNextBlockHidden)
        assertEquals(4, viewModel2.state.value.hiddenMovesRemaining)
    }
}

