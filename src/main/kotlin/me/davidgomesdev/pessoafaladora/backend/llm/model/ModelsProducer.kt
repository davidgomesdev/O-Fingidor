package me.davidgomesdev.pessoafaladora.backend.llm.model

import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.embedding.EmbeddingModel
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Singleton
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger

@ApplicationScoped
class ModelsProducer(
    private val ollama: OllamaLanguageModel,
    private val anthropic: AnthropicLanguageModel,
    @param:ConfigProperty(name = "model.name")
    private val name: String,
) : LanguageModel {
    val logger: Logger = Logger.getLogger(this::class.java)

    init {
        logger.info("Using '$name' language model")
    }

    @Singleton
    override fun chatModel(): ChatModel =
        when (name) {
            "ollama" -> ollama.chatModel()
            "anthropic" -> anthropic.chatModel()
            else -> throw IllegalArgumentException("Unknown chat model '$name'")
        }

    @Singleton
    override fun streamingChatModel(): StreamingChatModel =
        when (name) {
            "ollama" -> ollama.streamingChatModel()
            "anthropic" -> anthropic.streamingChatModel()
            else -> throw IllegalArgumentException("Unknown chat model '$name'")
        }

    @Singleton
    @Suppress("unused")
    fun embeddingModel(): EmbeddingModel {
        // Anthropic doesn't have an embedding model
        return ollama.embeddingModel()
    }
}
