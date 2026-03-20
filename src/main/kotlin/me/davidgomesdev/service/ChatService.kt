package me.davidgomesdev.service

import dev.langchain4j.rag.content.Content
import dev.langchain4j.rag.content.ContentMetadata
import dev.langchain4j.service.TokenStream
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.quarkus.runtime.Startup
import io.smallrye.mutiny.Multi
import jakarta.enterprise.context.ApplicationScoped
import me.davidgomesdev.dto.ChatEvent
import me.davidgomesdev.observability.attributes
import me.davidgomesdev.observability.span
import org.jboss.logging.Logger
import kotlin.math.roundToInt
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

fun interface Assistant {
    fun chat(userMessage: String): TokenStream
}

@ApplicationScoped
@Startup
class ChatService(val assistant: Assistant) {

    val log: Logger = Logger.getLogger(this::class.java)

    fun query(input: String): Multi<ChatEvent> {
        val span = span()
        val scope = span.makeCurrent()
        val chatStream = assistant.chat(input)
        val timeSource = TimeSource.Monotonic
        val startTime = timeSource.markNow()

        return Multi.createFrom().emitter { stream ->
            chatStream
                .onPartialResponse { partialResponse ->
                    stream.emit(ChatEvent.Token(partialResponse))
                }
                .onCompleteResponse { response ->
                    val timeTaken = startTime.elapsedNow().toString(DurationUnit.SECONDS, 2)
                    val tokensUsed = response.tokenUsage().outputTokenCount()

                    log.info("Took $timeTaken to respond (used $tokensUsed output tokens)")

                    span.apply {
                        addEvent(
                            "Response complete",
                            attributes {
                                put("query", input)
                                put("response", response.aiMessage().text())
                                put("thinking", response.aiMessage().thinking())
                                put("model", response.metadata().modelName())
                                put("model_duration.ms", timeTaken)
                                put("output_tokens_used", tokensUsed.toLong())
                                put("complete_reason", response.finishReason().name)
                            }
                        )
                    }

                    stream.emit(ChatEvent.Done(tokensUsed, timeTaken))
                    stream.complete()
                    scope.close()
                }
                .onRetrieved { contents ->
                    span.apply {
                        contents.forEachIndexed { index, content ->
                            val score = (content.metadata()[ContentMetadata.SCORE] as? Double) ?: 0.0
                            val metadata = content.textSegment().metadata()

                            addEvent(
                                "Source Retrieved",
                                attributes {
                                    put("index", index.toString())
                                    put("title", metadata.getString("title"))
                                    put("category", metadata.getString("categoryName"))
                                    put("score", String.format("%.2f", score))
                                }
                            )
                        }
                    }

                    val sources = contents.map(::toSourceItem)

                    log.info("Using sources:\n${sources.joinToString("\n") { "- ${it.author}: ${it.title} (${it.score}%)" }}")

                    stream.emit(ChatEvent.Sources(sources))
                }
                .onError { error ->
                    stream.fail(error)

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

        val title = metadata.getString("title") ?: ""
        val author = metadata.getString("author") ?: ""
        val category = metadata.getString("categoryName") ?: ""

        if (title == "" || author == "" || category == "") {
            log.warn("Some metadata fields are empty! (title: $title, author: $author, category: $category)")
            Span.current().addEvent("Some metadata fields are empty!", attributes {
                put("title", title)
                put("author", author)
                put("category", category)
            })
        }

        return ChatEvent.Sources.Source(
            title = title,
            author = author,
            category = category,
            score = score,
        )
    }
}
