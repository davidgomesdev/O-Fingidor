package me.davidgomesdev.pessoafaladora.backend.web

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import io.smallrye.common.annotation.Blocking
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import me.davidgomesdev.pessoafaladora.backend.dto.ChatEvent
import me.davidgomesdev.pessoafaladora.backend.llm.PersonaContext
import me.davidgomesdev.pessoafaladora.backend.model.Persona
import me.davidgomesdev.pessoafaladora.backend.service.ChatService
import org.jboss.logging.Logger
import org.jboss.resteasy.reactive.RestMulti

@Path("/pensa")
class ThinkingAPI(val chatService: ChatService, val personaContext: PersonaContext) {

    private val tracer = GlobalOpenTelemetry.getTracer(this::class.java.name)
    val log: Logger = Logger.getLogger(this::class.java)

    @PUT
    @Blocking
    @Produces("application/x-ndjson")
    fun queryModel(body: QueryPayload): RestMulti<ChatEvent> {
        if (body.persona.isBlank()) throw BadRequestException("persona must be present")

        personaContext.persona = Persona.entries.firstOrNull { it.codeName == body.persona }
            ?: throw NotFoundException("persona not found")

        val span = tracer.spanBuilder("API QueryModel").apply {
            personaContext.persona!!.also { persona ->
                log.info("Using persona: ${persona.displayName}")
                setAttribute("persona", persona.codeName)
            }
        }
            .setSpanKind(SpanKind.INTERNAL)
            .startSpan()

        val traceId = span.spanContext.traceId

        log.info("Querying model with trace ID: $traceId")

        return RestMulti
            .fromMultiData(chatService.query(body.input))
            .header("X-Trace-Id", traceId).build()
    }
}

data class QueryPayload(val input: String, val persona: String)
