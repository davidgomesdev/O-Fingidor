package me.davidgomesdev.pessoafaladora.backend.web

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import io.smallrye.common.annotation.Blocking
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import me.davidgomesdev.pessoafaladora.backend.dto.ChatEvent
import me.davidgomesdev.pessoafaladora.backend.llm.PersonaContext
import me.davidgomesdev.pessoafaladora.backend.model.Persona
import me.davidgomesdev.pessoafaladora.backend.service.ChatService
import me.davidgomesdev.pessoafaladora.backend.session.ConversationContext
import me.davidgomesdev.pessoafaladora.backend.session.SessionService
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
            sessionToken = sessionService.createSession(requestedPersona)
            val conversationId = sessionService.extractConversationId(sessionToken)
            conversationContext.conversationId = conversationId
            personaContext.persona = requestedPersona
        } else {
            val token = authorization.removePrefix("Bearer ").trim()
            val conversationId = sessionService.extractConversationId(token)
            val storedPersona = sessionService.getPersona(conversationId)
            if (storedPersona != requestedPersona) {
                throw WebApplicationException(
                    Response.status(409).entity("Persona mismatch: session was created for ${storedPersona.codeName}")
                        .build()
                )
            }
            conversationContext.conversationId = conversationId
            personaContext.persona = storedPersona
        }

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

        val multi = RestMulti
            .fromMultiData(chatService.query(body.input))
            .header("X-Trace-Id", traceId)

        if (sessionToken != null) {
            multi.header("X-Session-Token", sessionToken)
        }

        return multi.build()
    }
}

data class QueryPayload(val input: String, val persona: String)
