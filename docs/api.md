# O Fingidor — Backend API Reference

> **For UI agents:** This document is the authoritative contract between the backend and UI. Implement exactly what is
> described here — no assumptions.

---

## Base URL

Configured via `window.PESSOA_URL` (injected into the HTML at runtime). Default dev value: `http://127.0.0.1:8080`.

All requests go to `{PESSOA_URL}/pensa`.

---

## Endpoint: `PUT /pensa`

Streams a chat response as NDJSON. Each line is a JSON object (a `ChatEvent`). The connection stays open until the
`done` event is emitted.

### Request

**Method:** `PUT`  
**Content-Type:** `application/json`  
**Accept:** `application/x-ndjson`

#### Headers

| Header          | Required | Description                                                                                                   |
|-----------------|----------|---------------------------------------------------------------------------------------------------------------|
| `Authorization` | No       | `Bearer <session-token>` — omit on first message, include on all subsequent messages in the same conversation |

#### Body

```json
{
  "input": "O que é o fingimento para Fernando Pessoa?",
  "persona": "fernando_pessoa"
}
```

| Field     | Type   | Required | Description                                                  |
|-----------|--------|----------|--------------------------------------------------------------|
| `input`   | string | Yes      | User's question. Must be non-blank.                          |
| `persona` | string | Yes      | Persona code name (see Personas section). Must be non-blank. |

---

### Response

**Status:** `200 OK`  
**Content-Type:** `application/x-ndjson`

#### Response Headers

| Header            | Present when                                 | Description                                                                                      |
|-------------------|----------------------------------------------|--------------------------------------------------------------------------------------------------|
| `X-Trace-Id`      | Always                                       | OpenTelemetry trace ID for this request                                                          |
| `X-Session-Token` | First message only (no `Authorization` sent) | JWT session token. Store and send as `Authorization: Bearer <token>` on all subsequent messages. |

#### Streaming Events

Each line in the response body is a JSON object with a `type` discriminator field. Events arrive in this order:

```
start
token (repeating, 1..N times)
sources (once, may have 0 items)
done
```

---

**`start`**

Emitted immediately when the stream begins.

```json
{"type":"start","traceId":"4bf92f3577b34da6a3ce929d0e0e4736"}
```

| Field     | Type   | Description                  |
|-----------|--------|------------------------------|
| `traceId` | string | OTel trace ID (32 hex chars) |

---

**`token`**

One partial text chunk from the LLM. Concatenate all token values in order to build the full response text.

```json
{"type":"token","value":"O fingimento"}
{"type":"token","value":" para Pessoa é"}
{"type":"token","value":" uma forma de criar."}
```

| Field   | Type   | Description        |
|---------|--------|--------------------|
| `value` | string | Partial text chunk |

---

**`sources`**

Emitted once after all tokens. Contains the RAG sources used to answer the question. May be an empty list if the persona
skips RAG (e.g. `o_fingidor`) or no relevant sources were found.

```json
{"type":"sources","items":[
  {"id":42,"title":"Autopsicografia","author":"Fernando Pessoa","category":"Poesia Ortónima","score":93},
  {"id":17,"title":"Livro do Desassossego","author":"Bernardo Soares","category":"Prosa","score":81}
]}
```

| Field              | Type          | Description              |
|--------------------|---------------|--------------------------|
| `items`            | array         | List of source documents |
| `items[].id`       | number (long) | Internal document ID     |
| `items[].title`    | string        | Document title           |
| `items[].author`   | string        | Author display name      |
| `items[].category` | string        | Category label           |
| `items[].score`    | number (int)  | Relevance score 0–100    |

---

**`done`**

Final event. Signals the stream is complete.

```json
{"type":"done","tokensUsed":1240,"timeTaken":"3.14s"}
```

| Field        | Type         | Description                          |
|--------------|--------------|--------------------------------------|
| `tokensUsed` | number (int) | Total LLM tokens consumed            |
| `timeTaken`  | string       | Wall-clock duration (e.g. `"3.14s"`) |

