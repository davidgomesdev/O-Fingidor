package me.davidgomesdev.llm.config

import io.smallrye.config.ConfigMapping

@ConfigMapping(prefix = "rag")
interface RAGConfig {
    fun expandQuery(): Boolean
    fun expandingQueryTemplate(): String
    fun maxResults(): Int
    fun minScore(): Double
    fun qdrant(): QdrantConfig

    interface QdrantConfig {
        fun host(): String
        fun apiKey(): String
        fun collection(): CollectionConfig

        @SuppressWarnings("kotlin:S6517")
        interface CollectionConfig {
            fun name(): String
        }
    }
}
