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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.ui.components.paperBackground
import com.example.naughty.ui.theme.NoteTheme
import com.example.naughty.ui.theme.NoteThemeRegistry

import com.example.naughty.ui.theme.AmoledBlack
import com.example.naughty.ui.theme.CrystalWhite
import com.example.naughty.ui.theme.isAppInDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionSheet(
    currentThemeId: String,
    onSelectTheme: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = isAppInDarkTheme()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (isDark) AmoledBlack else CrystalWhite,
        contentColor = if (isDark) CrystalWhite else AmoledBlack,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Note Themes",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) CrystalWhite else AmoledBlack
                    )
                    Text(
                        text = "14 themes • Grid paper • Typography",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color(0xFF888888) else Color(0xFF666666)
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = if (isDark) Color(0xFF888888) else Color(0xFF666666)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Grid of 14 Themes
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = theme.surface,
        border = BorderStroke(
            if (isSelected) 2.dp else 0.6.dp,
            if (isSelected) theme.accent else theme.border
        )
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
                        text = "math: 42",
                        fontFamily = theme.fontFamily,
                        fontSize = 11.5.sp,
                        color = theme.accent,
                        fontWeight = FontWeight.Medium
                    )

                    // Accent chips
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(theme.accent)
                        )
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(theme.secondaryAccent)
                        )
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(theme.highlightColor)
                        )
                    }
                }
            }
        }
    }
}
