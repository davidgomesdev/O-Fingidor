# AGENTS.md — O Fingidor (Monorepo)

## Project Overview

Monorepo for **O Fingidor**, a chatbot that answers questions as various personas of the Portuguese poet Fernando
Pessoa.

**Modules:**

- **Root** — Quarkus + Kotlin/JVM backend: RAG pipeline (Qdrant), Ollama/Anthropic Claude LLM, streams NDJSON over HTTP
- **`:composeApp`** — Kotlin Multiplatform + Compose Multiplatform UI: targets JS (web) and JVM (desktop dev). Builds to
  a JS bundle deployed into the backend's static resources.

**Stack:** Quarkus 3.32.2, Kotlin 2.3.0/JVM 21, Gradle 9.1, LangChain4j (`quarkus-langchain4j`), Qdrant (vector
store), Ollama or Anthropic Claude (LLM), OpenTelemetry (traces via Jaeger), Compose Multiplatform 1.10.0, Ktor 3.4.0.

---

## Architecture

### UI module (`composeApp/`)

```
composeApp/src/
  commonMain/   ← All UI logic, Ktor HTTP client, shared widgets
  webMain/      ← Web-specific entry point + resources (index.html, styles.css)
  jsMain/       ← JS-target-specific code (Config.kt actual)
  jvmMain/      ← JVM desktop entry point (Window); Config.kt actual
  webTest/      ← Tests running on JS target
```

Key UI files: `App.kt` (root composable + state), `service/ThinkAPI.kt` (Ktor streaming client),
`service/Config.kt` (expect/actual for platform URL), `widget/Drawing.kt` (AppHeader, FernandoPessoaLogo),
`widget/PersonaSidebar.kt`, `Colors.kt`, `Theme.kt`.

UI strings are in **Portuguese**. Colors from `Colors.kt` only — no hardcoded hex. Access resources via
`ofingidor.composeapp.generated.resources.Res`.

### Backend module (root)

```
src/main/kotlin/me/davidgomesdev/pessoafaladora/backend/
  Main.kt                  ← @QuarkusMain entry point
  web/
    IndexResource.kt       ← GET / — serves the Qute HTML shell, injects PESSOA_URL
    ThinkingAPI.kt         ← PUT /pensa — streaming chat endpoint (NDJSON ChatEvent)
  service/
    ChatService.kt         ← Orchestrates LangChain4j TokenStream → Mutiny Multi<ChatEvent>
  llm/
    AiAssistant.kt         ← Builds the AiServices Assistant CDI bean
    RAG.kt                 ← Wires RAG pipeline: ingestion, Qdrant store, retriever, query transformer
    TextsContentInjector.kt← Custom ContentInjector: formats retrieved texts into the prompt
    PersonaContext.kt      ← @RequestScoped bean — carries persona for the current request
    config/
      RAGConfig.kt         ← @ConfigMapping for the `rag.*` config prefix
      OllamaConfig.kt      ← @ConfigMapping for the `model.ollama.*` config prefix
      AnthropicConfig.kt   ← @ConfigMapping for the `model.anthropic.*` config prefix
    model/
      LanguageModel.kt     ← Interface for language model abstraction
      OllamaLanguageModel.kt  ← Ollama implementation
      AnthropicLanguageModel.kt← Anthropic Claude implementation
      ModelsProducer.kt    ← CDI producer that selects LLM based on config
  observability/
    TracingUtils.kt        ← Helpers: span(), attributes { } builder DSL
  model/
    Persona.kt             ← Enum: Persona (FERNANDO_PESSOA, ALBERTO_CAEIRO, etc.) + PersonaCategory
    PessoaTextModels.kt    ← Data classes: PessoaCategory, PessoaText
  dto/
    ChatEvent.kt           ← Sealed class: Start, Token, Sources, Done events
    PessoaSourceDtos.kt    ← DTOs for source metadata
src/main/resources/
  application.yaml         ← All runtime config (model selection, Ollama, Anthropic, Qdrant, OTEL, CORS, …)
  prompts/
    system_message.txt     ← LLM system prompt — defines Fernando Pessoa's identity and rules
    content_injector.txt   ← Prompt template for injecting retrieved content
  templates/index.html     ← Qute template; injects window.PESSOA_URL for the JS UI
  web/static/              ← UI JS bundle deployed here by :composeApp:deployToBackend
assets/
  all_texts.json           ← Full corpus of Fernando Pessoa's texts (categories + texts tree)
  preview_texts.json       ← Subset of texts for preview mode (category 33 only)
uiResources/
  composeResources/        ← UI compiled resources deployed here by :composeApp:deployToBackend
```

