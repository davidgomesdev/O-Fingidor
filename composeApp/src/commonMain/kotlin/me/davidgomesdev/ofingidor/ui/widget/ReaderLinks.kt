package me.davidgomesdev.ofingidor.ui.widget

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import me.davidgomesdev.ofingidor.ui.purpleColor
import me.davidgomesdev.ofingidor.ui.service.openUrl

private const val READER_URL = """https?://pessoa\.davidgomes\.blog[^\s'"`()<>\[\]]*"""

/** Either a markdown link `[title](url)` (groups 1 and 2) or a bare URL (group 3). */
private val readerLinkRegex = Regex("""\[([^\[\]\n]+)]\(($READER_URL)\)|($READER_URL)""")
private const val TRAILING_PUNCTUATION = ".,;:!?"

private val readerLinkStyles =
    TextLinkStyles(SpanStyle(color = purpleColor, textDecoration = TextDecoration.Underline))

/** A pessoa.davidgomes.blog link found at [range] of a message, shown as [label]. */
internal data class ReaderLink(
    val range: IntRange,
    val url: String,
    val label: String,
)

/** Finds markdown and bare pessoa.davidgomes.blog links in [text]; bare URLs lose trailing punctuation. */
internal fun findReaderLinks(text: String): List<ReaderLink> =
    readerLinkRegex
        .findAll(text)
        .map { match ->
            val (title, markdownUrl, bareUrl) = match.destructured
            if (markdownUrl.isNotEmpty()) {
                ReaderLink(match.range, markdownUrl, title.trim())
            } else {
                val url = bareUrl.trimEnd { it in TRAILING_PUNCTUATION }
                ReaderLink(match.range.first until match.range.first + url.length, url, url)
            }
        }.toList()

/** Appends [text], turning pessoa.davidgomes.blog links into clickable links labelled with the text title. */
internal fun AnnotatedString.Builder.appendWithReaderLinks(text: String) {
    var cursor = 0
    for (link in findReaderLinks(text)) {
        append(text.substring(cursor, link.range.first))
        withLink(LinkAnnotation.Clickable(link.url, readerLinkStyles) { openUrl(link.url) }) { append(link.label) }
        cursor = link.range.last + 1
    }
    append(text.substring(cursor))
}
