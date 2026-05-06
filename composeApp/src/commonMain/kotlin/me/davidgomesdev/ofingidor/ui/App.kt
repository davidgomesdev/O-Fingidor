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
import me.davidgomesdev.ofingidor.ui.model.ConversationTurn
import me.davidgomesdev.ofingidor.ui.model.OngoingConversationTurn
import me.davidgomesdev.ofingidor.ui.model.Persona
import me.davidgomesdev.ofingidor.ui.model.Source
import me.davidgomesdev.ofingidor.ui.service.ThinkAPI
import me.davidgomesdev.ofingidor.ui.widget.AiBubble
import me.davidgomesdev.ofingidor.ui.widget.AppHeader
import me.davidgomesdev.ofingidor.ui.widget.ErrorBubble
import me.davidgomesdev.ofingidor.ui.widget.PersonaTab
import me.davidgomesdev.ofingidor.ui.widget.ThinkInputCard
import me.davidgomesdev.ofingidor.ui.widget.UserBubble

@Composable
@Preview
fun App() {
    val thinkAPI = remember { ThinkAPI() }

    MaterialTheme(typography = RobotoTypography()) {
        val textFieldState = remember { TextFieldState("") }
        val turns = remember { mutableStateListOf<ConversationTurn>() }
        var ongoingTurn by remember { mutableStateOf<OngoingConversationTurn?>(null) }
        var ongoingTurnError by remember { mutableStateOf<Throwable?>(null) }
        var conversationTraceId by remember { mutableStateOf("") }
        var selectedPersona by remember { mutableStateOf(Persona.FERNANDO_PESSOA) }
        var isDevMode by remember { mutableStateOf(false) }
        val scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()

        val hasConversationStarted = turns.isNotEmpty() || ongoingTurn != null || ongoingTurnError != null

        LaunchedEffect(turns.size) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
        LaunchedEffect(Unit) {
            snapshotFlow { scrollState.maxValue }
                .collect { scrollState.animateScrollTo(it) }
        }

        val onDevModeToggle: () -> Unit = {
            isDevMode = !isDevMode
            if (!isDevMode && selectedPersona == Persona.O_FINGIDOR) {
                selectedPersona = Persona.FERNANDO_PESSOA
            }
        }

        val onSubmit: () -> Unit = onSubmit@{
            if (textFieldState.text.isBlank()) return@onSubmit
            if (ongoingTurn != null) return@onSubmit

            val question = textFieldState.text.toString().trim()
            textFieldState.edit { replace(0, length, "") }

            coroutineScope.launch {
                ongoingTurnError = null
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
                    onError = {
                        ongoingTurnError = it; textFieldState.edit {
                        replace(
                            0,
                            length,
                            ongoingTurn?.question ?: ""
                        )
                    }
                    },
                    onTraceId = { if (conversationTraceId.isBlank()) conversationTraceId = it },
                )
                if (ongoingTurnError == null) ongoingTurn?.let { turns.add(it.toConversationTurn()) }
                ongoingTurn = null
                // ongoingTurnError stays set until next submit so ErrorBubble remains visible
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
                isDevMode = isDevMode,
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

                ongoingTurnError?.let { error ->
                    ErrorBubble(errorDetail = if (isDevMode) error.message else null)
                }

                ThinkInputCard(
                    state = textFieldState,
                    isLoading = ongoingTurn != null,
                    onSubmit = onSubmit,
                    onQuerySelected = { query -> textFieldState.edit { replace(0, length, query) } },
                    hasConversationStarted = hasConversationStarted,
                )
                if (isDevMode && conversationTraceId.isNotBlank()) {
                    Text(
                        "trace: $conversationTraceId",
                        color = devChipTextColor.copy(alpha = 0.45f),
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }
    }
}

private suspend fun collectThinkEvents(
    thinkAPI: ThinkAPI,
    question: String,
    persona: Persona,
    getOngoingTurn: () -> OngoingConversationTurn?,
    onNewEvent: (OngoingConversationTurn) -> Unit,
    onError: (Throwable) -> Unit,
    onTraceId: (String) -> Unit,
) {
    thinkAPI.sendThinkRequest(query = question, persona = persona).collect { result ->
        result.fold(
            onSuccess = { event ->
                val turn = getOngoingTurn() ?: return@collect
                when (event) {
                    is ChatEvent.Start -> {
                        onTraceId(event.traceId)
                        onNewEvent(turn.copy(traceId = event.traceId))
                    }

                    is ChatEvent.Token -> onNewEvent(turn.copy(message = turn.message + event.value))
                    is ChatEvent.Sources -> onNewEvent(turn.copy(sources = event.items.map(Source::from)))
                    is ChatEvent.Done -> Unit
                }
            },
            onFailure = { onError(it) },
        )
    }
}

@Composable
private fun StickyHeader(
    selectedPersona: Persona,
    isDevMode: Boolean,
    hasConversationStarted: Boolean,
    onDevModeToggle: () -> Unit,
    onPersonaSelected: (Persona) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AppHeader(
            selectedPersona,
            isDevMode,
            onDevModeToggle,
        )
        if (!hasConversationStarted) {
            PersonaTab(
                selectedPersona,
                onPersonaSelected,
                isDevMode,
            )
            HorizontalDivider(color = cardBorderColor, thickness = 1.dp)
        }
    }
}
