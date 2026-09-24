package me.davidgomesdev.ofingidor.ui.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.davidgomesdev.ofingidor.shared.dto.Persona
import me.davidgomesdev.ofingidor.ui.LocalAppFonts
import me.davidgomesdev.ofingidor.ui.amberColor
import me.davidgomesdev.ofingidor.ui.ctaBottomColor
import me.davidgomesdev.ofingidor.ui.ctaTopColor
import me.davidgomesdev.ofingidor.ui.hairlineColor
import me.davidgomesdev.ofingidor.ui.hairlineStrongColor
import me.davidgomesdev.ofingidor.ui.inkColor
import me.davidgomesdev.ofingidor.ui.isActionInputType
import me.davidgomesdev.ofingidor.ui.model.DebatePair
import me.davidgomesdev.ofingidor.ui.model.DebateSide
import me.davidgomesdev.ofingidor.ui.model.voice
import me.davidgomesdev.ofingidor.ui.panelColor
import me.davidgomesdev.ofingidor.ui.purpleColor
import me.davidgomesdev.ofingidor.ui.purpleDeepColor
import me.davidgomesdev.ofingidor.ui.shimmerHighlightColor
import me.davidgomesdev.ofingidor.ui.silverColor
import me.davidgomesdev.ofingidor.ui.silverSoftColor
import me.davidgomesdev.ofingidor.ui.surfaceColor
import me.davidgomesdev.ofingidor.ui.textMutedColor
import me.davidgomesdev.ofingidor.ui.textPrimaryColor
import me.davidgomesdev.ofingidor.ui.textSecondaryColor

private val exampleQueries =
    listOf(
        "O que é o amor para ti?",
        "Tens medo da morte?",
        "Como encontrar sentido na vida?",
        "O que pensas sobre a saudade?",
        "Explica-me porquê que decidiste criar heterónimos.",
        "Quem és?",
        "Como te chamas?",
        "O que é para ti a arte?",
        "Achavas que ias ser reconhecido depois de morrer?",
        "Qual a utilidade da escrita a teu ver?",
    )

enum class InputCardLayout {
    /** Landing page on wide screens: context row, two-line field, then hint and "Pensar". */
    FULL,

    /** Phones: field with a square arrow button beside it. */
    COMPACT,

    /** During a conversation on wide screens: one row with field, hint and "Pensar". */
    INLINE,
}

/** Text that shimmers from grey to pale violet, for "thinking" states. */
@Composable
fun ShimmerText(
    text: String,
    fontSize: TextUnit,
    italicSerif: Boolean = false,
) {
    val fonts = LocalAppFonts.current
    val shift by rememberInfiniteTransition().animateFloat(0f, 1f, infiniteRepeatable(tween(2_800, easing = LinearEasing)))
    val brush =
        Brush.linearGradient(
            0f to textMutedColor,
            0.35f to textMutedColor,
            0.5f to shimmerHighlightColor,
            0.65f to textMutedColor,
            1f to textMutedColor,
            start = Offset(-400f + shift * 800f, 0f),
            end = Offset(shift * 800f, 0f),
            tileMode = TileMode.Clamp,
        )
    Text(
        text,
        style =
            TextStyle(
                brush = brush,
                fontFamily = if (italicSerif) fonts.serif else fonts.sans,
                fontStyle = if (italicSerif) FontStyle.Italic else FontStyle.Normal,
                fontSize = fontSize,
            ),
    )
}

