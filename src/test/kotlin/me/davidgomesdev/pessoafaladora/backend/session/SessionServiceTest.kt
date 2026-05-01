package me.davidgomesdev.pessoafaladora.backend.session

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import jakarta.inject.Inject
import jakarta.ws.rs.NotAuthorizedException
import jakarta.ws.rs.WebApplicationException
import me.davidgomesdev.pessoafaladora.backend.model.Persona
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@QuarkusTest
@TestProfile(SessionServiceTestProfile::class)
class SessionServiceTest {

    @Inject
    lateinit var sessionService: SessionService

    @Test
    fun `createSession returns a valid JWT containing conversationId`() {
        val token = sessionService.createSession(Persona.ALBERTO_CAEIRO)
        assertNotNull(token)
        val conversationId = sessionService.extractConversationId(token)
        assertNotNull(conversationId)
        assertTrue(conversationId.isNotBlank())
    }

    @Test
    fun `extractConversationId throws NotAuthorizedException for invalid token`() {
        assertThrows(NotAuthorizedException::class.java) {
            sessionService.extractConversationId("not.a.valid.jwt")
        }
    }

    @Test
    fun `extractConversationId throws NotAuthorizedException for empty token`() {
        assertThrows(NotAuthorizedException::class.java) {
            sessionService.extractConversationId("")
        }
    }

    @Test
    fun `getPersona returns persona stored during createSession`() {
        val token = sessionService.createSession(Persona.RICARDO_REIS)
        val conversationId = sessionService.extractConversationId(token)
        val persona = sessionService.getPersona(conversationId)
        assertEquals(Persona.RICARDO_REIS, persona)
    }

    @Test
    fun `getPersona throws 404 for unknown conversationId`() {
        val ex = assertThrows(WebApplicationException::class.java) {
            sessionService.getPersona("00000000-0000-0000-0000-000000000000")
        }
        assertEquals(404, ex.response.status)
    }
}
