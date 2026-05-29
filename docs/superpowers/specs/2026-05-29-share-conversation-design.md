# Share Conversation Feature — Design Spec

**Date:** 2026-05-29  
**Status:** Draft

---

## Overview

Add a share button to the app header that lets users export the current conversation (questions and AI replies) as
formatted text. On mobile web the OS native share sheet is used; on desktop the text is copied to the clipboard.

---

## Scope

- Supports both **CHAT** and **DEBATE** conversation modes.
- Only completed turns are included (not an in-flight/loading turn).
- Error bubbles are excluded from the shared output.
- The JVM desktop target gets clipboard copy; the JS/web target gets Web Share API with clipboard fallback.

---

## Shared Text Format

### CHAT mode

Each turn is separated by `---`. User questions are labelled `Pergunta:`. AI replies are labelled with the persona's
display name. Sources (if any) appear on the line immediately after the reply.

```
Pergunta: Como é que vês o tempo?

Fernando Pessoa: O tempo é uma ilusão que o homem criou para se sentir eterno…
Fontes: Mensagem (1934), Livro do Desassossego

---

Pergunta: E a saudade?

Fernando Pessoa: A saudade é a presença da ausência…
```

### DEBATE mode

User questions are labelled `Pergunta:`. Each debate speaker turn is labelled with its persona's display name.
Multiple speakers per question are shown in order.

```
Pergunta: O que é a saudade?

Fernando Pessoa: A saudade é a presença da ausência…
Fontes: Mensagem (1934)

Alberto Caeiro: Eu não tenho saudades. Tenho apenas sensações…
```

---

## Architecture

### New files

| File | Purpose |
|------|---------|
| `composeApp/src/commonMain/.../service/ShareFormatter.kt` | Pure formatting functions for CHAT and DEBATE conversations |
| `composeApp/src/jsMain/.../service/ShareService.kt` | JS `actual` implementation — Web Share API + clipboard fallback |
| `composeApp/src/jvmMain/.../service/ShareService.kt` | JVM `actual` implementation — AWT clipboard |

### Modified files

| File | Change |
|------|--------|
| `composeApp/src/commonMain/.../service/ShareService.kt` | `expect fun shareConversation(text: String)` declaration |
| `composeApp/src/commonMain/.../widget/Drawing.kt` | Add `onShare: (() -> Unit)?` parameter to `AppHeader`; render share button when non-null |
| `composeApp/src/commonMain/.../App.kt` | Pass `hasConversationStarted` guard and `onShare` lambda to `AppHeader` |

---

## Component Details

### `ShareFormatter.kt`

```kotlin
fun formatChatConversation(turns: List<ConversationTurn>): String
fun formatDebateConversation(
    questions: List<DebateQuestionEntry>,
    turns: List<DebateTurn>,
): String
```

Both are pure functions with no side effects — straightforward to unit test.

- `formatChatConversation` iterates `turns`, emits `Pergunta: <question>\n\n<persona>: <message>` and optionally
  `Fontes: <csv of source titles>`, sections separated by `\n\n---\n\n`.
- `formatDebateConversation` interleaves `debateQuestions` (by `startOffset`) with `debateTurns` (by `turnIndex`) to
  reconstruct the chronological order before formatting.
- `DebateQuestionEntry` is currently a `private data class` inside `App.kt`. It must be moved to
  `model/DebateModels.kt` and made `internal` so `ShareFormatter` can reference it from `commonMain`.

### `expect fun shareConversation(text: String)`

**JS actual:**
```kotlin
actual fun shareConversation(text: String) {
    val shareData = js("({ title: 'O Fingidor', text: text })")
    val nav = js("navigator")
    if (js("'share' in navigator") as Boolean) {
        nav.share(shareData).catch { _ -> nav.clipboard.writeText(text) }
    } else {
        nav.clipboard.writeText(text)
    }
}
```

**JVM actual:**
```kotlin
actual fun shareConversation(text: String) {
    val selection = StringSelection(text)
    Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
}
```

### Share button in `AppHeader`

- Visible only when `onShare != null` (i.e. `hasConversationStarted`).
- Uses `Icons.Default.Share` icon.
- On click: calls `onShare()` and temporarily switches button label/icon to show feedback
  (`"Partilhado!"` on mobile / `"Copiado!"` on desktop) for 2 seconds via a `LaunchedEffect + delay(2000)`.

---

## Data Flow

```
App.kt
  ├─ formats conversation text via ShareFormatter
  ├─ calls shareConversation(text)        ← platform-specific
  └─ passes onShare lambda to AppHeader
       └─ AppHeader renders share button → onClick → onShare()
```

---

## Error Handling

- JS: `navigator.share()` rejection (e.g. user cancels) is caught and silently ignored — this is normal behaviour.
  Any other JS error falls back to clipboard.
- JVM: AWT clipboard write is synchronous and unlikely to fail; no special handling needed.
- If both clipboard and share API fail on JS, the button returns to its default state silently (no crash).

---

## Testing

- **Unit tests** (`ShareFormatterTest.kt` in `commonTest`): cover CHAT single turn, CHAT multiple turns, CHAT turn
  with sources, DEBATE single question with two speakers, DEBATE multiple questions.
- **No UI tests added** for the button itself — existing `ComposeAppWebTest` covers rendering smoke tests and adding
  a share-button-specific test would require mocking platform APIs.

---

## Out of Scope

- Sharing as an image/screenshot.
- Persisting or publishing the conversation to a URL.
- Including ongoing/in-progress turns.
- Sharing individual turns (the button shares the full conversation).
