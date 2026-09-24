package me.davidgomesdev.ofingidor.ui.widget

import kotlin.test.Test
import kotlin.test.assertEquals

class ReaderLinksTest {
    private fun links(text: String) = findReaderLinks(text).map { text.substring(it) }

    @Test
    fun findsBareLink() {
        assertEquals(
            listOf("https://pessoa.davidgomes.blog/textReader/42"),
            links("Lê aqui https://pessoa.davidgomes.blog/textReader/42 com calma"),
        )
    }

    @Test
    fun excludesQuotesAndTrailingPunctuation() {
        assertEquals(
            listOf(
                "https://pessoa.davidgomes.blog/textReader/1",
                "https://pessoa.davidgomes.blog/textReader/2",
            ),
            links("Link: 'https://pessoa.davidgomes.blog/textReader/1'. Ou (https://pessoa.davidgomes.blog/textReader/2)."),
        )
    }

    @Test
    fun ignoresOtherDomains() {
        assertEquals(emptyList(), links("Vê https://example.com/textReader/1"))
    }
}
