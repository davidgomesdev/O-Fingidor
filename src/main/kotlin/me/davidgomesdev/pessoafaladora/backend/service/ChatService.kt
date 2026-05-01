package me.davidgomesdev.pessoafaladora.backend.service

import dev.langchain4j.rag.content.Content
import dev.langchain4j.rag.content.ContentMetadata
import dev.langchain4j.service.MemoryId
import dev.langchain4j.service.TokenStream
import dev.langchain4j.service.UserMessage
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.quarkus.runtime.Startup
import io.smallrye.mutiny.Multi
import jakarta.enterprise.context.ApplicationScoped
import me.davidgomesdev.pessoafaladora.backend.dto.ChatEvent
import me.davidgomesdev.pessoafaladora.backend.llm.ChatHistoryRepository
import me.davidgomesdev.pessoafaladora.backend.llm.TextAttributes
import me.davidgomesdev.pessoafaladora.backend.observability.attributes
import me.davidgomesdev.pessoafaladora.backend.observability.span
import me.davidgomesdev.pessoafaladora.backend.session.ConversationContext
import org.jboss.logging.Logger
import java.util.UUID
import kotlin.math.roundToInt
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

fun interface Assistant {
    fun chat(@MemoryId memoryId: String, @UserMessage message: String): TokenStream
}

@ApplicationScoped
@Startup
class ChatService(
    val assistant: Assistant,
    val conversationContext: ConversationContext,
    val chatHistoryRepository: ChatHistoryRepository,
) {

    val log: Logger = Logger.getLogger(this::class.java)

    fun query(input: String, callerSpan: Span): Multi<ChatEvent> {
        val conversationId = conversationContext.conversationId
            ?: error("conversationId not set on ConversationContext")
        val span = span()
        val scope = span.makeCurrent()
        val chatStream = assistant.chat(conversationId, input)
        val timeSource = TimeSource.Monotonic
        val startTime = timeSource.markNow()
        var capturedSources: List<ChatEvent.Sources.Source> = emptyList()

        return Multi.createFrom().emitter { stream ->
            stream.emit(ChatEvent.Start(span.spanContext.traceId))

            chatStream
                .onPartialResponse { partialResponse ->
                    stream.emit(ChatEvent.Token(partialResponse))
                }
                .onCompleteResponse { response ->
                    val timeTaken = startTime.elapsedNow().toString(DurationUnit.SECONDS, 2)
                    val totalTokensUsed = response.tokenUsage().totalTokenCount()

                    log.info("Took $timeTaken to respond (used $totalTokensUsed output tokens)")

                    span.apply {
                        addEvent(
                            "Response complete",
                            attributes {
                                put("query", input)
                                put("response", response.aiMessage().text())
                                put("thinking", response.aiMessage().thinking())
                                put("model", response.metadata().modelName())
                                put("model_duration.ms", timeTaken)
                                put("total_tokens_used", totalTokensUsed.toLong())
                                put("input_tokens_used", response.tokenUsage().inputTokenCount().toLong())
                                put("output_tokens_used", response.tokenUsage().outputTokenCount().toLong())
                                put("complete_reason", response.finishReason().name)
                            }
                        )
                    }

                    chatHistoryRepository.persist(
                        conversationId = UUID.fromString(conversationId),
                        userMessage = input,
                        aiResponse = response.aiMessage().text() ?: "",
                        sources = capturedSources,
                    )

                    stream.emit(ChatEvent.Done(totalTokensUsed, timeTaken))
                    stream.complete()
                    scope.close()
                    callerSpan.end()
                }
                .onRetrieved { contents ->
                    span.apply {
                        val eventAttributes = attributes {
                            contents.forEachIndexed { index, content ->
                                val score = (content.metadata()[ContentMetadata.SCORE] as? Double) ?: 0.0
                                val metadata = content.textSegment().metadata()

                                TextAttributes.run {
                                    put("${index}_title", metadata.getString(TITLE))
                                    put("${index}_category", metadata.getString(CATEGORY_NAME))
                                }
                                put("${index}_score", String.format("%.2f", score))
                            }
                        }

                        addEvent("Sources Retrieved", eventAttributes)
                    }

                    val sources = contents.map(::toSourceItem)
                    capturedSources = sources

                    log.info("Using sources:\n${sources.joinToString("\n") { "- ${it.author}: ${it.title} (${it.score}%)" }}")

                    stream.emit(ChatEvent.Sources(sources))
                }
                .onError { error ->
                    stream.fail(error)
                    scope.close()
                    callerSpan.end()

                    span.apply {
                        recordException(error)
                        setStatus(StatusCode.ERROR)
                    }

                    log.error("There was a problem with the assistant!", error)
                }
                .start()
        }
    }

    private fun toSourceItem(source: Content): ChatEvent.Sources.Source {
        val score = ((source.metadata()[ContentMetadata.SCORE] as Double) * 100).roundToInt()
        val metadata = source.textSegment().metadata()

        val id = metadata.getLong(TextAttributes.TEXT_ID) ?: 0
        val title = metadata.getString(TextAttributes.TITLE) ?: ""
        val author = metadata.getString(TextAttributes.AUTHOR) ?: ""
        val category = metadata.getString(TextAttributes.CATEGORY_NAME) ?: ""

        if (listOf(title, author, category).any(String::isBlank)) {
            log.warn("Some metadata fields are empty! (title: $title, author: $author, category: $category)")
            Span.current().addEvent("Some metadata fields are empty!", attributes {
                put("title", title)
                put("author", author)
                put("category", category)
            })
        }

        if (id == 0L) {
            log.warn("Text $title has no ID! (0)")
            Span.current().addEvent("Text has no ID!", attributes {
                put("title", title)
                put("author", author)
                put("category", category)
            })
        }

        return ChatEvent.Sources.Source(
            id = id,
            title = title,
            author = author,
            category = category,
            score = score,
        )
    }
}
