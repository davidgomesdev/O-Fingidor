package me.davidgomesdev.ofingidor.ui.service

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.accept
import io.ktor.client.request.preparePut
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readLine
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.davidgomesdev.ofingidor.ui.dto.ChatEvent
import me.davidgomesdev.ofingidor.ui.model.Persona

const val DEFAULT_HOST = "127.0.0.1"

@Suppress("HttpUrlsUsage")
val apiUrl = "http://${getHost()}:8080"

class ThinkAPI {
    private val client = HttpClient {
        install(HttpTimeout) {
            socketTimeoutMillis = 60_000
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Napier.v("HTTP Client", null, message)
                }
            }
            level = LogLevel.ALL
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
            })
        }
    }.also { Napier.base(DebugAntilog()) }

    fun sendThinkRequest(
        query: String,
        persona: Persona
    ) = channelFlow {
        try {
            client.preparePut("$apiUrl/pensa") {
                accept(ContentType.Any)
                contentType(ContentType.Application.Json)
                setBody(ThinkPayload(query, persona.codeName))
            }.execute { httpResponse ->
                val channel: ByteReadChannel = httpResponse.body()
                while (!channel.isClosedForRead) {
                    val line = channel.readLine() ?: break
                    if (line.isBlank()) continue
                    println(line)
                    val event = Json.decodeFromString<ChatEvent>(line)
                    send(event)
                }
            }
        } catch (e: Exception) {
            Napier.e("Request failed", e)
            // todo: find a way of informing the user
        }
    }
}

@Serializable
data class ThinkPayload(val input: String, val persona: String)
