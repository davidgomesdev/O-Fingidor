package me.davidgomesdev.ofingidor.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import me.davidgomesdev.ofingidor.shared.dto.ChatEvent
import me.davidgomesdev.ofingidor.shared.dto.DebateEvent
import me.davidgomesdev.ofingidor.shared.dto.Persona
import me.davidgomesdev.ofingidor.ui.model.ConversationMode
import me.davidgomesdev.ofingidor.ui.model.ConversationTurn
import me.davidgomesdev.ofingidor.ui.model.DebatePair
import me.davidgomesdev.ofingidor.ui.model.DebateQuestionEntry
import me.davidgomesdev.ofingidor.ui.model.DebateSide
import me.davidgomesdev.ofingidor.ui.model.DebateTurn
import me.davidgomesdev.ofingidor.ui.model.OngoingConversationTurn
import me.davidgomesdev.ofingidor.ui.model.Source
import me.davidgomesdev.ofingidor.ui.model.chatHeroQuote
import me.davidgomesdev.ofingidor.ui.model.debateHeroQuotes
import me.davidgomesdev.ofingidor.ui.model.nextQuoteIndex
import me.davidgomesdev.ofingidor.ui.model.pick
import me.davidgomesdev.ofingidor.ui.service.ThinkAPI
import me.davidgomesdev.ofingidor.ui.service.formatChatConversation
import me.davidgomesdev.ofingidor.ui.service.formatDebateConversation
import me.davidgomesdev.ofingidor.ui.service.shareConversation
import me.davidgomesdev.ofingidor.ui.widget.AiBubble
import me.davidgomesdev.ofingidor.ui.widget.AppHeader
import me.davidgomesdev.ofingidor.ui.widget.Atmosphere
import me.davidgomesdev.ofingidor.ui.widget.CenteredUserBubble
import me.davidgomesdev.ofingidor.ui.widget.DebatePersonaBubble
import me.davidgomesdev.ofingidor.ui.widget.ErrorBubble
import me.davidgomesdev.ofingidor.ui.widget.UserBubble
import kotlin.random.Random

private val COMPACT_BREAKPOINT = 600.dp
private val WIDE_BREAKPOINT = 1080.dp

internal data class DevModeDisabledState(
    val selectedPersona: Persona,
    val debatePair: DebatePair,
    val shouldResetConversation: Boolean,
)

internal typealias DebateSubmitHandler = suspend (
    question: String,
    pair: DebatePair,
    thinkAPI: ThinkAPI,
    turns: SnapshotStateList<DebateTurn>,
    getOngoingTurn: () -> DebateTurn?,
    setOngoingTurn: (DebateTurn?) -> Unit,
    onTraceId: (String) -> Unit,
    onFailure: (Throwable) -> Unit,
) -> Result<Unit>

internal typealias ThinkEventsCollector = suspend (
    thinkAPI: ThinkAPI,
    question: String,
    persona: Persona,
    getOngoingTurn: () -> OngoingConversationTurn?,
    onNewEvent: (OngoingConversationTurn) -> Unit,
    onError: (Throwable) -> Unit,
    onTraceId: (String) -> Unit,
) -> Unit

internal fun devModeDisabledState(
    selectedPersona: Persona,
    debatePair: DebatePair,
): DevModeDisabledState {
    val normalizedSelectedPersona =
        if (selectedPersona == Persona.O_FINGIDOR) {
            Persona.FERNANDO_PESSOA
        } else {
            selectedPersona
        }
    val normalizedDebatePair =
        if (debatePair.left == Persona.O_FINGIDOR || debatePair.right == Persona.O_FINGIDOR) {
            DebatePair(Persona.FERNANDO_PESSOA, Persona.ALBERTO_CAEIRO)
        } else {
            debatePair
        }

    return DevModeDisabledState(
        selectedPersona = normalizedSelectedPersona,
        debatePair = normalizedDebatePair,
        shouldResetConversation = normalizedSelectedPersona != selectedPersona || normalizedDebatePair != debatePair,
    )
}

internal fun debateFeedItemCount(
    questionCount: Int,
    debateTurnCount: Int,
    hasOngoingQuestion: Boolean,
    hasOngoingTurn: Boolean,
    hasDebateError: Boolean,
): Int =
    questionCount +
        debateTurnCount +
        (if (hasOngoingQuestion) 1 else 0) +
        (if (hasOngoingTurn) 1 else 0) +
        (if (hasDebateError) 1 else 0)

