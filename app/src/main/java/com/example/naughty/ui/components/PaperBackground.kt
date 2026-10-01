package com.example.naughty.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.naughty.ui.theme.PaperPattern

fun Modifier.paperBackground(
    paperPattern: PaperPattern,
    lineColor: Color,
    stepDp: Dp = 22.dp,
    strokeWidthDp: Dp = 0.85.dp
): Modifier = this.drawBehind {
    if (paperPattern == PaperPattern.NONE || lineColor == Color.Transparent || lineColor.alpha <= 0.01f) {
        return@drawBehind
    }

    val step = stepDp.toPx()
    val strokeWidth = strokeWidthDp.toPx()

    when (paperPattern) {
        PaperPattern.GRID -> {
            var x = 0f
            while (x <= size.width) {
                drawLine(
                    color = lineColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = strokeWidth
                )
                x += step
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
                y += step
            }
        }
        PaperPattern.DOTS -> {
            val radius = strokeWidth * 1.35f
            var x = step / 2f
            while (x < size.width) {
                var y = step / 2f
                while (y < size.height) {
                    drawCircle(
                        color = lineColor,
                        radius = radius,
                        center = Offset(x, y)
                    )
                    y += step
                }
                x += step
            }
        }
        PaperPattern.LINES -> {
            var y = step
            while (y <= size.height) {
                drawLine(
                    color = lineColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
                y += step
            }
        }
        PaperPattern.NONE -> {}
    }
}
