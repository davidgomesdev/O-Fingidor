package me.davidgomesdev.ofingidor.backend.web

import arrow.core.Either
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import io.smallrye.common.annotation.Blocking
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.InternalServerErrorException
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import me.davidgomesdev.ofingidor.backend.dto.ChatEvent
import me.davidgomesdev.ofingidor.backend.llm.PersonaContext
import me.davidgomesdev.ofingidor.backend.model.Persona
import me.davidgomesdev.ofingidor.backend.service.ChatService
import me.davidgomesdev.ofingidor.backend.session.ConversationContext
import me.davidgomesdev.ofingidor.backend.session.SessionError
import me.davidgomesdev.ofingidor.backend.session.SessionService
import org.jboss.logging.Logger
import org.jboss.resteasy.reactive.RestMulti

@Path("/pensa")
class ThinkingAPI(
    val chatService: ChatService,
    val personaContext: PersonaContext,
    val conversationContext: ConversationContext,
    val sessionService: SessionService,
) {

    private val tracer = GlobalOpenTelemetry.getTracer(this::class.java.name)
    val log: Logger = Logger.getLogger(this::class.java)

    @PUT
    @Blocking
    @Produces("application/x-ndjson")
    fun queryModel(
        body: QueryPayload,
        @HeaderParam("Authorization") authorization: String?,
    ): RestMulti<ChatEvent> {
        if (body.persona.isBlank()) throw BadRequestException("persona must be present")

        val requestedPersona = Persona.entries.firstOrNull { it.codeName == body.persona }
            ?: throw NotFoundException("persona not found")

        var sessionToken: String? = null

        if (authorization == null) {
            val session = sessionService.createSession(requestedPersona)
            sessionToken = session.token
            conversationContext.conversationId = session.conversationId
            personaContext.persona = requestedPersona
            log.info("New session started: conversationId=${session.conversationId} persona=${requestedPersona.codeName}")
        } else {
            val token = authorization.removePrefix("Bearer ").trim()

            val conversationId = when (val result = sessionService.extractConversationId(token)) {
                is Either.Left -> throw WebApplicationException(Response.status(result.value.httpStatus).build())
                is Either.Right -> result.value
            }

            val storedPersona = when (val result = sessionService.getPersona(conversationId)) {
                is Either.Left -> throw WebApplicationException(Response.status(result.value.httpStatus).build())
                is Either.Right -> result.value
            }

            if (storedPersona != requestedPersona) {
                log.warn("Persona mismatch: session=$conversationId stored=${storedPersona.codeName} requested=${requestedPersona.codeName}")
                throw WebApplicationException(Response.status(SessionError.PERSONA_MISMATCH.httpStatus).build())
            }

            conversationContext.conversationId = conversationId
            personaContext.persona = storedPersona

            log.debug("Continuing session: conversationId=$conversationId persona=${storedPersona.codeName}")
        }

        val span = tracer.spanBuilder("API QueryModel").apply {
            personaContext.persona!!.also { persona ->
                log.info("Using persona: ${persona.displayName}")
                setAttribute("persona", persona.codeName)
            }
            setAttribute("conversationId", conversationContext.conversationId!!)
        }
            .setSpanKind(SpanKind.INTERNAL)
            .startSpan()

        val traceId = span.spanContext.traceId
        val spanId = span.spanContext.spanId
        log.info("Querying model with trace ID: $traceId")

        val responseStream = try {
            chatService.query(body.input, span)
        } catch (e: Exception) {
            log.error("Error in querying model", e)
            throw InternalServerErrorException("Failed to query model")
        }

        val multi = RestMulti
            .fromMultiData(responseStream)
            .header("X-Trace-Id", traceId)
            .header("X-Traceparent", "00-$traceId-$spanId-01")

        if (sessionToken != null) {
            multi.header("X-Session-Token", sessionToken)
        }

        return multi.build()
    }

}

data class QueryPayload(val input: String, val persona: String)
