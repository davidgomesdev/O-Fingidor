package me.davidgomesdev.ofingidor.backend.web

import arrow.core.Either
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
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
import me.davidgomesdev.ofingidor.backend.debate.DebateService
import me.davidgomesdev.ofingidor.backend.llm.PersonaContext
import me.davidgomesdev.ofingidor.backend.model.Persona
import me.davidgomesdev.ofingidor.backend.service.ChatService
import me.davidgomesdev.ofingidor.backend.session.ConversationContext
import me.davidgomesdev.ofingidor.backend.session.ConversationType
import me.davidgomesdev.ofingidor.backend.session.SessionError
import me.davidgomesdev.ofingidor.backend.session.SessionService
import me.davidgomesdev.ofingidor.shared.dto.ChatEvent
import me.davidgomesdev.ofingidor.shared.dto.DebateEvent
import org.jboss.logging.Logger
import org.jboss.resteasy.reactive.RestMulti
import java.util.UUID

@Path("/pensa")
class ThinkingAPI(
    val chatService: ChatService,
    val debateService: DebateService,
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
            val conversationId = extractConversationId(token)

            val participants = when (val result = sessionService.getConversationParticipants(conversationId)) {
                is Either.Left -> throw WebApplicationException(Response.status(result.value.httpStatus).build())
                is Either.Right -> result.value
            }

            if (participants.type != ConversationType.SINGLE) {
                throw WebApplicationException(Response.status(SessionError.SESSION_MODE_MISMATCH.httpStatus).build())
            }

            if (participants.persona != requestedPersona) {
                log.warn(
                    "Persona mismatch: session=$conversationId stored=${participants.persona.codeName} requested=${requestedPersona.codeName}"
                )
                throw WebApplicationException(Response.status(SessionError.PERSONA_MISMATCH.httpStatus).build())
            }

            conversationContext.conversationId = conversationId
            personaContext.persona = participants.persona

            log.debug("Continuing session: conversationId=$conversationId persona=${participants.persona.codeName}")
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

    @PUT
    @Path("/debate")
    @Blocking
    @Produces("application/x-ndjson")
    fun queryDebate(
        body: DebateQueryPayload,
        @HeaderParam("Authorization") authorization: String?,
    ): RestMulti<DebateEvent> {
        if (body.personaA.isBlank()) throw BadRequestException("personaA must be present")
        if (body.personaB.isBlank()) throw BadRequestException("personaB must be present")

        val requestedPersonaA = Persona.entries.firstOrNull { it.codeName == body.personaA }
            ?: throw NotFoundException("personaA not found")
        val requestedPersonaB = Persona.entries.firstOrNull { it.codeName == body.personaB }
            ?: throw NotFoundException("personaB not found")

        if (requestedPersonaA == requestedPersonaB) {
            throw BadRequestException("personaA and personaB must be different")
        }

        var sessionToken: String? = null
        val conversationId = if (authorization == null) {
            val session = sessionService.createDebateSession(requestedPersonaA, requestedPersonaB)
            sessionToken = session.token
            session.conversationId
        } else {
            val token = authorization.removePrefix("Bearer ").trim()
            val storedConversationId = extractConversationId(token)
            val participants = when (val result = sessionService.getConversationParticipants(storedConversationId)) {
                is Either.Left -> throw WebApplicationException(Response.status(result.value.httpStatus).build())
                is Either.Right -> result.value
            }

            if (participants.type != ConversationType.DEBATE) {
                throw WebApplicationException(Response.status(SessionError.SESSION_MODE_MISMATCH.httpStatus).build())
            }

            if (participants.persona != requestedPersonaA || participants.opponentPersona != requestedPersonaB) {
                throw WebApplicationException(Response.status(SessionError.PERSONA_PAIR_MISMATCH.httpStatus).build())
            }

            storedConversationId
        }

        conversationContext.conversationId = conversationId

        val span = tracer.spanBuilder("API QueryDebate").apply {
            setAttribute("personaA", requestedPersonaA.codeName)
            setAttribute("personaB", requestedPersonaB.codeName)
            setAttribute("conversationId", conversationId)
        }
            .setSpanKind(SpanKind.INTERNAL)
            .startSpan()

        val traceId = span.spanContext.traceId
        val spanId = span.spanContext.spanId

        val responseStream = try {
            debateService.query(
                input = body.input,
                conversationId = UUID.fromString(conversationId),
                personaA = requestedPersonaA,
                personaB = requestedPersonaB,
                callerSpan = span,
            )
        } catch (e: Exception) {
            log.error("Error in querying debate", e)
            span.recordException(e)
            span.setStatus(StatusCode.ERROR)
            span.end()
            throw InternalServerErrorException("Failed to query debate")
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

    private fun extractConversationId(token: String): String =
        when (val result = sessionService.extractConversationId(token)) {
            is Either.Left -> throw WebApplicationException(Response.status(result.value.httpStatus).build())
            is Either.Right -> result.value
        }

}

data class QueryPayload(val input: String, val persona: String)
data class DebateQueryPayload(
    val input: String,
    val personaA: String,
    val personaB: String,
)
