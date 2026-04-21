# Semantic Text Chunking for RAG Ingestion

**Date:** 2026-04-20  
**Status:** Approved  
**Context:** Improve RAG text ingestion quality by replacing regex-based chunking with semantic coherence-based
splitting

## Problem

Current ingestion uses `DocumentByRegexSplitter` that splits on `\n\n` and `\n` with fixed size limits (900/300 chars).
This causes quality issues:

- Splits mid-thought: poems and paragraphs broken at arbitrary points
- Poor context preservation: related content separated across chunks
- No structure awareness: poetry stanzas, prose paragraphs not respected
- Fixed sizes don't adapt to content semantics

Result: suboptimal retrieval relevance because chunks lack semantic coherence.

## Solution

Implement semantic chunking that uses embedding similarity to find natural breakpoints and keep related content
together.

### Approach

**Custom Semantic Chunker** — build a `SemanticDocumentSplitter` that:

1. Splits document on paragraph boundaries (`\n\n`)
2. Embeds all candidate segments using existing embedding model
3. Computes cosine similarity between adjacent segments
4. Merges segments while similarity > threshold AND size < max
5. Respects min/max chunk size constraints

This gives full control over chunking logic, allows tuning for Portuguese poetry/prose, and produces semantically
coherent chunks.

## Architecture

### New Component

**`SemanticDocumentSplitter`**

Implements LangChain4j `DocumentSplitter` interface. Injected into `RAG.kt` replacing current splitter.

```kotlin
class SemanticDocumentSplitter(
    private val embeddingModel: EmbeddingModel,
    private val minChunkSize: Int,
    private val maxChunkSize: Int,
    private val similarityThreshold: Double
) : DocumentSplitter {
    override fun split(document: Document): List<TextSegment>
}
```

### Integration Point

`RAG.kt`:

```kotlin
val splitter = if (config.semanticChunking().enabled()) {
    SemanticDocumentSplitter(
        embeddingModel,
        config.semanticChunking().minChunkSize(),
        config.semanticChunking().maxChunkSize(),
        config.semanticChunking().similarityThreshold()
    )
} else {
    // Existing regex splitter
    DocumentByRegexSplitter("\n\n", "\n", 900, 0, DocumentBySentenceSplitter(300, 0))
}
```

## Algorithm

### Core Logic

```
1. Initial Split
   - Split document on \n\n (paragraph/stanza boundaries)
   - Filter out empty segments
   
2. Batch Embedding
   - Embed all segments using embeddingModel.embedAll()
   - Efficient: single batch call instead of per-segment
   
3. Coherence-Based Merging
   For each adjacent pair (i, i+1):
     - Compute cosine similarity between embeddings
     - If similarity > threshold AND merged_size < maxChunkSize:
         - Merge segments (concatenate text with \n\n separator)
         - Discard individual embeddings (merged chunk re-embedded during ingestion)
     - Else:
         - Finalize chunk i, start new chunk at i+1
   
4. Size Constraints
   - If chunk < minChunkSize: force merge with next/previous
   - If chunk > maxChunkSize: fall back to sentence splitting
   
5. Return Final Chunks
   - Convert to TextSegment with preserved metadata
```

### Similarity Metric

Use `CosineSimilarity.between(embedding1, embedding2)` from LangChain4j. Returns value 0.0-1.0 where:

- 1.0 = identical semantic content
- 0.7-0.9 = highly related
- 0.5-0.7 = moderately related
- < 0.5 = weakly related

### Edge Cases

**Giant paragraph > maxChunkSize:**

- Fall back to `DocumentBySentenceSplitter(maxChunkSize, 0)`
- Recursively split oversized segment into sentence-level chunks
- Log warning for config tuning
- Maintain metadata through fallback

**Tiny segments < minChunkSize:**

- Force merge with adjacent chunk
- Prefer merging with higher similarity neighbor
- Last chunk: merge backward if undersized

**Empty segments after split:**

- Filter out before embedding
- Preserve original segment indices for merge tracking

## Configuration

### application.yaml

```yaml
rag:
  semantic-chunking:
    enabled: true
    similarity-threshold: 0.7  # 0.0-1.0, higher = stricter coherence
    min-chunk-size: 100        # chars, force merge below this
    max-chunk-size: 1000       # chars, hard limit per chunk
```

### Tuning Guidance

**similarity-threshold (0.6-0.8 typical, start at 0.7):**

- Lower (0.6): bigger chunks, more context, broader retrieval
- Higher (0.8): smaller chunks, more precision, narrower retrieval

**min-chunk-size (100-200 chars):**

- Too low: fragment overhead, poor context
- Too high: forced merges of unrelated content

**max-chunk-size (800-1200 chars):**

- Balance: LLM context window vs embedding precision
- Persona's corpus: poetry (smaller) vs prose (larger)

### Config Interface

```kotlin
interface RAGConfig {
    // ... existing methods
    fun semanticChunking(): SemanticChunkingConfig

    interface SemanticChunkingConfig {
        fun enabled(): Boolean
        fun similarityThreshold(): Double
        fun minChunkSize(): Int
        fun maxChunkSize(): Int
    }
}
```

## Observability

### OpenTelemetry Tracing

**New span:** `rag.semantic_chunking`

**Attributes:**

- `document_count` (long) — total documents to chunk
- `total_segments` (long) — initial paragraph splits
- `final_chunks` (long) — merged chunk count
- `avg_similarity` (double) — mean similarity across all merges
- `time_spent_ms` (long) — chunking duration
- `fallback_count` (long) — times fell back to sentence split

**Events:**

1. "Embedded segments" — batch embedding complete
    - `segment_count`, `embedding_time_ms`
2. "Merged chunks" — coherence-based merge done
    - `merge_count`, `avg_similarity`
3. "Fallback to sentence split" — oversized paragraph split
    - `segment_index`, `segment_size`

### Logging

```kotlin
log.info("Semantic chunking: ${documents.size} docs → $totalSegments segments → $finalChunks chunks (took $duration)")
log.debug("Chunk $i: ${chunk.text.length} chars, similarity to prev: $similarity")
log.warn("Fallback to sentence split for segment $i (${segment.length} chars > max $maxChunkSize)")
```

**Log warnings on:**

- Frequent fallbacks to sentence splitting (indicates max size too small)
- Chunks hitting maxChunkSize repeatedly (indicates threshold too low)
- Very low similarity scores < 0.3 (indicates poor semantic boundary detection)

## Implementation Steps

1. Create `SemanticDocumentSplitter.kt` in `llm/` package
2. Update `RAGConfig.kt` interface with `SemanticChunkingConfig`
3. Update `application.yaml` with semantic chunking config
4. Modify `RAG.kt` to conditionally use semantic splitter
5. Add OTel span and logging
6. Test with preview corpus, tune threshold
7. Enable for full corpus

## Testing Strategy

1. **Unit tests:** Mock embedding model, verify merge logic with known similarities
2. **Integration test:** Use real embedding model on sample Portuguese poetry/prose
3. **Quality assessment:** Compare retrieval results before/after on test queries
4. **Performance:** Measure ingestion time increase (acceptable per requirements)

## Success Criteria

- Chunks respect semantic boundaries (no mid-thought splits)
- Retrieval relevance improves on test query set
- Config is tunable without code changes
- Observability shows chunking quality metrics
- Fallback handling preserves all content

## Migration

**Phase 1:** Add feature flag (`enabled: false` by default)  
**Phase 2:** Enable for preview corpus, tune threshold  
**Phase 3:** Enable for full corpus after validation  
**Phase 4:** Remove old splitter after stable period

**Rollback:** Set `enabled: false`, restart service
