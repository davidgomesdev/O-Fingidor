package me.davidgomesdev.web

import io.netty.handler.codec.http.HttpResponseStatus
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import io.smallrye.common.annotation.Blocking
import io.smallrye.mutiny.Multi
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import me.davidgomesdev.llm.PersonaContext
import me.davidgomesdev.model.Persona
import me.davidgomesdev.service.ChatService
import org.jboss.logging.Logger
import org.jboss.resteasy.reactive.RestMulti

@Path("/pensa")
class ThinkingAPI(val chatService: ChatService, val personaContext: PersonaContext) {

    private val tracer = GlobalOpenTelemetry.getTracer(this::class.java.name)
    val log: Logger = Logger.getLogger(this::class.java)

    @PUT
    @Blocking
    @Produces(MediaType.TEXT_PLAIN)
    fun queryModel(body: QueryPayload): Multi<String> {
        if (body.persona.isBlank()) {
            return createResponse(HttpResponseStatus.BAD_REQUEST, "persona must be present")
        }
        personaContext.persona = Persona.entries.firstOrNull { it.codeName == body.persona } ?: return createResponse(
            HttpResponseStatus.NOT_FOUND,
            "persona not found"
        )

        val span = tracer.spanBuilder("API QueryModel").apply {
            personaContext.persona!!.also { persona ->
                log.info("Using persona: $persona")
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

private fun createResponse(statusCode: HttpResponseStatus, text: String): RestMulti<String> =
    RestMulti.fromMultiData(Multi.createFrom().item(text))
        .status(statusCode.code()).build()

data class QueryPayload(val input: String, val persona: String)
