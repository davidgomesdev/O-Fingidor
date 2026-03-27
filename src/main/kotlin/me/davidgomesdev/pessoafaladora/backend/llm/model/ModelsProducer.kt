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
    private val ollama: OllamaChatModel,
    @param:ConfigProperty(name = "model.name")
    private val name: String,
) : LanguageModel {
    val logger: Logger = Logger.getLogger(this::class.java)

    init {
        logger.info("Using '$name' language model")
    }

    @Singleton
    override fun chatModel(): ChatModel {
        return ollama.chatModel()
    }

    @Singleton
    override fun streamingChatModel(): StreamingChatModel {
        return ollama.streamingChatModel()
    }

    @Singleton
    override fun embeddingModel(): EmbeddingModel {
        return ollama.embeddingModel()
    }
}