---

### Key Files

| File                            | Role                                                                                                                                           |
|---------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| `web/ThinkingAPI.kt`            | `PUT /pensa` — accepts `QueryPayload(input, persona)`, returns `Multi<ChatEvent>` with OTel span                                               |
| `web/IndexResource.kt`          | `GET /` — renders `index.html` Qute template with `pessoa.url` config value                                                                    |
| `service/ChatService.kt`        | Calls `Assistant.chat()`, emits ChatEvent.Token, ChatEvent.Sources, ChatEvent.Done                                                             |
| `llm/PersonaContext.kt`         | `@RequestScoped` bean — carries the persona for the current request                                                                            |
| `llm/AiAssistant.kt`            | CDI `@Singleton` factory — builds `AiServices` with streaming model, RAG augmentor, listeners                                                  |
| `llm/RAG.kt`                    | Creates/recreates Qdrant collection, ingests documents, builds `ContentRetriever` and `QueryTransformer`. Uses semantic chunking when enabled. |
| `llm/TextsContentInjector.kt`   | Overrides `DefaultContentInjector` to format retrieved segments using `content_injector.txt` template                                          |
| `llm/config/RAGConfig.kt`       | `@ConfigMapping(prefix = "rag")` — typed access to RAG tuning parameters                                                                       |
| `llm/config/OllamaConfig.kt`    | `@ConfigMapping(prefix = "model.ollama")` — Ollama-specific config                                                                             |
| `llm/config/AnthropicConfig.kt` | `@ConfigMapping(prefix = "model.anthropic")` — Anthropic-specific config                                                                       |
| `llm/model/ModelsProducer.kt`   | CDI producer — selects LLM implementation based on `model.name` config                                                                         |
| `observability/TracingUtils.kt` | `span()` returns `Span.current()`; `attributes { }` is an `AttributesBuilder` DSL                                                              |
| `model/Persona.kt`              | Enum of personas (FERNANDO_PESSOA, ALBERTO_CAEIRO, etc.) with display names and categories                                                     |
| `model/PessoaTextModels.kt`     | `PessoaCategory` / `PessoaText` — data classes for corpus structure                                                                            |
| `dto/ChatEvent.kt`              | Sealed class hierarchy for streaming events (Start, Token, Sources, Done)                                                                      |
| `prompts/system_message.txt`    | System prompt loaded via `@SystemMessage(fromResource = "prompts/system_message.txt")`                                                         |
| `prompts/content_injector.txt`  | Prompt template for formatting retrieved content in RAG                                                                                        |

---

## API

### `PUT /pensa`

- **Body:** `{"input": "<question>", "persona": "<codename>"}` — `persona` is required
- **Response:** `application/x-ndjson` streaming — NDJSON ChatEvent objects
- **Header:** `X-Trace-Id` — OTel trace ID for the request
- **Personas:** `o_fingidor` (dev), `fernando_pessoa` (ortónimo), `alberto_caeiro`, `alvaro_de_campos`,
  `ricardo_reis` (heterónimos), `bernardo_soares` (semi-heterónimo)
- When persona is provided the retriever filters to embeddings whose `author` metadata matches that persona's display
  name.
  The persona is also recorded as an attribute on the OTel span.
- **ChatEvent types:**
  - `Start`: `{"type": "start", "traceId": "<id>"}`
  - `Token`: `{"type": "token", "value": "<text>"}`
  - `Sources`:
    `{"type": "sources", "items": [{"id": N, "title": "...", "author": "...", "category": "...", "score": N}]}`
  - `Done`: `{"type": "done", "tokensUsed": N, "timeTaken": "X.XXs"}`

### `GET /`

- Serves `index.html` (Qute template) with `window.PESSOA_URL` injected for the JS UI bundle

