package me.davidgomesdev.ofingidor.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.davidgomesdev.ofingidor.ui.backgroundColor
import me.davidgomesdev.ofingidor.ui.cardBorderColor
import me.davidgomesdev.ofingidor.ui.focusedIndicatorColor
import me.davidgomesdev.ofingidor.ui.inputCardBackgroundColor
import me.davidgomesdev.ofingidor.ui.model.Source
import me.davidgomesdev.ofingidor.ui.service.openUrl

private val accentColorLight = Color(0xFFA07FD4)
private const val textReaderUrl = "https://pessoa.davidgomes.blog/textReader"

@Composable
internal fun SourceChip(source: Source) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Layout(
        content = {
            DisableSelection {
                Text(
                    source.title,
                    color = if (isHovered) accentColorLight else focusedIndicatorColor.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .hoverable(interactionSource)
                        .clickable(interactionSource = interactionSource, indication = null) {
                            openUrl("$textReaderUrl/${source.id}")
                        }
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isHovered) accentColorLight.copy(alpha = 0.15f) else inputCardBackgroundColor)
                        .border(
                            1.dp,
                            if (isHovered) accentColorLight.copy(alpha = 0.5f) else cardBorderColor,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            if (isHovered) {
                SourcesTooltip(source)
            }
        }
    ) { measurables, constraints ->
        val chipPlaceable = measurables[0].measure(constraints)
        val tooltipPlaceable = measurables.getOrNull(1)?.measure(Constraints())

        layout(chipPlaceable.width, chipPlaceable.height) {
            chipPlaceable.place(0, 0)
            tooltipPlaceable?.place(
                x = (chipPlaceable.width - tooltipPlaceable.width) / 2,
                y = -tooltipPlaceable.height - 4.dp.roundToPx()
            )
        }
    }
}

@Composable
private fun SourcesTooltip(source: Source) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(1.dp, cardBorderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SourceTooltipRow("Autor", source.author)
        SourceTooltipRow("Categoria", source.category)
        SourceTooltipRow("Relevância", "${source.score}%")
    }
}

@Composable
private fun SourceTooltipRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            color = focusedIndicatorColor.copy(alpha = 0.5f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
        Text(
            value,
            color = focusedIndicatorColor.copy(alpha = 0.9f),
            fontSize = 10.sp
        )
    }
}
