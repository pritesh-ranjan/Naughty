package com.example.naughty.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.naughty.ui.theme.PaperPattern

fun Modifier.paperBackground(
    paperPattern: PaperPattern,
    lineColor: Color,
    stepDp: Dp = 22.dp,
    strokeWidthDp: Dp = 0.85.dp
): Modifier = this.drawWithCache {
    if (paperPattern == PaperPattern.NONE || lineColor == Color.Transparent || lineColor.alpha <= 0.01f || size.width <= 0f || size.height <= 0f) {
        onDrawBehind { }
    } else {
        val step = stepDp.toPx()
        val strokeWidth = strokeWidthDp.toPx()

        when (paperPattern) {
            PaperPattern.GRID -> {
                val gridPath = Path()
                var x = 0f
                while (x <= size.width) {
                    gridPath.moveTo(x, 0f)
                    gridPath.lineTo(x, size.height)
                    x += step
                }
                var y = 0f
                while (y <= size.height) {
                    gridPath.moveTo(0f, y)
                    gridPath.lineTo(size.width, y)
                    y += step
                }
                val strokeStyle = Stroke(width = strokeWidth)
                onDrawBehind {
                    drawPath(path = gridPath, color = lineColor, style = strokeStyle)
                }
            }
            PaperPattern.DOTS -> {
                val dotsPath = Path()
                val radius = strokeWidth * 1.35f
                var x = step / 2f
                while (x < size.width) {
                    var y = step / 2f
                    while (y < size.height) {
                        dotsPath.addOval(Rect(center = Offset(x, y), radius = radius))
                        y += step
                    }
                    x += step
                }
                onDrawBehind {
                    drawPath(path = dotsPath, color = lineColor)
                }
            }
            PaperPattern.LINES -> {
                val linesPath = Path()
                var y = step
                while (y <= size.height) {
                    linesPath.moveTo(0f, y)
                    linesPath.lineTo(size.width, y)
                    y += step
                }
                val strokeStyle = Stroke(width = strokeWidth)
                onDrawBehind {
                    drawPath(path = linesPath, color = lineColor, style = strokeStyle)
                }
            }
            PaperPattern.NONE -> {
                onDrawBehind { }
            }
        }
    }
}
