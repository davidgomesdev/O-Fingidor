package me.davidgomesdev.ofingidor.ui.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.davidgomesdev.ofingidor.ui.aiBubbleBorder
import me.davidgomesdev.ofingidor.ui.cardBorderColor
import me.davidgomesdev.ofingidor.ui.devChipBorderColor
import me.davidgomesdev.ofingidor.ui.devChipColor
import me.davidgomesdev.ofingidor.ui.devChipTextColor
import me.davidgomesdev.ofingidor.ui.model.Persona
import me.davidgomesdev.ofingidor.ui.personaLabelColor
import me.davidgomesdev.ofingidor.ui.portraitThumbnailBackgroundColor
import ofingidor.composeapp.generated.resources.Res
import ofingidor.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun AppHeader(
    selectedPersona: Persona,
    devMode: Boolean,
    hasConversationStarted: Boolean,
    onDevModeToggle: (() -> Unit)?,
    onNewConversation: (() -> Unit)?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(46.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(portraitThumbnailBackgroundColor)
                        .border(1.dp, aiBubbleBorder, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.logo),
                        contentDescription = selectedPersona.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(32.dp)
                            .height(46.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                }
                Column {
                    Text(
                        "O Fingidor",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        selectedPersona.displayName.uppercase(),
                        color = personaLabelColor,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (hasConversationStarted && onNewConversation != null) {
                    NewConversationButton(onClick = onNewConversation)
                }
                if (onDevModeToggle != null) {
                    DevModeToggle(active = devMode, onToggle = onDevModeToggle)
                }
            }
        }
        HorizontalDivider(color = cardBorderColor, thickness = 1.dp)
    }
}

@Composable
private fun NewConversationButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            "Nova conversa",
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun DevModeToggle(active: Boolean, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (active) devChipColor else Color.Transparent)
            .border(
                1.dp,
                if (active) devChipBorderColor else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            "Modo DEV",
            color = if (active) devChipTextColor else Color.White.copy(alpha = 0.2f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp
        )
    }
}
