package me.davidgomesdev.ofingidor.ui.widget

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.davidgomesdev.ofingidor.ui.LocalAppFonts
import me.davidgomesdev.ofingidor.ui.model.ConversationMode
import me.davidgomesdev.ofingidor.ui.model.HeroQuote
import me.davidgomesdev.ofingidor.ui.purpleColor
import me.davidgomesdev.ofingidor.ui.purpleDeepColor
import me.davidgomesdev.ofingidor.ui.silverLightColor
import me.davidgomesdev.ofingidor.ui.textMutedColor
import me.davidgomesdev.ofingidor.ui.textPrimaryColor
import me.davidgomesdev.ofingidor.ui.textSecondaryColor

@Composable
fun HeroEyebrow() {
    val fonts = LocalAppFonts.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SparkMark()
        Text(
            "UM ORÁCULO DE HETERÓNIMOS",
            color = textMutedColor,
            fontFamily = fonts.mono,
            fontSize = 12.sp,
            letterSpacing = 3.8.sp,
        )
    }
}

/** "Fala com quem fingiu ser tantos." in chat, "Faz discutir quem nunca foi um só." in debate. */
@Composable
fun HeroHeadline(
    mode: ConversationMode,
    isCompact: Boolean,
) {
    val fonts = LocalAppFonts.current
    val size = if (isCompact) 36.sp else 68.sp
    val accent =
        SpanStyle(
            color = purpleColor,
            fontStyle = FontStyle.Italic,
            shadow = Shadow(color = purpleDeepColor.copy(alpha = 0.55f), blurRadius = 40f),
        )
    val lineBreak = if (isCompact) " " else "\n"
    val text =
        when (mode) {
            ConversationMode.CHAT -> {
                buildAnnotatedString {
                    append("Fala com quem$lineBreak")
                    withStyle(accent) { append("fingiu") }
                    append(" ser tantos.")
                }
            }

            ConversationMode.DEBATE -> {
                buildAnnotatedString {
                    append("Faz ")
                    withStyle(accent) { append("discutir") }
                    append(" quem${lineBreak}nunca foi um só.")
                }
            }
        }
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (fadeIn(tween(700)) + slideInVertically(tween(700)) { it / 6 }) togetherWith fadeOut(tween(200))
        },
    ) { headline ->
        Text(
            headline,
            style =
                TextStyle(
                    color = textPrimaryColor,
                    fontFamily = fonts.serif,
                    fontWeight = FontWeight.Normal,
                    fontSize = size,
                    lineHeight = size,
                    letterSpacing = (-0.5).sp,
                ),
        )
    }
}

/** A verse with a thin fading rule before it and its source underneath. */
@Composable
fun HeroQuoteBlock(
    quote: HeroQuote,
    secondary: Boolean,
) {
    val fonts = LocalAppFonts.current
    AnimatedContent(
        targetState = quote,
        transitionSpec = { fadeIn(tween(900)) togetherWith fadeOut(tween(200)) },
    ) { current ->
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(
                Modifier
                    .padding(top = 12.dp)
                    .width(36.dp)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(if (secondary) silverLightColor else purpleColor, Color.Transparent),
                        ),
                    ),
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    current.text,
                    color = textSecondaryColor,
                    fontFamily = fonts.serif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 19.sp,
                    lineHeight = 27.5.sp,
                )
                Text(
                    current.citation,
                    color = textMutedColor,
                    fontFamily = fonts.mono,
                    fontSize = 11.sp,
                    letterSpacing = 2.4.sp,
                )
            }
        }
    }
}
