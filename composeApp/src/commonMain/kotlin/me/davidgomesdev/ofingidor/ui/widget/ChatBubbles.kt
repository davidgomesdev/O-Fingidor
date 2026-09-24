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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.davidgomesdev.ofingidor.shared.dto.Persona
import me.davidgomesdev.ofingidor.shared.dto.PersonaCategory
import me.davidgomesdev.ofingidor.ui.LocalAppFonts
import me.davidgomesdev.ofingidor.ui.amberColor
import me.davidgomesdev.ofingidor.ui.debateSidePalette
import me.davidgomesdev.ofingidor.ui.errorBubbleBackgroundColor
import me.davidgomesdev.ofingidor.ui.errorBubbleBorderColor
import me.davidgomesdev.ofingidor.ui.errorBubbleTextColor
import me.davidgomesdev.ofingidor.ui.hairlineColor
import me.davidgomesdev.ofingidor.ui.hairlineStrongColor
import me.davidgomesdev.ofingidor.ui.model.DebateSide
import me.davidgomesdev.ofingidor.ui.model.Source
import me.davidgomesdev.ofingidor.ui.purpleColor
import me.davidgomesdev.ofingidor.ui.purpleDeepColor
import me.davidgomesdev.ofingidor.ui.silverColor
import me.davidgomesdev.ofingidor.ui.surfaceColor
import me.davidgomesdev.ofingidor.ui.surfaceDeepColor
import me.davidgomesdev.ofingidor.ui.surfaceRaisedColor
import me.davidgomesdev.ofingidor.ui.textBodyColor
import me.davidgomesdev.ofingidor.ui.textMutedColor
import me.davidgomesdev.ofingidor.ui.textPrimaryColor

private val userBubbleShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp)

private fun personaAccent(persona: Persona): Color = if (persona == Persona.O_FINGIDOR) amberColor else purpleColor

internal fun categoryCaption(persona: Persona): String = when (persona.category) {
    PersonaCategory.HETERONIMO -> "HETERÓNIMO"
    else -> persona.category.label.uppercase()
}

@Composable
fun UserBubble(question: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            Modifier
                .widthIn(max = 520.dp)
                .background(purpleDeepColor.copy(alpha = 0.12f), userBubbleShape)
                .border(1.dp, purpleColor.copy(alpha = 0.4f), userBubbleShape)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(question, color = textPrimaryColor, fontSize = 16.sp, lineHeight = 24.sp)
        }
    }
}

/** The debate question, centred between the two voices. */
@Composable
fun CenteredUserBubble(question: String) {
    val fonts = LocalAppFonts.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Row(
            Modifier
                .widthIn(max = 620.dp)
                .background(surfaceColor.copy(alpha = 0.92f), RoundedCornerShape(999.dp))
                .border(1.dp, hairlineStrongColor, RoundedCornerShape(999.dp))
                .padding(horizontal = 22.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DisableSelection {
                Text("PERGUNTA", color = textMutedColor, fontFamily = fonts.mono, fontSize = 10.sp, letterSpacing = 2.6.sp)
            }
            Text(
                question,
                color = textPrimaryColor,
                fontFamily = fonts.serif,
                fontStyle = FontStyle.Italic,
                fontSize = 21.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PersonaSpeakerRow(
    persona: Persona,
    accent: Color,
    isLoading: Boolean,
    hasText: Boolean,
    alignEnd: Boolean = false,
) {
    val fonts = LocalAppFonts.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val avatar: @Composable () -> Unit = { GlowingAvatar(persona, accent, pulsing = isLoading) }
        if (!alignEnd) avatar()
        if (isLoading && !hasText) {
            ShimmerText("A invocar ${persona.displayName}…", 20.sp, italicSerif = true)
        } else {
            Text(
                persona.displayName,
                color = accent,
                fontFamily = fonts.serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
            )
            if (!alignEnd) {
                DisableSelection {
                    Text(categoryCaption(persona), color = textMutedColor, fontFamily = fonts.mono, fontSize = 10.sp, letterSpacing = 2.4.sp)
                }
            }
        }
        if (alignEnd) avatar()
    }
}

@Composable
private fun GlowingAvatar(persona: Persona, accent: Color, pulsing: Boolean, size: Dp = 34.dp) {
    val halo by rememberInfiniteTransition().animateFloat(0f, 1f, infiniteRepeatable(tween(2_800)))
    Box(
        Modifier
            .size(size)
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.45f), Color.Transparent), radius = this.size.width),
                    radius = this.size.width,
                )
                if (pulsing) {
                    drawCircle(
                        accent.copy(alpha = 0.8f * (1f - halo)),
                        radius = (this.size.width / 2f + 4.dp.toPx()) * (1f + 0.5f * halo),
                        style = Stroke(1.dp.toPx()),
                    )
                }
            }
    ) {
        PersonaAvatar(
            persona = persona,
            modifier = Modifier.fillMaxSize().border(1.5.dp, accent, CircleShape),
            contentDescriptionMode = AvatarContentDescriptionMode.DECORATIVE,
        )
    }
}

