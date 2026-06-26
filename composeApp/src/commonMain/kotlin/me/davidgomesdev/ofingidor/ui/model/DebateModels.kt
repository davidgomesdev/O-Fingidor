package me.davidgomesdev.ofingidor.ui.model

import me.davidgomesdev.ofingidor.shared.dto.Persona

enum class DebateSide {
    LEFT,
    RIGHT;

    fun other(): DebateSide = if (this == LEFT) RIGHT else LEFT
}

internal data class DebateQuestionEntry(
    val question: String,
    val startOffset: Int,
)

data class DebatePair(
    val left: Persona,
    val right: Persona,
) {
    init {
        require(left != right) { "Debate personas must be different" }
    }

    fun sideFor(persona: Persona): DebateSide =
        when (persona) {
            left -> DebateSide.LEFT
            right -> DebateSide.RIGHT
            else -> error("Persona $persona is not part of this debate pair")
        }

    operator fun contains(persona: Persona): Boolean = persona == left || persona == right

    fun personaAt(side: DebateSide): Persona = if (side == DebateSide.LEFT) left else right

    fun swapped(): DebatePair = DebatePair(left = right, right = left)
}

/** Result of picking a voice in the constellation while in debate mode. */
data class DebatePick(val pair: DebatePair, val nextSlot: DebateSide)

/**
 * Picking a voice replaces the one in [nextSlot]; the other slot is replaced next time.
 * Picking a voice that is already in the pair changes nothing.
 */
fun DebatePair.pick(persona: Persona, nextSlot: DebateSide): DebatePick {
    if (persona in this) return DebatePick(this, nextSlot)
    val pair = when (nextSlot) {
        DebateSide.LEFT -> copy(left = persona)
        DebateSide.RIGHT -> copy(right = persona)
    }
    return DebatePick(pair, nextSlot.other())
}

data class DebateTurn(
    val turnIndex: Int,
    val speaker: Persona,
    val message: String = "",
    val sources: List<Source> = emptyList(),
    val isComplete: Boolean = false,
)
