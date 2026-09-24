package me.davidgomesdev.ofingidor.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import me.davidgomesdev.ofingidor.shared.dto.Persona
import me.davidgomesdev.ofingidor.shared.dto.PersonaCategory
import me.davidgomesdev.ofingidor.ui.model.ConversationMode
import me.davidgomesdev.ofingidor.ui.model.DebatePair
import me.davidgomesdev.ofingidor.ui.model.DebateSide
import me.davidgomesdev.ofingidor.ui.model.HeroQuote
import me.davidgomesdev.ofingidor.ui.model.voice
import me.davidgomesdev.ofingidor.ui.widget.AvatarContentDescriptionMode
import me.davidgomesdev.ofingidor.ui.widget.ChatContextRow
import me.davidgomesdev.ofingidor.ui.widget.Constellation
import me.davidgomesdev.ofingidor.ui.widget.DebateContextRow
import me.davidgomesdev.ofingidor.ui.widget.HeroEyebrow
import me.davidgomesdev.ofingidor.ui.widget.HeroHeadline
import me.davidgomesdev.ofingidor.ui.widget.HeroQuoteBlock
import me.davidgomesdev.ofingidor.ui.widget.InputCardLayout
import me.davidgomesdev.ofingidor.ui.widget.PersonaAvatar
import me.davidgomesdev.ofingidor.ui.widget.SuggestionsRow
import me.davidgomesdev.ofingidor.ui.widget.ThinkInputCard
import me.davidgomesdev.ofingidor.ui.widget.categoryCaption
import me.davidgomesdev.ofingidor.ui.widget.personaPortrait
import org.jetbrains.compose.resources.painterResource

/** Everything the input card needs, bundled so screens can pass it along. */
internal class InputState(
    val text: String,
    val onTextChange: (String) -> Unit,
    val isLoading: Boolean,
    val onSubmit: () -> Unit,
    val placeholder: String,
)

/** Who is selected (chat) or debating (debate), and how to change it. */
internal class VoicesState(
    val mode: ConversationMode,
    val selectedPersona: Persona,
    val debatePair: DebatePair,
    val debateNextSlot: DebateSide,
    val devMode: Boolean,
    val onPersonaPicked: (Persona) -> Unit,
    val onSlotSelected: (DebateSide) -> Unit,
    val onSwap: () -> Unit,
)

@Composable
private fun InputContextRow(voices: VoicesState, isCompact: Boolean) {
    when (voices.mode) {
        ConversationMode.CHAT -> ChatContextRow(voices.selectedPersona)
        ConversationMode.DEBATE -> DebateContextRow(
            pair = voices.debatePair,
            nextSlot = voices.debateNextSlot,
            onSlotSelected = voices.onSlotSelected,
            onSwap = voices.onSwap,
            isCompact = isCompact,
        )
    }
}

@Composable
private fun InputCard(input: InputState, layout: InputCardLayout, contextRow: (@Composable () -> Unit)?) {
    ThinkInputCard(
        text = input.text,
        onTextChange = input.onTextChange,
        isLoading = input.isLoading,
        onSubmit = input.onSubmit,
        placeholder = input.placeholder,
        layout = layout,
        contextRow = contextRow,
    )
}