@Composable
@Preview
@Suppress("kotlin:S3776")
fun App() {
    val thinkAPI = remember { ThinkAPI() }

    MysticTheme {
        var inputText by remember { mutableStateOf("") }
        val turns = remember { mutableStateListOf<ConversationTurn>() }
        var ongoingTurn by remember { mutableStateOf<OngoingConversationTurn?>(null) }
        var ongoingTurnError by remember { mutableStateOf<Throwable?>(null) }
        var conversationTraceId by remember { mutableStateOf("") }
        var selectedPersona by remember { mutableStateOf(Persona.FERNANDO_PESSOA) }
        var isDevMode by remember { mutableStateOf(false) }
        var conversationMode by remember { mutableStateOf(ConversationMode.CHAT) }
        var debatePair by remember {
            mutableStateOf(DebatePair(Persona.FERNANDO_PESSOA, Persona.ALBERTO_CAEIRO))
        }
        val debateTurns = remember { mutableStateListOf<DebateTurn>() }
        var ongoingDebateTurn by remember { mutableStateOf<DebateTurn?>(null) }
        val debateQuestions = remember { mutableStateListOf<DebateQuestionEntry>() }
        var ongoingDebateQuestion by remember { mutableStateOf<String?>(null) }
        var ongoingDebateStartOffset by remember { mutableStateOf<Int?>(null) }
        var debateError by remember { mutableStateOf<Throwable?>(null) }
        var debateNextSlot by remember { mutableStateOf(DebateSide.RIGHT) }
        var debateQuoteIndex by remember { mutableStateOf(Random.nextInt(debateHeroQuotes.size)) }
        val scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()

        val hasChatConversationStarted = turns.isNotEmpty() || ongoingTurn != null || ongoingTurnError != null
        val hasDebateConversationStarted =
            debateQuestions.isNotEmpty() || ongoingDebateQuestion != null || ongoingDebateTurn != null || debateError != null
        val hasConversationStarted =
            when (conversationMode) {
                ConversationMode.CHAT -> hasChatConversationStarted
                ConversationMode.DEBATE -> hasDebateConversationStarted
            }
        val isLoading =
            when (conversationMode) {
                ConversationMode.CHAT -> ongoingTurn != null
                ConversationMode.DEBATE -> ongoingDebateQuestion != null
            }
        val feedItemCount =
            when (conversationMode) {
                ConversationMode.CHAT -> {
                    turns.size + if (ongoingTurn != null || ongoingTurnError != null) 1 else 0
                }

                ConversationMode.DEBATE -> {
                    debateFeedItemCount(
                        questionCount = debateQuestions.size,
                        debateTurnCount = debateTurns.size,
                        hasOngoingQuestion = ongoingDebateQuestion != null,
                        hasOngoingTurn = ongoingDebateTurn != null,
                        hasDebateError = debateError != null,
                    )
                }
            }

        AutoScrollEffect(turnCount = feedItemCount, scrollState = scrollState)

        val clearConversationState: () -> Unit = {
            turns.clear()
            ongoingTurn = null
            ongoingTurnError = null
            debateQuestions.clear()
            debateTurns.clear()
            ongoingDebateQuestion = null
            ongoingDebateStartOffset = null
            ongoingDebateTurn = null
            debateError = null
            conversationTraceId = ""
        }

        val resetConversationState: () -> Unit = {
            clearConversationState()
            inputText = ""
            thinkAPI.resetConversation()
        }

        val onDevModeToggle: () -> Unit = {
            isDevMode = !isDevMode
            if (!isDevMode) {
                val disabledState =
                    devModeDisabledState(
                        selectedPersona = selectedPersona,
                        debatePair = debatePair,
                    )
                selectedPersona = disabledState.selectedPersona
                if (disabledState.shouldResetConversation) {
                    debatePair = disabledState.debatePair
                    resetConversationState()
                } else {
                    debatePair = disabledState.debatePair
                }
            }
        }

        val onNewConversation: () -> Unit = resetConversationState

        val onShare: () -> Unit = {
            val text =
                when (conversationMode) {
                    ConversationMode.CHAT -> formatChatConversation(turns)
                    ConversationMode.DEBATE -> formatDebateConversation(debateQuestions, debateTurns)
                }
            if (text.isNotBlank()) shareConversation(text)
        }

        val onModeSelected: (ConversationMode) -> Unit = { mode ->
            if (mode != conversationMode) {
                resetConversationState()
                conversationMode = mode
                if (mode == ConversationMode.DEBATE) {
                    debateQuoteIndex = nextQuoteIndex(debateQuoteIndex, debateHeroQuotes.size) { Random.nextInt(it) }
                }
            }
        }

        val onSubmit: () -> Unit = onSubmit@{
            if (inputText.isBlank() || isLoading) return@onSubmit

            val question = inputText.trim()
            inputText = ""

            coroutineScope.launch {
                when (conversationMode) {
                    ConversationMode.CHAT -> {
                        ongoingTurnError = null
                        val result =
                            processSubmit(
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
                                inputText = question
                            },
                        )
                    }

                    ConversationMode.DEBATE -> {
                        debateError = null
                        val initialTurnCount = debateTurns.size
                        ongoingDebateQuestion = question
                        ongoingDebateStartOffset = initialTurnCount

                        try {
                            val result =
                                performDebateSubmit(
                                    question = question,
                                    pair = debatePair,
                                    thinkAPI = thinkAPI,
                                    turns = debateTurns,
                                    getOngoingTurn = { ongoingDebateTurn },
                                    setOngoingTurn = { ongoingDebateTurn = it },
                                    onTraceId = { if (conversationTraceId.isBlank()) conversationTraceId = it },
                                )

                            result.fold(
                                onSuccess = {
                                    debateQuestions +=
                                        DebateQuestionEntry(
                                            question = question,
                                            startOffset = initialTurnCount,
                                        )
                                },
                                onFailure = {
                                    while (debateTurns.size > initialTurnCount) {
                                        debateTurns.removeAt(debateTurns.lastIndex)
                                    }
                                    ongoingDebateTurn = null
                                    debateError = it
                                    inputText = question
                                },
                            )
                        } finally {
                            ongoingDebateQuestion = null
                            ongoingDebateStartOffset = null
                        }
                    }
                }
            }
        }

        val onPersonaPicked: (Persona) -> Unit = { persona ->
            when (conversationMode) {
                ConversationMode.CHAT -> {
                    selectedPersona = persona
                }

                ConversationMode.DEBATE -> {
                    val pick = debatePair.pick(persona, debateNextSlot)
                    debatePair = pick.pair
                    debateNextSlot = pick.nextSlot
                }
            }
        }

        BoxWithConstraints(Modifier.fillMaxSize().background(inkColor)) {
            val isCompact = maxWidth < COMPACT_BREAKPOINT
            val isWide = maxWidth >= WIDE_BREAKPOINT

            Atmosphere()
            Column(Modifier.fillMaxSize()) {
                AppHeader(
                    mode = conversationMode,
                    onModeSelected = onModeSelected,
                    devMode = isDevMode,
                    hasConversationStarted = hasConversationStarted,
                    onDevModeToggle = onDevModeToggle,
                    onNewConversation = onNewConversation,
                    onShare = onShare,
                    isCompact = isCompact,
                )

                val inputState =
                    InputState(
                        text = inputText,
                        onTextChange = { inputText = it },
                        isLoading = isLoading,
                        onSubmit = onSubmit,
                        placeholder =
                            when (conversationMode) {
                                ConversationMode.CHAT -> "Escreve o que te inquieta a alma…"
                                ConversationMode.DEBATE -> "Lança uma pergunta aos dois…"
                            },
                    )
                val voices =
                    VoicesState(
                        mode = conversationMode,
                        selectedPersona = selectedPersona,
                        debatePair = debatePair,
                        debateNextSlot = debateNextSlot,
                        devMode = isDevMode,
                        onPersonaPicked = onPersonaPicked,
                        onSlotSelected = { debateNextSlot = it },
                        onSwap = { debatePair = debatePair.swapped() },
                    )

                if (!hasConversationStarted) {
                    LandingScreen(
                        voices = voices,
                        input = inputState,
                        quote = if (conversationMode == ConversationMode.CHAT) chatHeroQuote else debateHeroQuotes[debateQuoteIndex],
                        onQuerySelected = { inputText = it },
                        isCompact = isCompact,
                        isWide = isWide,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    ConversationScreen(
                        voices = voices,
                        input = inputState,
                        scrollState = scrollState,
                        isCompact = isCompact,
                        isWide = isWide,
                        modifier = Modifier.weight(1f),
                    ) {
                        ConversationFeed(
                            conversationMode = conversationMode,
                            turns = turns,
                            ongoingTurn = ongoingTurn,
                            ongoingTurnError = ongoingTurnError,
                            debatePair = debatePair,
                            debateQuestions = debateQuestions,
                            debateTurns = debateTurns,
                            ongoingDebateQuestion = ongoingDebateQuestion,
                            ongoingDebateStartOffset = ongoingDebateStartOffset,
                            ongoingDebateTurn = ongoingDebateTurn,
                            debateError = debateError,
                            isDevMode = isDevMode,
                            conversationTraceId = conversationTraceId,
                        )
                    }
                }
            }
        }
    }
}

