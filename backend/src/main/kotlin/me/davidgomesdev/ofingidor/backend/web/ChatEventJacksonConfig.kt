package me.davidgomesdev.ofingidor.backend.web

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.jackson.ObjectMapperCustomizer
import jakarta.inject.Singleton
import me.davidgomesdev.ofingidor.shared.dto.ChatEvent

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ChatEvent.Start::class, name = "start"),
    JsonSubTypes.Type(value = ChatEvent.Token::class, name = "token"),
    JsonSubTypes.Type(value = ChatEvent.Sources::class, name = "sources"),
    JsonSubTypes.Type(value = ChatEvent.Done::class, name = "done"),
)
private interface ChatEventMixin

@Singleton
class ChatEventJacksonConfig : ObjectMapperCustomizer {
    override fun customize(objectMapper: ObjectMapper) {
        objectMapper.addMixIn(ChatEvent::class.java, ChatEventMixin::class.java)
    }
}