/** Under the constellation: the chosen voice's line, or who the next pick replaces in a debate. */
@Composable
private fun ConstellationCaption(voices: VoicesState, fontSize: Int) {
    val fonts = LocalAppFonts.current
    val persona = voices.selectedPersona
    val accent = if (persona == Persona.O_FINGIDOR) amberColor else purpleColor
    val text = when (voices.mode) {
        ConversationMode.CHAT -> buildAnnotatedString {
            if (voices.devMode) {
                withStyle(SpanStyle(color = accent, fontStyle = FontStyle.Normal, fontWeight = FontWeight.Medium)) {
                    append(persona.displayName)
                }
                append(" — ")
            }
            append("“${persona.voice().line}”")
        }

        ConversationMode.DEBATE -> {
            val side = voices.debateNextSlot
            val outgoing = voices.debatePair.personaAt(side)
            buildAnnotatedString {
                append("Toca noutra voz para substituir ")
                withStyle(
                    SpanStyle(
                        color = if (side == DebateSide.LEFT) purpleColor else silverPaleColor,
                        fontStyle = FontStyle.Normal,
                        fontWeight = FontWeight.Medium,
                    )
                ) { append(outgoing.displayName) }
                append(".")
            }
        }
    }
    Text(
        text,
        color = textSecondaryColor,
        fontFamily = fonts.serif,
        fontStyle = FontStyle.Italic,
        fontSize = fontSize.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
internal fun LandingScreen(
    voices: VoicesState,
    input: InputState,
    quote: HeroQuote,
    onQuerySelected: (String) -> Unit,
    isCompact: Boolean,
    isWide: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isWide) WideLanding(voices, input, quote, onQuerySelected, modifier)
    else StackedLanding(voices, input, onQuerySelected, isCompact, modifier)
}

@Composable
private fun WideLanding(
    voices: VoicesState,
    input: InputState,
    quote: HeroQuote,
    onQuerySelected: (String) -> Unit,
    modifier: Modifier,
) {
    val fonts = LocalAppFonts.current
    Column(modifier.fillMaxSize().padding(start = 96.dp, end = 56.dp, bottom = 28.dp)) {
        Row(Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.widthIn(max = 600.dp).weight(1f).fillMaxHeight().padding(top = 52.dp)) {
                HeroEyebrow()
                Spacer(Modifier.height(24.dp))
                HeroHeadline(voices.mode, isCompact = false)
                Spacer(Modifier.height(26.dp))
                HeroQuoteBlock(quote, secondary = voices.mode == ConversationMode.DEBATE)
                Spacer(Modifier.height(24.dp))
                Spacer(Modifier.weight(1f))
                InputCard(input, InputCardLayout.FULL) { InputContextRow(voices, isCompact = false) }
                Spacer(Modifier.height(22.dp))
                SuggestionsRow(onQuerySelected = onQuerySelected, isCompact = false)
                Spacer(Modifier.height(30.dp))
            }
            Spacer(Modifier.width(64.dp))
            BoxWithConstraints(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                val size = min(min(maxWidth, maxHeight - 110.dp), 620.dp)
                Column(
                    Modifier.padding(top = 44.dp).width(size),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Constellation(
                        size = size,
                        mode = voices.mode,
                        selectedPersona = voices.selectedPersona,
                        debatePair = voices.debatePair,
                        devMode = voices.devMode,
                        showLabels = true,
                        onPersonaPicked = voices.onPersonaPicked,
                    )
                    ConstellationCaption(voices, fontSize = 22)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "AS RESPOSTAS NASCEM DOS TEXTOS DE PESSOA · CADA UMA TRAZ AS SUAS FONTES",
                color = textMutedColor,
                fontFamily = fonts.mono,
                fontSize = 10.sp,
                letterSpacing = 2.4.sp,
            )
            Text(
                if (voices.mode == ConversationMode.CHAT) "ESCOLHE UMA VOZ NA CONSTELAÇÃO" else "ESCOLHE DUAS VOZES NA CONSTELAÇÃO",
                color = textMutedColor,
                fontFamily = fonts.mono,
                fontSize = 10.sp,
                letterSpacing = 2.4.sp,
            )
        }
    }
}

@Composable
private fun StackedLanding(
    voices: VoicesState,
    input: InputState,
    onQuerySelected: (String) -> Unit,
    isCompact: Boolean,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val size = min(min(maxWidth - 60.dp, maxHeight * 0.4f), 460.dp)
        Column(
            Modifier.fillMaxHeight().widthIn(max = 640.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(12.dp))
            Constellation(
                size = size,
                mode = voices.mode,
                selectedPersona = voices.selectedPersona,
                debatePair = voices.debatePair,
                devMode = voices.devMode,
                showLabels = false,
                onPersonaPicked = voices.onPersonaPicked,
            )
            Spacer(Modifier.height(10.dp))
            Box(Modifier.padding(horizontal = 20.dp)) { ConstellationCaption(voices, fontSize = 16) }
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                HeroHeadline(voices.mode, isCompact = true)
            }
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.weight(1f))
            Box(Modifier.padding(horizontal = 16.dp)) {
                InputCard(input, InputCardLayout.COMPACT) { InputContextRow(voices, isCompact = isCompact) }
            }
            Spacer(Modifier.height(16.dp))
            SuggestionsRow(onQuerySelected = onQuerySelected, isCompact = true)
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
internal fun ConversationScreen(
    voices: VoicesState,
    input: InputState,
    scrollState: ScrollState,
    isCompact: Boolean,
    isWide: Boolean,
    modifier: Modifier = Modifier,
    feed: @Composable ColumnScope.() -> Unit,
) {
    Row(modifier.fillMaxSize()) {
        if (isWide && voices.mode == ConversationMode.CHAT) {
            VoicesSidebar(voices, Modifier.padding(start = 24.dp, top = 20.dp, bottom = 24.dp))
        }
        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (voices.mode == ConversationMode.DEBATE) {
                DuelHeader(voices.debatePair, isCompact = !isWide)
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                Brush.verticalGradient(0f to Color.Transparent, 32.dp.toPx() / size.height to Color.Black),
                                blendMode = BlendMode.DstIn,
                            )
                        }
                        .verticalScroll(scrollState)
                        .padding(horizontal = if (isCompact) 14.dp else 32.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        Modifier.widthIn(max = if (voices.mode == ConversationMode.DEBATE) 1000.dp else 760.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(22.dp),
                        content = feed,
                    )
                }
            }
            Box(
                Modifier
                    .widthIn(max = 760.dp)
                    .padding(start = if (isCompact) 12.dp else 24.dp, end = if (isCompact) 12.dp else 24.dp, bottom = 20.dp, top = 8.dp)
            ) {
                InputCard(input, if (isCompact) InputCardLayout.COMPACT else InputCardLayout.INLINE, contextRow = null)
            }
        }
    }
}

