package me.davidgomesdev.ofingidor.backend.llm

import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.TextContent
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.observability.api.listener.AiServiceErrorListener
import dev.langchain4j.observability.api.listener.AiServiceStartedListener
import dev.langchain4j.rag.RetrievalAugmentor
import dev.langchain4j.service.AiServices
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Singleton
import me.davidgomesdev.ofingidor.backend.model.Persona
import me.davidgomesdev.ofingidor.backend.observability.attributes
import me.davidgomesdev.ofingidor.backend.observability.span
import me.davidgomesdev.ofingidor.backend.service.Assistant
import me.davidgomesdev.ofingidor.backend.session.SessionConfig
import org.jboss.logging.Logger
import java.io.File

@ApplicationScoped
class AiAssistant(
    val personaContext: PersonaContext,
    val postgresConversationStore: PostgresConversationStore,
    val sessionConfig: SessionConfig,
) {

    val log: Logger = Logger.getLogger(this::class.java)
    private val systemMessages: Map<String, String> = getPersonaSystemMessages()

    @Singleton
    @Suppress("unused")
    fun assistant(chatModel: StreamingChatModel, retrievalAugmentor: RetrievalAugmentor): Assistant {
        log.info("Creating assistant")

        return AiServices.builder(Assistant::class.java)
            .systemMessageProvider { _ -> resolveSystemMessage() }
            .chatMemoryProvider { memoryId ->
                MessageWindowChatMemory.builder()
                    .id(memoryId)
                    .maxMessages(sessionConfig.memory().maxMessages())
                    .chatMemoryStore(postgresConversationStore)
                    .build()
            }
            .registerListeners(AiServiceStartedListener { event ->
                span().addEvent(
                    "LLM query", attributes {
                        put(
                            "user_message",
                            event.userMessage().contents().filterIsInstance<TextContent>()
                                .joinToString("\n\n", transform = TextContent::text)
                        )
                        put(
                            "system_message", event.systemMessage().map(SystemMessage::text).orElseGet { "" })
                    })
            }, AiServiceErrorListener { error ->
                span().recordException(error.error())
            })
            .streamingChatModel(chatModel)
            .retrievalAugmentor(retrievalAugmentor)
            .build()
    }

    private fun getPersonaSystemMessages(): Map<String, String> = buildMap {
        val allFiles = Persona.entries.map(Persona::systemPromptFilename)
        allFiles.forEach { fileName ->
            val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("prompts/$fileName")
                ?: throw IllegalStateException("Prompt '$fileName' not found")

            stream.reader().readText().let { put(fileName, it) }
        }
    }

    private fun resolveSystemMessage(): String {
        val persona = personaContext.persona ?: throw IllegalStateException("Persona not set")
        val fileName = persona.systemPromptFilename

        val localFile = File(fileName)
        if (localFile.exists()) return localFile.readText()

        return systemMessages[fileName] ?: throw IllegalStateException("Prompt for Persona '$persona' not found")
    }
}
