package me.davidgomesdev.pessoafaladora.backend.llm

import com.github.f4b6a3.uuid.UuidCreator
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.ChatMessageDeserializer
import dev.langchain4j.data.message.ChatMessageSerializer
import dev.langchain4j.store.memory.chat.ChatMemoryStore
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.time.OffsetDateTime
import java.util.UUID

@ApplicationScoped
@Transactional
class PostgresConversationStore : ChatMemoryStore {

    override fun getMessages(memoryId: Any): List<ChatMessage> {
        val conversationId = UUID.fromString(memoryId.toString())
        return ChatMemoryEntity.findByConversationIdOrdered(conversationId)
            .map { ChatMessageDeserializer.messageFromJson(it.messageJson) }
    }

    override fun updateMessages(memoryId: Any, messages: List<ChatMessage>) {
        val conversationId = UUID.fromString(memoryId.toString())
        ChatMemoryEntity.deleteByConversationId(conversationId)
        val now = OffsetDateTime.now()
        messages.forEach { message ->
            val entity = ChatMemoryEntity()
            entity.id = UuidCreator.getTimeOrderedEpoch()
            entity.conversationId = conversationId
            entity.messageJson = ChatMessageSerializer.messageToJson(message)
            entity.createdAt = now
            entity.persist()
        }
    }

    override fun deleteMessages(memoryId: Any) {
        val conversationId = UUID.fromString(memoryId.toString())
        ChatMemoryEntity.deleteByConversationId(conversationId)
    }
}
