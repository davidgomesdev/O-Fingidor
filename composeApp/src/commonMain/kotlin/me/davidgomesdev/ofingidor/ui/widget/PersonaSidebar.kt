package me.davidgomesdev.ofingidor.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.davidgomesdev.ofingidor.ui.componentColumnBackgroundColor
import me.davidgomesdev.ofingidor.ui.devChipBorderColor
import me.davidgomesdev.ofingidor.ui.devChipColor
import me.davidgomesdev.ofingidor.ui.devChipTextColor
import me.davidgomesdev.ofingidor.ui.focusedIndicatorColor
import me.davidgomesdev.ofingidor.ui.model.Persona
import me.davidgomesdev.ofingidor.ui.model.PersonaCategory
import me.davidgomesdev.ofingidor.ui.ninguemChipBorderColor
import me.davidgomesdev.ofingidor.ui.ninguemChipColor
import me.davidgomesdev.ofingidor.ui.ninguemChipTextColor
import me.davidgomesdev.ofingidor.ui.orthonymChipBorderColor
import me.davidgomesdev.ofingidor.ui.orthonymChipColor
import me.davidgomesdev.ofingidor.ui.orthonymChipTextColor
import me.davidgomesdev.ofingidor.ui.semiHeteronymChipBorderColor
import me.davidgomesdev.ofingidor.ui.semiHeteronymChipColor
import me.davidgomesdev.ofingidor.ui.semiHeteronymChipTextColor

@Composable
fun PersonaTab(
    selectedPersona: Persona,
    onPersonaSelected: (Persona) -> Unit,
    devMode: Boolean,
    modifier: Modifier = Modifier,
) {
    val categories = PersonaCategory.entries.filter { it != PersonaCategory.DEV || devMode }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        categories.forEach { category ->
            val personas = Persona.entries.filter { it.category == category }
            PersonaCategorySection(
                category = category,
                personas = personas,
                selectedPersona = selectedPersona,
                onPersonaSelected = onPersonaSelected,
            )
        }
    }
}

@Composable
private fun PersonaCategorySection(
    category: PersonaCategory,
    personas: List<Persona>,
    selectedPersona: Persona,
    onPersonaSelected: (Persona) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            category.label.uppercase(),
            color = focusedIndicatorColor.copy(alpha = 0.5f),
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(personas) { persona ->
                PersonaTabChip(
                    persona = persona,
                    isSelected = persona == selectedPersona,
                    onSelected = { onPersonaSelected(persona) },
                )
            }
        }
    }
}

@Composable
private fun PersonaTabChip(
    persona: Persona,
    isSelected: Boolean,
    onSelected: () -> Unit,
) {
    val category = persona.category

    val bgColor = when {
        isSelected && category == PersonaCategory.ORTONIMO -> orthonymChipColor
        isSelected && category == PersonaCategory.SEMI_HETERONIMO -> semiHeteronymChipColor
        isSelected && persona == Persona.NINGUEM -> ninguemChipColor
        isSelected && category == PersonaCategory.DEV -> devChipColor
        isSelected -> componentColumnBackgroundColor
        else -> Color.Transparent
    }
    val borderColor = when {
        isSelected && category == PersonaCategory.ORTONIMO -> orthonymChipBorderColor
        isSelected && category == PersonaCategory.SEMI_HETERONIMO -> semiHeteronymChipBorderColor
        isSelected && persona == Persona.NINGUEM -> ninguemChipBorderColor
        isSelected && category == PersonaCategory.DEV -> devChipBorderColor
        isSelected -> focusedIndicatorColor
        else -> focusedIndicatorColor.copy(alpha = 0.3f)
    }
    val textColor = when {
        isSelected && category == PersonaCategory.ORTONIMO -> orthonymChipTextColor
        isSelected && category == PersonaCategory.SEMI_HETERONIMO -> semiHeteronymChipTextColor
        isSelected && persona == Persona.NINGUEM -> ninguemChipTextColor
        isSelected && category == PersonaCategory.DEV -> devChipTextColor
        isSelected -> Color.White
        else -> Color.White.copy(alpha = 0.4f)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onSelected)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            persona.displayName,
            color = textColor,
            fontSize = 11.sp,
        )
    }
}
