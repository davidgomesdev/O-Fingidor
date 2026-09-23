package me.davidgomesdev.ofingidor.backend.llm.model

import dev.langchain4j.model.bedrock.BedrockChatModel
import dev.langchain4j.model.bedrock.BedrockChatRequestParameters
import dev.langchain4j.model.bedrock.BedrockStreamingChatModel
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import jakarta.enterprise.context.ApplicationScoped
import me.davidgomesdev.ofingidor.backend.llm.config.BedrockConfig
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
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
            .client(BedrockRuntimeClient.builder().withAuth().build())
            .modelId(config.chatModel().modelId())
            .returnThinking(config.chatModel().thinking())
            .defaultRequestParameters(requestParameters())
            .build()

    override fun streamingChatModel(): StreamingChatModel =
        BedrockStreamingChatModel.builder()
            .client(BedrockRuntimeAsyncClient.builder().withAuth().build())
            .modelId(config.chatModel().modelId())
            .returnThinking(config.chatModel().thinking())
            .defaultRequestParameters(requestParameters())
            .build()

    // Bedrock API keys are sent as bearer tokens; without one, requests are SigV4-signed with AWS credentials
    private fun <B : BedrockRuntimeBaseClientBuilder<B, *>> B.withAuth(): B =
        region(Region.of(config.region()))
            .overrideConfiguration { it.apiCallTimeout(config.timeout()) }
            .apply {
                config.apiKey().ifPresent { apiKey ->
                    tokenProvider(StaticTokenProvider.create { apiKey })
                    authSchemeProvider(BedrockRuntimeAuthSchemeProvider.defaultProvider(listOf("smithy.api#httpBearerAuth")))
                }
                config.profile().ifPresent { profile ->
                    credentialsProvider(ProfileCredentialsProvider.create(profile))
                }
            }

    private fun requestParameters(): BedrockChatRequestParameters =
        config.chatModel().let { config ->
            BedrockChatRequestParameters.builder()
                .maxOutputTokens(config.maxTokens())
                .apply { if (config.thinking()) enableReasoning(config.thinkingBudgetTokens()) }
                .build()
        }
}
