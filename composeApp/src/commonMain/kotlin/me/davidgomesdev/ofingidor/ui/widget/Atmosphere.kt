package me.davidgomesdev.ofingidor.ui.widget

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.davidgomesdev.ofingidor.ui.inkColor
import me.davidgomesdev.ofingidor.ui.purpleColor
import me.davidgomesdev.ofingidor.ui.silverColor
import me.davidgomesdev.ofingidor.ui.textPrimaryColor
import me.davidgomesdev.ofingidor.ui.vignetteColor
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class Star(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float,
    val speed: Float,
)

/** The backdrop of every screen: slow purple and grey glows, twinkling stars and darkened edges. */
@Composable
fun Atmosphere(
    modifier: Modifier = Modifier,
    starCount: Int = 120,
) {
    val stars =
        remember(starCount) {
            val random = Random(11)
            List(starCount) {
                val kind = random.nextFloat()
                Star(
                    x = random.nextFloat(),
                    y = random.nextFloat(),
                    radius =
                        if (kind < 0.1f) {
                            1.2f
                        } else if (kind < 0.5f) {
                            0.55f
                        } else {
                            0.8f
                        },
                    phase = random.nextFloat(),
                    speed = 0.5f + random.nextFloat() * 1.2f,
                )
            }
        }
    val transition = rememberInfiniteTransition()
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing)),
    )
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(13_000), RepeatMode.Reverse),
    )

    Canvas(modifier.fillMaxSize()) {
        drawRect(inkColor)

        val purpleCenter = Offset(size.width * 0.74f + drift * 40f, size.height * 0.47f - drift * 30f)
        drawCircle(
            brush =
                Brush.radialGradient(
                    0f to purpleColor.copy(alpha = 0.22f),
                    0.42f to purpleColor.copy(alpha = 0.06f),
                    1f to Color.Transparent,
                    center = purpleCenter,
                    radius = size.minDimension * 0.55f,
                ),
            radius = size.minDimension * 0.55f,
            center = purpleCenter,
        )
        val greyCenter = Offset(size.width * 0.08f - drift * 30f, size.height * 0.95f + drift * 20f)
        drawCircle(
            brush =
                Brush.radialGradient(
                    0f to silverColor.copy(alpha = 0.1f),
                    1f to Color.Transparent,
                    center = greyCenter,
                    radius = size.minDimension * 0.5f,
                ),
            radius = size.minDimension * 0.5f,
            center = greyCenter,
        )

        stars.forEach { star ->
            val wave = sin(((time * 60f * star.speed / 6f) + star.phase) * 2f * PI.toFloat())
            val alpha = 0.12f + 0.83f * (wave * 0.5f + 0.5f)
            drawCircle(
                color = textPrimaryColor.copy(alpha = alpha),
                radius = star.radius * density,
                center = Offset(star.x * size.width, star.y * size.height),
            )
        }

        drawRect(
            brush =
                Brush.radialGradient(
                    0.55f to Color.Transparent,
                    1f to vignetteColor.copy(alpha = 0.75f),
                    center = Offset(size.width * 0.55f, size.height * 0.45f),
                    radius = maxOf(size.width, size.height) * 0.75f,
                ),
        )
    }
}

/** The O Fingidor mark: two rings around a four-pointed star. */
@Composable
fun BrandSigil(
    size: Dp = 38.dp,
    color: Color = purpleColor,
) {
    Canvas(Modifier.size(size)) {
        val unit = this.size.width / 38f
        val stroke = Stroke(width = 1.2f * unit)
        drawCircle(color.copy(alpha = 0.45f), radius = 17.5f * unit, style = stroke)
        drawCircle(color.copy(alpha = 0.8f), radius = 11f * unit, style = stroke)
        val star =
            Path().apply {
                val points =
                    listOf(
                        19f to 4f,
                        22.5f to 15.5f,
                        34f to 19f,
                        22.5f to 22.5f,
                        19f to 34f,
                        15.5f to 22.5f,
                        4f to 19f,
                        15.5f to 15.5f,
                    )
                points.forEachIndexed { index, (x, y) ->
                    if (index == 0) moveTo(x * unit, y * unit) else lineTo(x * unit, y * unit)
                }
                close()
            }
        drawPath(star, color.copy(alpha = 0.18f))
        drawPath(star, color.copy(alpha = 0.8f), style = stroke)
        drawCircle(textPrimaryColor, radius = 2.2f * unit)
    }
}

/** A small four-pointed star used as a bullet before labels. */
@Composable
fun SparkMark(
    size: Dp = 10.dp,
    color: Color = purpleColor,
) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val path =
            Path().apply {
                moveTo(w * 0.5f, 0f)
                lineTo(w * 0.62f, w * 0.38f)
                lineTo(w, w * 0.5f)
                lineTo(w * 0.62f, w * 0.62f)
                lineTo(w * 0.5f, w)
                lineTo(w * 0.38f, w * 0.62f)
                lineTo(0f, w * 0.5f)
                lineTo(w * 0.38f, w * 0.38f)
                close()
            }
        drawPath(path, color)
    }
}
