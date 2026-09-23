package me.davidgomesdev.ofingidor.backend.llm.model

import dev.langchain4j.model.bedrock.BedrockChatModel
import dev.langchain4j.model.bedrock.BedrockChatRequestParameters
import dev.langchain4j.model.bedrock.BedrockStreamingChatModel
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import jakarta.enterprise.context.ApplicationScoped
import me.davidgomesdev.ofingidor.backend.llm.config.BedrockConfig
import software.amazon.awssdk.auth.token.credentials.StaticTokenProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeBaseClientBuilder
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient
import software.amazon.awssdk.services.bedrockruntime.auth.scheme.BedrockRuntimeAuthSchemeProvider

@ApplicationScoped
class BedrockLanguageModel(val config: BedrockConfig) : LanguageModel {
    override fun chatModel(): ChatModel =
        BedrockChatModel.builder()
            .client(BedrockRuntimeClient.builder().withApiKey().build())
            .modelId(config.chatModel().modelId())
            .returnThinking(config.chatModel().thinking())
            .defaultRequestParameters(requestParameters())
            .build()

    override fun streamingChatModel(): StreamingChatModel =
        BedrockStreamingChatModel.builder()
            .client(BedrockRuntimeAsyncClient.builder().withApiKey().build())
            .modelId(config.chatModel().modelId())
            .returnThinking(config.chatModel().thinking())
            .defaultRequestParameters(requestParameters())
            .build()

    // Bedrock API keys are sent as bearer tokens instead of SigV4-signed AWS credentials
    private fun <B : BedrockRuntimeBaseClientBuilder<B, *>> B.withApiKey(): B =
        region(Region.of(config.region()))
            .tokenProvider(StaticTokenProvider.create { config.apiKey() })
            .authSchemeProvider(BedrockRuntimeAuthSchemeProvider.defaultProvider(listOf("smithy.api#httpBearerAuth")))
            .overrideConfiguration { it.apiCallTimeout(config.timeout()) }

    private fun requestParameters(): BedrockChatRequestParameters =
        config.chatModel().let { config ->
            BedrockChatRequestParameters.builder()
                .temperature(config.temperature())
                .maxOutputTokens(config.maxTokens())
                .apply { if (config.thinking()) enableReasoning(config.thinkingBudgetTokens()) }
                .build()
        }
}