---

## Infrastructure (docker-compose)

| Service  | Image                         | Ports (host)                      | Purpose             |
|----------|-------------------------------|-----------------------------------|---------------------|
| `db`     | `qdrant/qdrant:v1.17`         | `6333` (REST), `6334` (gRPC)      | Vector store        |
| `jaeger` | `jaegertracing/jaeger:latest` | `16686` (UI), `14317` (OTLP gRPC) | Distributed tracing |

Start with: `docker compose up -d`

---

## Configuration (`application.yaml` / env overrides)

| Key                                          | Default                  | Notes                                                              |
|----------------------------------------------|--------------------------|--------------------------------------------------------------------|
| `model.name`                                 | `ollama`                 | LLM provider: `ollama` or `anthropic`                              |
| `model.ollama.base-url`                      | `http://127.0.0.1:11434` | Ollama server URL                                                  |
| `model.ollama.timeout`                       | `600s`                   | Ollama request timeout                                             |
| `model.ollama.chat-model.model-id`           | `qwen3:1.7b`             | Ollama LLM model for chat                                          |
| `model.ollama.chat-model.temperature`        | `0.7`                    | Temperature for Ollama chat model                                  |
| `model.ollama.chat-model.thinking`           | `false`                  | Enable thinking/reasoning for Ollama                               |
| `model.ollama.embedding-model.model-id`      | `qwen3-embedding:8b`     | Ollama embedding model for RAG                                     |
| `model.anthropic.api-key`                    | `REPLACE_ME`             | Anthropic API key                                                  |
| `model.anthropic.timeout`                    | `60s`                    | Anthropic request timeout                                          |
| `model.anthropic.chat-model.model-id`        | `claude-haiku-4-5`       | Anthropic model ID (e.g. `claude-haiku-4-5`, `claude-sonnet-4-6`)  |
| `model.anthropic.chat-model.temperature`     | `0.7`                    | Temperature for Anthropic chat model                               |
| `model.anthropic.chat-model.thinking`        | `true`                   | Enable extended thinking for Claude                                |
| `model.anthropic.chat-model.max-tokens`      | `50000`                  | Max output tokens for Anthropic                                    |
| `quarkus.otel.exporter.otlp.endpoint`        | `http://localhost:14317` | OTLP gRPC endpoint (Jaeger)                                        |
| `rag.max-results`                            | `6`                      | Max retrieved chunks per query                                     |
| `rag.min-score`                              | `0.75`                   | Minimum cosine similarity score                                    |
| `rag.expand-query`                           | `false`                  | Enable `ExpandingQueryTransformer`                                 |
| `rag.ingestion-chunk-size`                   | `25`                     | Number of documents to ingest in parallel                          |
| `rag.expanding-query-template`               | (see yaml)               | Portuguese prompt template for query expansion                     |
| `rag.semantic-chunking.enabled`              | `false`                  | Use semantic chunking instead of regex splitting                   |
| `rag.semantic-chunking.similarity-threshold` | `0.7`                    | Cosine similarity threshold for merging adjacent chunks (0.0-1.0)  |
| `rag.semantic-chunking.min-chunk-size`       | `100`                    | Minimum chunk size in chars, force merge below this                |
| `rag.semantic-chunking.max-chunk-size`       | `1000`                   | Maximum chunk size in chars, fallback to sentence split above      |
| `rag.qdrant.host`                            | `127.0.0.1`              | Qdrant host                                                        |
| `rag.qdrant.api-key`                         | (see yaml)               | Qdrant API key (matches docker-compose config)                     |
| `rag.qdrant.collection.name`                 | `pessoa_texts`           | Qdrant collection name; `_preview` suffix when `preview-only=true` |
| `pessoa.url`                                 | `http://127.0.0.1:8080`  | Injected into the HTML template as `window.PESSOA_URL`             |
| `preview-only`                               | `false`                  | Limits corpus to preview subset (uses `preview_texts.json`)        |
| `recreate.embeddings`                        | `false`                  | Drop and re-ingest the Qdrant collection on startup                |
| `QUARKUS_HTTP_CORS_ORIGINS`                  | (see yaml)               | Allowed CORS origins (env override)                                |

