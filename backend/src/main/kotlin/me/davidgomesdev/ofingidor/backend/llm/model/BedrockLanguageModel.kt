package me.davidgomesdev.ofingidor.backend.llm.model

import dev.langchain4j.model.bedrock.BedrockChatModel
import dev.langchain4j.model.bedrock.BedrockChatRequestParameters
import dev.langchain4j.model.bedrock.BedrockStreamingChatModel
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import jakarta.enterprise.context.ApplicationScoped
import me.davidgomesdev.ofingidor.backend.llm.config.BedrockConfig
import software.amazon.awssdk.regions.Region

/**
 * AWS credentials are resolved by the default AWS provider chain
 * (e.g. `AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY`, `AWS_PROFILE`).
 */
@ApplicationScoped
class BedrockLanguageModel(val config: BedrockConfig) : LanguageModel {
    override fun chatModel(): ChatModel =
        BedrockChatModel.builder()
            .region(Region.of(config.region()))
            .modelId(config.chatModel().modelId())
            .timeout(config.timeout())
            .returnThinking(config.chatModel().thinking())
            .defaultRequestParameters(requestParameters())
            .build()

    override fun streamingChatModel(): StreamingChatModel =
        BedrockStreamingChatModel.builder()
            .region(Region.of(config.region()))
            .modelId(config.chatModel().modelId())
            .timeout(config.timeout())
            .returnThinking(config.chatModel().thinking())
            .defaultRequestParameters(requestParameters())
            .build()

    private fun requestParameters(): BedrockChatRequestParameters =
        config.chatModel().let { config ->
            BedrockChatRequestParameters.builder()
                .temperature(config.temperature())
                .maxOutputTokens(config.maxTokens())
                .apply { if (config.thinking()) enableReasoning(config.thinkingBudgetTokens()) }
                .build()
        }
}
