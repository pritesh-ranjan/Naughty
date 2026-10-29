package com.example.naughty.ui.oneui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.naughty.ui.oneui.theme.OneUi

@Composable
fun OneUiProgressBar(
    progress: Float, // 0f to 1f
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    trackColor: Color = OneUi.colors.divider,
    progressColor: Color = OneUi.colors.accent,
    color: Color = progressColor
) {
    val effectiveColor = if (color != progressColor) color else progressColor
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "oneUiProgress"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val radius = size.height / 2f
        // Draw track
        drawRoundRect(
            color = trackColor,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = CornerRadius(radius, radius)
        )
        // Draw active
        if (animatedProgress > 0f) {
            drawRoundRect(
                color = effectiveColor,
                topLeft = Offset.Zero,
                size = Size(size.width * animatedProgress, size.height),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
    }
}

data class OneUiDonutSegment(
    val fraction: Float,
    val color: Color
)

@Composable
fun OneUiDonut(
    segments: List<OneUiDonutSegment>,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    strokeWidth: Dp = 14.dp,
    trackColor: Color = OneUi.colors.divider,
    centerContent: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val canvasSize = this.size.minDimension
            val diameter = canvasSize - strokeWidth.toPx()
            val topLeft = Offset(strokeWidth.toPx() / 2f, strokeWidth.toPx() / 2f)

            // Draw background track ring
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = stroke
            )

            // Draw segments
            var currentStartAngle = -90f
            segments.forEach { segment ->
                val sweep = (segment.fraction.coerceIn(0f, 1f)) * 360f
                if (sweep > 0f) {
                    drawArc(
                        color = segment.color,
                        startAngle = currentStartAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = stroke
                    )
                    currentStartAngle += sweep
                }
            }
        }

        centerContent?.invoke()
    }
}
