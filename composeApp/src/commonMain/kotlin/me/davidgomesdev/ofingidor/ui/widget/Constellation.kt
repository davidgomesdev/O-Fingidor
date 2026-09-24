package me.davidgomesdev.ofingidor.ui.widget

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import me.davidgomesdev.ofingidor.shared.dto.Persona
import me.davidgomesdev.ofingidor.shared.dto.PersonaCategory
import me.davidgomesdev.ofingidor.ui.LocalAppFonts
import me.davidgomesdev.ofingidor.ui.amberColor
import me.davidgomesdev.ofingidor.ui.model.ConversationMode
import me.davidgomesdev.ofingidor.ui.model.DebatePair
import me.davidgomesdev.ofingidor.ui.model.constellationPersonas
import me.davidgomesdev.ofingidor.ui.model.voice
import me.davidgomesdev.ofingidor.ui.orbShadeColor
import me.davidgomesdev.ofingidor.ui.purpleColor
import me.davidgomesdev.ofingidor.ui.purpleDeepColor
import me.davidgomesdev.ofingidor.ui.purpleSoftColor
import me.davidgomesdev.ofingidor.ui.silverColor
import me.davidgomesdev.ofingidor.ui.silverLightColor
import me.davidgomesdev.ofingidor.ui.silverPaleColor
import me.davidgomesdev.ofingidor.ui.silverSoftColor
import me.davidgomesdev.ofingidor.ui.textMutedColor
import me.davidgomesdev.ofingidor.ui.textPrimaryColor
import me.davidgomesdev.ofingidor.ui.textSecondaryColor
import org.jetbrains.compose.resources.painterResource

/** All geometry is authored on a 620-unit square and scaled to the rendered size. */
private const val DESIGN_SIZE = 620f
private const val CENTER = 310f
private const val NODE_RADIUS = 236f
private val nodeAngles = listOf(-90f, -18f, 54f, 126f, 198f)

/** The twelve zodiac signs, Aries to Pisces, drawn on a ±8 unit grid. Gemini (index 2) was Pessoa's sign. */
private val zodiacGlyphs = listOf(
    "M0 7 L0 -1 C0 -7 -7 -8 -7 -2.5 M0 -1 C0 -7 7 -8 7 -2.5",
    "M-4.2 3 a4.2 4.2 0 1 0 8.4 0 a4.2 4.2 0 1 0 -8.4 0 M-7 -7 C-6 -1.5 6 -1.5 7 -7",
    "M-6 -7 C-2 -5.2 2 -5.2 6 -7 M-6 7 C-2 5.2 2 5.2 6 7 M-3 -5.6 L-3 5.6 M3 -5.6 L3 5.6",
    "M-6.4 -2.2 a2.2 2.2 0 1 0 4.4 0 a2.2 2.2 0 1 0 -4.4 0 M-4.2 -4.4 C-1 -7 5 -6.5 7 -3 " +
        "M2 2.2 a2.2 2.2 0 1 0 4.4 0 a2.2 2.2 0 1 0 -4.4 0 M4.2 4.4 C1 7 -5 6.5 -7 3",
    "M-7 3 a2.6 2.6 0 1 0 5.2 0 a2.6 2.6 0 1 0 -5.2 0 M-1.8 3 C-1.8 -2 -2 -7 2 -7 C6 -7 6 -2 3 2 C1 5 3 8 6.5 6",
    "M-7 -5 L-7 6 M-7 -3 C-7 -6 -3.5 -6 -3.5 -3 L-3.5 6 M-3.5 -3 C-3.5 -6 0 -6 0 -3 L0 4 C0 7 4 7 5.5 2 " +
        "C6.5 -1 4 -1 2.5 2 L0.5 7",
    "M-7 6 L7 6 M-7 2 L-3 2 C-5 -6 5 -6 3 2 L7 2",
    "M-7 -5 L-7 5 M-7 -3 C-7 -6 -3.5 -6 -3.5 -3 L-3.5 5 M-3.5 -3 C-3.5 -6 0 -6 0 -3 L0 4 C0 6 2 6.5 4 6.5 " +
        "L7 6.5 M5 4.5 L7 6.5 L5 8.5",
    "M-6 6 L6 -6 M0.5 -6 L6 -6 L6 -0.5 M-5 -1 L1 5",
    "M-7 -6 L-4 5 L-1 -3 C0 -6.5 3.5 -6 3.5 -2 L3.5 4 C3.5 7 7 7 7 4 C7 1.5 4 1.5 3.5 4 C3 7 0 8 -2 7",
    "M-7 -1 L-4.5 -3.5 L-2 -1 L0.5 -3.5 L3 -1 L5.5 -3.5 L7 -2 M-7 5 L-4.5 2.5 L-2 5 L0.5 2.5 L3 5 L5.5 2.5 L7 4",
    "M-6 -7 C-2 -3 -2 3 -6 7 M6 -7 C2 -3 2 3 6 7 M-4 0 L4 0",
)
private const val GEMINI_INDEX = 2