@Composable
private fun BreathingDots(accent: Color) {
    val transition = rememberInfiniteTransition()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 46.dp)) {
        listOf(accent, silverColor, accent).forEachIndexed { index, color ->
            val scale by transition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(tween(900, delayMillis = index * 300), RepeatMode.Reverse),
            )
            Box(Modifier.size(6.dp).scale(scale).background(color, CircleShape))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiBubble(
    persona: Persona,
    message: String,
    sources: List<Source>,
    isLoading: Boolean,
) {
    val fonts = LocalAppFonts.current
    val accent = personaAccent(persona)
    val shape = RoundedCornerShape(topStart = 6.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 22.dp)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PersonaSpeakerRow(persona, accent, isLoading, hasText = message.isNotEmpty())
        if (message.isEmpty() && isLoading) {
            BreathingDots(accent)
        } else {
            Box(
                Modifier
                    .widthIn(max = 760.dp)
                    .background(Brush.linearGradient(listOf(surfaceRaisedColor.copy(alpha = 0.92f), surfaceDeepColor.copy(alpha = 0.92f))), shape)
                    .border(1.dp, hairlineColor, shape)
                    .padding(horizontal = 28.dp, vertical = 24.dp)
            ) {
                StreamingText(message, isLoading, fonts.serif, textAlign = TextAlign.Start)
            }
            BubbleSources(sources = sources)
        }
    }
}

@Composable
fun DebatePersonaBubble(
    speaker: Persona,
    side: DebateSide,
    message: String,
    sources: List<Source>,
    isLoading: Boolean,
) {
    val fonts = LocalAppFonts.current
    val palette = debateSidePalette(side)
    val alignEnd = side == DebateSide.RIGHT
    val shape = if (alignEnd) {
        RoundedCornerShape(topStart = 22.dp, topEnd = 6.dp, bottomStart = 22.dp, bottomEnd = 22.dp)
    } else {
        RoundedCornerShape(topStart = 6.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 22.dp)
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PersonaSpeakerRow(speaker, palette.label, isLoading, hasText = message.isNotEmpty(), alignEnd = alignEnd)
        if (message.isEmpty() && isLoading) {
            BreathingDots(palette.accent)
        } else {
            Box(
                Modifier
                    .widthIn(max = 560.dp)
                    .background(Brush.linearGradient(listOf(palette.bubbleTop, surfaceColor.copy(alpha = 0.9f))), shape)
                    .border(1.dp, palette.border, shape)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                StreamingText(
                    message,
                    isLoading,
                    fonts.serif,
                    textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
                    fontSize = 21,
                )
            }
            BubbleSources(sources = sources)
        }
    }
}

@Composable
private fun StreamingText(
    message: String,
    isLoading: Boolean,
    family: FontFamily,
    textAlign: TextAlign,
    fontSize: Int = 21,
) {
    val inlineContent = if (isLoading) {
        mapOf(
            "cursor" to InlineTextContent(
                placeholder = Placeholder(2.sp, fontSize.sp, PlaceholderVerticalAlign.TextCenter)
            ) { BlinkingCursor() }
        )
    } else {
        emptyMap()
    }
    val text: AnnotatedString = buildAnnotatedString {
        append(message)
        if (isLoading) appendInlineContent("cursor", "|")
    }
    Text(
        text = text,
        color = textBodyColor,
        fontFamily = family,
        fontSize = fontSize.sp,
        lineHeight = (fontSize * 1.55f).sp,
        textAlign = textAlign,
        inlineContent = inlineContent,
    )
}

@Composable
private fun BlinkingCursor() {
    val cursorAlpha by rememberInfiniteTransition().animateFloat(
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
            .background(purpleColor)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BubbleSources(sources: List<Source>) {
    if (sources.isEmpty()) return
    val fonts = LocalAppFonts.current

    var expanded by remember { mutableStateOf(false) }
    var tappedSourceId by remember { mutableStateOf<Long?>(null) }
    val visibleSources = if (sources.size > 3 && !expanded) sources.take(3) else sources

    FlowRow(
        modifier = Modifier.widthIn(max = 760.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        DisableSelection {
            Text("FONTES", color = textMutedColor, fontFamily = fonts.mono, fontSize = 10.sp, letterSpacing = 2.6.sp)
        }
        visibleSources.forEachIndexed { index, source ->
            key(source.id) {
                SourceChip(
                    source = source,
                    highlighted = index == 0,
                    tappedSourceId = tappedSourceId,
                    onTap = { tappedSourceId = it },
                )
            }
        }
        if (sources.size > 3) {
            ExpandToggleChip(
                expanded = expanded,
                hiddenCount = sources.size - 3,
                onClick = { expanded = !expanded },
            )
        }
    }
}

@Composable
private fun ExpandToggleChip(expanded: Boolean, hiddenCount: Int, onClick: () -> Unit) {
    val fonts = LocalAppFonts.current
    DisableSelection {
        Text(
            if (expanded) "− menos" else "+$hiddenCount mais",
            color = textMutedColor,
            fontFamily = fonts.mono,
            fontSize = 11.sp,
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

@Composable
fun ErrorBubble(errorDetail: String? = null) {
    val shape = RoundedCornerShape(topStart = 6.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 22.dp)
    Column(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .background(errorBubbleBackgroundColor, shape)
            .border(1.dp, errorBubbleBorderColor, shape)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Algo correu mal. Tenta de novo.", color = errorBubbleTextColor, fontSize = 14.sp)
        if (errorDetail != null) {
            SelectionContainer {
                Text(
                    errorDetail,
                    color = errorBubbleTextColor.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontFamily = LocalAppFonts.current.mono,
                )
            }
        }
    }
}
