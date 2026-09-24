package me.davidgomesdev.ofingidor.ui.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.davidgomesdev.ofingidor.shared.dto.Persona
import me.davidgomesdev.ofingidor.ui.LocalAppFonts
import me.davidgomesdev.ofingidor.ui.amberColor
import me.davidgomesdev.ofingidor.ui.hairlineColor
import me.davidgomesdev.ofingidor.ui.hairlineStrongColor
import me.davidgomesdev.ofingidor.ui.model.ConversationMode
import me.davidgomesdev.ofingidor.ui.model.PersonaPortrait
import me.davidgomesdev.ofingidor.ui.switchOffColor
import me.davidgomesdev.ofingidor.ui.textMutedColor
import me.davidgomesdev.ofingidor.ui.textPrimaryColor
import me.davidgomesdev.ofingidor.ui.textSecondaryColor
import kotlin.time.Duration.Companion.milliseconds

data class AppHeaderIdentity(
    val portrait: PersonaPortrait,
    val personaLabel: String,
)

fun appHeaderIdentity(persona: Persona): AppHeaderIdentity =
    AppHeaderIdentity(
        portrait = requireNotNull(personaPortrait(persona)) { "Missing portrait for $persona" },
        personaLabel = persona.displayName.uppercase(),
    )

@Composable
fun AppHeader(
    mode: ConversationMode,
    onModeSelected: (ConversationMode) -> Unit,
    devMode: Boolean,
    hasConversationStarted: Boolean,
    onDevModeToggle: () -> Unit,
    onNewConversation: () -> Unit,
    onShare: (() -> Unit)?,
    isCompact: Boolean,
) {
    val fonts = LocalAppFonts.current
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(if (isCompact) 64.dp else 88.dp)
                    .padding(horizontal = if (isCompact) 14.dp else 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (isCompact) 10.dp else 14.dp),
            ) {
                BrandSigil(if (isCompact) 30.dp else 38.dp)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "O Fingidor",
                        color = textPrimaryColor,
                        fontFamily = fonts.serif,
                        fontWeight = FontWeight.Medium,
                        fontSize = if (isCompact) 23.sp else 26.sp,
                        lineHeight = 26.sp,
                    )
                    if (!isCompact) {
                        Text(
                            "FERNANDO PESSOA & OS OUTROS",
                            color = textMutedColor,
                            fontFamily = fonts.mono,
                            fontSize = 10.sp,
                            letterSpacing = 2.8.sp,
                        )
                    }
                }
            }
            if (!isCompact) {
                ConversationModeToggle(mode = mode, onModeSelected = onModeSelected)
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (hasConversationStarted && onShare != null) {
                    ShareButton(onClick = onShare, isCompact = isCompact)
                }
                if (hasConversationStarted) {
                    HeaderButton(
                        label = "Nova conversa",
                        iconOnly = isCompact,
                        icon = { PlusIcon() },
                        onClick = onNewConversation,
                    )
                }
                DevModeToggle(active = devMode, onToggle = onDevModeToggle, isCompact = isCompact)
            }
        }
        if (isCompact) {
            Box(Modifier.fillMaxWidth().padding(bottom = 6.dp), contentAlignment = Alignment.Center) {
                ConversationModeToggle(mode = mode, onModeSelected = onModeSelected)
            }
        }
        if (hasConversationStarted) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(hairlineColor))
        }
    }
}

@Composable
private fun HeaderButton(
    label: String,
    iconOnly: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val fonts = LocalAppFonts.current
    Row(
        modifier =
            Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, hairlineStrongColor, RoundedCornerShape(12.dp))
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { if (iconOnly) contentDescription = label }
                .padding(horizontal = if (iconOnly) 13.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        icon()
        if (!iconOnly) {
            Text(label, color = textSecondaryColor, fontFamily = fonts.sans, fontSize = 14.sp)
        }
    }
}

@Composable
private fun PlusIcon(
    color: Color = textSecondaryColor,
    size: Dp = 16.dp,
) {
    Canvas(Modifier.size(size)) {
        val stroke = 1.5.dp.toPx()
        val w = this.size.width
        drawLine(color, Offset(w / 2f, w * 0.19f), Offset(w / 2f, w * 0.81f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.19f, w / 2f), Offset(w * 0.81f, w / 2f), stroke, StrokeCap.Round)
    }
}

@Composable
private fun ShareIcon(
    color: Color = textSecondaryColor,
    size: Dp = 16.dp,
) {
    Canvas(Modifier.size(size)) {
        val stroke = 1.5.dp.toPx()
        val u = this.size.width / 16f
        drawLine(color, Offset(8 * u, 2 * u), Offset(8 * u, 10 * u), stroke, StrokeCap.Round)
        drawLine(color, Offset(5 * u, 5 * u), Offset(8 * u, 2 * u), stroke, StrokeCap.Round)
        drawLine(color, Offset(11 * u, 5 * u), Offset(8 * u, 2 * u), stroke, StrokeCap.Round)
        drawLine(color, Offset(3 * u, 9 * u), Offset(3 * u, 13 * u), stroke, StrokeCap.Round)
        drawLine(color, Offset(3 * u, 13 * u), Offset(13 * u, 13 * u), stroke, StrokeCap.Round)
        drawLine(color, Offset(13 * u, 13 * u), Offset(13 * u, 9 * u), stroke, StrokeCap.Round)
    }
}

@Composable
private fun DevModeToggle(
    active: Boolean,
    onToggle: () -> Unit,
    isCompact: Boolean,
) {
    val fonts = LocalAppFonts.current
    val knobOffset by animateDpAsState(if (active) 16.dp else 0.dp)
    val knobColor by animateColorAsState(if (active) amberColor else switchOffColor)
    Row(
        modifier =
            Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Switch, onClick = onToggle)
                .semantics {
                    contentDescription = "Modo DEV"
                    stateDescription = if (active) "Ligado" else "Desligado"
                }.padding(horizontal = if (isCompact) 8.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            if (isCompact) "DEV" else "MODO DEV",
            color = if (active) amberColor else textMutedColor,
            fontFamily = fonts.mono,
            fontSize = 11.sp,
            letterSpacing = 1.8.sp,
        )
        Box(
            Modifier
                .size(width = 34.dp, height = 18.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(textPrimaryColor.copy(alpha = 0.08f))
                .border(1.dp, textPrimaryColor.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                .padding(horizontal = 2.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .offset(x = knobOffset)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(knobColor),
            )
        }
    }
}

@Composable
private fun ShareButton(
    onClick: () -> Unit,
    isCompact: Boolean,
) {
    var shared by remember { mutableStateOf(false) }

    LaunchedEffect(shared) {
        if (shared) {
            delay(2000.milliseconds)
            shared = false
        }
    }

    HeaderButton(
        label = if (shared) "Partilhado!" else "Partilhar",
        iconOnly = isCompact,
        icon = { ShareIcon() },
        enabled = !shared,
        onClick = {
            onClick()
            shared = true
        },
    )
}
