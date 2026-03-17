package me.davidgomesdev.llm

import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.document.splitter.DocumentByRegexSplitter
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.input.PromptTemplate
import dev.langchain4j.rag.DefaultRetrievalAugmentor
import dev.langchain4j.rag.RetrievalAugmentor
import dev.langchain4j.rag.content.retriever.ContentRetriever
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.rag.query.Query
import dev.langchain4j.rag.query.transformer.DefaultQueryTransformer
import dev.langchain4j.rag.query.transformer.ExpandingQueryTransformer
import dev.langchain4j.rag.query.transformer.QueryTransformer
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.qdrant.client.QdrantClient
import io.qdrant.client.QdrantGrpcClient
import io.qdrant.client.grpc.Collections.Distance
import io.qdrant.client.grpc.Collections.VectorParams
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Named
import jakarta.inject.Singleton
import kotlinx.serialization.json.Json
import me.davidgomesdev.llm.config.RAGConfig
import me.davidgomesdev.model.Persona
import me.davidgomesdev.observability.attributes
import me.davidgomesdev.observability.span
import me.davidgomesdev.source.PessoaCategory
import me.davidgomesdev.source.PessoaText
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.io.File
import kotlin.time.measureTime

// Livro do Desassossego
const val PREVIEW_CATEGORY_ID = 33

typealias TextsByCategory = Map<Pair<Int, String>, List<PessoaText>>