private val grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** The voices, grouped as on the site, with the one in conversation lit and its portrait at the bottom. */
@Composable
private fun VoicesSidebar(voices: VoicesState, modifier: Modifier = Modifier) {
    val fonts = LocalAppFonts.current
    val categories = PersonaCategory.entries.filter { it != PersonaCategory.DEV || voices.devMode }
    Column(
        modifier
            .width(288.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(22.dp))
            .background(surfaceColor.copy(alpha = 0.72f))
            .border(1.dp, textPrimaryColor.copy(alpha = 0.07f), RoundedCornerShape(22.dp))
            .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        categories.forEach { category ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    when (category) {
                        PersonaCategory.HETERONIMO -> "HETERÓNIMOS"
                        else -> category.label.uppercase()
                    },
                    color = textMutedColor,
                    fontFamily = fonts.mono,
                    fontSize = 10.sp,
                    letterSpacing = 2.6.sp,
                    modifier = Modifier.padding(start = 8.dp).semantics { heading() },
                )
                Persona.entries.filter { it.category == category }.forEach { persona ->
                    VoiceRow(persona, isCurrent = persona == voices.selectedPersona)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        val portrait = personaPortrait(voices.selectedPersona)
        if (portrait != null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .drawBehind {
                        drawRoundRect(
                            Brush.radialGradient(listOf(purpleDeepColor.copy(alpha = 0.45f), Color.Transparent)),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                        )
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, purpleColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(portrait.resource),
                    contentDescription = portrait.contentDescription,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(0.55f to Color.Transparent, 1f to inkColor.copy(alpha = 0.55f)))
                )
            }
        }
    }
}

