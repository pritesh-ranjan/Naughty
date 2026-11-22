package com.example.naughty.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.TimerOff
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.naughty.ui.oneui.components.OneUiBottomSheet
import com.example.naughty.ui.oneui.components.OneUiProgressBar
import com.example.naughty.ui.oneui.components.OneUiTonalButton
import com.example.naughty.ui.oneui.theme.OneUi
import com.example.naughty.util.VideoMetadata
import com.example.naughty.util.VideoProcessingState
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShareVideoOptionSheet(
    thumbnail: Bitmap?,
    metadata: VideoMetadata?,
    isInspecting: Boolean,
    processingState: VideoProcessingState,
    onStartSummarize: () -> Unit,
    onDismiss: () -> Unit
) {
    OneUiBottomSheet(
        title = "Summarize Shared Video",
        subtitle = "100% offline • Max 3 minutes",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Video preview & metadata card
            Surface(
                shape = OneUi.shapes.inner,
                color = OneUi.colors.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 88.dp, height = 66.dp)
                            .clip(OneUi.shapes.inner)
                            .background(OneUi.colors.surfaceElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        if (thumbnail != null && !thumbnail.isRecycled) {
                            Image(
                                bitmap = thumbnail.asImageBitmap(),
                                contentDescription = "Video thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Movie,
                                contentDescription = null,
                                tint = OneUi.colors.badge.purple,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        if (isInspecting) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = OneUi.colors.accent,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Analyzing FPS & resolution...",
                                    style = OneUi.typography.caption,
                                    color = OneUi.colors.textSecondary
                                )
                            }
                        } else if (metadata != null) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MetadataBadge(
                                    text = formatDuration(metadata.durationMs),
                                    color = if (metadata.isWithinDurationLimit) {
                                        OneUi.colors.badge.blue
                                    } else {
                                        OneUi.colors.destructive
                                    }
                                )
                                MetadataBadge(
                                    text = "${metadata.detectedFps.roundToInt()} FPS",
                                    color = OneUi.colors.badge.teal
                                )
                                val resLabel = if (metadata.originalWidth == metadata.reducedWidth &&
                                    metadata.originalHeight == metadata.reducedHeight
                                ) {
                                    "${metadata.reducedWidth}×${metadata.reducedHeight}"
                                } else {
                                    "${metadata.originalWidth}×${metadata.originalHeight} → ${metadata.reducedWidth}×${metadata.reducedHeight}"
                                }
                                MetadataBadge(
                                    text = resLabel,
                                    color = OneUi.colors.badge.purple
                                )
                                MetadataBadge(
                                    text = if (metadata.hasAudioTrack) "Audio + OCR" else "OCR only",
                                    color = OneUi.colors.badge.green
                                )
                            }
                        } else {
                            Text(
                                text = "Shared video ready",
                                style = OneUi.typography.body,
                                color = OneUi.colors.textPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isInspecting -> {
                    Surface(
                        shape = OneUi.shapes.inner,
                        color = OneUi.colors.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = OneUi.colors.accent,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Inspecting video metadata...",
                                style = OneUi.typography.body,
                                color = OneUi.colors.textPrimary
                            )
                        }
                    }
                }

                processingState is VideoProcessingState.Processing -> {
                    Surface(
                        shape = OneUi.shapes.inner,
                        color = OneUi.colors.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = processingState.stageLabel,
                                    style = OneUi.typography.body,
                                    fontWeight = FontWeight.Medium,
                                    color = OneUi.colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${processingState.progressPercent}%",
                                    style = OneUi.typography.caption,
                                    fontWeight = FontWeight.Bold,
                                    color = OneUi.colors.accent
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OneUiProgressBar(
                                progress = processingState.progressPercent / 100f
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OneUiTonalButton(
                                    text = "Continue in Background",
                                    onClick = onDismiss,
                                    containerColor = OneUi.colors.surfaceElevated
                                )
                            }
                        }
                    }
                }

                metadata == null || !metadata.isWithinDurationLimit -> {
                    val isTooLong = metadata != null && !metadata.isWithinDurationLimit
                    Surface(
                        shape = OneUi.shapes.inner,
                        color = OneUi.colors.destructive.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isTooLong) Icons.Outlined.TimerOff else Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = OneUi.colors.destructive,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isTooLong) "Video exceeds 3-minute limit" else "Unable to read video",
                                    style = OneUi.typography.body,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OneUi.colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isTooLong) {
                                        "Please share a video of 3 minutes or less for offline transcription & OCR."
                                    } else {
                                        "This video format or URI could not be opened on-device."
                                    },
                                    style = OneUi.typography.caption,
                                    color = OneUi.colors.textSecondary
                                )
                            }
                        }
                    }
                }

                processingState is VideoProcessingState.Error -> {
                    Surface(
                        shape = OneUi.shapes.inner,
                        color = OneUi.colors.destructive.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = OneUi.colors.destructive,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Processing failed",
                                    style = OneUi.typography.body,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OneUi.colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = processingState.message,
                                    style = OneUi.typography.caption,
                                    color = OneUi.colors.textSecondary
                                )
                            }
                        }
                    }
                }

                else -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(OneUi.shapes.inner)
                            .clickable(onClick = onStartSummarize),
                        shape = OneUi.shapes.inner,
                        color = OneUi.colors.surface
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
                                    .background(OneUi.colors.badge.green.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = OneUi.colors.badge.green,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Summarize Video to Crisp Note",
                                    style = OneUi.typography.body,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OneUi.colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Runs offline speech & frame OCR in the background into bullet points",
                                    style = OneUi.typography.caption,
                                    color = OneUi.colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataBadge(
    text: String,
    color: Color
) {
    Surface(
        shape = OneUi.shapes.pill,
        color = color.copy(alpha = 0.14f)
    ) {
        Text(
            text = text,
            style = OneUi.typography.caption,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val mins = totalSeconds / 60L
    val secs = totalSeconds % 60L
    return String.format(Locale.getDefault(), "%d:%02d", mins, secs)
}
