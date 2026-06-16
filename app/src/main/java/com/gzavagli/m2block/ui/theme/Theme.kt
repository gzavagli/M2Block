package com.gzavagli.m2block.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.gzavagli.m2block.GameTheme

data class BlockColors(
    val background: Brush,
    val textColor: Color,
    val hasGlow: Boolean = false,
    val glowColor: Color = Color.Transparent
)

data class ThemeColors(
    val bgApp: Color,
    val bgPhone: Color,
    val bgPanel: Color,
    val borderUi: Color,
    val borderActive: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val btnBg: Color,
    val btnText: Color,
    val accent: Color,
    val colorCoin: Color,
    val blockColorsMap: Map<Int, BlockColors>
)

// Helper to obtain ThemeColors based on GameTheme enum
fun getThemeColors(theme: GameTheme): ThemeColors {
    return when (theme) {
        GameTheme.CYBERPUNK -> cyberpunkColors
        GameTheme.MIDNIGHT -> midnightColors
        GameTheme.CANDY -> candyColors
        GameTheme.CLASSIC -> classicColors
    }
}

// 1. CYBERPUNK
private val cyberpunkColors = ThemeColors(
    bgApp = Color(0xFF080710),
    bgPhone = Color(0xFF0C0B1A),
    bgPanel = Color(0xEC151429),
    borderUi = Color(0x4DAA3BFF),
    borderActive = Color(0xFFAA3BFF),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF8E8CA2),
    btnBg = Color(0xFFAA3BFF),
    btnText = Color(0xFFFFFFFF),
    accent = Color(0xFF00FFFF),
    colorCoin = Color(0xFFFFD700),
    blockColorsMap = mapOf(
        2 to BlockColors(Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))), Color(0xFF00FFFF)),
        4 to BlockColors(Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1))), Color(0xFFFFFFFF)),
        8 to BlockColors(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF6D28D9))), Color(0xFFFFFFFF)),
        16 to BlockColors(Brush.linearGradient(listOf(Color(0xFFDB2777), Color(0xFFBE185D))), Color(0xFFFFFFFF)),
        32 to BlockColors(Brush.linearGradient(listOf(Color(0xFFEA580C), Color(0xFFC2410C))), Color(0xFFFFFFFF)),
        64 to BlockColors(Brush.linearGradient(listOf(Color(0xFFE11D48), Color(0xFFBE123C))), Color(0xFFFFFFFF)),
        128 to BlockColors(Brush.linearGradient(listOf(Color(0xFF059669), Color(0xFF047857))), Color(0xFFFFFFFF), true, Color(0xFF059669)),
        256 to BlockColors(Brush.linearGradient(listOf(Color(0xFFCA8A04), Color(0xFFA16207))), Color(0xFFFFFFFF), true, Color(0xFFCA8A04)),
        512 to BlockColors(Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF1D4ED8))), Color(0xFFFFFFFF), true, Color(0xFF2563EB)),
        1024 to BlockColors(Brush.linearGradient(listOf(Color(0xFFD946EF), Color(0xFFA21CAF))), Color(0xFFFFFFFF), true, Color(0xFFD946EF)),
        2048 to BlockColors(Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFFF43F5E), Color(0xFFEAB308))), Color(0xFFFFFFFF), true, Color(0xFFF43F5E))
    )
)

