package me.davidgomesdev.ofingidor.ui

import androidx.compose.ui.graphics.Color
import me.davidgomesdev.ofingidor.ui.model.DebateSide

// --- Mystic palette: near-black violet ink, purple as first accent, grey as second ---
val inkColor = Color(0xFF09080D)
val surfaceColor = Color(0xFF100E17)
val surfaceRaisedColor = Color(0xFF1A1624)
val hairlineColor = Color(0x14ECE8F4)
val hairlineStrongColor = Color(0x24ECE8F4)

val textPrimaryColor = Color(0xFFECE8F4)
val textBodyColor = Color(0xFFE4DFEC)
val textSecondaryColor = Color(0xFFB8B4C2)
val textMutedColor = Color(0xFF8C8996)

val purpleColor = Color(0xFFB49CF5)
val purpleSoftColor = Color(0xFFC9B6FF)
val purpleDeepColor = Color(0xFF7C5CD6)
val ctaTopColor = Color(0xFF8467E0)
val ctaBottomColor = Color(0xFF6A4DC9)

val silverColor = Color(0xFF9A97A3)
val silverLightColor = Color(0xFFD4D1DB)

val amberColor = Color(0xFFCFAA50)

// Supporting shades used by the mystic surfaces and states
val panelColor = Color(0xFF121019)
val surfaceDeepColor = Color(0xFF0E0C14)
val vignetteColor = Color(0xFF040307)
val orbShadeColor = Color(0xFF5A3EAA)
val silverSoftColor = Color(0xFFCFCBD6)
val silverPaleColor = Color(0xFFE2DFE8)
val shimmerHighlightColor = Color(0xFFEDE4FF)
val switchOffColor = Color(0xFF6B6875)

val backgroundColor = inkColor
val cardBorderColor = hairlineColor
val focusedIndicatorColor = Color(0xFF575757)

// --- Persona chip palette ---
//  - ortónimo active state
val orthonymChipColor = Color(0xFF1F1A2E)
val orthonymChipBorderColor = Color(0x557C5CBF)
val orthonymChipTextColor = Color(0xFFC4AFF5)

//  - dev mode active state (amber)
val devChipColor = Color(0xFF1F1800)
val devChipBorderColor = Color(0x55BFA040)
val devChipTextColor = amberColor

//  - semi-heterónimo active state (same hue, halfway between orthonym and heteronym)
val semiHeteronymChipColor = Color(0xFF211F2A)
val semiHeteronymChipBorderColor = Color(0x3D7C5CBF)
val semiHeteronymChipTextColor = Color(0xFFAD9EDC)

val heteronymChipColor = Color(0xFF242424)

// Error bubble colors
val errorBubbleBackgroundColor = Color(0xFF1A1010)
val errorBubbleBorderColor = Color(0xFF4A2020)
val errorBubbleTextColor = Color(0xFFC98080)

/** Colours for one side of a debate: the first voice is purple, the second is grey. */
data class DebateSidePalette(
    val accent: Color,
    val label: Color,
    val bubbleTop: Color,
    val border: Color,
    val glow: Color,
)

fun debateSidePalette(side: DebateSide): DebateSidePalette = when (side) {
    DebateSide.LEFT -> DebateSidePalette(
        accent = purpleColor,
        label = purpleColor,
        bubbleTop = Color(0xBF281E42),
        border = Color(0x4DB49CF5),
        glow = Color(0xA67C5CD6),
    )

    DebateSide.RIGHT -> DebateSidePalette(
        accent = silverLightColor,
        label = Color(0xFFCFCBD6),
        bubbleTop = Color(0xBF28272E),
        border = Color(0x4D9A97A3),
        glow = Color(0x809A97A3),
    )
}
