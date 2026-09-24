package me.davidgomesdev.ofingidor.ui.widget

import kotlin.test.Test
import kotlin.test.assertEquals

class ReaderLinksTest {
    private fun links(text: String) = findReaderLinks(text).map { Triple(text.substring(it.range), it.url, it.label) }

    @Test
    fun markdownLinkUsesTitleAsLabel() {
        val markdown = "[Tabacaria](https://pessoa.davidgomes.blog/textReader/42)"
        assertEquals(
            listOf(Triple(markdown, "https://pessoa.davidgomes.blog/textReader/42", "Tabacaria")),
            links("Lê $markdown."),
        )
    }

    @Test
    fun bareLinkUsesUrlAsLabel() {
        val url = "https://pessoa.davidgomes.blog/textReader/42"
        assertEquals(listOf(Triple(url, url, url)), links("Lê aqui $url com calma"))
    }

    @Test
    fun bareLinkExcludesQuotesAndTrailingPunctuation() {
        assertEquals(
            listOf(
                "https://pessoa.davidgomes.blog/textReader/1",
                "https://pessoa.davidgomes.blog/textReader/2",
            ),
            links("Link: 'https://pessoa.davidgomes.blog/textReader/1'. Ou (https://pessoa.davidgomes.blog/textReader/2).")
                .map { it.second },
        )
    }

    @Test
    fun ignoresOtherDomains() {
        assertEquals(emptyList(), links("Vê [isto](https://example.com/textReader/1) e https://example.com/1"))
    }
}
