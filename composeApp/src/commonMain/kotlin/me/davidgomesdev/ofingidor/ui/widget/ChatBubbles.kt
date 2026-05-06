package me.davidgomesdev.ofingidor.ui.widget

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.davidgomesdev.ofingidor.ui.aiBubbleBackgroundColor
import me.davidgomesdev.ofingidor.ui.aiBubbleBorder
import me.davidgomesdev.ofingidor.ui.devChipTextColor
import me.davidgomesdev.ofingidor.ui.inputCardBackgroundColor
import me.davidgomesdev.ofingidor.ui.model.Source
import me.davidgomesdev.ofingidor.ui.personaLabelColor
import me.davidgomesdev.ofingidor.ui.userBubbleBorder

@Composable
fun UserBubble(question: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 460.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 2.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
                    .background(inputCardBackgroundColor)
                    .border(
                        width = 2.dp,
                        color = userBubbleBorder,
                        shape = RoundedCornerShape(
                            topStart = 10.dp,
                            topEnd = 2.dp,
                            bottomStart = 10.dp,
                            bottomEnd = 10.dp
                        )
                    )
                    .padding(horizontal = 13.dp, vertical = 9.dp)
            ) {
                Text(
                    question,
                    color = Color(0xFFCCCCCC),
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiBubble(
    isDevMode: Boolean,
    message: String,
    sources: List<Source>,
    traceId: String,
    isLoading: Boolean,
) {
    val inlineContent = if (isLoading) {
        mapOf(
            "cursor" to InlineTextContent(
                placeholder = Placeholder(2.sp, 14.sp, PlaceholderVerticalAlign.TextCenter)
            ) { BlinkingCursor() }
        )
    } else {
        emptyMap()
    }

    val annotatedText = buildAnnotatedString {
        append(message)
        if (isLoading) appendInlineContent("cursor", "|")
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 10.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
                .background(aiBubbleBackgroundColor)
                .border(
                    width = 2.dp,
                    color = aiBubbleBorder,
                    shape = RoundedCornerShape(topStart = 2.dp, topEnd = 10.dp, bottomStart = 10.dp, bottomEnd = 10.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = annotatedText,
                color = Color(0xFFCCCCCC),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                inlineContent = inlineContent,
            )
        }

        if (isDevMode && traceId.isNotBlank()) {
            Text(
                "trace: $traceId",
                color = devChipTextColor.copy(alpha = 0.45f),
                fontSize = 10.sp,
                letterSpacing = 0.5.sp,
            )
        }

        if (sources.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.widthIn(max = 560.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                sources.forEach { source ->
                    key(source.id) { SourceChip(source) }
                }
            }
        }
    }
}

@Composable
private fun BlinkingCursor() {
    val infiniteTransition = rememberInfiniteTransition()
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
            },
            repeatMode = RepeatMode.Restart,
        )
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(cursorAlpha)
            .background(personaLabelColor)
    )
}
