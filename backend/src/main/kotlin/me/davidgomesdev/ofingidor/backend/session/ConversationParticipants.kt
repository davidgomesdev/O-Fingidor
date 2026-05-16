package me.davidgomesdev.ofingidor.backend.session

import me.davidgomesdev.ofingidor.backend.model.Persona

data class ConversationParticipants(
    val type: ConversationType,
    val persona: Persona,
    val opponentPersona: Persona? = null,
)