---

## Developer Workflows

### Run backend in dev mode (hot-reload)

```shell
./gradlew quarkusDev
```

Or use the convenience script (starts Docker services, sets Java 21, launches a tmux session):

```shell
./scripts/run-dev.sh
```

`PREVIEW_ONLY` and `ALLOWED_ORIGINS` environment variables are forwarded by the script.

### Run UI (JVM desktop, hot-reload)

```shell
./gradlew :composeApp:hotRunJvm --mainClass "me.davidgomesdev.ofingidor.ui.MainKt" --quiet
```

### Run UI (web dev server)

```shell
./gradlew :composeApp:jsBrowserDevelopmentRun
```

### Deploy UI to backend static resources

```shell
./gradlew :composeApp:deployToBackend
```

Builds the JS bundle and copies it to `src/main/resources/web/static/` and `uiResources/composeResources/`.

### Build backend JAR

```shell
./gradlew build
```

### Run tests

```shell
./gradlew test                        # backend tests
./gradlew :composeApp:jsBrowserTest   # UI tests
```

---

## Conventions & Patterns

- **CDI `@Singleton` factory methods in `@ApplicationScoped` beans**: LangChain4j components (`Assistant`,
  `RetrievalAugmentor`, `ContentRetriever`, etc.) are produced via `@Singleton`-annotated functions inside
  `@ApplicationScoped` classes (`AiAssistant`, `RAG`). Follow this pattern when adding new LangChain4j beans.
- **LLM provider abstraction**: The `LanguageModel` interface allows switching between Ollama and Anthropic.
  `ModelsProducer` creates the appropriate implementation based on `model.name` config. Both implementations
  support streaming, temperature, and thinking/reasoning modes.
- **OTel tracing**: Use `span()` to get the current span and `attributes { }` to build `Attributes`. Always close
  scopes in a `finally` block. Spans created manually must be ended with `span.end()`.
- **Streaming via Mutiny**: The LLM token stream is bridged from LangChain4j's `TokenStream` to a
  `Multi<ChatEvent>` inside `ChatService.query()`. New streaming endpoints should follow the same
  `Multi.createFrom().emitter { }` pattern and use `@Blocking` on the JAX-RS method.
- **ChatEvent streaming**: `ChatService` emits structured events: `Start` (with trace ID), `Token` (partial text),
  `Sources` (retrieved documents with metadata and scores), `Done` (token counts and timing). All events are
  serialized as NDJSON.
- **Corpus loading**: `RAG.allTextsByCategory()` deserializes `assets/all_texts.json` (or `preview_texts.json` if
  `preview-only=true`) into a `Map<Pair<Int, String>, List<PessoaText>>` at startup.
- **Persona-based filtering**: The `PersonaContext` request-scoped bean carries the selected persona.
  `RAG.contentRetriever()` applies a metadata filter to retrieve only texts authored by the selected persona's
  display name (e.g., "Alberto Caeiro").
- **Content injection**: `TextsContentInjector` uses a customizable prompt template (`content_injector.txt`) to
  format retrieved content before injection into the LLM context. The template can be overridden at runtime by
  placing a local copy in the working directory.
- **Dependency versions**: All inline versions live in `build.gradle.kts` as `val` properties at the top of the
  file. The Quarkus BOM version is driven by `gradle.properties` (`quarkusPlatformVersion`). The Qdrant
  LangChain4j dependency is force-resolved due to a broken version in Quarkus' BOM — keep the
  `resolutionStrategy { force(…) }` block when updating.
- **`preview-only` mode**: Injected as a `@ConfigProperty` into `RAG`. When `true`, loads `preview_texts.json`
  instead of the full corpus and uses a `_preview`-suffixed Qdrant collection.
- **Semantic chunking**: When enabled (`rag.semantic-chunking.enabled=true`), uses `SemanticDocumentSplitter`
  instead of regex-based splitting. Splits on paragraph boundaries, embeds segments, and merges based on
  cosine similarity to preserve semantic coherence. Falls back to sentence splitting for oversized paragraphs.
  Tunable via `similarity-threshold` (0.6-0.8), `min-chunk-size`, and `max-chunk-size` config.
