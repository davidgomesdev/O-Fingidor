package me.davidgomesdev.llm

import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.TextContent
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.observability.api.listener.AiServiceErrorListener
import dev.langchain4j.observability.api.listener.AiServiceStartedListener
import dev.langchain4j.rag.RetrievalAugmentor
import dev.langchain4j.service.AiServices
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Singleton
import me.davidgomesdev.model.Persona
import me.davidgomesdev.observability.attributes
import me.davidgomesdev.observability.span
import me.davidgomesdev.service.Assistant
import org.jboss.logging.Logger

@ApplicationScoped
class AiAssistant(val personaContext: PersonaContext) {

    val log: Logger = Logger.getLogger(this::class.java)

    private val systemMessage: String =
        Thread.currentThread().contextClassLoader
            .getResourceAsStream("system_message.txt")!!
            .reader().readText()

    @Singleton
    @Suppress("unused")
    fun assistant(chatModel: StreamingChatModel, retrievalAugmentor: RetrievalAugmentor): Assistant {
        log.info("Creating assistant")
        return AiServices.builder(Assistant::class.java)
            .systemMessageProvider { _ ->
                if (personaContext.persona == Persona.NINGUEM) null else systemMessage
            }
            .registerListeners(
                AiServiceStartedListener { event ->
                    span().addEvent(
                        "LLM query",
                        attributes {
                            put(
                                "user_message",
                                event.userMessage().contents()
                                    .filterIsInstance<TextContent>()
                                    .joinToString("\n\n", transform = TextContent::text)
                            )
                            put(
                                "system_message", event.systemMessage()
                                    .map(SystemMessage::text)
                                    .orElseGet { "" })
                        })
                },
                AiServiceErrorListener { error ->
                    span().recordException(error.error())
                })
            .streamingChatModel(chatModel)
            .retrievalAugmentor(retrievalAugmentor)
            .build()
    }
}