/** A glass card with a slowly shimmering purple and grey border. */
@Composable
fun SheenCard(
    radius: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shift by rememberInfiniteTransition().animateFloat(0f, 1f, infiniteRepeatable(tween(9_000, easing = LinearEasing)))
    Box(
        modifier
            .drawBehind {
                val period = size.width * 1.5f
                val brush =
                    Brush.linearGradient(
                        0f to purpleColor.copy(alpha = 0.75f),
                        0.25f to silverColor.copy(alpha = 0.14f),
                        0.5f to purpleColor.copy(alpha = 0.08f),
                        0.7f to silverColor.copy(alpha = 0.2f),
                        1f to purpleColor.copy(alpha = 0.75f),
                        start = Offset(-shift * period, 0f),
                        end = Offset(period - shift * period, size.height),
                        tileMode = TileMode.Repeated,
                    )
                drawRoundRect(brush, cornerRadius = CornerRadius(radius.toPx()))
            }.padding(1.dp)
            .clip(RoundedCornerShape(radius - 1.dp))
            .background(surfaceColor.copy(alpha = 0.92f)),
    ) {
        content()
    }
}

@Composable
fun ThinkInputCard(
    text: String,
    onTextChange: (String) -> Unit,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    placeholder: String,
    layout: InputCardLayout,
    modifier: Modifier = Modifier,
    contextRow: (@Composable () -> Unit)? = null,
) {
    SheenCard(radius = if (layout == InputCardLayout.FULL) 22.dp else 20.dp, modifier = modifier.fillMaxWidth()) {
        when (layout) {
            InputCardLayout.FULL -> {
                Column(
                    Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    contextRow?.invoke()
                    ThinkInputField(text, onTextChange, isLoading, onSubmit, placeholder, minLines = 2)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        KeyboardHint(Modifier.weight(1f))
                        ThinkButton(onSubmit, isLoading)
                    }
                }
            }

            InputCardLayout.COMPACT -> {
                Column(
                    Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    contextRow?.invoke()
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            ThinkInputField(text, onTextChange, isLoading, onSubmit, placeholder, minLines = 2)
                        }
                        ThinkIconButton(onSubmit, isLoading)
                    }
                }
            }

            InputCardLayout.INLINE -> {
                Row(
                    Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.weight(1f)) {
                        ThinkInputField(text, onTextChange, isLoading, onSubmit, placeholder, minLines = 1)
                    }
                    Text("Ctrl + Enter", color = textMutedColor, fontSize = 12.sp)
                    ThinkButton(onSubmit, isLoading)
                }
            }
        }
    }
}

@Composable
private fun KeyboardHint(modifier: Modifier = Modifier) {
    val fonts = LocalAppFonts.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("Ctrl", "+", "Enter").forEach { part ->
            if (part == "+") {
                Text(part, color = textMutedColor, fontSize = 12.sp)
            } else {
                Text(
                    part,
                    color = textSecondaryColor,
                    fontFamily = fonts.mono,
                    fontSize = 11.sp,
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(textPrimaryColor.copy(alpha = 0.04f))
                            .border(1.dp, hairlineStrongColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
        }
        Text("para enviar", color = textMutedColor, fontSize = 12.sp)
    }
}

@Composable
private fun ThinkInputField(
    text: String,
    onTextChange: (String) -> Unit,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    placeholder: String,
    minLines: Int,
) {
    val fonts = LocalAppFonts.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor by animateColorAsState(if (isFocused) purpleColor.copy(alpha = 0.5f) else hairlineColor)
    val fillColor by animateColorAsState(if (isFocused) purpleDeepColor.copy(alpha = 0.07f) else textPrimaryColor.copy(alpha = 0.03f))
    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        enabled = !isLoading,
        minLines = minLines,
        maxLines = 6,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(purpleColor),
        textStyle =
            TextStyle(
                color = if (isLoading) textMutedColor else textPrimaryColor,
                fontFamily = fonts.sans,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "A tua pergunta" }
                .onPreviewKeyEvent { keyEvent ->
                    if (isActionInputType(keyEvent) && !isLoading) {
                        onSubmit()
                        true
                    } else {
                        false
                    }
                },
        decorationBox = { innerTextField ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        if (isFocused) {
                            drawRoundRect(
                                purpleDeepColor.copy(alpha = 0.14f),
                                topLeft = Offset(-4.dp.toPx(), -4.dp.toPx()),
                                size =
                                    androidx.compose.ui.geometry.Size(
                                        size.width + 8.dp.toPx(),
                                        size.height + 8.dp.toPx(),
                                    ),
                                cornerRadius = CornerRadius(18.dp.toPx()),
                            )
                        }
                    }.clip(RoundedCornerShape(14.dp))
                    .background(fillColor)
                    .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 13.dp),
            ) {
                if (text.isEmpty()) {
                    Text(placeholder, color = textMutedColor, fontFamily = fonts.sans, fontSize = 16.sp, lineHeight = 24.sp)
                }
                innerTextField()
            }
        },
    )
}

