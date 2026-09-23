package me.davidgomesdev.ofingidor.backend.llm.model

import dev.langchain4j.model.scoring.ScoringModel

@Suppress("kotlin:S6517")
interface ScoringChatModel {
    fun scoringModel(): ScoringModel
}
