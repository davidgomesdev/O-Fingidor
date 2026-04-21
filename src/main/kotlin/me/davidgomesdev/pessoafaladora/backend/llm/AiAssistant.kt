package me.davidgomesdev.pessoafaladora.backend.llm

import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.TextContent
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.observability.api.listener.AiServiceErrorListener
import dev.langchain4j.observability.api.listener.AiServiceStartedListener
import dev.langchain4j.rag.RetrievalAugmentor
import dev.langchain4j.service.AiServices
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Singleton
import me.davidgomesdev.pessoafaladora.backend.model.Persona
import me.davidgomesdev.pessoafaladora.backend.observability.attributes
import me.davidgomesdev.pessoafaladora.backend.observability.span
import me.davidgomesdev.pessoafaladora.backend.service.Assistant
import org.jboss.logging.Logger
import java.io.File

@ApplicationScoped
class AiAssistant(val personaContext: PersonaContext) {

    val log: Logger = Logger.getLogger(this::class.java)
    private val systemMessages: Map<String, String> = getPersonaSystemMessages()

    @Singleton
    @Suppress("unused")
    fun assistant(chatModel: StreamingChatModel, retrievalAugmentor: RetrievalAugmentor): Assistant {
        log.info("Creating assistant")
        return AiServices.builder(Assistant::class.java).systemMessageProvider { _ ->
            resolveSystemMessage()
        }.registerListeners(AiServiceStartedListener { event ->
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
        }).streamingChatModel(chatModel).retrievalAugmentor(retrievalAugmentor).build()
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

        // Allow local file override for dev (e.g. system_message_alberto_caeiro.txt in working dir)
        val localFile = File(fileName)
        if (localFile.exists()) return localFile.readText()

        return systemMessages[fileName] ?: throw IllegalStateException("Prompt for Persona '$persona' not found")
    }
}
