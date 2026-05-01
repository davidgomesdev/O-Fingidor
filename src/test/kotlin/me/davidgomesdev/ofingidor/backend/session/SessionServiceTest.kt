package me.davidgomesdev.ofingidor.backend.session

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import jakarta.inject.Inject
import me.davidgomesdev.ofingidor.backend.model.Persona
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@QuarkusTest
@TestProfile(SessionServiceTestProfile::class)
class SessionServiceTest {

    @Inject
    lateinit var sessionService: SessionService

    @Test
    fun `createSession returns a valid JWT containing conversationId`() {
        val session = sessionService.createSession(Persona.ALBERTO_CAEIRO)
        assertNotNull(session.token)
        assertNotNull(session.conversationId)
        assertTrue(session.conversationId.isNotBlank())
    }

    @Test
    fun `extractConversationId returns Left INVALID_TOKEN for invalid token`() {
        val result = sessionService.extractConversationId("not.a.valid.jwt")
        assertTrue(result.isLeft())
        assertEquals(SessionError.INVALID_TOKEN, result.leftOrNull())
    }

    @Test
    fun `extractConversationId returns Left INVALID_TOKEN for empty token`() {
        val result = sessionService.extractConversationId("")
        assertTrue(result.isLeft())
        assertEquals(SessionError.INVALID_TOKEN, result.leftOrNull())
    }

    @Test
    fun `extractConversationId returns Right with conversationId for valid token`() {
        val session = sessionService.createSession(Persona.ALBERTO_CAEIRO)
        val result = sessionService.extractConversationId(session.token)
        assertTrue(result.isRight())
        assertEquals(session.conversationId, result.getOrNull())
    }

    @Test
    fun `getPersona returns Right with persona stored during createSession`() {
        val session = sessionService.createSession(Persona.RICARDO_REIS)
        val result = sessionService.getPersona(session.conversationId)
        assertTrue(result.isRight())
        assertEquals(Persona.RICARDO_REIS, result.getOrNull())
    }

    @Test
    fun `getPersona returns Left SESSION_NOT_FOUND for unknown conversationId`() {
        val result = sessionService.getPersona("00000000-0000-0000-0000-000000000000")
        assertTrue(result.isLeft())
        assertEquals(SessionError.SESSION_NOT_FOUND, result.leftOrNull())
    }
}
