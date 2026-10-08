package com.example.naughty.ui.components

import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
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
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PostAdd
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.ui.theme.AmoledBlack
import com.example.naughty.ui.theme.CrystalWhite
import com.example.naughty.ui.theme.ElectricCyan
import com.example.naughty.ui.theme.ElectricGreen
import com.example.naughty.ui.theme.isAppInDarkTheme
import com.example.naughty.util.AudioRecorderHelper
import com.example.naughty.util.SpeechTranscriptionHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

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
            // Main WhatsApp-style voice note row
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

                Spacer(modifier = Modifier.width(12.dp))

                // WhatsApp Waveform Canvas with interactive scrubbing
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .pointerInput(durationMillis) {
                                detectTapGestures { offset ->
                                    val fraction = offset.x / size.width
                                    seekToFraction(fraction)
                                }
                            }
                            .pointerInput(durationMillis) {
                                detectDragGestures { change, _ ->
                                    val fraction = change.position.x / size.width
                                    seekToFraction(fraction)
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val barCount = amplitudes.size
                            if (barCount == 0) return@Canvas

                            val totalWidth = size.width
                            val totalHeight = size.height
                            val spacing = 2.5.dp.toPx()
                            val barWidth = ((totalWidth - (spacing * (barCount - 1))) / barCount).coerceAtLeast(2.dp.toPx())
                            val currentPlayheadX = progressFraction * totalWidth

                            for (i in 0 until barCount) {
                                val barX = i * (barWidth + spacing)
                                val amplitude = amplitudes[i]
                                val barHeight = (totalHeight * amplitude * 0.85f).coerceAtLeast(4.dp.toPx())
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

                            // WhatsApp scrub playhead thumb
                            drawCircle(
                                color = playedColor,
                                radius = 4.5.dp.toPx(),
                                center = Offset(currentPlayheadX, totalHeight / 2f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Time display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val displayMillis = if (isPlaying) currentPositionMillis else durationMillis
                        Text(
                            text = formatDuration(displayMillis),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFFA1A1AA) else Color(0xFF71717A)
                        )

                        Text(
                            text = "Voice note",
                            fontSize = 10.sp,
                            color = if (isDark) Color(0xFF52525B) else Color(0xFFA1A1AA)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Playback speed selector pill (1x, 1.5x, 2x)
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { cycleSpeed() },
                    shape = RoundedCornerShape(8.dp),
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
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Transcribe button
                IconButton(
                    onClick = {
                        if (transcriptText != null) {
                            showTranscript = !showTranscript
                        } else {
                            // Run offline speech transcription
                            isTranscribing = true
                            val transcriptionHelper = SpeechTranscriptionHelper(context)
                            transcriptionHelper.startListening(
                                onFinal = { result ->
                                    isTranscribing = false
                                    transcriptText = result
                                    showTranscript = true
                                    AudioRecorderHelper.saveTranscript(audioFile, result)
                                    transcriptionHelper.destroy()
                                },
                                onError = {
                                    isTranscribing = false
                                    transcriptionHelper.destroy()
                                }
                            )
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    if (isTranscribing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = playedColor
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.RecordVoiceOver,
                            contentDescription = "Transcribe",
                            tint = if (transcriptText != null) playedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete voice note",
                        tint = if (isDark) Color(0xFF71717A) else Color(0xFFA1A1AA),
                        modifier = Modifier.size(18.dp)
                    )
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
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
