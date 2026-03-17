# AGENTS.md — Pessoa Faladora Backend

## Project Overview

Quarkus + Kotlin backend for **Pessoa Faladora**, a chatbot that answers questions as the Portuguese poet Fernando
Pessoa. It uses a RAG (Retrieval-Augmented Generation) pipeline backed by Qdrant for storing embeddings and Ollama for
querying the chat model, and streams responses to the UI over HTTP.

**Stack:** Quarkus 3.32, Kotlin/JVM 21, LangChain4j (via `quarkus-langchain4j`), Qdrant (vector store), Ollama (LLM +
embeddings), OpenTelemetry (traces via Jaeger).

---

## Architecture

```
src/main/kotlin/me/davidgomesdev/
  Main.kt                  ← @QuarkusMain entry point
  web/
    IndexResource.kt       ← GET / — serves the Qute HTML shell, injects PESSOA_URL
    ThinkingAPI.kt         ← PUT /pensa — streaming chat endpoint
  service/
    ChatService.kt         ← Orchestrates LangChain4j TokenStream → Mutiny Multi<String>
  llm/
    AiAssistant.kt         ← Builds the AiServices Assistant CDI bean
    RAG.kt                 ← Wires RAG pipeline: ingestion, Qdrant store, retriever, query transformer
    TextsContentInjector.kt← Custom ContentInjector: formats retrieved texts into the prompt
    config/
      RAGConfig.kt         ← @ConfigMapping for the `rag.*` config prefix
  observability/
    TracingUtils.kt        ← Helpers: span(), attributes { } builder DSL
  source/
    PessoaModels.kt        ← @Serializable data classes: PessoaCategory, PessoaText
src/main/resources/
  application.yaml         ← All runtime config (Ollama, Qdrant, OTEL, CORS, …)
  system_message.txt       ← LLM system prompt — defines Fernando Pessoa's identity and rules
  templates/index.html     ← Qute template; injects window.PESSOA_URL for the JS UI
assets/
  all_texts.json           ← Full corpus of Fernando Pessoa's texts (categories + texts tree)
```

---

### Key Files

| File                            | Role                                                                                                     |
|---------------------------------|----------------------------------------------------------------------------------------------------------|
| `web/ThinkingAPI.kt`            | `PUT /pensa` — accepts `QueryPayload(input)`, returns `Multi<String>` with OTel span                     |
| `web/IndexResource.kt`          | `GET /` — renders `index.html` Qute template with `pessoa.url` config value                              |
| `service/ChatService.kt`        | Calls `Assistant.chat()`, emits partial tokens, emits `<sources>…</sources>` on retrieval                |
| `llm/HeteronymContext.kt`       | `@RequestScoped` bean — carries the optional heteronym filter for the current request                    |
| `llm/AiAssistant.kt`            | CDI `@Singleton` factory — builds `AiServices` with streaming model, RAG augmentor, listeners            |
| `llm/RAG.kt`                    | Creates/recreates Qdrant collection, ingests documents, builds `ContentRetriever` and `QueryTransformer` |
| `llm/TextsContentInjector.kt`   | Overrides `DefaultContentInjector` to format each retrieved segment with title/category/author           |
| `llm/config/RAGConfig.kt`       | `@ConfigMapping(prefix = "rag")` — typed access to RAG tuning parameters                                 |
| `observability/TracingUtils.kt` | `span()` returns `Span.current()`; `attributes { }` is an `AttributesBuilder` DSL                        |
| `source/PessoaModels.kt`        | `PessoaCategory` / `PessoaText` — used to deserialize `assets/all_texts.json`                            |
| `system_message.txt`            | System prompt loaded via `@SystemMessage(fromResource = "system_message.txt")`                           |

---

## API

### `PUT /pensa`

- **Body:** `{"input": "<question>", "heteronym": "<name>"}`  — `heteronym` is optional
- **Response:** `text/plain` streaming — partial LLM tokens followed by a final `<sources>…</sources>` chunk
- **Header:** `X-Trace-Id` — OTel trace ID for the request
- When `heteronym` is provided the retriever filters to embeddings whose `author` metadata equals that value
  (e.g. `"Alberto Caeiro"`, `"Álvaro de Campos"`, `"Ricardo Reis"`, `"Bernardo Soares"`, `"Fernando Pessoa"`).
  The value is also recorded as a `heteronym` attribute on the OTel span.
