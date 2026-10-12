package com.example.naughty.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.ui.theme.DarkElevatedSurface
import com.example.naughty.ui.theme.ElectricAmber
import com.example.naughty.ui.theme.ElectricCyan
import com.example.naughty.ui.theme.ElectricGreen
import com.example.naughty.ui.theme.StealthCardBorder
import com.example.naughty.ui.theme.isAppInDarkTheme
import com.example.naughty.util.AudioShareMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareAudioOptionSheet(
    audioUris: List<Uri>,
    subject: String? = null,
    text: String? = null,
    isProcessing: Boolean = false,
    processingText: String = "",
    onSelectMode: (AudioShareMode) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDark = isAppInDarkTheme()

    val surfaceColor = if (isDark) DarkElevatedSurface else MaterialTheme.colorScheme.surface
    val cardColor = if (isDark) Color(0xFF14151B) else Color(0xFFF4F4F6)
    val borderColor = if (isDark) StealthCardBorder else Color(0xFFE4E4E7)
    val textPrimary = if (isDark) Color.White else Color(0xFF18181B)
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)

    ModalBottomSheet(
        onDismissRequest = {
            if (!isProcessing) onDismiss()
        },
        sheetState = sheetState,
        containerColor = surfaceColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF3F3F46) else Color(0xFFD4D4D8))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Save Shared Audio",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (audioUris.size == 1) "1 voice recording received" else "${audioUris.size} recordings received",
                        fontSize = 13.sp,
                        color = textSecondary
                    )
                }

                // Count Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF26262F) else Color(0xFFE4E4E7),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Audiotrack,
                            contentDescription = null,
                            tint = ElectricGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${audioUris.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Audio info preview banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = cardColor,
                border = BorderStroke(0.6.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ElectricGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Mic,
                            contentDescription = null,
                            tint = ElectricGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subject?.ifBlank { null } ?: "Incoming Voice Note",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary,
                            maxLines = 1
                        )
                        if (!text.isNullOrBlank()) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary,
                                maxLines = 1
                            )
                        } else {
                            Text(
                                text = "Ready to attach or transcribe",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Progress Overlay while transcribing
            AnimatedVisibility(
                visible = isProcessing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = ElectricGreen,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = processingText.ifBlank { "Processing audio..." },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimary
                    )
                    Text(
                        text = "Transcribing 100% on-device offline",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            }

            if (!isProcessing) {
                Text(
                    text = "CHOOSE NOTE FORMAT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 2.dp, bottom = 12.dp)
                )

                // Option 1: Attach Voice Note
                AudioOptionActionCard(
                    title = "Attach Voice Note",
                    subtitle = "Embed as interactive WhatsApp-style audio player with waveform",
                    icon = Icons.Outlined.Mic,
                    accentColor = ElectricGreen,
                    cardColor = cardColor,
                    borderColor = borderColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    isDark = isDark,
                    onClick = { onSelectMode(AudioShareMode.ATTACH_ONLY) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Option 2: Transcribe Speech
                AudioOptionActionCard(
                    title = "Transcribe Audio",
                    subtitle = "Transcribe speech offline and save as a written text note",
                    icon = Icons.Outlined.RecordVoiceOver,
                    accentColor = ElectricCyan,
                    cardColor = cardColor,
                    borderColor = borderColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    isDark = isDark,
                    onClick = { onSelectMode(AudioShareMode.TRANSCRIBE_ONLY) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Option 3: Both
                AudioOptionActionCard(
                    title = "Both (Audio & Transcript)",
                    subtitle = "Transcribe recognized speech and attach the voice note below it",
                    icon = Icons.Outlined.AutoAwesome,
                    accentColor = ElectricAmber,
                    cardColor = cardColor,
                    borderColor = borderColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    isDark = isDark,
                    onClick = { onSelectMode(AudioShareMode.BOTH) }
                )
            }
        }
    }
}

@Composable
private fun AudioOptionActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    cardColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        border = BorderStroke(0.6.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = if (isDark) 0.16f else 0.12f))
                    .border(
                        width = 1.dp,
                        color = accentColor.copy(alpha = if (isDark) 0.35f else 0.25f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = textSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
