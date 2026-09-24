package me.davidgomesdev.ofingidor.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import ofingidor.composeapp.generated.resources.Res
import ofingidor.composeapp.generated.resources.cormorant_400
import ofingidor.composeapp.generated.resources.cormorant_400_italic
import ofingidor.composeapp.generated.resources.cormorant_500
import ofingidor.composeapp.generated.resources.cormorant_500_italic
import ofingidor.composeapp.generated.resources.cormorant_600
import ofingidor.composeapp.generated.resources.geist_400
import ofingidor.composeapp.generated.resources.geist_500
import ofingidor.composeapp.generated.resources.geist_600
import ofingidor.composeapp.generated.resources.geist_mono_400
import ofingidor.composeapp.generated.resources.geist_mono_500
import org.jetbrains.compose.resources.Font

/** The three typefaces: Cormorant Garamond for literary display, Geist for UI text, Geist Mono for small labels. */
data class AppFonts(
    val serif: FontFamily,
    val sans: FontFamily,
    val mono: FontFamily,
)

val LocalAppFonts = staticCompositionLocalOf { AppFonts(FontFamily.Serif, FontFamily.SansSerif, FontFamily.Monospace) }

@Composable
private fun rememberAppFonts(): AppFonts =
    AppFonts(
        serif =
            FontFamily(
                Font(Res.font.cormorant_400, FontWeight.Normal),
                Font(Res.font.cormorant_500, FontWeight.Medium),
                Font(Res.font.cormorant_600, FontWeight.SemiBold),
                Font(Res.font.cormorant_400_italic, FontWeight.Normal, FontStyle.Italic),
                Font(Res.font.cormorant_500_italic, FontWeight.Medium, FontStyle.Italic),
            ),
        sans =
            FontFamily(
                Font(Res.font.geist_400, FontWeight.Normal),
                Font(Res.font.geist_500, FontWeight.Medium),
                Font(Res.font.geist_600, FontWeight.SemiBold),
            ),
        mono =
            FontFamily(
                Font(Res.font.geist_mono_400, FontWeight.Normal),
                Font(Res.font.geist_mono_500, FontWeight.Medium),
            ),
    )

private fun sansTypography(sans: FontFamily): Typography {
    val defaults = Typography()
    return Typography(
        displayLarge = defaults.displayLarge.copy(fontFamily = sans),
        displayMedium = defaults.displayMedium.copy(fontFamily = sans),
        displaySmall = defaults.displaySmall.copy(fontFamily = sans),
        headlineLarge = defaults.headlineLarge.copy(fontFamily = sans),
        headlineMedium = defaults.headlineMedium.copy(fontFamily = sans),
        headlineSmall = defaults.headlineSmall.copy(fontFamily = sans),
        titleLarge = defaults.titleLarge.copy(fontFamily = sans),
        titleMedium = defaults.titleMedium.copy(fontFamily = sans),
        titleSmall = defaults.titleSmall.copy(fontFamily = sans),
        bodyLarge = defaults.bodyLarge.copy(fontFamily = sans),
        bodyMedium = defaults.bodyMedium.copy(fontFamily = sans),
        bodySmall = defaults.bodySmall.copy(fontFamily = sans),
        labelLarge = defaults.labelLarge.copy(fontFamily = sans),
        labelMedium = defaults.labelMedium.copy(fontFamily = sans),
        labelSmall = defaults.labelSmall.copy(fontFamily = sans),
    )
}

@Composable
fun MysticTheme(content: @Composable () -> Unit) {
    val fonts = rememberAppFonts()
    CompositionLocalProvider(LocalAppFonts provides fonts) {
        MaterialTheme(typography = sansTypography(fonts.sans), content = content)
    }
}
