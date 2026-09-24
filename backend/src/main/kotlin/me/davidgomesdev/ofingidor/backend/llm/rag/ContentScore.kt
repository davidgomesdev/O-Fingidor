package me.davidgomesdev.ofingidor.backend.llm.rag

import dev.langchain4j.rag.content.Content
import dev.langchain4j.rag.content.ContentMetadata

/** Re-ranked content only carries [ContentMetadata.RERANKED_SCORE], otherwise the embedding [ContentMetadata.SCORE]. */
fun Content.score(): Double = (metadata()[ContentMetadata.RERANKED_SCORE] ?: metadata()[ContentMetadata.SCORE]) as? Double ?: 0.0
