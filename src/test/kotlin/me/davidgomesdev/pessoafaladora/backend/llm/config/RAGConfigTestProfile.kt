package me.davidgomesdev.pessoafaladora.backend.llm.config

import com.google.common.util.concurrent.Futures
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.embedding.EmbeddingModel
import io.qdrant.client.QdrantClient
import io.qdrant.client.grpc.Collections
import io.quarkus.test.Mock
import io.quarkus.test.junit.QuarkusTestProfile
import jakarta.enterprise.context.Dependent
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton
import org.mockito.Mockito

class RAGConfigTestProfile : QuarkusTestProfile {
    override fun getConfigOverrides(): Map<String, String> = mapOf(
        "recreate.embeddings" to "false",
        "preview-only" to "true"
    )

    @Dependent
    class MockProducers {
        @Produces
        @Mock
        @Singleton
        fun qdrantClient(): QdrantClient {
            val mock = Mockito.mock(QdrantClient::class.java)
            Mockito.`when`(mock.listCollectionsAsync()).thenReturn(
                Futures.immediateFuture(emptyList<String>())
            )
            Mockito.`when`(
                mock.createCollectionAsync(
                    Mockito.anyString(),
                    Mockito.any(Collections.VectorParams::class.java)
                )
            ).thenReturn(
                Futures.immediateFuture(Collections.CollectionOperationResponse.getDefaultInstance())
            )
            return mock
        }

        @Produces
        @Mock
        @Singleton
        fun embeddingModel(): EmbeddingModel {
            val mock = Mockito.mock(EmbeddingModel::class.java)
            Mockito.`when`(mock.dimension()).thenReturn(384)
            return mock
        }

        @Produces
        @Mock
        @Singleton
        fun chatModel(): ChatModel = Mockito.mock(ChatModel::class.java)

        @Produces
        @Mock
        @Singleton
        fun streamingChatModel(): StreamingChatModel = Mockito.mock(StreamingChatModel::class.java)
    }
}
