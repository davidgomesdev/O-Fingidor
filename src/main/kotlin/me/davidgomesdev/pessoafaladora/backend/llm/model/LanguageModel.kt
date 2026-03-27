package me.davidgomesdev.pessoafaladora.backend.llm.model

import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.embedding.EmbeddingModel

interface LanguageModel {
    fun chatModel(): ChatModel
    fun streamingChatModel(): StreamingChatModel
    fun embeddingModel(): EmbeddingModel
}