// 2. MIDNIGHT LUXURY
private val midnightColors = ThemeColors(
    bgApp = Color(0xFF05070A),
    bgPhone = Color(0xFF0B111A),
    bgPanel = Color(0xE60E1621),
    borderUi = Color(0x40D4AF37),
    borderActive = Color(0xFFD4AF37),
    textPrimary = Color(0xFFF1F5F9),
    textSecondary = Color(0xFF64748B),
    btnBg = Color(0xFFD4AF37),
    btnText = Color(0xFF05070A),
    accent = Color(0xFFD4AF37),
    colorCoin = Color(0xFFD4AF37),
    blockColorsMap = mapOf(
        2 to BlockColors(Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))), Color(0xFF94A3B8)),
        4 to BlockColors(Brush.linearGradient(listOf(Color(0xFF334155), Color(0xFF1E293B))), Color(0xFFCBD5E1)),
        8 to BlockColors(Brush.linearGradient(listOf(Color(0xFF115E59), Color(0xFF0F766E))), Color(0xFFCCFBF1)),
        16 to BlockColors(Brush.linearGradient(listOf(Color(0xFF1E3A8A), Color(0xFF1D4ED8))), Color(0xFFDBEAFE)),
        32 to BlockColors(Brush.linearGradient(listOf(Color(0xFF581C87), Color(0xFF701A75))), Color(0xFFF3E8FF)),
        64 to BlockColors(Brush.linearGradient(listOf(Color(0xFF881337), Color(0xFF9F1239))), Color(0xFFFFE4E6)),
        128 to BlockColors(Brush.linearGradient(listOf(Color(0xFF78350F), Color(0xFF92400E))), Color(0xFFFEF3C7), true, Color(0xFF92400E)),
        256 to BlockColors(Brush.linearGradient(listOf(Color(0xFF3F3F46), Color(0xFF52525B))), Color(0xFFF4F4F5), true, Color(0xFF52525B)),
        512 to BlockColors(Brush.linearGradient(listOf(Color(0xFF18181B), Color(0xFF27272A))), Color(0xFFE4E4E7), true, Color(0xFF27272A)),
        1024 to BlockColors(Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B))), Color(0xFFD4AF37), true, Color(0xFFD4AF37)),
        2048 to BlockColors(Brush.linearGradient(listOf(Color(0xFF000000), Color(0xFF111111))), Color(0xFFFFFFFF), true, Color(0xFFD4AF37))
    )
)

// 3. CANDY POP
private val candyColors = ThemeColors(
    bgApp = Color(0xFFFDF2F8),
    bgPhone = Color(0xFFFFF1F2),
    bgPanel = Color(0xF2FFFFFF),
    borderUi = Color(0xFFFDA4AF),
    borderActive = Color(0xFFF43F5E),
    textPrimary = Color(0xFF4C0519),
    textSecondary = Color(0xFF9F1239),
    btnBg = Color(0xFFF43F5E),
    btnText = Color(0xFFFFFFFF),
    accent = Color(0xFFEC4899),
    colorCoin = Color(0xFFEAB308),
    blockColorsMap = mapOf(
        2 to BlockColors(Brush.linearGradient(listOf(Color(0xFFFED7AA), Color(0xFFFED7AA))), Color(0xFFEA580C)),
        4 to BlockColors(Brush.linearGradient(listOf(Color(0xFFBBF7D0), Color(0xFFBBF7D0))), Color(0xFF16A34A)),
        8 to BlockColors(Brush.linearGradient(listOf(Color(0xFFBFDBFE), Color(0xFFBFDBFE))), Color(0xFF2563EB)),
        16 to BlockColors(Brush.linearGradient(listOf(Color(0xFFFBCFE8), Color(0xFFFBCFE8))), Color(0xFFDB2777)),
        32 to BlockColors(Brush.linearGradient(listOf(Color(0xFFC7D2FE), Color(0xFFC7D2FE))), Color(0xFF4F46E5)),
        64 to BlockColors(Brush.linearGradient(listOf(Color(0xFFFDE047), Color(0xFFFDE047))), Color(0xFFA16207)),
        128 to BlockColors(Brush.linearGradient(listOf(Color(0xFFA5F3FC), Color(0xFFA5F3FC))), Color(0xFF0891b2)),
        256 to BlockColors(Brush.linearGradient(listOf(Color(0xFFDDD6FE), Color(0xFFDDD6FE))), Color(0xFF7C3AED)),
        512 to BlockColors(Brush.linearGradient(listOf(Color(0xFFFDA4AF), Color(0xFFFDA4AF))), Color(0xFFE11D48)),
        1024 to BlockColors(Brush.linearGradient(listOf(Color(0xFFFED7AA), Color(0xFFFDA4AF))), Color(0xFF9F1239)),
        2048 to BlockColors(Brush.linearGradient(listOf(Color(0xFFF43F5E), Color(0xFFEC4899), Color(0xFF3B82F6))), Color(0xFFFFFFFF), true, Color(0xFFF43F5E))
    )
)

