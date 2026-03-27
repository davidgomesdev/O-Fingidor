package me.davidgomesdev.pessoafaladora.backend.llm.model

import dev.langchain4j.model.anthropic.AnthropicChatModel
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import jakarta.enterprise.context.ApplicationScoped
import me.davidgomesdev.pessoafaladora.backend.llm.config.AnthropicConfig

@ApplicationScoped
class AnthropicLanguageModel(val config: AnthropicConfig) : LanguageModel {
    override fun chatModel(): ChatModel =
        AnthropicChatModel.builder()
            .modelName(config.chatModel().modelId())
            .apiKey(config.apiKey())
            .timeout(config.timeout())
            .cacheSystemMessages(true)
            .apply {
                config.chatModel().also { config ->
                    returnThinking(config.thinking())
                    temperature(config.temperature())
                    maxTokens(config.maxTokens())
                }
            }
            .build()

    override fun streamingChatModel(): StreamingChatModel =
        AnthropicStreamingChatModel.builder()
            .modelName(config.chatModel().modelId())
            .apiKey(config.apiKey())
            .timeout(config.timeout())
            .cacheSystemMessages(true)
            .apply {
                config.chatModel().also { config ->
                    returnThinking(config.thinking())
                    temperature(config.temperature())
                    maxTokens(config.maxTokens())
                }
            }
            .build()
}