---

### Error Responses

Errors are returned as standard HTTP responses (not NDJSON). The stream does not open.

| Status             | Condition                                                                                |
|--------------------|------------------------------------------------------------------------------------------|
| `400 Bad Request`  | `persona` or `input` field is blank                                                      |
| `401 Unauthorized` | `Authorization` header present but JWT is invalid, malformed, or expired                 |
| `404 Not Found`    | `persona` code name does not match any known persona                                     |
| `404 Not Found`    | Session for the given JWT's conversation ID does not exist in the database               |
| `409 Conflict`     | `persona` in request body does not match the persona stored when the session was created |

---

## Session Flow

Sessions enable multi-turn conversation memory. The backend stores conversation history in PostgreSQL per session.
Session JWTs expire after 1 hour by default.

### First message (no session)

1. Send `PUT /pensa` with no `Authorization` header.
2. Response includes `X-Session-Token: <jwt>` header.
3. Store this token client-side (memory, not localStorage — tokens expire).

### Subsequent messages (continuing conversation)

1. Send `PUT /pensa` with `Authorization: Bearer <token>`.
2. The persona in the request body **must match** the persona used when the session was created. Mismatch → `409`.
3. If token is expired → `401`. Start a new session (send without `Authorization`).
4. No new `X-Session-Token` is returned on continuation requests.

### Session state machine

```
No token → PUT /pensa (no Authorization)
             → 200 + X-Session-Token header
             → store token

Has token → PUT /pensa (Authorization: Bearer <token>)
             → 200 (no X-Session-Token)    ← continue
             → 401                          ← token expired/invalid → start over
             → 409                          ← persona changed → start over or keep token with same persona
```

---

## Personas

| Code name          | Display name     | Category        |
|--------------------|------------------|-----------------|
| `o_fingidor`       | O Fingidor       | Dev (skips RAG) |
| `fernando_pessoa`  | Fernando Pessoa  | Ortónimo        |
| `alberto_caeiro`   | Alberto Caeiro   | Heterónimo      |
| `alvaro_de_campos` | Álvaro de Campos | Heterónimo      |
| `ricardo_reis`     | Ricardo Reis     | Heterónimo      |
| `bernardo_soares`  | Bernardo Soares  | Semi-heterónimo |

---

## Full Example

### First message

**Request:**

```http
PUT /pensa HTTP/1.1
Content-Type: application/json
Accept: application/x-ndjson

{"input":"Quem és tu?","persona":"alberto_caeiro"}
```

**Response headers:**

```http
HTTP/1.1 200 OK
Content-Type: application/x-ndjson
X-Trace-Id: 4bf92f3577b34da6a3ce929d0e0e4736
X-Session-Token: eyJhbGciOiJIUzI1NiJ9.eyJjb252ZXJzYXRpb25JZCI6Ii4uLiJ9.signature
```

**Response body (each line is one NDJSON event):**

```
{"type":"start","traceId":"4bf92f3577b34da6a3ce929d0e0e4736"}
{"type":"token","value":"Sou o guardador de rebanhos."}
{"type":"token","value":" Não tenho pensamentos,"}
{"type":"token","value":" tenho sensações."}
{"type":"sources","items":[{"id":12,"title":"O Guardador de Rebanhos","author":"Alberto Caeiro","category":"Poesia","score":97}]}
{"type":"done","tokensUsed":320,"timeTaken":"1.82s"}
```

### Follow-up message (same conversation)

**Request:**

```http
PUT /pensa HTTP/1.1
Content-Type: application/json
Accept: application/x-ndjson
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJjb252ZXJzYXRpb25JZCI6Ii4uLiJ9.signature

{"input":"E o que pensas sobre Deus?","persona":"alberto_caeiro"}
```

**Response headers:**

```http
HTTP/1.1 200 OK
Content-Type: application/x-ndjson
X-Trace-Id: 9a3f2b1c4d5e6f7a8b9c0d1e2f3a4b5c
```

*(No `X-Session-Token` — continuation request)*
