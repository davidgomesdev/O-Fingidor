package me.davidgomesdev.ofingidor.ui.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.davidgomesdev.ofingidor.ui.LocalAppFonts
import me.davidgomesdev.ofingidor.ui.hairlineColor
import me.davidgomesdev.ofingidor.ui.model.ConversationMode
import me.davidgomesdev.ofingidor.ui.panelColor
import me.davidgomesdev.ofingidor.ui.purpleColor
import me.davidgomesdev.ofingidor.ui.purpleDeepColor
import me.davidgomesdev.ofingidor.ui.textMutedColor
import me.davidgomesdev.ofingidor.ui.textPrimaryColor

@Composable
fun ConversationModeToggle(
    mode: ConversationMode,
    onModeSelected: (ConversationMode) -> Unit,
) {
    val fonts = LocalAppFonts.current
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(panelColor.copy(alpha = 0.7f))
                .border(1.dp, hairlineColor, RoundedCornerShape(999.dp))
                .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ConversationMode.entries.forEach { option ->
            val isSelected = option == mode
            val background by animateColorAsState(if (isSelected) purpleDeepColor.copy(alpha = 0.2f) else Color.Transparent)
            val textColor by animateColorAsState(if (isSelected) textPrimaryColor else textMutedColor)
            Box(
                modifier =
                    Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(background)
                        .then(
                            if (isSelected) {
                                Modifier.border(1.dp, purpleColor.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                            } else {
                                Modifier
                            },
                        ).clickable(role = Role.Tab) { onModeSelected(option) }
                        .semantics { selected = isSelected }
                        .padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    when (option) {
                        ConversationMode.CHAT -> "Conversa"
                        ConversationMode.DEBATE -> "Debate"
                    },
                    color = textColor,
                    fontFamily = fonts.sans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                )
            }
        }
    }
}
