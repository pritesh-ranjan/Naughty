package com.example.naughty.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.naughty.ui.theme.ElectricAmber
import com.example.naughty.ui.theme.ElectricPink
import com.example.naughty.ui.theme.isAppInDarkTheme
import kotlin.math.roundToInt

enum class SwipeAction { NONE, ARCHIVE, DELETE }

@Composable
fun SwipeableNoteItem(
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val actionWidth = with(density) { 80.dp.toPx() }

    val anchors = remember(actionWidth) {
        DraggableAnchors {
            SwipeAction.DELETE at actionWidth
            SwipeAction.NONE at 0f
            SwipeAction.ARCHIVE at -actionWidth
        }
    }

    val state = remember {
        AnchoredDraggableState(
            initialValue = SwipeAction.NONE,
            positionalThreshold = { distance: Float -> distance * 0.4f },
            velocityThreshold = { with(density) { 125.dp.toPx() } },
            snapAnimationSpec = tween(durationMillis = 250),
            decayAnimationSpec = androidx.compose.animation.core.exponentialDecay()
        )
    }

    androidx.compose.runtime.SideEffect {
        state.updateAnchors(anchors)
    }

    // Handle settled state
    LaunchedEffect(state.settledValue) {
        when (state.settledValue) {
            SwipeAction.ARCHIVE -> {
                onArchive()
                state.animateTo(SwipeAction.NONE)
            }
            SwipeAction.DELETE -> {
                onDelete()
                state.animateTo(SwipeAction.NONE)
            }
            SwipeAction.NONE -> { /* idle */ }
        }
    }

    val isDark = isAppInDarkTheme()

    Box(modifier = modifier.fillMaxWidth()) {
        // Background actions
        val offset = try { state.requireOffset() } catch (_: Exception) { 0f }
        val revealColor by animateColorAsState(
            targetValue = when {
                offset > 20f -> ElectricPink.copy(alpha = 0.16f)
                offset < -20f -> ElectricAmber.copy(alpha = 0.16f)
                else -> MaterialTheme.colorScheme.surface
            },
            animationSpec = tween(200),
            label = "revealColor"
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(20.dp))
                .background(revealColor)
        ) {
            // Delete icon (swipe right)
            if (offset > 20f) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete",
                    tint = ElectricPink,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 24.dp)
                        .size(22.dp)
                )
            }
            // Archive icon (swipe left)
            if (offset < -20f) {
                Icon(
                    imageVector = Icons.Outlined.Archive,
                    contentDescription = "Archive",
                    tint = ElectricAmber,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 24.dp)
                        .size(22.dp)
                )
            }
        }

        // Foreground content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offset.roundToInt(), 0) }
                .anchoredDraggable(state, Orientation.Horizontal)
        ) {
            content()
        }
    }
}
