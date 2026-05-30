# Conversation Persistence & History Sidebar — Design Spec

**Date:** 2026-05-30
**Status:** Draft

---

## Overview

Persist CHAT and DEBATE conversations across page refreshes using a `ConversationRepository` abstraction backed by `localStorage` on JS/web. A history sidebar lets users switch between past conversations. The repository interface is designed for easy migration to backend storage in a future iteration.

---

## Scope

- Both CHAT and DEBATE conversations are stored and browsable.
- Conversations survive page refreshes (localStorage on web; in-memory on JVM desktop).
- Full session continuation after restore: `sessionToken` and `traceparent` are persisted so the LLM remembers context.
- Desktop: persistent sidebar panel. Mobile/compact: slide-in drawer with hamburger icon.
- Sidebar filtered by the active `ConversationMode` (CHAT mode shows chat history, DEBATE mode shows debate history).
- Auto-restored from startup is **not** included — the user picks from the sidebar. (Potential follow-up.)

---

## Data Model

New file: `commonMain/model/StoredConversation.kt`

```kotlin
@Serializable
sealed class StoredConversation {
    abstract val id: String         // UUID, assigned at first turn submission
    abstract val createdAt: Long    // epoch millis
    abstract val sessionToken: String?
    abstract val traceparent: String?

    @Serializable
    data class Chat(
        override val id: String,
        override val createdAt: Long,
        override val sessionToken: String?,
        override val traceparent: String?,
        val persona: Persona,
        val turns: List<ConversationTurn>,
    ) : StoredConversation()

    @Serializable
    data class Debate(
        override val id: String,
        override val createdAt: Long,
        override val sessionToken: String?,
        override val traceparent: String?,
        val debatePair: DebatePair,
        val questions: List<DebateQuestionEntry>,
        val turns: List<DebateTurn>,
    ) : StoredConversation()
}
```

**Title derivation (not stored):** `StoredConversation.Chat` → `turns.firstOrNull()?.question`; `StoredConversation.Debate` → `questions.firstOrNull()?.question`. Truncated to 40 characters for display.

**Serialization additions required:** `@Serializable` added to `ConversationTurn`, `DebateTurn`, `Source`, `DebateQuestionEntry`, `DebatePair`, and `Persona` (or use a custom serializer for the enum).

---

## Repository

New file: `commonMain/service/ConversationRepository.kt`

```kotlin
interface ConversationRepository {
    fun loadAll(): List<StoredConversation>
    fun save(conversation: StoredConversation)
    fun delete(id: String)
}
```

### JS actual (`jsMain/service/ConversationRepositoryImpl.kt`)

- Storage key: `"ofingidor_conversations"`
- Serializes the full list as a JSON array via `kotlinx.serialization`.
- `save`: load list → replace-or-insert by `id` → write back.
- `delete`: load list → filter out by `id` → write back.
- `loadAll`: read and deserialize; return empty list on missing/corrupt key.

### JVM actual (`jvmMain/service/ConversationRepositoryImpl.kt`)

- In-memory `MutableList<StoredConversation>`.
- No disk persistence (desktop is a dev target; backend will replace this later).

### Migration path

To move to backend storage: implement `ConversationRepository` as an HTTP client class and inject it instead. No call-site changes in `App.kt`.

---

## App.kt Integration

### Conversation identity

- New state: `var currentConversationId by remember { mutableStateOf<String?>(null) }`
- New state: `var currentConversationCreatedAt by remember { mutableStateOf<Long?>(null) }`
- Both assigned when the first message of a new conversation is submitted. ID is generated as `"${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt(10000)}"` (no extra library needed — `kotlinx.datetime` is already available via Ktor).
- Both cleared by `resetConversationState` — reassigned on next submission.

### Auto-save

Two `LaunchedEffect` blocks, one per mode:

```kotlin
// CHAT
LaunchedEffect(turns.size) {
    val id = currentConversationId ?: return@LaunchedEffect
    if (turns.isEmpty()) return@LaunchedEffect
    repository.save(StoredConversation.Chat(
        id = id,
        createdAt = currentConversationCreatedAt ?: return@LaunchedEffect,
        sessionToken = thinkAPI.conversationState().sessionToken,
        traceparent = thinkAPI.conversationState().traceparent,
        persona = selectedPersona,
        turns = turns.toList(),
    ))
}

// DEBATE
LaunchedEffect(debateQuestions.size) {
    val id = currentConversationId ?: return@LaunchedEffect
    if (debateQuestions.isEmpty()) return@LaunchedEffect
    repository.save(StoredConversation.Debate(...))
}
```

Only complete turns are saved — `ongoingTurn` and `ongoingDebateTurn` are excluded.

### Load on startup

```kotlin
val conversations = remember { mutableStateListOf<StoredConversation>() }
LaunchedEffect(Unit) {
    conversations.addAll(repository.loadAll())
}
```

### Switching conversations

When the user selects a conversation from the sidebar:

1. Save current conversation (if `currentConversationId != null` and turns are non-empty).
2. Clear current state (`clearConversationState()`).
3. Restore `turns` / `debateQuestions` / `debateTurns` from selected `StoredConversation`.
4. Call `thinkAPI.restoreConversation(sessionToken, traceparent)`.
5. Restore `selectedPersona` / `debatePair`.
6. Set `conversationMode` to match (`Chat` → `CHAT`, `Debate` → `DEBATE`).
7. Set `currentConversationId` to the restored `id`.

---

## Sidebar UI

### Layout

**Desktop (>500dp):**
```
Row {
    ConversationSidebar(width = 220.dp)   // persistent, left
    Column { /* existing chat content */ } // right
}
```

**Mobile/compact (≤500dp):**
```
ModalNavigationDrawer(
    drawerContent = { ConversationSidebar() }
) {
    Column { /* existing chat content */ }
}
```

A hamburger icon added to the left of the header (compact only) toggles `drawerState.open()`.

### `ConversationSidebar` composable

New file: `widget/ConversationSidebar.kt`

Parameters:
```kotlin
fun ConversationSidebar(
    conversations: List<StoredConversation>,
    selectedId: String?,
    mode: ConversationMode,
    onSelect: (StoredConversation) -> Unit,
    onDelete: (String) -> Unit,
    onNewConversation: () -> Unit,
)
```

Content:
- "Nova conversa" button at the top.
- `LazyColumn` of conversations filtered by `mode`, sorted newest-first.
- Each row: title (first question, truncated 40 chars) + persona name or `"A vs B"` sub-label.
- Selected row highlighted with `accentColorLight` tint.
- Swipe-to-delete or trash icon button per row.

---

## Error Handling

- `localStorage` write failure (quota exceeded): caught silently, logged via `Napier`. The in-memory state remains correct — the user just won't have persistence for that save.
- Corrupt `localStorage` data on `loadAll`: catch `SerializationException`, log, return empty list (clears corrupted state gracefully).

---

## Testing

- **Unit tests** for `ConversationRepository` JVM impl: save, load, delete, overwrite.
- **Unit tests** for title derivation helper (truncation, empty turns edge case).
- No UI tests for the sidebar — existing rendering tests are smoke tests only.

---

## Out of Scope

- Auto-restoring the last active conversation on page load.
- Conversation search or filtering beyond mode.
- Renaming conversations.
- Export/import of conversation JSON.
- Sidebar on mobile as a bottom sheet alternative.
