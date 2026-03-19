package me.davidgomesdev.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = ChatEvent.Token::class, name = "token"),
    JsonSubTypes.Type(value = ChatEvent.Sources::class, name = "sources"),
    JsonSubTypes.Type(value = ChatEvent.Done::class, name = "done"),
)
sealed class ChatEvent {

    data class Token(val value: String) : ChatEvent()

    data class Sources(val items: List<Source>) : ChatEvent() {

        data class Source(
            val title: String,
            val author: String,
            val category: String,
            val score: Int,
        )
    }

    data class Done(
        val tokensUsed: Int,
        val timeTaken: String,
    ) : ChatEvent()
}
