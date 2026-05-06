package me.davidgomesdev.ofingidor.ui

import me.davidgomesdev.ofingidor.ui.model.OngoingConversationTurn
import me.davidgomesdev.ofingidor.ui.model.Source
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComposeAppWebTest {

    private val sampleSource = Source(1L, "Mensagem", "Fernando Pessoa", "Poesia", 92)

    @Test
    fun ongoingTurn_defaultsToEmptyMessageAndSources() {
        val turn = OngoingConversationTurn(question = "Quem és?", personaName = "Fernando Pessoa")
        assertEquals("", turn.message)
        assertTrue(turn.sources.isEmpty())
        assertEquals("", turn.traceId)
    }

    @Test
    fun ongoingTurn_toConversationTurn_mapsAllFields() {
        val ongoing = OngoingConversationTurn(
            question = "O que é o amor?",
            message = "O amor é...",
            sources = listOf(sampleSource),
            traceId = "abc123",
            personaName = "Fernando Pessoa"
        )
        val completed = ongoing.toConversationTurn()
        assertEquals(ongoing.question, completed.question)
        assertEquals(ongoing.message, completed.message)
        assertEquals(ongoing.sources, completed.sources)
        assertEquals(ongoing.traceId, completed.traceId)
        assertEquals(ongoing.personaName, completed.personaName)
    }

    @Test
    fun ongoingTurn_appendsToken() {
        val turn = OngoingConversationTurn(question = "Quem és?", message = "Eu sou ", personaName = "Fernando Pessoa")
        val updated = turn.copy(message = turn.message + "Fernando.")
        assertEquals("Eu sou Fernando.", updated.message)
    }
}
