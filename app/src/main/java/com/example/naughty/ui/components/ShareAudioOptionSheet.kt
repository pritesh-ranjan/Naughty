package com.example.naughty.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.ui.oneui.components.OneUiBottomSheet
import com.example.naughty.ui.oneui.theme.OneUi
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
    OneUiBottomSheet(
        title = "Save Shared Audio",
        subtitle = if (audioUris.size == 1) "1 audio recording received" else "${audioUris.size} audio recordings received",
        onDismiss = { if (!isProcessing) onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Audio info card
            Surface(
                shape = OneUi.shapes.inner,
                color = OneUi.colors.surfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(OneUi.badgePalette.purple.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Audiotrack,
                            contentDescription = null,
                            tint = OneUi.badgePalette.purple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = subject?.ifBlank { null } ?: "Audio voice note",
                            style = OneUi.typography.body,
                            fontWeight = FontWeight.SemiBold,
                            color = OneUi.colors.textPrimary
                        )
                        if (!text.isNullOrBlank()) {
                            Text(
                                text = text,
                                style = OneUi.typography.caption,
                                color = OneUi.colors.textSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Processing state
            if (isProcessing) {
                Surface(
                    shape = OneUi.shapes.inner,
                    color = OneUi.colors.surfaceElevated,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = OneUi.colors.accent,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = processingText.ifBlank { "Processing audio..." },
                            style = OneUi.typography.body,
                            fontWeight = FontWeight.Medium,
                            color = OneUi.colors.textPrimary
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !isProcessing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AudioShareOptionCard(
                        icon = Icons.Outlined.Mic,
                        accentColor = OneUi.badgePalette.blue,
                        title = "Attach Voice Note",
                        description = "Save with playable audio waveform player",
                        onClick = { onSelectMode(AudioShareMode.ATTACH_ONLY) }
                    )

                    AudioShareOptionCard(
                        icon = Icons.Outlined.RecordVoiceOver,
                        accentColor = OneUi.badgePalette.orange,
                        title = "Transcribe to Text Note",
                        description = "Transcribe speech on-device into text",
                        onClick = { onSelectMode(AudioShareMode.TRANSCRIBE_ONLY) }
                    )

                    AudioShareOptionCard(
                        icon = Icons.Outlined.AutoAwesome,
                        accentColor = OneUi.badgePalette.green,
                        title = "Both (Audio & Transcript)",
                        description = "Attach audio waveform and append transcribed text",
                        onClick = { onSelectMode(AudioShareMode.BOTH) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioShareOptionCard(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(OneUi.shapes.inner)
            .clickable(onClick = onClick),
        shape = OneUi.shapes.inner,
        color = OneUi.colors.surfaceElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = OneUi.typography.body,
                    fontWeight = FontWeight.SemiBold,
                    color = OneUi.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = OneUi.typography.caption,
                    color = OneUi.colors.textSecondary
                )
            }
        }
    }
}
