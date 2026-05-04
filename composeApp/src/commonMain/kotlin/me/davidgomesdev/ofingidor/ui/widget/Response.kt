package me.davidgomesdev.ofingidor.ui.widget

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.davidgomesdev.ofingidor.ui.backgroundColor
import me.davidgomesdev.ofingidor.ui.cardBorderColor
import me.davidgomesdev.ofingidor.ui.focusedIndicatorColor
import me.davidgomesdev.ofingidor.ui.inputCardBackgroundColor
import me.davidgomesdev.ofingidor.ui.model.Source
import me.davidgomesdev.ofingidor.ui.responseCardBackgroundColor
import me.davidgomesdev.ofingidor.ui.service.openUrl

private val accentColor = Color(0xFF7C5CBF)
private val accentColorLight = Color(0xFFA07FD4)
private const val textReaderUrl = "https://pessoa.davidgomes.blog/textReader"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResponseCard(
    response: String,
    sources: List<Source>,
    // Shown only in DEV mode
    traceId: String,
    isLoading: Boolean,
    isDevMode: Boolean,
) {
    val infiniteTransition = rememberInfiniteTransition()

    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        )
    )

    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                1f at 0
                1f at 499
                0f at 500
                0f at 999
            }
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(responseCardBackgroundColor, RoundedCornerShape(10.dp))
            .border(1.dp, cardBorderColor, RoundedCornerShape(10.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .background(inputCardBackgroundColor)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val dotColor = if (isLoading) accentColor.copy(alpha = dotAlpha) else Color(0xFF444444)
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                "Resposta",
                color = focusedIndicatorColor.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }

        HorizontalDivider(color = cardBorderColor, thickness = 1.dp)

        // Body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Decorative opening quote
            Text(
                "\u201C",
                color = Color(0xFF2A2A2A),
                fontSize = 24.sp,
                lineHeight = 24.sp
            )

            val inlineContent = mapOf(
                "cursor" to InlineTextContent(
                    placeholder = Placeholder(2.sp, 14.sp, PlaceholderVerticalAlign.TextCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(cursorAlpha)
                            .background(accentColor)
                    )
                }
            )

            val text = buildAnnotatedString {
                append(response)
                if (isLoading) appendInlineContent("cursor", "|")
            }

            Text(
                text = text,
                color = Color(0xFFCCCCCC),
                fontSize = 14.sp,
                lineHeight = 24.sp,
                fontFamily = FontFamily.Serif,
                inlineContent = inlineContent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp)
            )
        }

        // Sources — only shown when there is a response
        if (response.isNotBlank()) {
            HorizontalDivider(color = cardBorderColor, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Fontes",
                    color = focusedIndicatorColor.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 3.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sources.forEach {
                        key(it.id) { SourceChip(it) }
                    }
                }
            }
            if (isDevMode) {
                Text(
                    "Trace ID: $traceId",
                    color = focusedIndicatorColor.copy(alpha = 0.7f),
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 3.dp, start = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun SourceChip(source: Source) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box {
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
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .layout { measurable, _ ->
                        val placeable = measurable.measure(Constraints())
                        layout(0, 0) {
                            placeable.place(
                                x = -placeable.width / 2,
                                y = -placeable.height - 4.dp.roundToPx()
                            )
                        }
                    }
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


