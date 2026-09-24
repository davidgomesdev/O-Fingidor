package me.davidgomesdev.ofingidor.ui.widget

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import me.davidgomesdev.ofingidor.ui.purpleColor
import me.davidgomesdev.ofingidor.ui.service.openUrl

private val readerLinkRegex = Regex("""https?://pessoa\.davidgomes\.blog[^\s'"`()<>\[\]]*""")
private const val TRAILING_PUNCTUATION = ".,;:!?"

private val readerLinkStyles =
    TextLinkStyles(SpanStyle(color = purpleColor, textDecoration = TextDecoration.Underline))

/** Ranges of pessoa.davidgomes.blog URLs in [text], without trailing sentence punctuation. */
internal fun findReaderLinks(text: String): List<IntRange> =
    readerLinkRegex
        .findAll(text)
        .map { match ->
            val url = match.value.trimEnd { it in TRAILING_PUNCTUATION }
            match.range.first until match.range.first + url.length
        }.toList()

/** Appends [text], turning pessoa.davidgomes.blog URLs into clickable links. */
internal fun AnnotatedString.Builder.appendWithReaderLinks(text: String) {
    var cursor = 0
    for (range in findReaderLinks(text)) {
        append(text.substring(cursor, range.first))
        val url = text.substring(range)
        withLink(LinkAnnotation.Clickable(url, readerLinkStyles) { openUrl(url) }) { append(url) }
        cursor = range.last + 1
    }
    append(text.substring(cursor))
}
