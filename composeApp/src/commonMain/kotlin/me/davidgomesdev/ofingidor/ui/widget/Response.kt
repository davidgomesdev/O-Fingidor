package me.davidgomesdev.ofingidor.ui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.davidgomesdev.ofingidor.ui.LocalAppFonts
import me.davidgomesdev.ofingidor.ui.hairlineColor
import me.davidgomesdev.ofingidor.ui.hairlineStrongColor
import me.davidgomesdev.ofingidor.ui.inkColor
import me.davidgomesdev.ofingidor.ui.model.Source
import me.davidgomesdev.ofingidor.ui.panelColor
import me.davidgomesdev.ofingidor.ui.purpleColor
import me.davidgomesdev.ofingidor.ui.purpleDeepColor
import me.davidgomesdev.ofingidor.ui.service.isMobileDevice
import me.davidgomesdev.ofingidor.ui.service.openUrl
import me.davidgomesdev.ofingidor.ui.silverColor
import me.davidgomesdev.ofingidor.ui.silverSoftColor
import me.davidgomesdev.ofingidor.ui.textMutedColor
import me.davidgomesdev.ofingidor.ui.textSecondaryColor

private const val textReaderUrl = "https://pessoa.davidgomes.blog/textReader"

/** A retrieved text: document icon, title and a small relevance bar. Hover (or tap on phones) shows details. */
@Composable
internal fun SourceChip(
    source: Source,
    highlighted: Boolean = false,
    tappedSourceId: Long? = null,
    onTap: (Long?) -> Unit = {},
) {
    val fonts = LocalAppFonts.current
    val isMobile = isMobileDevice()
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isTapped = tappedSourceId == source.id
    val showTooltip = if (isMobile) isTapped else isHovered
    val accent = if (highlighted) purpleColor else silverColor

    Layout(
        content = {
            DisableSelection {
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .hoverable(interactionSource)
                        .combinedClickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = {
                                if (isMobile) {
                                    onTap(if (isTapped) null else source.id)
                                } else {
                                    openUrl("$textReaderUrl/${source.id}")
                                }
                            },
                            onLongClick = if (isMobile) {
                                { openUrl("$textReaderUrl/${source.id}") }
                            } else {
                                null
                            },
                        )
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (showTooltip) purpleDeepColor.copy(alpha = 0.12f) else panelColor.copy(alpha = 0.6f))
                        .border(1.dp, if (showTooltip) purpleColor.copy(alpha = 0.5f) else hairlineStrongColor, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DocumentIcon(accent)
                    Text(
                        source.title,
                        color = silverSoftColor,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 200.dp),
                    )
                    RelevanceBar(source.score, accent)
                    Text("${source.score}%", color = accent, fontFamily = fonts.mono, fontSize = 11.sp)
                }
            }
            if (showTooltip) {
                SourcesTooltip(source)
            }
        },
        measurePolicy = MeasureScope::centerTooltip,
    )
}

@Composable
private fun DocumentIcon(color: Color) {
    Canvas(Modifier.size(14.dp)) {
        val u = size.width / 16f
        val stroke = Stroke(1.4.dp.toPx())
        val page = Path().apply {
            moveTo(3 * u, 2 * u)
            lineTo(10 * u, 2 * u)
            lineTo(13 * u, 5 * u)
            lineTo(13 * u, 14 * u)
            lineTo(3 * u, 14 * u)
            close()
        }
        drawPath(page, color, style = stroke)
        drawLine(color, Offset(6 * u, 8 * u), Offset(10 * u, 8 * u), stroke.width)
        drawLine(color, Offset(6 * u, 11 * u), Offset(10 * u, 11 * u), stroke.width)
    }
}

@Composable
private fun RelevanceBar(score: Int, color: Color) {
    Box(
        Modifier
            .width(34.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(silverColor.copy(alpha = 0.2f))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .width((34f * score.coerceIn(0, 100) / 100f).dp)
                .background(color)
        )
    }
}

@Composable
private fun SourcesTooltip(source: Source) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(inkColor)
            .border(1.dp, hairlineColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SourceTooltipRow("Autor", source.author)
        SourceTooltipRow("Categoria", source.category)
        SourceTooltipRow("Relevância", "${source.score}%")
    }
}

@Composable
private fun SourceTooltipRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = textMutedColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(value, color = textSecondaryColor, fontSize = 11.sp)
    }
}

private fun MeasureScope.centerTooltip(
    measurables: List<Measurable>,
    constraints: Constraints,
): MeasureResult {
    val chipPlaceable = measurables[0].measure(constraints)
    val tooltipPlaceable = measurables.getOrNull(1)?.measure(Constraints())

    return layout(chipPlaceable.width, chipPlaceable.height) {
        chipPlaceable.place(0, 0)
        tooltipPlaceable?.place(
            x = (chipPlaceable.width - tooltipPlaceable.width) / 2,
            y = -tooltipPlaceable.height - 4.dp.roundToPx(),
        )
    }
}
