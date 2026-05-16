package me.davidgomesdev.ofingidor.ui.widget

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.davidgomesdev.ofingidor.ui.focusedIndicatorColor
import me.davidgomesdev.ofingidor.ui.model.DebatePair
import me.davidgomesdev.ofingidor.ui.model.Persona
import me.davidgomesdev.ofingidor.ui.model.PersonaCategory

@Composable
fun DebatePicker(
    selectedPair: DebatePair,
    onLeftPersonaSelected: (Persona) -> Unit,
    onRightPersonaSelected: (Persona) -> Unit,
    devMode: Boolean,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        DebatePersonaSlot(
            title = "Esquerda",
            selectedPersona = selectedPair.left,
            oppositePersona = selectedPair.right,
            onPersonaSelected = onLeftPersonaSelected,
            devMode = devMode,
        )
        DebatePersonaSlot(
            title = "Direita",
            selectedPersona = selectedPair.right,
            oppositePersona = selectedPair.left,
            onPersonaSelected = onRightPersonaSelected,
            devMode = devMode,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DebatePersonaSlot(
    title: String,
    selectedPersona: Persona,
    oppositePersona: Persona,
    onPersonaSelected: (Persona) -> Unit,
    devMode: Boolean,
) {
    val categories = PersonaCategory.entries.filter { it != PersonaCategory.DEV || devMode }

    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(),
            color = focusedIndicatorColor.copy(alpha = 0.65f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            modifier = Modifier.sizeIn(minWidth = 80.dp, maxWidth = 80.dp),
        )
        categories.forEach { category ->
            val personas = Persona.entries.filter { it.category == category && it != oppositePersona }

            if (personas.isEmpty()) return@forEach

            PersonaCategorySection(
                category = category,
                personas = personas,
                selectedPersona = selectedPersona,
                onPersonaSelected = onPersonaSelected,
            )
        }
    }
}