- The `<sources>` chunk format (one source per line):
  ```
  - Categoria: <categoryName>
    Título: <title>
    Autor <author> (score: <N>%)
  ```

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

| Key                                                   | Default                  | Notes                                                         |
|-------------------------------------------------------|--------------------------|---------------------------------------------------------------|
| `quarkus.langchain4j.ollama.base-url`                 | `http://127.0.0.1:11434` | Ollama server URL                                             |
| `quarkus.langchain4j.ollama.chat-model.model-id`      | `qwen3:0.6b`             | LLM model for chat                                            |
| `quarkus.langchain4j.ollama.embedding-model.model-id` | `qwen3-embedding:0.6b`   | Embedding model for RAG                                       |
| `quarkus.otel.exporter.otlp.endpoint`                 | `http://localhost:14317` | OTLP gRPC endpoint (Jaeger)                                   |
| `rag.max-results`                                     | `6`                      | Max retrieved chunks per query                                |
| `rag.min-score`                                       | `0.75`                   | Minimum cosine similarity score                               |
| `rag.expand-query`                                    | `false`                  | Enable `ExpandingQueryTransformer`                            |
| `rag.qdrant.host`                                     | `127.0.0.1`              | Qdrant host                                                   |
| `rag.qdrant.collection.name`                          | `pessoa_texts`           | Qdrant collection; `_preview` suffix when `preview-only=true` |
| `pessoa.url`                                          | `http://127.0.0.1:8080`  | Injected into the HTML template as `window.PESSOA_URL`        |
| `preview-only`                                        | `false`                  | Limits corpus to category 33 (Livro do Desassossego)          |
| `recreate.embeddings`                                 | `false`                  | Drop and re-ingest the Qdrant collection on startup           |
| `QUARKUS_HTTP_CORS_ORIGINS`                           | (see yaml)               | Allowed CORS origins (env override)                           |

---

## Developer Workflows

### Run in dev mode (hot-reload)

```shell
./gradlew quarkusDev
```

Or use the convenience script (starts Docker services, sets Java 21, launches a tmux session):

```shell
./scripts/run-dev.sh
```

`PREVIEW_ONLY` and `ALLOWED_ORIGINS` environment variables are forwarded by the script.

### Build a runnable JAR

```shell
./gradlew build
```

### Run tests

```shell
./gradlew test
```

---

## Conventions & Patterns

- **CDI `@Singleton` factory methods in `@ApplicationScoped` beans**: LangChain4j components (`Assistant`,
  `RetrievalAugmentor`, `ContentRetriever`, etc.) are produced via `@Singleton`-annotated functions inside
  `@ApplicationScoped` classes (`AiAssistant`, `RAG`). Follow this pattern when adding new LangChain4j beans.
- **OTel tracing**: Use `span()` to get the current span and `attributes { }` to build `Attributes`. Always close
  scopes in a `finally` block. Spans created manually must be ended with `span.end()`.
- **Streaming via Mutiny**: The LLM token stream is bridged from LangChain4j's `TokenStream` to a
  `Multi<String>` inside `ChatService.query()`. New streaming endpoints should follow the same
  `Multi.createFrom().emitter { }` pattern and use `@Blocking` on the JAX-RS method.
- **Sources sentinel**: `ChatService` emits retrieved sources as a single `<sources>…</sources>` chunk via
  `stream.emit(...)` inside `onRetrieved`. The UI (and any other consumer) must handle this sentinel separately
  from the main token stream.
- **Corpus loading**: `RAG.allTextsByCategory()` deserializes `assets/all_texts.json` into a
  `Map<Pair<Int, String>, List<PessoaText>>` at startup. Category 33 is the preview subset
  (`PREVIEW_CATEGORY_ID`).
- **Dependency versions**: All inline versions live in `build.gradle.kts` as `val` properties at the top of the
  file. The Quarkus BOM version is driven by `gradle.properties` (`quarkusPlatformVersion`). The Qdrant
  LangChain4j dependency is force-resolved due to a broken version in Quarkus' BOM — keep the
  `resolutionStrategy { force(…) }` block when updating.
- **`preview-only` mode**: Injected as a `@ConfigProperty` into `RAG`. When `true`, only texts from the
  *Livro do Desassossego* category are ingested and a `_preview`-suffixed Qdrant collection is used.
