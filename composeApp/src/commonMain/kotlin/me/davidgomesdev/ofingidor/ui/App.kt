package me.davidgomesdev.ofingidor.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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

private val COMPACT_BREAKPOINT = 500.dp

@Composable
@Preview
@Suppress("kotlin:S3776")
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

        AutoScrollEffect(turnCount = turns.size, scrollState = scrollState)

        val onDevModeToggle: () -> Unit = {
            isDevMode = !isDevMode
            if (!isDevMode && selectedPersona == Persona.O_FINGIDOR) {
                selectedPersona = Persona.FERNANDO_PESSOA
            }
        }

        val onNewConversation: () -> Unit = {
            turns.clear()
            ongoingTurn = null
            ongoingTurnError = null
            conversationTraceId = ""
            textFieldState.edit { replace(0, length, "") }
            thinkAPI.resetConversation()
        }

        val onSubmit: () -> Unit = onSubmit@{
            if (textFieldState.text.isBlank() || ongoingTurn != null) return@onSubmit

            val question = textFieldState.text.toString().trim()
            textFieldState.edit { replace(0, length, "") }

            coroutineScope.launch {
                ongoingTurnError = null
                val result = processSubmit(
                    question = question,
                    selectedPersona = selectedPersona,
                    thinkAPI = thinkAPI,
                    getOngoingTurn = { ongoingTurn },
                    setOngoingTurn = { ongoingTurn = it },
                    onTraceId = { if (conversationTraceId.isBlank()) conversationTraceId = it },
                )
                result.fold(
                    onSuccess = { turn -> turn?.let { turns.add(it) } },
                    onFailure = {
                        ongoingTurnError = it
                        textFieldState.edit { replace(0, length, question) }
                    },
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .background(backgroundColor)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            val isCompact = maxWidth < COMPACT_BREAKPOINT
            val horizontalPadding = if (isCompact) 12.dp else 24.dp

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .sizeIn(maxWidth = 700.dp)
            ) {
                StickyHeader(
                    selectedPersona = selectedPersona,
                    isDevMode = isDevMode,
                    hasConversationStarted = hasConversationStarted,
                    onDevModeToggle = onDevModeToggle,
                    onNewConversation = onNewConversation,
                    onPersonaSelected = { selectedPersona = it },
                    isCompact = isCompact,
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = horizontalPadding, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ConversationFeed(
                        turns = turns,
                        ongoingTurn = ongoingTurn,
                        ongoingTurnError = ongoingTurnError,
                        isDevMode = isDevMode,
                        conversationTraceId = conversationTraceId,
                        hasConversationStarted = hasConversationStarted,
                    )
                    ThinkInputCard(
                        state = textFieldState,
                        isLoading = ongoingTurn != null,
                        onSubmit = onSubmit,
                        onQuerySelected = { query -> textFieldState.edit { replace(0, length, query) } },
                        hasConversationStarted = hasConversationStarted,
                        isCompact = isCompact,
                    )
                }
            }
        }
    }
}

private suspend fun processSubmit(
    question: String,
    selectedPersona: Persona,
    thinkAPI: ThinkAPI,
    getOngoingTurn: () -> OngoingConversationTurn?,
    setOngoingTurn: (OngoingConversationTurn?) -> Unit,
    onTraceId: (String) -> Unit,
): Result<ConversationTurn?> {
    setOngoingTurn(OngoingConversationTurn(question = question, personaName = selectedPersona.displayName))

    var error: Throwable? = null

    collectThinkEvents(
        thinkAPI = thinkAPI,
        question = question,
        persona = selectedPersona,
        getOngoingTurn = getOngoingTurn,
        onNewEvent = setOngoingTurn,
        onError = { error = it },
        onTraceId = onTraceId,
    )

    val completedTurn = getOngoingTurn()?.toConversationTurn()

    setOngoingTurn(null)

    return error?.let { Result.failure(it) } ?: Result.success(completedTurn)
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
                handleChatEvent(event, turn, onTraceId, onNewEvent)
            },
            onFailure = onError,
        )
    }
}

private fun handleChatEvent(
    event: ChatEvent,
    turn: OngoingConversationTurn,
    onTraceId: (String) -> Unit,
    onNewEvent: (OngoingConversationTurn) -> Unit,
) {
    when (event) {
        is ChatEvent.Start -> {
            onTraceId(event.traceId)
            onNewEvent(turn.copy(traceId = event.traceId))
        }

        is ChatEvent.Token -> onNewEvent(turn.copy(message = turn.message + event.value))
        is ChatEvent.Sources -> onNewEvent(turn.copy(sources = event.items.map(Source::from)))
        is ChatEvent.Done -> Unit
    }
}

@Composable
private fun AutoScrollEffect(turnCount: Int, scrollState: ScrollState) {
    LaunchedEffect(turnCount) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }
    LaunchedEffect(Unit) {
        snapshotFlow { scrollState.maxValue }
            .collect { scrollState.animateScrollTo(it) }
    }
}

@Composable
private fun ConversationFeed(
    turns: List<ConversationTurn>,
    ongoingTurn: OngoingConversationTurn?,
    ongoingTurnError: Throwable?,
    isDevMode: Boolean,
    conversationTraceId: String,
    hasConversationStarted: Boolean,
) {
    if (!hasConversationStarted) {
        Box(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Começa uma conversa...", color = Color(0xFF333333), fontSize = 12.sp)
        }
    }

    turns.forEach { turn ->
        SelectionContainer { CompletedTurnView(turn) }
    }

    ongoingTurn?.let { turn ->
        SelectionContainer { OngoingTurnView(turn) }
    }

    ongoingTurnError?.let { error ->
        ErrorBubble(errorDetail = if (isDevMode) error.message else null)
    }

    if (isDevMode && conversationTraceId.isNotBlank()) {
        SelectionContainer {
            Text(
                "trace: $conversationTraceId",
                color = devChipTextColor.copy(alpha = 0.45f),
                fontSize = 10.sp,
                letterSpacing = 0.5.sp,
            )
        }
    }
}

@Composable
private fun CompletedTurnView(turn: ConversationTurn) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        UserBubble(question = turn.question)
        AiBubble(message = turn.message, sources = turn.sources, isLoading = false)
    }
}

@Composable
private fun OngoingTurnView(turn: OngoingConversationTurn) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        UserBubble(question = turn.question)
        AiBubble(message = turn.message, sources = turn.sources, isLoading = true)
    }
}

@Composable
private fun StickyHeader(
    selectedPersona: Persona,
    isDevMode: Boolean,
    hasConversationStarted: Boolean,
    onDevModeToggle: () -> Unit,
    onNewConversation: () -> Unit,
    onPersonaSelected: (Persona) -> Unit,
    isCompact: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AppHeader(
            selectedPersona,
            isDevMode,
            hasConversationStarted,
            onDevModeToggle,
            onNewConversation,
            isCompact = isCompact,
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