@Composable
private fun VoiceRow(persona: Persona, isCurrent: Boolean) {
    val accent = if (persona == Persona.O_FINGIDOR) amberColor else purpleColor
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (isCurrent) {
                    Modifier
                        .background(Brush.horizontalGradient(listOf(purpleDeepColor.copy(alpha = 0.24f), purpleDeepColor.copy(alpha = 0.06f))))
                        .border(1.dp, purpleColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                } else {
                    Modifier
                }
            )
            .semantics { selected = isCurrent }
            .padding(start = 8.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PersonaAvatar(
            persona = persona,
            modifier = Modifier
                .size(36.dp)
                .border(if (isCurrent) 2.dp else 1.dp, if (isCurrent) accent else silverColor.copy(alpha = 0.35f), CircleShape)
                .graphicsLayer { alpha = if (isCurrent) 1f else 0.75f },
            contentDescriptionMode = AvatarContentDescriptionMode.DECORATIVE,
            colorFilter = if (isCurrent) null else grayscale,
        )
        Text(
            persona.displayName,
            color = if (isCurrent) textPrimaryColor else textSecondaryColor,
            fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
        )
        if (isCurrent) {
            Box(
                Modifier
                    .size(7.dp)
                    .drawBehind { drawCircle(accent.copy(alpha = 0.5f), radius = size.width * 1.4f) }
                    .background(accent, CircleShape)
            )
        }
    }
}

/** The two debaters facing each other across a turning "vs" sigil. */
@Composable
private fun DuelHeader(pair: DebatePair, isCompact: Boolean) {
    val portraitWidth = if (isCompact) 64.dp else 132.dp
    val portraitHeight = if (isCompact) 72.dp else 148.dp
    Row(
        Modifier.fillMaxWidth().padding(top = if (isCompact) 12.dp else 24.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 14.dp else 36.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (isCompact) 10.dp else 22.dp)) {
            DuelName(pair.left, DebateSide.LEFT, isCompact)
            DuelPortrait(pair.left, purpleColor, purpleDeepColor.copy(alpha = 0.55f), portraitWidth, portraitHeight)
        }
        VsSigil(if (isCompact) 52.dp else 84.dp)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (isCompact) 10.dp else 22.dp)) {
            DuelPortrait(pair.right, silverColor, silverColor.copy(alpha = 0.25f), portraitWidth, portraitHeight)
            DuelName(pair.right, DebateSide.RIGHT, isCompact)
        }
    }
}

@Composable
private fun DuelName(persona: Persona, side: DebateSide, isCompact: Boolean) {
    val fonts = LocalAppFonts.current
    Column(
        horizontalAlignment = if (side == DebateSide.LEFT) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (!isCompact) {
            Text(
                categoryCaption(persona),
                color = if (side == DebateSide.LEFT) purpleColor else silverColor,
                fontFamily = fonts.mono,
                fontSize = 10.sp,
                letterSpacing = 2.8.sp,
            )
        }
        Text(
            if (isCompact) persona.voice().shortName else persona.displayName,
            color = textPrimaryColor,
            fontFamily = fonts.serif,
            fontWeight = FontWeight.Medium,
            fontSize = if (isCompact) 20.sp else 34.sp,
        )
    }
}

@Composable
private fun DuelPortrait(persona: Persona, ring: Color, glow: Color, width: Dp, height: Dp) {
    val portrait = personaPortrait(persona) ?: return
    Box(
        Modifier
            .size(width, height)
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(glow, Color.Transparent), radius = size.maxDimension),
                    radius = size.maxDimension,
                )
            }
            .clip(RoundedCornerShape(18.dp))
            .border(2.dp, ring, RoundedCornerShape(18.dp))
    ) {
        Image(
            painter = painterResource(portrait.resource),
            contentDescription = portrait.contentDescription,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun VsSigil(size: Dp) {
    val fonts = LocalAppFonts.current
    val transition = rememberInfiniteTransition()
    val spin by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(160_000, easing = LinearEasing)))
    val counterSpin by transition.animateFloat(0f, -360f, infiniteRepeatable(tween(100_000, easing = LinearEasing)))
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = spin }) {
            drawCircle(
                silverColor.copy(alpha = 0.45f),
                radius = this.size.width / 2f - 1.dp.toPx(),
                style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 5.dp.toPx()))),
            )
        }
        Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = counterSpin }) {
            drawCircle(
                purpleColor.copy(alpha = 0.6f),
                radius = this.size.width * 0.36f,
                style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(30.dp.toPx() * this.size.width / 84.dp.toPx(), 12.dp.toPx()))),
            )
        }
        Text("vs", color = textPrimaryColor, fontFamily = fonts.serif, fontStyle = FontStyle.Italic, fontSize = (size.value * 0.31f).sp)
    }
}
