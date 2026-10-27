package com.example.naughty.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.ui.theme.AmoledBlack
import com.example.naughty.ui.theme.CrystalWhite
import com.example.naughty.ui.theme.ElectricAmber
import com.example.naughty.ui.theme.ElectricCyan
import com.example.naughty.ui.theme.ElectricGreen
import com.example.naughty.ui.theme.ElectricPink
import com.example.naughty.ui.theme.ElectricPurple
import com.example.naughty.ui.theme.isAppInDarkTheme

data class FeatureShowcaseItem(
    val title: String,
    val category: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

fun getFeatureShowcaseItems(): List<FeatureShowcaseItem> = listOf(
    FeatureShowcaseItem(
        title = "App-Linked Notes",
        category = "Smart Context",
        description = "Notes appear automatically as alerts when you open specific apps — like grocery checklists in Instacart, or meeting notes in Zoom.",
        icon = Icons.Outlined.Apps,
        accentColor = ElectricCyan
    ),
    FeatureShowcaseItem(
        title = "Circle to Search Capture",
        category = "Instant Save",
        description = "Look up anything with Circle to Search or Google Lens, and Naughty promptly offers to save the result as a note the moment you exit.",
        icon = Icons.Outlined.Search,
        accentColor = ElectricAmber
    ),
    FeatureShowcaseItem(
        title = "Timeline Tasks",
        category = "Project Tracking",
        description = "Track multi-step goals with customizable stages, ordered milestones, due dates, and real-time visual progress bars.",
        icon = Icons.Outlined.Timeline,
        accentColor = ElectricGreen
    ),
    FeatureShowcaseItem(
        title = "Photo & Audio Voice Notes",
        category = "Rich Media",
        description = "Attach photos and record studio-clarity voice memos with interactive waveform scrubbing and AI neural background noise cancellation.",
        icon = Icons.Outlined.GraphicEq,
        accentColor = ElectricPink
    ),
    FeatureShowcaseItem(
        title = "Offline Text Scanner (OCR)",
        category = "On-Device AI",
        description = "Extract text from photos and documents instantly on your phone — both inside note drafts and directly when sharing images to Naughty.",
        icon = Icons.Outlined.DocumentScanner,
        accentColor = Color(0xFF3B82F6)
    ),
    FeatureShowcaseItem(
        title = "Offline Speech-to-Text",
        category = "Private Voice",
        description = "Dictate live thoughts or transcribe audio files into clean text without an internet connection or cloud delay.",
        icon = Icons.Outlined.RecordVoiceOver,
        accentColor = ElectricPurple
    ),
    FeatureShowcaseItem(
        title = "@ Note Mentions & Links",
        category = "Connected Notes",
        description = "Type @ while writing to link related notes, connect thoughts, or reference contacts with instant one-tap navigation.",
        icon = Icons.Outlined.AlternateEmail,
        accentColor = Color(0xFFF97316)
    ),
    FeatureShowcaseItem(
        title = "100% Offline & Private",
        category = "Zero Tracking",
        description = "Zero accounts, zero cloud servers, and zero telemetry. Your notes stay strictly on your phone in standard Markdown.",
        icon = Icons.Outlined.Shield,
        accentColor = Color(0xFF14B8A6)
    ),
    FeatureShowcaseItem(
        title = "Biometric Lock & Fast Search",
        category = "Security",
        description = "Lock confidential notes behind fingerprint or face authentication. Pull down from anywhere to search notes and milestones instantly.",
        icon = Icons.Outlined.Lock,
        accentColor = Color(0xFF059669)
    )
)

/**
 * Pure helper function determining whether the showcase popup should be presented.
 * Triggers on first ever install (lastSeenVersionCode == null) and after every update (lastSeenVersionCode < currentVersionCode).
 */
fun shouldShowFeatureShowcase(lastSeenVersionCode: Long?, currentVersionCode: Long): Boolean {
    return lastSeenVersionCode == null || lastSeenVersionCode < currentVersionCode
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureShowcaseSheet(
    versionName: String,
    versionCode: Long,
    isFirstLaunch: Boolean,
    onDismiss: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val scrollState = rememberScrollState()
    val items = remember { getFeatureShowcaseItems() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isDark) Color(0xFF121417) else Color(0xFFF9FAFB),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) ElectricGreen.copy(alpha = 0.15f) else AmoledBlack.copy(alpha = 0.08f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = if (isDark) ElectricGreen else AmoledBlack,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = if (isFirstLaunch) "Welcome to Naughty" else "What's New in Naughty",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDark) ElectricGreen.copy(alpha = 0.15f) else AmoledBlack.copy(alpha = 0.08f)
                            ) {
                                Text(
                                    text = if (isFirstLaunch) "v$versionName • First Launch" else "v$versionName • Updated",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) ElectricGreen else AmoledBlack,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isFirstLaunch) {
                    "Your fast, context-aware, 100% offline note-taking hub. Here are the key features at your fingertips:"
                } else {
                    "Here is what makes your notes private, smart, and fast in this release:"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable feature list
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items.forEach { item ->
                    FeatureItemRow(item = item, isDark = isDark)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Action Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) ElectricGreen else AmoledBlack,
                    contentColor = if (isDark) AmoledBlack else CrystalWhite
                )
            ) {
                Text(
                    text = if (isFirstLaunch) "Explore Naughty" else "Got It, Let's Go",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FeatureItemRow(
    item: FeatureShowcaseItem,
    isDark: Boolean
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) Color(0xFF191C22) else Color(0xFFF1F3F5),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isDark) Color(0xFF282C34) else Color(0xFFE2E5E9)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = item.accentColor.copy(alpha = if (isDark) 0.18f else 0.12f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = item.accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp,
                        color = item.accentColor,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp,
                    fontSize = 12.5.sp
                )
            }
        }
    }
}