private enum class NodeRole { SELECTED, FIRST_VOICE, SECOND_VOICE }

private fun nodeCenter(index: Int): Offset {
    val angle = nodeAngles[index] * PI.toFloat() / 180f
    return Offset(CENTER + NODE_RADIUS * cos(angle), CENTER + NODE_RADIUS * sin(angle))
}

/** A point on a circle, measured clockwise from the top. */
private fun polar(degreesFromTop: Float, radius: Float): Offset {
    val angle = (degreesFromTop - 90f) * PI.toFloat() / 180f
    return Offset(CENTER + radius * cos(angle), CENTER + radius * sin(angle))
}

private fun personaCenter(persona: Persona): Offset {
    val index = constellationPersonas.indexOf(persona)
    return if (index < 0) Offset(CENTER, CENTER) else nodeCenter(index)
}

private fun grayscaleFilter(brightness: Float): ColorFilter {
    val r = 0.2126f * brightness
    val g = 0.7152f * brightness
    val b = 0.0722f * brightness
    return ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                r, g, b, 0f, 0f,
                r, g, b, 0f, 0f,
                r, g, b, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    )
}

/**
 * The five voices on a pentagram inside a slowly turning zodiac wheel.
 * In chat mode one voice is lit; in debate mode the first voice glows purple and the second grey.
 * With dev mode on, O Fingidor sits at the centre.
 */
