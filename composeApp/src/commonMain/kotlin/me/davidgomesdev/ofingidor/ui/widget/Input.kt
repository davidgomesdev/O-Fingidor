package me.davidgomesdev.ofingidor.ui.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.davidgomesdev.ofingidor.ui.cardBorderColor
import me.davidgomesdev.ofingidor.ui.componentsBackgroundColor
import me.davidgomesdev.ofingidor.ui.focusedIndicatorColor
import me.davidgomesdev.ofingidor.ui.inputCardBackgroundColor
import me.davidgomesdev.ofingidor.ui.isActionInputType
import me.davidgomesdev.ofingidor.ui.service.isMobileDevice

private val exampleQueries = listOf(
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

@Composable
fun ThinkInputCard(
    state: TextFieldState,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    onQuerySelected: (String) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) focusedIndicatorColor else cardBorderColor
    )
    var showExamples by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(inputCardBackgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
    ) {
        ThinkInputField(state, isLoading, interactionSource, onSubmit)
        HorizontalDivider(color = cardBorderColor, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = if (isMobileDevice()) Arrangement.Center else Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isMobileDevice()) {
                Text(
                    "Control + Enter para enviar",
                    color = Color.White.copy(alpha = 0.25f),
                    fontSize = 11.sp
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isLoading) {
                    ExamplesToggleButton(showExamples) { showExamples = !showExamples }
                }
                ThinkButton(onSubmit, isLoading)
            }
        }
        if (!isLoading && showExamples) {
            ExampleQueriesRow(
                onQuerySelected = onQuerySelected,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExampleQueriesRow(onQuerySelected: (String) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        exampleQueries.forEach { query ->
            Text(
                text = query,
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(componentsBackgroundColor.copy(alpha = 0.5f))
                    .border(1.dp, cardBorderColor, RoundedCornerShape(6.dp))
                    .clickable { onQuerySelected(query) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun ExamplesToggleButton(showExamples: Boolean, onToggle: () -> Unit) {
    Text(
        text = if (showExamples) "Esconder exemplos" else "Mostrar exemplos",
        color = Color.White.copy(alpha = 0.3f),
        fontSize = 11.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(componentsBackgroundColor)
            .border(1.dp, cardBorderColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun ThinkInputField(
    state: TextFieldState,
    isLoading: Boolean,
    interactionSource: MutableInteractionSource,
    onSubmit: () -> Unit
) {
    TextField(
        state = state,
        placeholder = {
            Text(
                "Escreve o que te inquieta a alma...",
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 14.sp
            )
        },
        enabled = !isLoading,
        lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3),
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { keyEvent ->
                if (isActionInputType(keyEvent) && !isLoading) {
                    onSubmit()
                    true
                } else {
                    false
                }
            },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            disabledTextColor = Color.White.copy(alpha = 0.4f),
            cursorColor = Color.White,
        )
    )
}

@Composable
fun ThinkButton(onSubmit: () -> Unit, isLoading: Boolean) {
    Button(
        onClick = onSubmit,
        enabled = !isLoading,
        shape = RoundedCornerShape(6.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = componentsBackgroundColor,
            contentColor = Color.White,
            disabledContainerColor = inputCardBackgroundColor,
            disabledContentColor = Color.White.copy(alpha = 0.3f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Always reserve space for the wider label so the button never resizes
            Text("A pensar…", fontSize = 13.sp, modifier = Modifier.alpha(0f))
            Text(
                if (isLoading) "A pensar…" else "Pensar",
                fontSize = 13.sp
            )
        }
    }
}
