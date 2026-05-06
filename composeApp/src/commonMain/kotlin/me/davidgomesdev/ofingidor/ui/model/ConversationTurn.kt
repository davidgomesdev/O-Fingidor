package me.davidgomesdev.ofingidor.ui.model

data class ConversationTurn(
    val question: String,
    val message: String,
    val sources: List<Source>,
    val traceId: String,
    val personaName: String,
)

data class OngoingConversationTurn(
    val question: String,
    val personaName: String,
    val message: String = "",
    val sources: List<Source> = emptyList(),
    val traceId: String = "",
) {
    fun toConversationTurn() = ConversationTurn(
        question = question,
        message = message,
        sources = sources,
        traceId = traceId,
        personaName = personaName,
    )
}
