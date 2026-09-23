package me.davidgomesdev.ofingidor.backend.llm.config

import io.smallrye.config.ConfigMapping
import java.time.Duration

@ConfigMapping(prefix = "model.bedrock")
interface BedrockConfig {
    fun apiKey(): String

    fun region(): String

    fun timeout(): Duration

    fun chatModel(): ChatModelConfig

    interface ChatModelConfig {
        fun modelId(): String

        fun temperature(): Double

        fun thinking(): Boolean

        fun thinkingBudgetTokens(): Int

        fun maxTokens(): Int
    }
}