@Composable
fun Constellation(
    size: Dp,
    mode: ConversationMode,
    selectedPersona: Persona,
    debatePair: DebatePair,
    devMode: Boolean,
    showLabels: Boolean,
    onPersonaPicked: (Persona) -> Unit,
) {
    val fonts = LocalAppFonts.current
    val scale = size.value / DESIGN_SIZE
    val glyphPaths = remember { zodiacGlyphs.map { PathParser().parsePathString(it).toPath() } }
    val pentagram = remember {
        Path().apply {
            listOf(0, 2, 4, 1, 3).forEachIndexed { i, index ->
                val point = nodeCenter(index)
                if (i == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
            close()
        }
    }

    val transition = rememberInfiniteTransition()
    val wheel by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(160_000, easing = LinearEasing)))
    val innerWheel by transition.animateFloat(0f, -360f, infiniteRepeatable(tween(100_000, easing = LinearEasing)))
    val flow by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2_200, easing = LinearEasing)))
    val breathe by transition.animateFloat(1f, 1.07f, infiniteRepeatable(tween(4_500), RepeatMode.Reverse))

    val isChat = mode == ConversationMode.CHAT
    val roleOf: (Persona) -> NodeRole? = { persona ->
        when {
            isChat -> if (persona == selectedPersona) NodeRole.SELECTED else null
            persona == debatePair.left -> NodeRole.FIRST_VOICE
            persona == debatePair.right -> NodeRole.SECOND_VOICE
            else -> null
        }
    }
    val spokes: List<Pair<Offset, Color>> = if (isChat) {
        listOf(personaCenter(selectedPersona) to purpleColor)
    } else {
        listOf(personaCenter(debatePair.left) to purpleColor, personaCenter(debatePair.right) to silverLightColor)
    }

    Box(Modifier.size(size).semantics { contentDescription = "Constelação de vozes" }) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = wheel }) {
            drawZodiac(this.size.width / DESIGN_SIZE, glyphPaths)
        }
        Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = innerWheel }) {
            val k = this.size.width / DESIGN_SIZE
            scale(k, pivot = Offset.Zero) {
                drawCircle(
                    silverColor.copy(alpha = 0.35f),
                    radius = 276f,
                    center = Offset(CENTER, CENTER),
                    style = Stroke(width = 6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(1f, 9f))),
                )
                drawCircle(
                    silverColor.copy(alpha = 0.16f),
                    radius = 150f,
                    center = Offset(CENTER, CENTER),
                    style = Stroke(width = density / k, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 5f))),
                )
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val k = this.size.width / DESIGN_SIZE
            val hairline = density / k
            scale(k, pivot = Offset.Zero) {
                val center = Offset(CENTER, CENTER)
                drawCircle(purpleColor.copy(alpha = 0.3f), radius = NODE_RADIUS, center = center, style = Stroke(hairline))
                drawCircle(silverColor.copy(alpha = 0.12f), radius = 262f, center = center, style = Stroke(hairline))
                drawPath(pentagram, silverColor.copy(alpha = 0.2f), style = Stroke(hairline))
                spokes.forEach { (point, color) ->
                    if (point == center) return@forEach
                    drawLine(
                        color = color.copy(alpha = 0.9f),
                        start = center,
                        end = point,
                        strokeWidth = 1.4f * hairline,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 7f), phase = -flow * 10f),
                    )
                    drawCircle(color.copy(alpha = 0.35f), radius = 58f, center = point, style = Stroke(hairline))
                }
            }
        }

        val orbSize = 220f * scale
        Box(
            Modifier
                .offset(((CENTER - 110f) * scale).dp, ((CENTER - 110f) * scale).dp)
                .size(orbSize.dp)
                .graphicsLayer { scaleX = breathe; scaleY = breathe }
                .drawBehind {
                    drawCircle(
                        Brush.radialGradient(
                            0f to purpleColor.copy(alpha = 0.34f),
                            0.45f to orbShadeColor.copy(alpha = 0.22f),
                            0.72f to Color.Transparent,
                            center = Offset(this.size.width / 2f, this.size.height * 0.38f),
                            radius = this.size.width / 2f,
                        )
                    )
                    drawCircle(purpleColor.copy(alpha = 0.22f), style = Stroke(1.dp.toPx()))
                }
        )
        if (!devMode) {
            Box(
                Modifier
                    .offset(((CENTER - 120f) * scale).dp, ((CENTER - 110f) * scale).dp)
                    .size((240f * scale).dp, orbSize.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isChat) {
                    val voice = selectedPersona.voice()
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            categoryLabel(selectedPersona),
                            color = purpleColor,
                            fontFamily = fonts.mono,
                            fontSize = (10f * scale).coerceAtLeast(8f).sp,
                            letterSpacing = 3.sp,
                        )
                        Text(
                            "${voice.firstName}\n${voice.lastName}",
                            color = textPrimaryColor,
                            fontFamily = fonts.serif,
                            fontWeight = FontWeight.Medium,
                            fontSize = (32f * scale).coerceAtLeast(20f).sp,
                            lineHeight = (32f * scale).coerceAtLeast(20f).sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    val nameSize = (25f * scale).coerceAtLeast(18f).sp
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (showLabels) debatePair.left.displayName else debatePair.left.voice().shortName,
                            color = purpleSoftColor,
                            fontFamily = fonts.serif,
                            fontWeight = FontWeight.Medium,
                            fontSize = nameSize,
                            textAlign = TextAlign.Center,
                        )
                        Text("vs", color = textMutedColor, fontFamily = fonts.serif, fontStyle = FontStyle.Italic, fontSize = (20f * scale).coerceAtLeast(15f).sp)
                        Text(
                            if (showLabels) debatePair.right.displayName else debatePair.right.voice().shortName,
                            color = silverPaleColor,
                            fontFamily = fonts.serif,
                            fontWeight = FontWeight.Medium,
                            fontSize = nameSize,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        val nodes = constellationPersonas.mapIndexed { index, persona -> persona to nodeCenter(index) } +
            if (devMode) listOf(Persona.O_FINGIDOR to Offset(CENTER, CENTER)) else emptyList()
        nodes.forEach { (persona, center) ->
            val nodeSize = (if (persona == Persona.O_FINGIDOR) 96f else 88f) * scale
            ConstellationNode(
                persona = persona,
                role = roleOf(persona),
                center = Offset(center.x * scale, center.y * scale),
                size = nodeSize,
                actionLabel = if (isChat) "Falar com ${persona.displayName}" else "Pôr ${persona.displayName} no debate",
                onClick = { onPersonaPicked(persona) },
            )
            if (showLabels) {
                NodeLabel(persona, roleOf(persona), Offset(center.x * scale, center.y * scale + nodeSize / 2f + 10f))
            }
        }
    }
}

private fun categoryLabel(persona: Persona): String = when (persona.category) {
    PersonaCategory.HETERONIMO -> "HETERÓNIMO"
    else -> persona.category.label.uppercase()
}

private fun DrawScope.drawZodiac(k: Float, glyphPaths: List<Path>) {
    val hairline = density / k
    scale(k, pivot = Offset.Zero) {
        val center = Offset(CENTER, CENTER)
        drawCircle(silverColor.copy(alpha = 0.22f), radius = 280f, center = center, style = Stroke(hairline))
        drawCircle(silverColor.copy(alpha = 0.22f), radius = 306f, center = center, style = Stroke(hairline))
        for (sign in 0 until 12) {
            drawLine(
                silverColor.copy(alpha = 0.4f),
                start = polar(sign * 30f, 280f),
                end = polar(sign * 30f, 306f),
                strokeWidth = hairline,
            )
        }
        drawCircle(purpleDeepColor.copy(alpha = 0.3f), radius = 15f, center = polar(GEMINI_INDEX * 30f + 15f, 293f))
        glyphPaths.forEachIndexed { index, path ->
            val angle = index * 30f + 15f
            val position = polar(angle, 293f)
            val isGemini = index == GEMINI_INDEX
            val glyphScale = 1.15f
            val strokeOnScreen = maxOf((if (isGemini) 1.6f else 1.2f) * k, 1.1f * density)
            translate(position.x, position.y) {
                rotate(angle, pivot = Offset.Zero) {
                    scale(glyphScale, pivot = Offset.Zero) {
                        drawPath(
                            path,
                            color = if (isGemini) purpleSoftColor else silverColor,
                            style = Stroke(
                                width = strokeOnScreen / (glyphScale * k),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConstellationNode(
    persona: Persona,
    role: NodeRole?,
    center: Offset,
    size: Float,
    actionLabel: String,
    onClick: () -> Unit,
) {
    val portrait = requireNotNull(personaPortrait(persona)) { "Missing portrait for $persona" }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val halo by rememberInfiniteTransition().animateFloat(0f, 1f, infiniteRepeatable(tween(2_800, easing = LinearEasing)))
    val isCentre = persona == Persona.O_FINGIDOR
    val ringColor = when (role) {
        NodeRole.SELECTED -> if (isCentre) amberColor else purpleColor
        NodeRole.FIRST_VOICE -> purpleColor
        NodeRole.SECOND_VOICE -> silverLightColor
        null -> silverColor.copy(alpha = 0.35f)
    }
    val glowColor = when (role) {
        NodeRole.SELECTED -> if (isCentre) amberColor.copy(alpha = 0.5f) else purpleDeepColor.copy(alpha = 0.65f)
        NodeRole.FIRST_VOICE -> purpleDeepColor.copy(alpha = 0.65f)
        NodeRole.SECOND_VOICE -> silverColor.copy(alpha = 0.5f)
        null -> Color.Transparent
    }
    val lift by animateDpAsState(if (isHovered) (-4).dp else 0.dp)

    // The hover and click area stays put; only the drawn portrait lifts. If the hit area moved
    // with the lift, a pointer near the bottom edge would leave it, drop it back, and flicker forever.
    Box(
        Modifier
            .offset((center.x - size / 2f).dp, (center.y - size / 2f).dp)
            .size(size.dp)
            .hoverable(interactionSource)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .semantics {
                this.role = Role.Button
                this.selected = role != null
                contentDescription = actionLabel
            }
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = lift.toPx() }
                .drawBehind {
                    if (role != null) {
                        val radius = this.size.width / 2f
                        drawCircle(
                            Brush.radialGradient(
                                0f to glowColor,
                                1f to Color.Transparent,
                                radius = radius * 1.9f,
                            ),
                            radius = radius * 1.9f,
                        )
                        drawCircle(ringColor.copy(alpha = 0.18f), radius = radius + 5.dp.toPx())
                        drawCircle(
                            ringColor.copy(alpha = 0.75f * (1f - halo)),
                            radius = (radius + 6.dp.toPx()) * (1f + 0.55f * halo),
                            style = Stroke(1.dp.toPx()),
                        )
                    }
                }
                .clip(CircleShape)
                .border(if (role != null) 2.dp else 1.dp, ringColor, CircleShape)
        ) {
            Image(
                painter = painterResource(portrait.resource),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                colorFilter = when {
                    role != null -> null
                    isHovered -> grayscaleFilter(0.95f)
                    else -> grayscaleFilter(0.6f)
                },
                modifier = Modifier.fillMaxSize().portraitZoom(),
            )
            if (isCentre) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BrandSigil((size * 0.5f).dp, if (role != null) amberColor else silverColor)
                }
            }
        }
    }
}

@Composable
private fun NodeLabel(persona: Persona, role: NodeRole?, topCenter: Offset) {
    val fonts = LocalAppFonts.current
    val (sub, subColor) = when (role) {
        NodeRole.FIRST_VOICE -> "PRIMEIRA VOZ" to purpleColor
        NodeRole.SECOND_VOICE -> "SEGUNDA VOZ" to silverSoftColor
        else -> categoryLabel(persona) to if (persona == Persona.O_FINGIDOR) amberColor else textMutedColor
    }
    Column(
        Modifier
            .offset((topCenter.x - 90f).dp, topCenter.y.dp)
            .width(180.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            persona.displayName,
            color = if (role != null) textPrimaryColor else textSecondaryColor,
            fontFamily = fonts.serif,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
        )
        Text(sub, color = subColor, fontFamily = fonts.mono, fontSize = 9.sp, letterSpacing = 2.sp)
    }
}
