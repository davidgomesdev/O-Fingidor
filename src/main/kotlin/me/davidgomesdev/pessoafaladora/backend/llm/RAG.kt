package me.davidgomesdev.pessoafaladora.backend.llm

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
import io.qdrant.client.grpc.Points
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Named
import jakarta.inject.Singleton
import kotlinx.serialization.json.Json
import me.davidgomesdev.pessoafaladora.backend.llm.config.RAGConfig
import me.davidgomesdev.pessoafaladora.backend.model.Persona
import me.davidgomesdev.pessoafaladora.backend.model.PessoaCategory
import me.davidgomesdev.pessoafaladora.backend.model.PessoaText
import me.davidgomesdev.pessoafaladora.backend.observability.attributes
import me.davidgomesdev.pessoafaladora.backend.observability.span
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.context.ManagedExecutor
import org.jboss.logging.Logger
import java.io.File
import kotlin.time.measureTime

// Livro do Desassossego
const val PREVIEW_CATEGORY_ID = 33

typealias TextsByCategory = Map<Pair<Int, String>, List<PessoaText>>


object TextAttributes {
    const val TEXT_ID = "textId"
    const val TITLE = "title"
    const val AUTHOR = "author"
    const val CATEGORY_NAME = "categoryName"
    const val CATEGORY_ID = "categoryId"
}

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
                if (personaContext.persona == Persona.O_FINGIDOR || personaContext.persona == Persona.NINGUEM) {
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
        qdrantClient: QdrantClient,
        managedExecutor: ManagedExecutor,
        ingestor: EmbeddingStoreIngestor
    ): ContentRetriever {
        log.info("Preparing content retriever")

        val span = tracer.spanBuilder("rag.initializing")
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

        if (recreateEmbeddings) {
            log.info("Recreating embeddings, deleting")
            qdrantClient.deleteCollectionAsync(collectionName).get()
            span.addEvent("Deleted collection to recreate")
        }

        val existingCollections: List<String> = qdrantClient.listCollectionsAsync().get()

        if (collectionName !in existingCollections) {
            log.info("Collection '$collectionName' not found")

            qdrantClient.createCollectionAsync(
                collectionName,
                VectorParams.newBuilder()
                    .setDistance(Distance.Cosine)
                    .setSize(embeddingModel.dimension().toLong())
                    .build()
            ).get()

            log.info("Collection '$collectionName' created successfully with dimension ${embeddingModel.dimension()}")
            span.addEvent("Created collection")
        } else {
            log.info("Collection '$collectionName' already exists")
        }
        managedExecutor.runAsync {
            ingestDocuments(
                qdrantClient,
                collectionName,
                ingestor,
                documents
            )
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


    fun ingestDocuments(
        qdrantClient: QdrantClient,
        collectionName: String,
        ingestor: EmbeddingStoreIngestor,
        documents: List<Document>,
    ) {
        val span = tracer.spanBuilder("rag.ingesting")
            .setSpanKind(SpanKind.INTERNAL).apply {
                setAttribute("mode", if (isPreviewOnly) "preview" else "full")
                setAttribute("recreate-embeddings", recreateEmbeddings)
                setAttribute("min-score", config.minScore())
                setAttribute("max-results", config.maxResults().toLong())
            }
            .startSpan()
        val ingestedDocuments = getIngestedDocumentIDs(qdrantClient, collectionName)

        val scope = span.makeCurrent()
        try {

            if (isPreviewOnly) {
                log.info("Running for preview ONLY")
            }
            val span = Span.current()

            val totalDocumentsLeftToIngest =
                documents.filterNot { ingestedDocuments.contains(it.metadata().getLong(TextAttributes.TEXT_ID)) }

            if (totalDocumentsLeftToIngest.isEmpty()) {
                log.info("No documents needed to ingest")
                return
            }

            val uniqueDocumentsToIngest =
                totalDocumentsLeftToIngest.distinctBy { it.metadata().getLong(TextAttributes.TEXT_ID) }

            log.info("Ingesting ${uniqueDocumentsToIngest.size} unique documents (out of ${documents.size} total; non-unique count is ${totalDocumentsLeftToIngest.size})")

            val wholeTimeSpent = measureTime {
                uniqueDocumentsToIngest
                    .chunked(50)
                    .forEach { chunk ->
                        val chunkTimeSpent = measureTime {
                            ingestor
                                .ingest(chunk)
                        }

                        log.info("Ingested chunk (took $chunkTimeSpent)")
                        span.addEvent("Ingested chunk", attributes {
                            put("ingested_documents_count", chunk.size.toLong())
                            put("time_spent_ms", chunkTimeSpent.inWholeMilliseconds)
                        })
                    }
            }

            log.info("Documents ingested (took $wholeTimeSpent for ${uniqueDocumentsToIngest.size} documents)")
            span.addEvent("Ingested documents", attributes {
                put("ingested_documents_count", uniqueDocumentsToIngest.size.toLong())
                put("total_documents_count", documents.size.toLong())
                put("time_spent_ms", wholeTimeSpent.inWholeMilliseconds)
            })

            span.setStatus(StatusCode.OK)
        } finally {
            scope.close()
            span.end()
        }
    }

    @Singleton
    @Suppress("unused")
    fun qdrantClient(): QdrantClient = QdrantClient(
        QdrantGrpcClient.newBuilder(config.qdrant().host(), 6334, false)
            .withApiKey(config.qdrant().apiKey())
            .build()
    )

    @Singleton
    @Suppress("unused")
    fun ingestor(
        embeddingStore: EmbeddingStore<TextSegment>,
        embeddingModel: EmbeddingModel
    ): EmbeddingStoreIngestor = EmbeddingStoreIngestor.builder()
        .documentSplitter(splitter)
        .embeddingStore(embeddingStore)
        .embeddingModel(embeddingModel)
        .build()

    private fun getIngestedDocumentIDs(
        qdrantClient: QdrantClient,
        collectionName: String,
    ): List<Long> {
        log.info("Getting ingested text IDs")

        val results: List<Points.RetrievedPoint> = qdrantClient.scrollAsync(
            Points.ScrollPoints.newBuilder()
                .setCollectionName(collectionName)
                // This is really fast and doesn't require much memory (they are only IDs)
                .setLimit(-1)
                .setWithPayload(
                    Points.WithPayloadSelector.newBuilder()
                        .setInclude(
                            Points.PayloadIncludeSelector.newBuilder()
                                .addFields(TextAttributes.TEXT_ID)
                        )
                )
                .setWithVectors(Points.WithVectorsSelector.newBuilder().setEnable(false))
                .build()
        ).get().resultList

        return results.map { it.getPayloadOrThrow(TextAttributes.TEXT_ID).integerValue }.distinct()
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
                            TextAttributes.run {
                                mapOf(
                                    TITLE to it.title,
                                    AUTHOR to it.author,
                                    TEXT_ID to it.id,
                                    CATEGORY_ID to category.key.first,
                                    CATEGORY_NAME to category.key.second,
                                )
                            }
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

        if (isPreviewOnly) {
            val previewTexts = allTexts.filter { it.key.first == PREVIEW_CATEGORY_ID }

            log.info("Using preview amount of texts ${previewTexts.map { it.value.size }.sum()}")

            return previewTexts
        }

        log.info("Total amount of texts ${allTexts.map { it.value.size }.sum()}")

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
