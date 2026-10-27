package com.example.naughty.ui.notelist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.naughty.ui.theme.ElectricAmber
import com.example.naughty.ui.theme.ElectricCyan
import com.example.naughty.ui.theme.isAppInDarkTheme

@Composable
fun HomeFeatureTiles(
    trackerCount: Int,
    onOpenTimeline: () -> Unit,
    ctsClipCount: Int = 0,
    hasPendingCtsClip: Boolean = false,
    pendingClipSourceApp: String = "",
    onCtsTileClick: () -> Unit = {},
    onOpenCtsDirectly: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()

    // Timeline colors
    val timelineAccent = if (isDark) ElectricCyan else Color(0xFF0083A0)
    val timelineBg = if (isDark) Color(0xFF08151D) else Color(0xFFEAF5FA)

    // Circle to Search colors (switches to Amber when a new clip is waiting to be added)
    val ctsAccent = if (hasPendingCtsClip) ElectricAmber else (if (isDark) ElectricCyan else Color(0xFF0083A0))
    val ctsBg = if (hasPendingCtsClip) {
        if (isDark) Color(0xFF1F1707) else Color(0xFFFEF8ED)
    } else {
        if (isDark) Color(0xFF08151D) else Color(0xFFEAF5FA)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Circle to Search Feature Tile
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onCtsTileClick),
            shape = RoundedCornerShape(18.dp),
            color = ctsBg
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ctsAccent.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = ctsAccent,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Circle to Search",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                lineHeight = 16.sp
                            )
                            if (hasPendingCtsClip) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ElectricAmber.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "NEW CLIP",
                                        color = ElectricAmber,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (hasPendingCtsClip) {
                                "Clip ready from ${pendingClipSourceApp.ifBlank { "app" }} • Tap to add"
                            } else if (ctsClipCount > 0) {
                                "$ctsClipCount ${if (ctsClipCount == 1) "clip" else "clips"} captured • Tap to view"
                            } else {
                                "Capture text from screen searches • Tap to view"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasPendingCtsClip) ElectricAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }

                IconButton(
                    onClick = onOpenCtsDirectly,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                        contentDescription = "Open Circle to Search Note",
                        tint = ctsAccent.copy(alpha = 0.85f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // Timeline Tasks Feature Tile
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onOpenTimeline),
            shape = RoundedCornerShape(18.dp),
            color = timelineBg
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = timelineAccent.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Timeline,
                                contentDescription = null,
                                tint = timelineAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Timeline",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (trackerCount > 0) {
                                "$trackerCount ${if (trackerCount == 1) "task active" else "tasks active"} • Track workflows"
                            } else {
                                "Track multi-step workflows & milestones"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                    contentDescription = null,
                    tint = timelineAccent.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