internal suspend fun processSubmit(
    question: String,
    selectedPersona: Persona,
    thinkAPI: ThinkAPI,
    getOngoingTurn: () -> OngoingConversationTurn?,
    setOngoingTurn: (OngoingConversationTurn?) -> Unit,
    onTraceId: (String) -> Unit,
    collectEvents: ThinkEventsCollector = ::collectThinkEvents,
): Result<ConversationTurn?> {
    setOngoingTurn(OngoingConversationTurn(question = question, persona = selectedPersona))

    var error: Throwable? = null

    collectEvents(
        thinkAPI,
        question,
        selectedPersona,
        getOngoingTurn,
        setOngoingTurn,
        { error = it },
        onTraceId,
    )

    val completedTurn = getOngoingTurn()?.toConversationTurn()

    setOngoingTurn(null)

    return error?.let { Result.failure(it) } ?: Result.success(completedTurn)
}

internal suspend fun performDebateSubmit(
    question: String,
    pair: DebatePair,
    thinkAPI: ThinkAPI,
    turns: SnapshotStateList<DebateTurn>,
    getOngoingTurn: () -> DebateTurn?,
    setOngoingTurn: (DebateTurn?) -> Unit,
    onTraceId: (String) -> Unit,
    submitDebate: DebateSubmitHandler = ::processDebateSubmit,
): Result<Unit> =
    try {
        submitDebate(
            question,
            pair,
            thinkAPI,
            turns,
            getOngoingTurn,
            setOngoingTurn,
            onTraceId,
        ) { thinkAPI.resetConversation() }
    } catch (error: Throwable) {
        thinkAPI.resetConversation()
        Result.failure(error)
    }

