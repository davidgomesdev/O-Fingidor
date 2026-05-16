package me.davidgomesdev.ofingidor.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.davidgomesdev.ofingidor.shared.dto.ChatEvent
import me.davidgomesdev.ofingidor.shared.dto.DebateEvent
import me.davidgomesdev.ofingidor.ui.model.DebatePair
import me.davidgomesdev.ofingidor.ui.model.DebateSide
import me.davidgomesdev.ofingidor.ui.model.DebateTurn
import me.davidgomesdev.ofingidor.ui.model.OngoingConversationTurn
import me.davidgomesdev.ofingidor.ui.model.Persona
import me.davidgomesdev.ofingidor.ui.model.Source
import me.davidgomesdev.ofingidor.ui.service.ThinkAPI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
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

    @Test
    fun debatePair_sideForMapsLeftAndRightSpeakers() {
        val pair = DebatePair(Persona.FERNANDO_PESSOA, Persona.ALBERTO_CAEIRO)

        assertEquals(DebateSide.LEFT, pair.sideFor(Persona.FERNANDO_PESSOA))
        assertEquals(DebateSide.RIGHT, pair.sideFor(Persona.ALBERTO_CAEIRO))
    }

    @Test
    fun debatePair_sideForRejectsUnknownSpeaker() {
        val pair = DebatePair(Persona.FERNANDO_PESSOA, Persona.ALBERTO_CAEIRO)

        assertFailsWith<IllegalStateException> {
            pair.sideFor(Persona.RICARDO_REIS)
        }
    }

    @Test
    fun disablingDevMode_normalizesOFingidorDebatePairAndRequiresConversationReset() {
        val result = devModeDisabledState(
            selectedPersona = Persona.FERNANDO_PESSOA,
            debatePair = DebatePair(Persona.O_FINGIDOR, Persona.ALBERTO_CAEIRO),
        )

        assertEquals(Persona.FERNANDO_PESSOA, result.selectedPersona)
        assertEquals(DebatePair(Persona.FERNANDO_PESSOA, Persona.ALBERTO_CAEIRO), result.debatePair)
        assertTrue(result.shouldResetConversation)
    }

    @Test
    fun disablingDevMode_normalizesOFingidorSelectedPersonaAndRequiresConversationReset() {
        val result = devModeDisabledState(
            selectedPersona = Persona.O_FINGIDOR,
            debatePair = DebatePair(Persona.FERNANDO_PESSOA, Persona.ALBERTO_CAEIRO),
        )

        assertEquals(Persona.FERNANDO_PESSOA, result.selectedPersona)
        assertEquals(DebatePair(Persona.FERNANDO_PESSOA, Persona.ALBERTO_CAEIRO), result.debatePair)
        assertTrue(result.shouldResetConversation)
    }

    @Test
    fun debateFeedItemCount_countsQuestionTurnAndErrorExplicitly() {
        val count = debateFeedItemCount(
            questionCount = 2,
            debateTurnCount = 3,
            hasOngoingQuestion = true,
            hasOngoingTurn = true,
            hasDebateError = true,
        )

        assertEquals(8, count)
    }

    @Test
    fun debateTurn_defaultsToEmptyMessageAndSources() {
        val turn = DebateTurn(turnIndex = 1, speaker = Persona.RICARDO_REIS)

        assertEquals("", turn.message)
        assertTrue(turn.sources.isEmpty())
        assertEquals(false, turn.isComplete)
    }

    @Test
    fun debateBubblePalette_usesPersonaSpecificColors() {
        val palette = debateBubblePalette(Persona.ALVARO_DE_CAMPOS)

        assertEquals(Color(0xFF1F1713), palette.background)
        assertEquals(Color(0xFF9A5A3A), palette.border)
        assertEquals(Color(0xFFF2C5AE), palette.label)
    }

    @Test
    fun processDebateEvents_returnsFailureAndCleansUpWhenEventHandlerThrows() = runTest {
        var cleanupCalls = 0
        var failures = 0

        val result = processDebateEvents(
            events = flowOf(Result.success(DebateEvent.Start("trace-123", "fernando_pessoa", "alberto_caeiro"))),
            onEvent = { throw IllegalStateException("bad debate event") },
            onCleanup = { cleanupCalls += 1 },
            onFailure = { failures += 1 },
        )

        assertEquals("bad debate event", result.exceptionOrNull()?.message)
        assertEquals(1, cleanupCalls)
        assertEquals(1, failures)
    }

    @Test
    fun performDebateSubmit_resetsClientConversationWhenDebateRequestFails() = runTest {
        val thinkAPI = ThinkAPI()

        thinkAPI.restoreConversation(
            sessionToken = "session-token",
            traceparent = "00-traceparent",
        )

        val result = performDebateSubmit(
            question = "Debatam isto",
            pair = DebatePair(Persona.FERNANDO_PESSOA, Persona.ALBERTO_CAEIRO),
            thinkAPI = thinkAPI,
            turns = mutableStateListOf(),
            getOngoingTurn = { null },
            setOngoingTurn = {},
            onTraceId = {},
            submitDebate = { _: String, _: DebatePair, _: ThinkAPI, _: SnapshotStateList<DebateTurn>, _: () -> DebateTurn?, _: (DebateTurn?) -> Unit, _: (String) -> Unit, onFailure: (Throwable) -> Unit ->
                val failure = IllegalStateException("debate request failed")
                onFailure(failure)
                Result.failure(failure)
            },
        )

        assertEquals("debate request failed", result.exceptionOrNull()?.message)
        assertEquals(
            ThinkAPI.ConversationState(sessionToken = null, traceparent = null),
            thinkAPI.conversationState(),
        )
    }

    @Test
    fun handleDebateEvent_buildsAndCompletesSpeakerTurn() {
        val turns = mutableStateListOf<DebateTurn>()
        var ongoing: DebateTurn? = null
        var traceId = ""
        val pair = DebatePair(Persona.FERNANDO_PESSOA, Persona.ALBERTO_CAEIRO)
        val eventSource = ChatEvent.Sources.Source(
            id = 1L,
            title = "Poema",
            author = "Alberto Caeiro",
            category = "Poesia",
            score = 97,
        )

        handleDebateEvent(
            event = DebateEvent.Start(
                traceId = "trace-123",
                personaA = Persona.FERNANDO_PESSOA.codeName,
                personaB = Persona.ALBERTO_CAEIRO.codeName,
            ),
            pair = pair,
            turns = turns,
            getOngoingTurn = { ongoing },
            setOngoingTurn = { ongoing = it },
            onTraceId = { traceId = it },
        )

        handleDebateEvent(
            event = DebateEvent.TurnStart(turnIndex = 0, speaker = Persona.ALBERTO_CAEIRO.codeName),
            pair = pair,
            turns = turns,
            getOngoingTurn = { ongoing },
            setOngoingTurn = { ongoing = it },
            onTraceId = { traceId = it },
        )
        handleDebateEvent(
            event = DebateEvent.Token(
                turnIndex = 0,
                speaker = Persona.ALBERTO_CAEIRO.codeName,
                value = "Sou ",
            ),
            pair = pair,
            turns = turns,
            getOngoingTurn = { ongoing },
            setOngoingTurn = { ongoing = it },
            onTraceId = { traceId = it },
        )
        handleDebateEvent(
            event = DebateEvent.Token(
                turnIndex = 0,
                speaker = Persona.ALBERTO_CAEIRO.codeName,
                value = "eu.",
            ),
            pair = pair,
            turns = turns,
            getOngoingTurn = { ongoing },
            setOngoingTurn = { ongoing = it },
            onTraceId = { traceId = it },
        )
        handleDebateEvent(
            event = DebateEvent.Sources(
                turnIndex = 0,
                speaker = Persona.ALBERTO_CAEIRO.codeName,
                items = listOf(eventSource),
            ),
            pair = pair,
            turns = turns,
            getOngoingTurn = { ongoing },
            setOngoingTurn = { ongoing = it },
            onTraceId = { traceId = it },
        )
        handleDebateEvent(
            event = DebateEvent.TurnDone(
                turnIndex = 0,
                speaker = Persona.ALBERTO_CAEIRO.codeName,
                tokensUsed = 12,
                timeTaken = "0.4s",
            ),
            pair = pair,
            turns = turns,
            getOngoingTurn = { ongoing },
            setOngoingTurn = { ongoing = it },
            onTraceId = { traceId = it },
        )

        assertEquals("trace-123", traceId)
        assertNull(ongoing)
        assertEquals(1, turns.size)
        assertEquals(Persona.ALBERTO_CAEIRO, turns.single().speaker)
        assertEquals("Sou eu.", turns.single().message)
        assertEquals(listOf(Source.from(eventSource)), turns.single().sources)
        assertTrue(turns.single().isComplete)
    }
}
