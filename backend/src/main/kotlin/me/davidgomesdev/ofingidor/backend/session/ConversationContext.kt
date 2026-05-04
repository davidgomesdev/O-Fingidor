package me.davidgomesdev.ofingidor.backend.session

import jakarta.enterprise.context.RequestScoped

@RequestScoped
class ConversationContext {
    var conversationId: String? = null
}
