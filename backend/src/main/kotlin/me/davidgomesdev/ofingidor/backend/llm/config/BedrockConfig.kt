package me.davidgomesdev.ofingidor.backend.llm.config

import io.smallrye.config.ConfigMapping
import java.time.Duration
import java.util.Optional

@ConfigMapping(prefix = "model.bedrock")
interface BedrockConfig {
    /** When absent, the default AWS credentials provider chain is used instead. */
    fun apiKey(): Optional<String>

    /** AWS profile from ~/.aws; when absent (and no API key), the default AWS credentials provider chain is used. */
    fun profile(): Optional<String>

    fun region(): String

    fun timeout(): Duration

    fun chatModel(): ChatModelConfig

    interface ChatModelConfig {
        fun modelId(): String

        fun thinking(): Boolean

        fun thinkingBudgetTokens(): Int

        fun maxTokens(): Int
    }
}
