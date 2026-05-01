package me.davidgomesdev.pessoafaladora.backend.llm

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.f4b6a3.uuid.UuidCreator
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import me.davidgomesdev.pessoafaladora.backend.dto.ChatEvent
import java.time.OffsetDateTime
import java.util.UUID

@ApplicationScoped
class ChatHistoryRepository(private val objectMapper: ObjectMapper) {

    @Transactional
    fun persist(
        conversationId: UUID,
        userMessage: String,
        aiResponse: String,
        sources: List<ChatEvent.Sources.Source>,
    ) {
        ChatHistoryEntity().also { entity ->
            entity.id = UuidCreator.getTimeOrderedEpoch()
            entity.conversationId = conversationId
            entity.userMessage = userMessage
            entity.aiResponse = aiResponse
            entity.sourcesJson = if (sources.isEmpty()) null else objectMapper.writeValueAsString(sources)
            entity.createdAt = OffsetDateTime.now()
        }.persist()
    }
}
