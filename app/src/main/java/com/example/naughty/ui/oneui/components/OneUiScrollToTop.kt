package com.example.naughty.ui.oneui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.naughty.ui.oneui.theme.OneUi
import kotlinx.coroutines.launch

@Composable
fun OneUiScrollToTop(
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable {
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                }
            },
        shape = CircleShape,
        color = OneUi.colors.surfaceElevated,
        shadowElevation = 6.dp
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowUp,
                contentDescription = "Scroll to top",
                tint = OneUi.colors.textPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