@Composable
private fun ArrowIcon(
    color: Color,
    size: Dp = 16.dp,
) {
    Canvas(Modifier.size(size)) {
        val u = this.size.width / 16f
        val stroke = 1.6.dp.toPx()
        drawLine(color, Offset(3 * u, 8 * u), Offset(13 * u, 8 * u), stroke, StrokeCap.Round)
        drawLine(color, Offset(9 * u, 4 * u), Offset(13 * u, 8 * u), stroke, StrokeCap.Round)
        drawLine(color, Offset(9 * u, 12 * u), Offset(13 * u, 8 * u), stroke, StrokeCap.Round)
    }
}

private fun Modifier.ctaSurface(
    isLoading: Boolean,
    radius: Dp,
): Modifier =
    this
        .clip(RoundedCornerShape(radius))
        .background(
            if (isLoading) {
                Brush.verticalGradient(listOf(purpleDeepColor.copy(alpha = 0.22f), purpleDeepColor.copy(alpha = 0.22f)))
            } else {
                Brush.verticalGradient(listOf(ctaTopColor, ctaBottomColor))
            },
        ).border(1.dp, purpleColor.copy(alpha = 0.45f), RoundedCornerShape(radius))

@Composable
fun ThinkButton(
    onSubmit: () -> Unit,
    isLoading: Boolean,
) {
    val fonts = LocalAppFonts.current
    Row(
        modifier =
            Modifier
                .height(48.dp)
                .ctaSurface(isLoading, 14.dp)
                .clickable(enabled = !isLoading, role = Role.Button, onClick = onSubmit)
                .padding(start = 24.dp, end = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (isLoading) {
            ShimmerText("A pensar…", 15.sp)
        } else {
            Text("Pensar", color = Color.White, fontFamily = fonts.sans, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            ArrowIcon(Color.White)
        }
    }
}

@Composable
private fun ThinkIconButton(
    onSubmit: () -> Unit,
    isLoading: Boolean,
) {
    Box(
        modifier =
            Modifier
                .size(52.dp)
                .ctaSurface(isLoading, 14.dp)
                .clickable(enabled = !isLoading, role = Role.Button, onClick = onSubmit)
                .semantics { contentDescription = if (isLoading) "A pensar" else "Pensar" },
        contentAlignment = Alignment.Center,
    ) {
        ArrowIcon(if (isLoading) textMutedColor else Color.White, 18.dp)
    }
}

/** "A falar com Fernando Pessoa" above the field in chat mode. */
@Composable
fun ChatContextRow(persona: Persona) {
    val accent = if (persona == Persona.O_FINGIDOR) amberColor else purpleColor
    Row(
        Modifier.height(32.dp).padding(start = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PersonaAvatar(
            persona = persona,
            modifier = Modifier.size(26.dp).border(1.dp, accent, CircleShape),
            contentDescriptionMode = AvatarContentDescriptionMode.DECORATIVE,
        )
        Text(
            androidx.compose.ui.text.buildAnnotatedString {
                append("A falar com ")
                pushStyle(
                    androidx.compose.ui.text
                        .SpanStyle(color = accent, fontWeight = FontWeight.Medium),
                )
                append(persona.displayName)
                pop()
            },
            color = textMutedColor,
            fontSize = 13.sp,
        )
    }
}

/**
 * The two debaters as chips. Clicking a chip marks it as the one the next constellation pick replaces
 * (shown with a dashed outline); the arrows swap their order.
 */
@Composable
fun DebateContextRow(
    pair: DebatePair,
    nextSlot: DebateSide,
    onSlotSelected: (DebateSide) -> Unit,
    onSwap: () -> Unit,
    isCompact: Boolean,
) {
    val fonts = LocalAppFonts.current
    Row(
        Modifier.height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp),
    ) {
        if (!isCompact) {
            Text(
                "EM DEBATE",
                color = textMutedColor,
                fontFamily = fonts.mono,
                fontSize = 10.sp,
                letterSpacing = 2.4.sp,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        DebaterChip(pair.left, DebateSide.LEFT, nextSlot == DebateSide.LEFT, isCompact) { onSlotSelected(DebateSide.LEFT) }
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onSwap)
                .semantics { contentDescription = "Trocar a ordem" },
            contentAlignment = Alignment.Center,
        ) {
            SwapIcon()
        }
        DebaterChip(pair.right, DebateSide.RIGHT, nextSlot == DebateSide.RIGHT, isCompact) { onSlotSelected(DebateSide.RIGHT) }
    }
}

@Composable
private fun SwapIcon() {
    Canvas(Modifier.size(16.dp)) {
        val u = size.width / 16f
        val stroke = 1.4.dp.toPx()
        val color = textMutedColor
        drawLine(color, Offset(2 * u, 5 * u), Offset(13 * u, 5 * u), stroke, StrokeCap.Round)
        drawLine(color, Offset(10 * u, 2 * u), Offset(13 * u, 5 * u), stroke, StrokeCap.Round)
        drawLine(color, Offset(10 * u, 8 * u), Offset(13 * u, 5 * u), stroke, StrokeCap.Round)
        drawLine(color, Offset(14 * u, 11 * u), Offset(3 * u, 11 * u), stroke, StrokeCap.Round)
        drawLine(color, Offset(6 * u, 8 * u), Offset(3 * u, 11 * u), stroke, StrokeCap.Round)
        drawLine(color, Offset(6 * u, 14 * u), Offset(3 * u, 11 * u), stroke, StrokeCap.Round)
    }
}

@Composable
private fun DebaterChip(
    persona: Persona,
    side: DebateSide,
    isNext: Boolean,
    isCompact: Boolean,
    onClick: () -> Unit,
) {
    val isFirst = side == DebateSide.LEFT
    val ring = if (isFirst) purpleColor else silverSoftColor
    Row(
        Modifier
            .height(32.dp)
            .drawBehind {
                if (isNext) {
                    val inset = 3.dp.toPx()
                    drawRoundRect(
                        color = textPrimaryColor.copy(alpha = 0.45f),
                        topLeft = Offset(-inset, -inset),
                        size =
                            androidx.compose.ui.geometry
                                .Size(size.width + inset * 2, size.height + inset * 2),
                        cornerRadius = CornerRadius(size.height),
                        style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))),
                    )
                }
            }.clip(RoundedCornerShape(999.dp))
            .background(if (isFirst) purpleDeepColor.copy(alpha = 0.16f) else silverColor.copy(alpha = 0.12f))
            .border(1.dp, if (isFirst) purpleColor.copy(alpha = 0.45f) else silverSoftColor.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Substituir ${persona.displayName} a seguir" }
            .padding(start = 4.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PersonaAvatar(
            persona = persona,
            modifier = Modifier.size(24.dp).border(1.5.dp, ring, CircleShape),
            contentDescriptionMode = AvatarContentDescriptionMode.DECORATIVE,
        )
        Text(
            if (isCompact) persona.voice().shortName else persona.displayName,
            color = textPrimaryColor,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
        )
    }
}

/**
 * The example questions in one sideways-scrolling row. Questions cut off by either edge are blurred
 * and faded; on wide screens a "›" button scrolls further.
 */
@Composable
fun SuggestionsRow(
    onQuerySelected: (String) -> Unit,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
) {
    val fonts = LocalAppFonts.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val bounds = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    var viewportWidth by remember { mutableFloatStateOf(0f) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(if (isCompact) 10.dp else 12.dp)) {
        Text(
            "PERGUNTAS QUE ECOAM",
            color = textMutedColor,
            fontFamily = fonts.mono,
            fontSize = if (isCompact) 10.sp else 11.sp,
            letterSpacing = 2.6.sp,
            modifier = Modifier.padding(start = if (isCompact) 20.dp else 0.dp),
        )
        Box(Modifier.fillMaxWidth().height(44.dp).onSizeChanged { viewportWidth = it.width.toFloat() }) {
            Row(
                Modifier
                    .fillMaxHeight()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = if (isCompact) 16.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                exampleQueries.forEachIndexed { index, query ->
                    val (left, right) = bounds[index] ?: (0f to 0f)
                    val start = scrollState.value.toFloat()
                    val end = start + viewportWidth
                    val cutOff = right > 0f && (left < start - 1f || right > end + 1f)
                    SuggestionPill(
                        query = query,
                        fontSize = if (isCompact) 16.sp else 17.sp,
                        cutOff = cutOff,
                        onClick = { onQuerySelected(query) },
                        modifier =
                            Modifier.onGloballyPositioned {
                                val x = it.positionInParent().x
                                bounds[index] = x to x + it.size.width
                            },
                    )
                }
                Box(Modifier.width(if (isCompact) 8.dp else 72.dp))
            }
            if (scrollState.canScrollBackward) {
                EdgeFade(Modifier.align(Alignment.CenterStart).width(56.dp), fromLeft = true)
            }
            if (scrollState.canScrollForward) {
                EdgeFade(Modifier.align(Alignment.CenterEnd).width(if (isCompact) 72.dp else 120.dp), fromLeft = false)
                if (!isCompact) {
                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(surfaceColor.copy(alpha = 0.85f))
                            .border(1.dp, hairlineStrongColor, CircleShape)
                            .clickable(role = Role.Button) {
                                scope.launch { scrollState.animateScrollBy(with(density) { 320.dp.toPx() }) }
                            }.semantics { contentDescription = "Mais perguntas" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Canvas(Modifier.size(16.dp)) {
                            val u = size.width / 16f
                            val stroke = 1.6.dp.toPx()
                            drawLine(textSecondaryColor, Offset(6 * u, 3 * u), Offset(11 * u, 8 * u), stroke, StrokeCap.Round)
                            drawLine(textSecondaryColor, Offset(11 * u, 8 * u), Offset(6 * u, 13 * u), stroke, StrokeCap.Round)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EdgeFade(
    modifier: Modifier,
    fromLeft: Boolean,
) {
    val colors = listOf(Color.Transparent, inkColor.copy(alpha = 0.85f))
    Box(
        modifier
            .fillMaxHeight()
            .background(Brush.horizontalGradient(if (fromLeft) colors.reversed() else colors)),
    )
}

@Composable
private fun SuggestionPill(
    query: String,
    fontSize: TextUnit,
    cutOff: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fonts = LocalAppFonts.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val border by animateColorAsState(if (isHovered) purpleColor.copy(alpha = 0.55f) else hairlineStrongColor)
    val fill by animateColorAsState(if (isHovered) purpleDeepColor.copy(alpha = 0.12f) else panelColor.copy(alpha = 0.5f))
    val textColor by animateColorAsState(if (isHovered) textPrimaryColor else textSecondaryColor)
    Box(
        modifier
            .height(44.dp)
            .then(if (cutOff) Modifier.blur(2.5.dp).alpha(0.55f) else Modifier)
            .clip(RoundedCornerShape(999.dp))
            .background(fill)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(query, color = textColor, fontFamily = fonts.serif, fontStyle = FontStyle.Italic, fontSize = fontSize, maxLines = 1)
    }
}
