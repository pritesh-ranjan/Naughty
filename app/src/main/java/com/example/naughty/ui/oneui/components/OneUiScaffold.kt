package com.example.naughty.ui.oneui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.ui.oneui.theme.OneUi

enum class OneUiHeaderStyle {
    Expanded,
    Compact
}

@Composable
fun OneUiScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    headerStyle: OneUiHeaderStyle = OneUiHeaderStyle.Compact,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomDock: (@Composable () -> Unit)? = null,
    floatingAction: (@Composable () -> Unit)? = null,
    listState: LazyListState = rememberLazyListState(),
    showScrollToTop: Boolean = true,
    content: LazyListScope.() -> Unit
) {
    val isScrolledPastHeader by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40
        }
    }

    val showScrollToTopButton by remember {
        derivedStateOf {
            showScrollToTop && (listState.firstVisibleItemIndex > 2 || listState.firstVisibleItemScrollOffset > 400)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OneUi.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            // Top App Bar Row (Fixed height ~56dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left slot: Back icon or spacer
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = OneUi.colors.textPrimary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(44.dp))
                }

                // Center slot: Compact title (visible if Compact style OR scrolled in Expanded style)
                val showCompactTitle = headerStyle == OneUiHeaderStyle.Compact || isScrolledPastHeader
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = showCompactTitle,
                        enter = fadeIn(tween(150)),
                        exit = fadeOut(tween(100))
                    ) {
                        Text(
                            text = title,
                            style = OneUi.type.barTitle,
                            color = OneUi.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Right slot: Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.heightIn(max = 44.dp)
                ) {
                    actions()
                }
            }

            // LazyColumn Content
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(OneUi.dimens.groupSpacing)
            ) {
                // If Expanded style, top viewing area header as first item
                if (headerStyle == OneUiHeaderStyle.Expanded) {
                    item(key = "oneui_expanded_header") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = title,
                                style = OneUi.type.largeTitle,
                                color = OneUi.colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            if (!subtitle.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = subtitle,
                                    style = OneUi.type.headerSubtitle,
                                    color = OneUi.colors.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Screen items
                content()

                // Bottom spacer to ensure scrolling clears the bottom dock / nav bar
                item(key = "oneui_bottom_spacer") {
                    Spacer(
                        modifier = Modifier
                            .height(if (bottomDock != null || floatingAction != null) 100.dp else 40.dp)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    )
                }
            }
        }

        // Scroll to top floating pill button
        AnimatedVisibility(
            visible = showScrollToTopButton,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = if (bottomDock != null) 76.dp else 24.dp)
        ) {
            OneUiScrollToTop(listState = listState)
        }

        // Floating Action Button slot (e.g. bottom-right FAB)
        if (floatingAction != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(end = 20.dp, bottom = if (bottomDock != null) 76.dp else 24.dp)
            ) {
                floatingAction()
            }
        }

        // Bottom dock slot (e.g. Search + Plus capsule)
        if (bottomDock != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                bottomDock()
            }
        }
    }
}
