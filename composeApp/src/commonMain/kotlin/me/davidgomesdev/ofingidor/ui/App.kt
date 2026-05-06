package me.davidgomesdev.ofingidor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.davidgomesdev.ofingidor.ui.dto.ChatEvent
import me.davidgomesdev.ofingidor.ui.model.Persona
import me.davidgomesdev.ofingidor.ui.model.Source
import me.davidgomesdev.ofingidor.ui.service.ThinkAPI
import me.davidgomesdev.ofingidor.ui.service.isDevMode
import me.davidgomesdev.ofingidor.ui.widget.AiBubble
import me.davidgomesdev.ofingidor.ui.widget.AppHeader
import me.davidgomesdev.ofingidor.ui.widget.PersonaTab
import me.davidgomesdev.ofingidor.ui.widget.ThinkInputCard
import me.davidgomesdev.ofingidor.ui.widget.UserBubble

data class ConversationTurn(
    val question: String,
    val message: String,
    val sources: List<Source>,
    val traceId: String,
    val personaName: String,
)

data class OngoingConversationTurn(
    val question: String,
    val personaName: String,
    val message: String = "",
    val sources: List<Source> = emptyList(),
    val traceId: String = "",
) {
    fun toConversationTurn() = ConversationTurn(
        question = question,
        message = message,
        sources = sources,
        traceId = traceId,
        personaName = personaName,
    )
}

@Composable
@Preview
fun App() {
    val thinkAPI = remember { ThinkAPI() }

    MaterialTheme(typography = RobotoTypography()) {
        val isDevModeEnabled = remember { isDevMode() }
        val textFieldState = remember { TextFieldState("") }
        val turns = remember { mutableStateListOf<ConversationTurn>() }
        var ongoingTurn by remember { mutableStateOf<OngoingConversationTurn?>(null) }
        var selectedPersona by remember { mutableStateOf(Persona.FERNANDO_PESSOA) }
        var devMode by remember { mutableStateOf(false) }
        val scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()

        val hasConversationStarted = turns.isNotEmpty() || ongoingTurn != null

        LaunchedEffect(turns.size) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
        LaunchedEffect(Unit) {
            snapshotFlow { scrollState.maxValue }
                .collect { scrollState.animateScrollTo(it) }
        }

        val onDevModeToggle: () -> Unit = {
            devMode = !devMode
            if (!devMode && selectedPersona == Persona.O_FINGIDOR) {
                selectedPersona = Persona.FERNANDO_PESSOA
            }
        }

        val onSubmit: () -> Unit = onSubmit@{
            if (textFieldState.text.isBlank()) return@onSubmit
            if (ongoingTurn != null) return@onSubmit

            val question = textFieldState.text.toString().trim()
            textFieldState.edit { replace(0, length, "") }

            coroutineScope.launch {
                ongoingTurn = OngoingConversationTurn(
                    question = question,
                    personaName = selectedPersona.displayName,
                )
                collectThinkEvents(
                    thinkAPI = thinkAPI,
                    question = question,
                    persona = selectedPersona,
                    getOngoingTurn = { ongoingTurn },
                    onNewEvent = { ongoingTurn = it },
                )
                ongoingTurn?.let { turns.add(it.toConversationTurn()) }
                ongoingTurn = null
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .background(backgroundColor)
                .fillMaxSize()
                .sizeIn(maxWidth = 700.dp)
        ) {
            StickyHeader(
                selectedPersona = selectedPersona,
                devMode = devMode,
                isDevModeEnabled = isDevModeEnabled,
                hasConversationStarted = hasConversationStarted,
                onDevModeToggle = onDevModeToggle,
                onPersonaSelected = { selectedPersona = it },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (!hasConversationStarted) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Começa uma conversa...",
                            color = Color(0xFF333333),
                            fontSize = 12.sp,
                        )
                    }
                }

                turns.forEach { turn ->
                    SelectionContainer {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            UserBubble(question = turn.question)
                            AiBubble(
                                message = turn.message,
                                sources = turn.sources,
                                isLoading = false,
                            )
                        }
                    }
                }

                ongoingTurn?.let { turn ->
                    SelectionContainer {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            UserBubble(question = turn.question)
                            AiBubble(
                                message = turn.message,
                                sources = turn.sources,
                                isLoading = true,
                            )
                        }
                    }
                }

                ThinkInputCard(
                    state = textFieldState,
                    isLoading = ongoingTurn != null,
                    onSubmit = onSubmit,
                    onQuerySelected = { query -> textFieldState.edit { replace(0, length, query) } },
                    hasConversationStarted = hasConversationStarted,
                )
            }
        }
    }
}

private suspend fun collectThinkEvents(
    thinkAPI: ThinkAPI,
    question: String,
    persona: Persona,
    getOngoingTurn: () -> OngoingConversationTurn?,
    onNewEvent: (OngoingConversationTurn?) -> Unit,
) {
    thinkAPI.sendThinkRequest(query = question, persona = persona).collect { event ->
        when (event) {
            is ChatEvent.Start -> onNewEvent(getOngoingTurn()?.copy(traceId = event.traceId))
            is ChatEvent.Token -> onNewEvent(getOngoingTurn()?.let { it.copy(message = it.message + event.value) })
            is ChatEvent.Sources -> onNewEvent(getOngoingTurn()?.copy(sources = event.items.map(Source::from)))
            is ChatEvent.Done -> Unit
        }
    }
}

@Composable
private fun StickyHeader(
    selectedPersona: Persona,
    devMode: Boolean,
    isDevModeEnabled: Boolean,
    hasConversationStarted: Boolean,
    onDevModeToggle: () -> Unit,
    onPersonaSelected: (Persona) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AppHeader(
            selectedPersona = selectedPersona,
            devMode = devMode,
            onDevModeToggle = if (isDevModeEnabled) onDevModeToggle else null,
        )
        if (!hasConversationStarted) {
            PersonaTab(
                selectedPersona = selectedPersona,
                onPersonaSelected = onPersonaSelected,
                devMode = devMode,
            )
            HorizontalDivider(color = cardBorderColor, thickness = 1.dp)
        }
    }
}