private suspend fun processDebateSubmit(
    question: String,
    pair: DebatePair,
    thinkAPI: ThinkAPI,
    turns: SnapshotStateList<DebateTurn>,
    getOngoingTurn: () -> DebateTurn?,
    setOngoingTurn: (DebateTurn?) -> Unit,
    onTraceId: (String) -> Unit,
    onFailure: (Throwable) -> Unit,
): Result<Unit> =
    processDebateEvents(
        events = thinkAPI.sendDebateRequest(query = question, pair = pair),
        onEvent = { event ->
            handleDebateEvent(
                event = event,
                pair = pair,
                turns = turns,
                getOngoingTurn = getOngoingTurn,
                setOngoingTurn = setOngoingTurn,
                onTraceId = onTraceId,
            )
        },
        onCleanup = { setOngoingTurn(null) },
        onFailure = onFailure,
    )

internal suspend fun processDebateEvents(
    events: Flow<Result<DebateEvent>>,
    onEvent: (DebateEvent) -> Unit,
    onCleanup: () -> Unit,
    onFailure: (Throwable) -> Unit,
): Result<Unit> {
    var error: Throwable? = null

    try {
        events.collect { result ->
            result.fold(
                onSuccess = onEvent,
                onFailure = {
                    error = it
                    onFailure(it)
                },
            )
        }
    } catch (failure: Throwable) {
        error = failure
        onFailure(failure)
    } finally {
        onCleanup()
    }

    return error?.let { Result.failure(it) } ?: Result.success(Unit)
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

        is ChatEvent.Token -> {
            onNewEvent(turn.copy(message = turn.message + event.value))
        }

        is ChatEvent.Sources -> {
            onNewEvent(turn.copy(sources = event.items.map(Source::from)))
        }

        is ChatEvent.Done -> {}
    }
}