@ApplicationScoped
class RAG(
    @param:ConfigProperty(name = "preview-only", defaultValue = "false")
    val isPreviewOnly: Boolean,
    @param:ConfigProperty(name = "recreate.embeddings", defaultValue = "false")
    val recreateEmbeddings: Boolean,
    val config: RAGConfig,
    val personaContext: PersonaContext,
) {
    val log: Logger = Logger.getLogger(this::class.java)
    private val tracer = GlobalOpenTelemetry.getTracer(this::class.java.name)
    val splitter = DocumentByRegexSplitter("\n\n", "\n", 900, 0, DocumentBySentenceSplitter(300, 0))

    @Singleton
    @Suppress("unused")
    fun augmentor(
        contentRetriever: ContentRetriever,
        queryTransformer: QueryTransformer,
        contentInjector: TextsContentInjector
    ): RetrievalAugmentor =
        DefaultRetrievalAugmentor.builder()
            .queryRouter { _ ->
                if (personaContext.persona == Persona.O_FINGIDOR) {
                    Span.current().addEvent("Skipping RAG")
                    log.info("Skipping RAG for persona ${Persona.O_FINGIDOR.codeName}")
                    emptyList()
                } else {
                    listOf(contentRetriever)
                }
            }
            .queryTransformer { originalQuery ->
                queryTransformer
                    .transform(originalQuery)
                    .also { transformedQuery ->
                        traceQueryExpansion(transformedQuery, originalQuery)
                    }
            }
            .contentInjector(contentInjector)
            .build()

    @Singleton
    @Suppress("unused")
    fun contentRetriever(
        @Named("PessoaDocuments")
        documents: List<Document>,
        embeddingModel: EmbeddingModel,
        embeddingStore: EmbeddingStore<TextSegment>,
    ): ContentRetriever {
        log.info("Preparing content retriever")

        val span = tracer.spanBuilder("rag.creating")
            .setSpanKind(SpanKind.INTERNAL).apply {
                setAttribute("mode", if (isPreviewOnly) "preview" else "full")
                setAttribute("recreate-embeddings", recreateEmbeddings)
                setAttribute("min-score", config.minScore())
                setAttribute("max-results", config.maxResults().toLong())
            }
            .startSpan()

        val qdrantConfig = config.qdrant()
        val baseName = qdrantConfig.collection().name()
        val collectionName = if (isPreviewOnly) "${baseName}_preview" else baseName
        var isIngestNeeded = recreateEmbeddings

        QdrantClient(
            QdrantGrpcClient.newBuilder(qdrantConfig.host(), 6334, false)
                .withApiKey(qdrantConfig.apiKey())
                .build()
        ).use { client ->
            if (recreateEmbeddings) {
                log.info("Recreating embeddings, deleting")
                client.deleteCollectionAsync(collectionName).get()
            }

            val existingCollections: List<String> = client.listCollectionsAsync().get()

            if (collectionName !in existingCollections) {
                log.info("Collection '$collectionName' not found")

                client.createCollectionAsync(
                    collectionName,
                    VectorParams.newBuilder()
                        .setDistance(Distance.Cosine)
                        .setSize(embeddingModel.dimension().toLong())
                        .build()
                ).get()

                log.info("Collection '$collectionName' created successfully with dimension ${embeddingModel.dimension()}")
                isIngestNeeded = true
            } else {
                log.info("Collection '$collectionName' already exists")
            }
        }


        val scope = span.makeCurrent()
        try {
            if (isPreviewOnly) {
                log.info("Running for preview ONLY")
            }

            if (isIngestNeeded) {
                log.info("Ingesting ${documents.size} documents")

                val wholeTimeSpent = measureTime {
                    documents
                        .chunked(50)
                        .forEach { chunk ->
                            val chunkTimeSpent = measureTime {
                                EmbeddingStoreIngestor.builder()
                                    .documentSplitter(splitter)
                                    .embeddingStore(embeddingStore)
                                    .embeddingModel(embeddingModel)
                                    .build()
                                    .ingest(chunk)
                            }

                            log.info("Ingested chunk (took $chunkTimeSpent)")
                            span.addEvent("Ingested chunk", attributes {
                                put("documents_count", chunk.size.toLong())
                                put("time_spent_ms", chunkTimeSpent.inWholeMilliseconds)
                            })
                        }
                }

                log.info("Documents ingested (took $wholeTimeSpent)")
                span.addEvent("Ingested all documents", attributes {
                    put("documents_count", documents.size.toLong())
                    put("time_spent_ms", wholeTimeSpent.inWholeMilliseconds)
                })
            }

            span.setStatus(StatusCode.OK)
        } finally {
            scope.close()
            span.end()
        }

        return EmbeddingStoreContentRetriever.builder()
            .embeddingStore(embeddingStore)
            .embeddingModel(embeddingModel)
            .maxResults(config.maxResults())
            .minScore(config.minScore())
            .dynamicFilter { _ ->
                val persona = personaContext.persona

                if (persona == null) {
                    Span.current().addEvent("⚠\uFE0F Received null persona when filtering for content!")
                    log.warn("Received null persona when filtering for content!")
                    return@dynamicFilter null
                }

                when (persona) {
                    Persona.FERNANDO_PESSOA -> null
                    else ->
                        metadataKey("author").isEqualTo(persona.name)
                }
            }
            .build()
    }

    @Singleton
    @Suppress("unused")
    fun queryTransformer(chatModel: ChatModel): QueryTransformer {
        if (!config.expandQuery()) {
            log.info("Using simple query transformer for preview")
            return DefaultQueryTransformer()
        }

        return ExpandingQueryTransformer(chatModel, PromptTemplate.from(config.expandingQueryTemplate()))
    }

    @Singleton
    @Suppress("unused")
    fun embeddingStore(embeddingModel: EmbeddingModel): EmbeddingStore<TextSegment> {
        log.info("Creating Embedding store")

        val qdrantConfig = config.qdrant()
        val baseName = qdrantConfig.collection().name()
        val collectionName = if (isPreviewOnly) "${baseName}_preview" else baseName

        val embeddingStore = QdrantEmbeddingStore.builder()
            .host(qdrantConfig.host())
            .apiKey(qdrantConfig.apiKey())
            .collectionName(collectionName)
            .build()

        return embeddingStore
    }

    @Named("PessoaDocuments")
    @ApplicationScoped
    @Suppress("unused")
    fun documents(allTextsByCategory: TextsByCategory): List<Document> {
        return allTextsByCategory.map { category ->
            category.value
                .filter { it.content.isNotBlank() }
                .map {
                    Document.document(
                        it.content, Metadata.from(
                            mapOf(
                                "title" to it.title,
                                "author" to it.author,
                                "textId" to it.id,
                                "categoryId" to category.key.first,
                                "categoryName" to category.key.second,
                            )
                        )
                    )
                }
        }.flatten()
    }

    @ApplicationScoped
    @Suppress("unused")
    fun allTextsByCategory(): TextsByCategory {
        val rootCategories = Json.decodeFromString<List<PessoaCategory>>(File("assets/all_texts.json").readText())
        val allTexts = mutableMapOf<Pair<Int, String>, MutableList<PessoaText>>()

        val categoriesToBeProcessed = rootCategories.toMutableList()

        while (categoriesToBeProcessed.isNotEmpty()) {
            val currentCategories = categoriesToBeProcessed.toList()

            categoriesToBeProcessed.clear()

            currentCategories.forEach { category ->
                categoriesToBeProcessed.addAll(category.subcategories)

                val categoryTexts = allTexts.getOrPut(Pair(category.id, category.title)) { mutableListOf() }

                if (category.texts != null)
                    categoryTexts.addAll(category.texts)
            }
        }

        log.info("Total amount of texts ${allTexts.map { it.value.size }.sum()}")

        if (isPreviewOnly) {
            return allTexts.filter { it.key.first == PREVIEW_CATEGORY_ID }
        }

        return allTexts
    }

    private fun traceQueryExpansion(
        transformedQuery: Collection<Query>,
        originalQuery: Query
    ) {
        val transformedQueries = transformedQuery.joinToString(
            "\n",
            prefix = "[ ",
            postfix = " ]"
        ) { "'" + it.text() + "'" }

        log.info("Transformed original query '${originalQuery.text()}' to '$transformedQueries'")

        span().addEvent(
            "Query Transformed",
            attributes {
                put("original_query", originalQuery.text())
                put("transformed_queries", transformedQueries)
                put("transform_queries_count", transformedQuery.size.toLong())
            }
        )
    }
}
