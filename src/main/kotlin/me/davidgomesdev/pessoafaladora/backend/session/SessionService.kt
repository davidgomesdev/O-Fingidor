package me.davidgomesdev.pessoafaladora.backend.session

import com.github.f4b6a3.uuid.UuidCreator
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jose.crypto.MACVerifier
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.quarkus.runtime.Startup
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import jakarta.ws.rs.NotAuthorizedException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import me.davidgomesdev.pessoafaladora.backend.model.Persona
import org.jboss.logging.Logger
import java.time.Instant
import java.util.Date
import java.util.UUID

data class ConversationSession(val token: String, val conversationId: String)

@ApplicationScoped
@Startup
class SessionService(private val config: SessionConfig) {

    private val log: Logger = Logger.getLogger(this::class.java)

    private val signer: MACSigner by lazy {
        MACSigner(config.jwt().secret().toByteArray(Charsets.UTF_8))
    }

    private val verifier: MACVerifier by lazy {
        MACVerifier(config.jwt().secret().toByteArray(Charsets.UTF_8))
    }

    @PostConstruct
    fun validateConfig() {
        val secretBytes = config.jwt().secret().toByteArray(Charsets.UTF_8)
        require(secretBytes.size >= 32) {
            "session.jwt.secret must be at least 32 bytes for HS256 (got ${secretBytes.size})"
        }
        log.info(
            "SessionService initialized (JWT TTL: ${config.jwt().ttl()}, memory max: ${
                config.memory().maxMessages()
            })"
        )
    }

    @Transactional
    fun createSession(persona: Persona): ConversationSession {
        val personaEntity = PersonaEntity.findByCodeName(persona.codeName)
            ?: error("Persona '${persona.codeName}' not found in database")
        val conversationId = UuidCreator.getTimeOrderedEpoch()
        val session = SessionEntity()

        session.conversationId = conversationId
        session.persona = personaEntity

        session.persist()

        log.info("New session created: conversationId=$conversationId persona=${persona.codeName}")

        val token = buildJwt(conversationId.toString())

        return ConversationSession(token, conversationId.toString())
    }

    fun extractConversationId(token: String): String {
        return try {
            val jwt = SignedJWT.parse(token)
            if (!jwt.verify(verifier)) {
                log.warn("JWT rejected: invalid signature")
                throw NotAuthorizedException("Invalid JWT signature")
            }
            val expiry = jwt.jwtClaimsSet.expirationTime
            if (expiry != null && expiry.before(Date())) {
                log.warn("JWT rejected: token expired at $expiry")
                throw NotAuthorizedException("JWT expired")
            }
            jwt.jwtClaimsSet.getStringClaim("conversationId")
                ?: throw NotAuthorizedException("Missing conversationId claim")
        } catch (e: NotAuthorizedException) {
            throw e
        } catch (e: Exception) {
            log.warn("JWT rejected: malformed token (${e.message})")
            throw NotAuthorizedException("Malformed JWT")
        }
    }

    @Transactional
    fun getPersona(conversationId: String): Persona {
        val uuid = try {
            UUID.fromString(conversationId)
        } catch (e: IllegalArgumentException) {
            log.warn("Session lookup failed: invalid UUID format '$conversationId'", e)

            throw WebApplicationException(Response.status(404).build())
        }
        val session = SessionEntity.findById(uuid)
            ?: run {
                log.warn("Session not found: conversationId=$conversationId")
                throw WebApplicationException(Response.status(404).build())
            }
        return Persona.entries.firstOrNull { it.codeName == session.persona.codeName }
            ?: error("Unknown persona code '${session.persona.codeName}' in database")
    }

    private fun buildJwt(conversationId: String): String {
        val now = Instant.now()
        val expiry = now.plus(config.jwt().ttl())
        val claims = JWTClaimsSet.Builder()
            .claim("conversationId", conversationId)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(expiry))
            .build()
        val header = JWSHeader(JWSAlgorithm.HS256)
        val jwt = SignedJWT(header, claims)
        jwt.sign(signer)
        return jwt.serialize()
    }
}
