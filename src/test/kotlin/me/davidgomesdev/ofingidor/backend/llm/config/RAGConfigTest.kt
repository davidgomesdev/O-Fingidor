package me.davidgomesdev.ofingidor.backend.llm.config

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

@QuarkusTest
@TestProfile(RAGConfigTestProfile::class)
class RAGConfigTest {
    @Inject
    lateinit var ragConfig: RAGConfig

    @Test
    fun `should load semantic chunking config with defaults`() {
        val semanticConfig = ragConfig.semanticChunking()
        assertFalse(semanticConfig.enabled())
        assertEquals(0.7, semanticConfig.similarityThreshold(), 0.001)
        assertEquals(100, semanticConfig.minChunkSize())
        assertEquals(1000, semanticConfig.maxChunkSize())
    }
}