// 4. CLASSIC GLOW
private val classicColors = ThemeColors(
    bgApp = Color(0xFF121214),
    bgPhone = Color(0xFF1E1E24),
    bgPanel = Color(0xF21E1E24),
    borderUi = Color(0xFF3E3E50),
    borderActive = Color(0xFFFF9F1C),
    textPrimary = Color(0xFFF4F4F9),
    textSecondary = Color(0xFF8A8A9E),
    btnBg = Color(0xFFFF9F1C),
    btnText = Color(0xFF121214),
    accent = Color(0xFF2EC4B6),
    colorCoin = Color(0xFFFFD700),
    blockColorsMap = mapOf(
        2 to BlockColors(Brush.linearGradient(listOf(Color(0xFFEEE4DA), Color(0xFFEEE4DA))), Color(0xFF776E65)),
        4 to BlockColors(Brush.linearGradient(listOf(Color(0xFFEDE0C8), Color(0xFFEDE0C8))), Color(0xFF776E65)),
        8 to BlockColors(Brush.linearGradient(listOf(Color(0xFFF2B179), Color(0xFFF2B179))), Color(0xFFF9F6F2)),
        16 to BlockColors(Brush.linearGradient(listOf(Color(0xFFF59563), Color(0xFFF59563))), Color(0xFFF9F6F2)),
        32 to BlockColors(Brush.linearGradient(listOf(Color(0xFFF67C5F), Color(0xFFF67C5F))), Color(0xFFF9F6F2)),
        64 to BlockColors(Brush.linearGradient(listOf(Color(0xFFF65E3B), Color(0xFFF65E3B))), Color(0xFFF9F6F2)),
        128 to BlockColors(Brush.linearGradient(listOf(Color(0xFFEDCF72), Color(0xFFEDCF72))), Color(0xFFF9F6F2), true, Color(0xFFEDCF72)),
        256 to BlockColors(Brush.linearGradient(listOf(Color(0xFFEDCC61), Color(0xFFEDCC61))), Color(0xFFF9F6F2), true, Color(0xFFEDCC61)),
        512 to BlockColors(Brush.linearGradient(listOf(Color(0xFFEDC850), Color(0xFFEDC850))), Color(0xFFF9F6F2), true, Color(0xFFEDC850)),
        1024 to BlockColors(Brush.linearGradient(listOf(Color(0xFFEDC53F), Color(0xFFEDC53F))), Color(0xFFF9F6F2), true, Color(0xFFEDC53F)),
        2048 to BlockColors(Brush.linearGradient(listOf(Color(0xFFEDC22E), Color(0xFFEDC22E))), Color(0xFFF9F6F2), true, Color(0xFFEDC22E))
    )
)

fun ThemeColors.getBlockColors(value: Int): BlockColors {
    blockColorsMap[value]?.let { return it }
    if (value <= 0) {
        return BlockColors(Brush.linearGradient(listOf(Color.Gray, Color.DarkGray)), Color.White)
    }
    val keys = blockColorsMap.keys.sorted()
    if (keys.isEmpty()) {
        return BlockColors(Brush.linearGradient(listOf(Color.Gray, Color.DarkGray)), Color.White)
    }
    var log2 = 0
    var temp = value
    while (temp > 1) {
        temp /= 2
        log2++
    }
    val index = if (log2 > 0) (log2 - 1) % keys.size else 0
    return blockColorsMap[keys[index]] ?: BlockColors(Brush.linearGradient(listOf(Color.Gray, Color.DarkGray)), Color.White)
}