internal fun handleDebateEvent(
    event: DebateEvent,
    pair: DebatePair,
    turns: SnapshotStateList<DebateTurn>,
    getOngoingTurn: () -> DebateTurn?,
    setOngoingTurn: (DebateTurn?) -> Unit,
    onTraceId: (String) -> Unit,
) {
    when (event) {
        is DebateEvent.Start -> {
            onTraceId(event.traceId)
        }

        is DebateEvent.TurnStart -> {
            val speaker = Persona.entries.first { it == event.speaker }
            pair.sideFor(speaker)
            setOngoingTurn(DebateTurn(turnIndex = event.turnIndex, speaker = speaker))
        }

        is DebateEvent.Token -> {
            val current = getOngoingTurn() ?: return
            setOngoingTurn(current.copy(message = current.message + event.value))
        }

        is DebateEvent.Sources -> {
            val current = getOngoingTurn() ?: return
            setOngoingTurn(current.copy(sources = event.sources.items.map(Source::from)))
        }

        is DebateEvent.TurnDone -> {
            val current = getOngoingTurn() ?: return
            turns += current.copy(isComplete = true)
            setOngoingTurn(null)
        }

        DebateEvent.Done -> {}
    }
}

@Composable
private fun AutoScrollEffect(
    turnCount: Int,
    scrollState: ScrollState,
) {
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
    conversationMode: ConversationMode,
    turns: List<ConversationTurn>,
    ongoingTurn: OngoingConversationTurn?,
    ongoingTurnError: Throwable?,
    debatePair: DebatePair,
    debateQuestions: List<DebateQuestionEntry>,
    debateTurns: List<DebateTurn>,
    ongoingDebateQuestion: String?,
    ongoingDebateStartOffset: Int?,
    ongoingDebateTurn: DebateTurn?,
    debateError: Throwable?,
    isDevMode: Boolean,
    conversationTraceId: String,
) {
    when (conversationMode) {
        ConversationMode.CHAT -> {
            turns.forEach { turn ->
                SelectionContainer { CompletedTurnView(turn) }
            }

            ongoingTurn?.let { turn ->
                SelectionContainer { OngoingTurnView(turn) }
            }

            ongoingTurnError?.let { error ->
                ErrorBubble(errorDetail = if (isDevMode) error.message else null)
            }
        }

        ConversationMode.DEBATE -> {
            val completedTurnUpperBound = ongoingDebateStartOffset ?: debateTurns.size

            debateQuestions.forEachIndexed { index, entry ->
                SelectionContainer { CenteredUserBubble(question = entry.question) }
                val nextOffset = debateQuestions.getOrNull(index + 1)?.startOffset ?: completedTurnUpperBound
                debateTurns
                    .subList(entry.startOffset, nextOffset)
                    .forEach { turn ->
                        SelectionContainer {
                            DebateTurnView(turn = turn, pair = debatePair, isLoading = false)
                        }
                    }
            }

            ongoingDebateQuestion?.let { question ->
                SelectionContainer { CenteredUserBubble(question = question) }
                val startOffset = ongoingDebateStartOffset ?: debateTurns.size
                debateTurns.drop(startOffset).forEach { turn ->
                    SelectionContainer {
                        DebateTurnView(turn = turn, pair = debatePair, isLoading = false)
                    }
                }
            }

            ongoingDebateTurn?.let { turn ->
                SelectionContainer {
                    DebateTurnView(turn = turn, pair = debatePair, isLoading = true)
                }
            }

            debateError?.let { error ->
                ErrorBubble(errorDetail = if (isDevMode) error.message else null)
            }
        }
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
        AiBubble(
            persona = turn.persona,
            message = turn.message,
            sources = turn.sources,
            isLoading = false,
        )
    }
}

@Composable
private fun OngoingTurnView(turn: OngoingConversationTurn) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        UserBubble(question = turn.question)
        AiBubble(
            persona = turn.persona,
            message = turn.message,
            sources = turn.sources,
            isLoading = true,
        )
    }
}

@Composable
private fun DebateTurnView(
    turn: DebateTurn,
    pair: DebatePair,
    isLoading: Boolean,
) {
    DebatePersonaBubble(
        speaker = turn.speaker,
        side = pair.sideFor(turn.speaker),
        message = turn.message,
        sources = turn.sources,
        isLoading = isLoading,
    )
}
