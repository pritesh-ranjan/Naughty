package com.example.naughty.ui.components

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.ui.theme.AmoledBlack
import com.example.naughty.ui.theme.CrystalWhite
import com.example.naughty.ui.theme.ElectricGreen
import com.example.naughty.ui.theme.isAppInDarkTheme
import com.example.naughty.util.AudioRecorderHelper
import com.example.naughty.util.SpeechTranscriptionHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun VoiceNotePlayer(
    audioUriString: String,
    accentColor: Color = ElectricGreen,
    onDelete: () -> Unit,
    onInsertTranscript: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val cleanPath = remember(audioUriString) {
        if (audioUriString.startsWith("file://")) audioUriString.removePrefix("file://") else audioUriString
    }
    val audioFile = remember(cleanPath) { File(cleanPath) }

    val amplitudes = remember(cleanPath) {
        AudioRecorderHelper.readWaveform(audioFile, targetCount = 38)
    }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMillis by remember { mutableLongStateOf(0L) }
    var durationMillis by remember { mutableLongStateOf(0L) }
    var playbackSpeedIndex by remember { mutableIntStateOf(0) } // 0 -> 1.0x, 1 -> 1.5x, 2 -> 2.0x
    val speedOptions = remember { listOf(1.0f, 1.5f, 2.0f) }

    var isTranscribing by remember { mutableStateOf(false) }
    var transcriptText by remember { mutableStateOf(AudioRecorderHelper.readTranscript(audioFile)) }
    var showTranscript by remember { mutableStateOf(transcriptText != null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Initialize MediaPlayer to query duration
    LaunchedEffect(cleanPath) {
        if (audioFile.exists()) {
            try {
                val mp = MediaPlayer().apply {
                    setDataSource(context, Uri.fromFile(audioFile))
                    prepare()
                }
                durationMillis = mp.duration.toLong().coerceAtLeast(1000L)
                mp.release()
            } catch (_: Exception) {}
        }
    }

    // Progress update loop when playing
    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            val mp = mediaPlayer
            if (mp != null && mp.isPlaying) {
                currentPositionMillis = mp.currentPosition.toLong()
            }
            delay(50)
        }
    }

    DisposableEffect(cleanPath) {
        onDispose {
            mediaPlayer?.run {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
        }
    }

    fun togglePlayPause() {
        if (!audioFile.exists()) return

        val currentMp = mediaPlayer
        if (currentMp != null && isPlaying) {
            currentMp.pause()
            isPlaying = false
        } else if (currentMp != null) {
            try {
                currentMp.start()
                isPlaying = true
            } catch (_: Exception) {
                currentMp.release()
                mediaPlayer = null
            }
        } else {
            try {
                val newMp = MediaPlayer().apply {
                    setDataSource(context, Uri.fromFile(audioFile))
                    prepare()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        playbackParams = playbackParams.setSpeed(speedOptions[playbackSpeedIndex])
                    }
                    setOnCompletionListener {
                        isPlaying = false
                        currentPositionMillis = 0L
                    }
                    start()
                }
                durationMillis = newMp.duration.toLong().coerceAtLeast(1000L)
                mediaPlayer = newMp
                isPlaying = true
            } catch (_: Exception) {}
        }
    }

    fun seekToFraction(fraction: Float) {
        val targetMs = (fraction.coerceIn(0f, 1f) * durationMillis).toLong()
        currentPositionMillis = targetMs
        mediaPlayer?.let { mp ->
            try {
                mp.seekTo(targetMs.toInt())
            } catch (_: Exception) {}
        }
    }

    fun cycleSpeed() {
        val nextIdx = (playbackSpeedIndex + 1) % speedOptions.size
        playbackSpeedIndex = nextIdx
        val newSpeed = speedOptions[nextIdx]
        mediaPlayer?.let { mp ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    mp.playbackParams = mp.playbackParams.setSpeed(newSpeed)
                } catch (_: Exception) {}
            }
        }
    }

    val progressFraction = if (durationMillis > 0) {
        (currentPositionMillis.toFloat() / durationMillis.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val cardBg = if (isDark) Color(0xFF141418) else Color(0xFFF4F4F6)
    val cardBorder = if (isDark) Color(0xFF26262E) else Color(0xFFE4E4E8)
    val playedColor = accentColor
    val unplayedColor = if (isDark) Color(0xFF3F3F46) else Color(0xFFD4D4D8)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(0.6.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // Row 1: Audio Playback Controls (Play/Pause Button & Full-width Waveform Canvas)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause Button
                Surface(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable { togglePlayPause() },
                    shape = CircleShape,
                    color = playedColor,
                    shadowElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = if (isDark) AmoledBlack else CrystalWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // WhatsApp Waveform Canvas with interactive scrubbing
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clipToBounds()
                        .pointerInput(durationMillis) {
                            detectTapGestures { offset ->
                                val thumbRadius = 4.5.dp.toPx()
                                val usableWidth = (size.width - thumbRadius * 2).coerceAtLeast(1f)
                                val fraction = ((offset.x - thumbRadius) / usableWidth).coerceIn(0f, 1f)
                                seekToFraction(fraction)
                            }
                        }
                        .pointerInput(durationMillis) {
                            detectDragGestures { change, _ ->
                                val thumbRadius = 4.5.dp.toPx()
                                val usableWidth = (size.width - thumbRadius * 2).coerceAtLeast(1f)
                                val fraction = ((change.position.x - thumbRadius) / usableWidth).coerceIn(0f, 1f)
                                seekToFraction(fraction)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
                        val barCount = amplitudes.size
                        if (barCount == 0) return@Canvas

                        val totalWidth = size.width
                        val totalHeight = size.height
                        val spacing = 2.dp.toPx()
                        val thumbRadius = 4.5.dp.toPx()

                        // Usable width for bars ensuring space for thumb at start and end
                        val usableWidth = (totalWidth - thumbRadius * 2).coerceAtLeast(10f)

                        // Calculate dynamic bar count so bars are evenly distributed across usableWidth
                        val targetBarWidth = 2.5.dp.toPx()
                        val optimalBarCount = ((usableWidth + spacing) / (targetBarWidth + spacing)).toInt().coerceIn(16, 50)
                        val barWidth = ((usableWidth - (spacing * (optimalBarCount - 1))) / optimalBarCount).coerceAtLeast(1.5.dp.toPx())
                        val currentPlayheadX = thumbRadius + progressFraction * usableWidth

                        for (i in 0 until optimalBarCount) {
                            val barX = thumbRadius + i * (barWidth + spacing)
                            val ampIndex = ((i.toFloat() / optimalBarCount) * amplitudes.size).toInt().coerceIn(0, amplitudes.size - 1)
                            val amplitude = amplitudes[ampIndex]
                            val scaledAmp = kotlin.math.sqrt(amplitude.coerceIn(0f, 1f))
                            val barHeight = (totalHeight * scaledAmp * 0.88f).coerceIn(4.dp.toPx(), totalHeight)
                            val barY = (totalHeight - barHeight) / 2f

                            val isPlayed = (barX + barWidth / 2f) <= currentPlayheadX
                            val barColor = if (isPlayed) playedColor else unplayedColor

                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(barX, barY),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                        }

                        // Scrub playhead thumb
                        drawCircle(
                            color = playedColor,
                            radius = thumbRadius,
                            center = Offset(currentPlayheadX, totalHeight / 2f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Metadata & Action Icons (Speed Pill, Share, Transcribe, Delete)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 52.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Duration & label
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val displayMillis = if (isPlaying) currentPositionMillis else durationMillis
                    Text(
                        text = if (isPlaying && durationMillis > 0) {
                            "${formatDuration(currentPositionMillis)} / ${formatDuration(durationMillis)}"
                        } else {
                            formatDuration(displayMillis)
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A),
                        maxLines = 1
                    )

                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF52525B) else Color(0xFFA1A1AA)
                    )

                    Text(
                        text = "Voice note",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF71717A) else Color(0xFF8E8E93),
                        maxLines = 1
                    )
                }

                // Action buttons: Speed Pill, Share, Transcribe, Delete
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Playback speed selector pill (1x, 1.5x, 2x)
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { cycleSpeed() },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7)
                        ) {
                            Text(
                                text = when (playbackSpeedIndex) {
                                    1 -> "1.5x"
                                    2 -> "2x"
                                    else -> "1x"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) CrystalWhite else AmoledBlack,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp)
                            )
                        }

                        // Share button
                        IconButton(
                            onClick = { shareVoiceNote(context, audioUriString) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Share voice note",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Transcribe button
                        IconButton(
                            onClick = {
                                if (transcriptText != null) {
                                    showTranscript = !showTranscript
                                } else {
                                    isTranscribing = true
                                    val transcriptionHelper = SpeechTranscriptionHelper(context)
                                    transcriptionHelper.transcribeAudioFile(
                                        audioFile = audioFile,
                                        onFinal = { result ->
                                            isTranscribing = false
                                            transcriptText = result
                                            showTranscript = true
                                            AudioRecorderHelper.saveTranscript(audioFile, result)
                                            transcriptionHelper.destroy()
                                        },
                                        onError = { error ->
                                            isTranscribing = false
                                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                            transcriptionHelper.destroy()
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            if (isTranscribing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = playedColor
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.RecordVoiceOver,
                                    contentDescription = "Transcribe",
                                    tint = if (transcriptText != null) playedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Delete button
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Delete voice note",
                                tint = if (isDark) Color(0xFF71717A) else Color(0xFFA1A1AA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Expandable Transcription Section
            AnimatedVisibility(
                visible = showTranscript && !transcriptText.isNullOrBlank(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                transcriptText?.let { text ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF1E1E24) else Color(0xFFEBEBF0))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Transcript (Offline)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = playedColor
                            )

                            Row {
                                // Copy action
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(text))
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = "Copy transcript",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                if (onInsertTranscript != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    // Insert into note action
                                    IconButton(
                                        onClick = { onInsertTranscript(text) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.PostAdd,
                                            contentDescription = "Insert into note",
                                            tint = playedColor,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete Voice Note?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text("Are you sure you want to delete this voice note? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        try {
                            mediaPlayer?.run {
                                if (isPlaying) stop()
                                release()
                            }
                        } catch (_: Exception) {}
                        mediaPlayer = null
                        isPlaying = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

/**
 * Resolves local audio reference into a content URI via FileProvider and launches Android share sheet.
 */
fun shareVoiceNote(context: Context, audioUriString: String) {
    try {
        val cleanPath = if (audioUriString.startsWith("file://")) audioUriString.removePrefix("file://") else audioUriString
        val file = java.io.File(cleanPath)
        if (!file.exists()) {
            Toast.makeText(context, "Audio file not found", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("Voice Note", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Voice Note").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to share audio: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
