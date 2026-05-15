package me.davidgomesdev.ofingidor.shared.dto

import kotlin.test.Test
import kotlin.test.assertEquals

class ChatEventSerializationTest {
    @Test
    fun `Token serializes with type discriminator`() {
        val result = json.encodeToString<ChatEvent>(ChatEvent.Token("hello"))
        assertEquals("""{"type":"token","value":"hello"}""", result)
    }

    @Test
    fun `Start serializes with type discriminator`() {
        val result = json.encodeToString<ChatEvent>(ChatEvent.Start("abc-123"))
        assertEquals("""{"type":"start","traceId":"abc-123"}""", result)
    }

    @Test
    fun `Done serializes with type discriminator`() {
        val result = json.encodeToString<ChatEvent>(ChatEvent.Done(100, "1.23s"))
        assertEquals("""{"type":"done","tokensUsed":100,"timeTaken":"1.23s"}""", result)
    }

    @Test
    fun `Sources serializes with type discriminator`() {
        val result = json.encodeToString<ChatEvent>(ChatEvent.Sources(listOf()))
        assertEquals("""{"type":"sources","items":[]}""", result)
    }
}
