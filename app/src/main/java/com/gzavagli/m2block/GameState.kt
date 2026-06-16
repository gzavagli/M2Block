package com.gzavagli.m2block

import java.util.UUID

enum class BlockStatus {
    IDLE,
    DROPPING,
    MERGING,
    NEW
}

data class BlockItem(
    val id: String = UUID.randomUUID().toString(),
    val row: Int,
    val col: Int,
    val value: Int,
    val status: BlockStatus = BlockStatus.IDLE,
    val targetRow: Int? = null,
    val targetCol: Int? = null,
    val age: Int = 0
)

enum class GameTheme {
    CYBERPUNK,
    MIDNIGHT,
    CANDY,
    CLASSIC
}

enum class GameStatus {
    HOME,
    PLAYING,
    PAUSED,
    GAMEOVER,
    TARGETING_HAMMER,   // Tapping a block deletes it
    TARGETING_VORTEX,   // Tapping a column clears it
    TARGETING_DOUBLE    // Tapping a block doubles its value
}

data class GameState(
    val grid: List<BlockItem> = emptyList(),
    val currentBlockValue: Int = 2,
    val nextBlockValue: Int = 4,
    val score: Int = 0,
    val highScore: Int = 0,
    val coins: Int = 200,
    val combo: Int = 1,
    val status: GameStatus = GameStatus.HOME,
    val isMuted: Boolean = false,
    val isHapticsEnabled: Boolean = true,
    val theme: GameTheme = GameTheme.CYBERPUNK,
    val hasActiveSavedGame: Boolean = false
)
