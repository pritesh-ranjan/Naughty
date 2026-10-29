package com.example.naughty.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.ui.components.paperBackground
import com.example.naughty.ui.oneui.components.OneUiBottomSheet
import com.example.naughty.ui.oneui.theme.OneUi
import com.example.naughty.ui.theme.NoteTheme
import com.example.naughty.ui.theme.NoteThemeRegistry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionSheet(
    currentThemeId: String,
    onSelectTheme: (String) -> Unit,
    onDismiss: () -> Unit
) {
    OneUiBottomSheet(
        title = "Note Themes",
        subtitle = "14 themes • Grid paper • Typography",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp)
            ) {
                items(NoteThemeRegistry.allThemes, key = { it.id }) { theme ->
                    val isSelected = theme.id.equals(currentThemeId, ignoreCase = true) ||
                            NoteThemeRegistry.getTheme(currentThemeId).id == theme.id

                    ThemePreviewCard(
                        theme = theme,
                        isSelected = isSelected,
                        onClick = {
                            onSelectTheme(theme.id)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(
    theme: NoteTheme,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val selectedBorder = if (isSelected) {
        BorderStroke(2.dp, OneUi.colors.accent)
    } else {
        BorderStroke(1.dp, OneUi.colors.divider)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OneUi.shapes.inner)
            .border(selectedBorder, OneUi.shapes.inner)
            .clickable(onClick = onClick),
        shape = OneUi.shapes.inner,
        color = theme.surface
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .paperBackground(
                    paperPattern = theme.paperPattern,
                    lineColor = theme.gridLineColor,
                    stepDp = 14.dp,
                    strokeWidthDp = 0.7.dp
                )
                .padding(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = theme.name,
                        fontFamily = theme.titleFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = theme.textPrimary
                    )

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(theme.accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Selected",
                                tint = if (theme.isDark) Color.Black else Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = theme.subtitle,
                    fontSize = 10.sp,
                    color = theme.textSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Typography & syntax accent preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Aa 123",
                        fontFamily = theme.fontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.textPrimary
                    )

                    // Color swatch dots
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(theme.accent)
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(theme.linkColor)
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(theme.cursorColor)
                        )
                    }
                }
            }
        }
    }
}
