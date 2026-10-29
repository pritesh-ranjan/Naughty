package com.example.naughty.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.naughty.ui.oneui.theme.OneUi
import com.example.naughty.ui.oneui.components.OneUiBottomSheet
import com.example.naughty.ui.oneui.components.OneUiConfirmDialog
import com.example.naughty.ui.oneui.components.OneUiSegmentedControl
import com.example.naughty.ui.theme.AmoledBlack
import com.example.naughty.ui.theme.CrystalWhite
import com.example.naughty.ui.theme.ElectricGreen
import com.example.naughty.ui.theme.isAppInDarkTheme
import com.example.naughty.util.AudioRecordResult
import com.example.naughty.util.AudioRecorderHelper
import com.example.naughty.util.SpeechTranscriptionHelper
import java.io.File
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRecordingSheet(
    initialTab: Int = 0, // 0 = Voice Note, 1 = Voice to Text
    accentColor: Color = ElectricGreen,
    onSaveVoiceNote: (audioFile: File, transcript: String?) -> Unit,
    onInsertText: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDark = isAppInDarkTheme()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedTabIndex by remember { mutableIntStateOf(initialTab) }

    // Permission check
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAudioPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
    }

    // Audio recorder helper
    val audioRecorder = remember { AudioRecorderHelper(context) }
    val isRecording by audioRecorder.isRecording.collectAsState()
    val elapsedMillis by audioRecorder.elapsedMillis.collectAsState()
    val liveAmplitudes by audioRecorder.liveAmplitudes.collectAsState()
    var isPaused by remember { mutableStateOf(false) }
    var recordedResult by remember { mutableStateOf<AudioRecordResult?>(null) }
    var voiceNoteTranscript by remember { mutableStateOf<String?>(null) }

    // Speech transcription helper
    val transcriptionHelper = remember { SpeechTranscriptionHelper(context) }
    val isListening by transcriptionHelper.isListening.collectAsState()
    val partialTranscription by transcriptionHelper.partialText.collectAsState()
    val soundLevel by transcriptionHelper.soundLevel.collectAsState()
    val errorMessage by transcriptionHelper.errorMessage.collectAsState()
    val errorCode by transcriptionHelper.errorCode.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) {
                audioRecorder.cancelRecording()
            }
            transcriptionHelper.destroy()
        }
    }

    OneUiBottomSheet(
        title = "Voice Actions",
        subtitle = "Voice notes & offline dictation",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            OneUiSegmentedControl(
                items = listOf("🎙️ Voice Note", "✨ Voice to Text"),
                selectedIndex = selectedTabIndex,
                onSelect = {
                    if (!isRecording) {
                        selectedTabIndex = it
                        if (it == 0) transcriptionHelper.stopListening()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (!hasAudioPermission) {
                // Permission Request Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Microphone Permission Needed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "To record voice notes or dictate text offline, Naughty needs microphone access.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Allow Microphone",
                                color = if (isDark) AmoledBlack else CrystalWhite,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        TextButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        ) {
                            Text(
                                text = "App Settings",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (selectedTabIndex == 0) {
                // TAB 0: Voice Note Recording Mode
                VoiceNoteRecordingSection(
                    audioRecorder = audioRecorder,
                    isRecording = isRecording,
                    isPaused = isPaused,
                    elapsedMillis = elapsedMillis,
                    liveAmplitudes = liveAmplitudes,
                    accentColor = accentColor,
                    recordedResult = recordedResult,
                    voiceNoteTranscript = voiceNoteTranscript,
                    onStart = {
                        recordedResult = null
                        voiceNoteTranscript = null
                        isPaused = false
                        audioRecorder.startRecording()
                        transcriptionHelper.startListening(
                            onPartial = { text -> voiceNoteTranscript = text },
                            onFinal = { text -> voiceNoteTranscript = text },
                            onError = { _ -> }
                        )
                    },
                    onPause = {
                        isPaused = true
                        audioRecorder.pauseRecording()
                        transcriptionHelper.stopListening()
                    },
                    onResume = {
                        isPaused = false
                        audioRecorder.resumeRecording()
                        val previousText = voiceNoteTranscript.orEmpty()
                        transcriptionHelper.startListening(
                            onPartial = { text ->
                                voiceNoteTranscript = if (previousText.isNotBlank()) "$previousText $text" else text
                            },
                            onFinal = { text ->
                                voiceNoteTranscript = if (previousText.isNotBlank()) "$previousText $text" else text
                            },
                            onError = { _ -> }
                        )
                    },
                    onFinish = {
                        val result = audioRecorder.stopRecording()
                        transcriptionHelper.stopListening()
                        if (result != null) {
                            if (!voiceNoteTranscript.isNullOrBlank()) {
                                AudioRecorderHelper.saveTranscript(result.file, voiceNoteTranscript!!)
                            } else {
                                val bgHelper = SpeechTranscriptionHelper(context)
                                bgHelper.transcribeAudioFile(
                                    audioFile = result.file,
                                    onFinal = { text ->
                                        voiceNoteTranscript = text
                                        AudioRecorderHelper.saveTranscript(result.file, text)
                                        bgHelper.destroy()
                                    },
                                    onError = { _ -> bgHelper.destroy() }
                                )
                            }
                        }
                        recordedResult = result
                    },
                    onDiscard = {
                        audioRecorder.cancelRecording()
                        transcriptionHelper.stopListening()
                        recordedResult = null
                        voiceNoteTranscript = null
                    },
                    onSave = {
                        recordedResult?.let { res ->
                            val finalTranscript = voiceNoteTranscript ?: AudioRecorderHelper.readTranscript(res.file)
                            onSaveVoiceNote(res.file, finalTranscript)
                            onDismiss()
                        }
                    }
                )
            } else {
                // TAB 1: Offline Voice to Text Mode
                VoiceToTextSection(
                    transcriptionHelper = transcriptionHelper,
                    isListening = isListening,
                    partialText = partialTranscription,
                    soundLevel = soundLevel,
                    errorMessage = errorMessage,
                    errorCode = errorCode,
                    accentColor = accentColor,
                    onInsert = { text ->
                        onInsertText(text)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun VoiceNoteRecordingSection(
    audioRecorder: AudioRecorderHelper,
    isRecording: Boolean,
    isPaused: Boolean,
    elapsedMillis: Long,
    liveAmplitudes: List<Float>,
    accentColor: Color,
    recordedResult: AudioRecordResult?,
    voiceNoteTranscript: String?,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    onSave: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val isDenoiseEnabled by audioRecorder.isNoiseSuppressionEnabled.collectAsState()
    var showDiscardConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (recordedResult != null) {
            // Preview recorded voice note before attaching
            Text(
                text = "Voice Note Ready",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            VoiceNotePlayer(
                audioUriString = "file://${recordedResult.file.absolutePath}",
                accentColor = accentColor,
                onDelete = onDiscard,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showDiscardConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Discard",
                        color = if (isDark) CrystalWhite else AmoledBlack
                    )
                }

                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1.5f)
                ) {
                    Text(
                        text = "Attach Voice Note",
                        color = if (isDark) AmoledBlack else CrystalWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else if (!isRecording) {
            // Initial Idle state
            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .clickable { onStart() },
                shape = CircleShape,
                color = accentColor,
                shadowElevation = 6.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Start Recording",
                        tint = if (isDark) AmoledBlack else CrystalWhite,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tap to record voice note",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Studio-quality audio with live waveform",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // AI Noise Cancellation Toggle
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        audioRecorder.setNoiseSuppressionEnabled(!isDenoiseEnabled)
                    },
                shape = RoundedCornerShape(18.dp),
                color = if (isDenoiseEnabled) {
                    accentColor.copy(alpha = if (isDark) 0.16f else 0.12f)
                } else {
                    if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7)
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = if (isDenoiseEnabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDenoiseEnabled) "AI Denoise: ON" else "AI Denoise: OFF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDenoiseEnabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        } else {
            // Active Recording State
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseAlpha"
            )

            // AI Noise Cancellation Real-Time Toggle Pill
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                        audioRecorder.setNoiseSuppressionEnabled(!isDenoiseEnabled)
                    },
                shape = RoundedCornerShape(18.dp),
                color = if (isDenoiseEnabled) {
                    accentColor.copy(alpha = if (isDark) 0.16f else 0.12f)
                } else {
                    if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7)
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = if (isDenoiseEnabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDenoiseEnabled) "AI Denoise: ON" else "AI Denoise: OFF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDenoiseEnabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = if (isPaused) 0.4f else pulseAlpha))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatTimer(elapsedMillis),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPaused) Color.Gray else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Real-time animated waveform canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0xFF18181B) else Color(0xFFF4F4F5))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val count = 38
                    val samples = liveAmplitudes.takeLast(count)
                    val totalWidth = size.width
                    val totalHeight = size.height
                    val spacing = 3.dp.toPx()
                    val barWidth = ((totalWidth - (spacing * (count - 1))) / count).coerceAtLeast(2.5.dp.toPx())

                    for (i in 0 until count) {
                        val barX = i * (barWidth + spacing)
                        val sampleIdx = samples.size - count + i
                        val amplitude = if (sampleIdx >= 0 && sampleIdx < samples.size) samples[sampleIdx] else 0.12f
                        val scaledAmp = kotlin.math.sqrt(amplitude.coerceIn(0f, 1f))
                        val barHeight = (totalHeight * scaledAmp * 0.92f).coerceIn(6.dp.toPx(), totalHeight)
                        val barY = (totalHeight - barHeight) / 2f

                        drawRoundRect(
                            color = if (isPaused) (if (isDark) Color(0xFF52525B) else Color(0xFFA1A1AA)) else accentColor,
                            topLeft = Offset(barX, barY),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recording Controls: Discard, Pause/Resume, Stop
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Discard
                IconButton(
                    onClick = {
                        if (elapsedMillis > 2000L) {
                            showDiscardConfirmDialog = true
                        } else {
                            onDiscard()
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Discard",
                        tint = Color(0xFFEF4444)
                    )
                }

                // Pause / Resume
                IconButton(
                    onClick = {
                        if (isPaused) onResume() else onPause()
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7))
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Stop & Finish
                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .clickable { onFinish() },
                    shape = CircleShape,
                    color = accentColor
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = "Stop",
                            tint = if (isDark) AmoledBlack else CrystalWhite,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDiscardConfirmDialog) {
        OneUiConfirmDialog(
            title = "Discard Recording?",
            message = "Are you sure you want to discard this voice recording?",
            confirmLabel = "Discard",
            destructive = true,
            onConfirm = {
                showDiscardConfirmDialog = false
                onDiscard()
            },
            onDismiss = { showDiscardConfirmDialog = false }
        )
    }
}

@Composable
private fun VoiceToTextSection(
    transcriptionHelper: SpeechTranscriptionHelper,
    isListening: Boolean,
    partialText: String,
    soundLevel: Float,
    errorMessage: String?,
    errorCode: Int?,
    accentColor: Color,
    onInsert: (String) -> Unit
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()
    val isLanguageUnavailable = errorCode == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ||
            errorCode == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val isActivelyListening = isListening && errorMessage == null

        // Microphone sound-wave ripple button
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isActivelyListening) {
                Box(
                    modifier = Modifier
                        .size((70 + soundLevel * 30).dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.25f))
                )
            }

            Surface(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (isActivelyListening) {
                            transcriptionHelper.stopListening()
                        } else {
                            transcriptionHelper.startListening()
                        }
                    },
                shape = CircleShape,
                color = if (isActivelyListening) accentColor else if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = if (isActivelyListening) "Stop listening" else "Start listening",
                        tint = if (isActivelyListening) (if (isDark) AmoledBlack else CrystalWhite) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = when {
                isActivelyListening -> "Listening (100% Offline)..."
                isLanguageUnavailable -> "Offline model required"
                !errorMessage.isNullOrBlank() -> "Recognition stopped"
                else -> "Tap mic to start speech dictation"
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isActivelyListening) accentColor else MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Live transcription text card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .clip(RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) Color(0xFF141418) else Color(0xFFF4F4F6)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                if (partialText.isNotBlank()) {
                    Text(
                        text = partialText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else if (isLanguageUnavailable) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Offline Speech Model Missing",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                        }

                        Text(
                            text = "Naughty transcribes 100% offline. The Google Speech on-device language pack is not installed on this device (standard on emulators).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { transcriptionHelper.triggerModelDownload() },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isDark) AmoledBlack else CrystalWhite
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Download",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) AmoledBlack else CrystalWhite
                                )
                            }

                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Settings",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                } else if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFEF4444)
                    )
                } else {
                    Text(
                        text = "Spoken words will appear here in real time via on-device SpeechRecognizer...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (isListening) transcriptionHelper.stopListening()
                    transcriptionHelper.startListening()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF27272A) else Color(0xFFE4E4E7)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Clear / Restart",
                    color = if (isDark) CrystalWhite else AmoledBlack
                )
            }

            Button(
                onClick = {
                    if (partialText.isNotBlank()) {
                        onInsert(partialText)
                    }
                },
                enabled = partialText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1.5f)
            ) {
                Text(
                    text = "Insert into Note",
                    color = if (isDark) AmoledBlack else CrystalWhite,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatTimer(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
