package me.davidgomesdev.ofingidor.backend.llm.model

import dev.langchain4j.model.scoring.ScoringModel
import dev.langchain4j.model.voyageai.VoyageAiScoringModel
import jakarta.enterprise.context.ApplicationScoped
import me.davidgomesdev.ofingidor.backend.llm.config.VoyageConfig

@ApplicationScoped
class VoyageScoringChatModel(
    val config: VoyageConfig,
) : ScoringChatModel {
    override fun scoringModel(): ScoringModel =
        VoyageAiScoringModel
            .builder()
            .apiKey(config.apiKey())
            .modelName(config.scoringModel().modelId())
            .maxRetries(3)
            .timeout(config.timeout())
            .build()
}
