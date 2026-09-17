package me.davidgomesdev.ofingidor.backend.llm.tool

import dev.langchain4j.agent.tool.P
import dev.langchain4j.agent.tool.Tool
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.store.embedding.EmbeddingSearchRequest
import dev.langchain4j.store.embedding.EmbeddingSearchResult
import dev.langchain4j.store.embedding.EmbeddingStore
import jakarta.inject.Singleton
import me.davidgomesdev.ofingidor.backend.llm.config.RAGConfig
import me.davidgomesdev.ofingidor.backend.llm.rag.PessoaText
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger

@Singleton
class PoemRetrievalTool(
    private val embeddingModel: EmbeddingModel,
    private val embeddingStore: EmbeddingStore<TextSegment>,
    @param:ConfigProperty(name = "pessoa.text-reader-base-url") private val readerBaseUrl: String,
    private val config: RAGConfig,
) {
    val log: Logger = Logger.getLogger(this::class.java)

    @Tool("Encontra poemas com base no significado fornecido pelo utilizador")
    fun getPoemByMeaning(
        @P(
            "As palavras do próprio utilizador sobre o texto que procura: o verso de que se lembra, " +
                    "as imagens ou o tema que descreveu. Copia as palavras dele tal como as escreveu e remove " +
                    "apenas a pergunta à volta ('há um poema que...', 'qual é?'). " +
                    "NÃO traduzas para termos abstratos nem reformules em linguagem literária — " +
                    "as palavras originais são o que permite encontrar o texto. " +
                    "Exemplo: de 'Há um poema do Pessoa que fala sobre Deus querer e o Homem sonhar, qual é?' " +
                    "passa apenas 'Deus quer, o homem sonha'."
        )
        description: String
    ): String {
        log.info("Searching for poem with description: $description")

        val embed = embeddingModel.embed(description).content()
        val searchResult = config.identification().run {
            embeddingStore.search(
                EmbeddingSearchRequest.builder()
                    .maxResults(maxCandidates())
                    .minScore(minScore())
                    .queryEmbedding(embed)
                    .build()
            )
        }
        val sortedTexts = sortTextsByScore(searchResult)

        val bestMatch = sortedTexts.first().pessoaText

        if (sortedTexts.size == 1) {
            return "Texto '${bestMatch.title}' da coleção '${bestMatch.categoryTitle}' escrito pelo autor '${bestMatch.author}'. Link: '${
                toLink(
                    bestMatch.id
                )
            }'"
        }

        val bestMatchText =
            "O texto mais próximo do que procuras é '${bestMatch.title}' da coleção '${bestMatch.categoryTitle}' escrito pelo autor '${bestMatch.author}'. Link: '${
                toLink(
                    bestMatch.id
                )
            }'."
        val remainingTextsText = sortedTexts.drop(1).joinToString("\n") { text ->
            "- '${text.pessoaText.title}' da coleção '${text.pessoaText.categoryTitle}' escrito pelo autor '${text.pessoaText.author}'. Link: '${
                toLink(
                    text.pessoaText.id
                )
            }'"
        }

        return "$bestMatchText\n\nOutros textos que podem ser relevantes:\n$remainingTextsText"
    }

    private fun toLink(id: Int): String = "$readerBaseUrl/$id"

    private fun sortTextsByScore(searchResult: EmbeddingSearchResult<TextSegment>): List<RetrievedText> =
        searchResult.matches()
            .map { RetrievedText(it.score(), it.embedded().run { PessoaText.from(text(), metadata()) }) }
            .groupBy { it.pessoaText.id }
            .mapValues { it.value.maxByOrNull(RetrievedText::score)!! }
            .values.toList()
}

data class RetrievedText(val score: Double, val pessoaText: PessoaText)
