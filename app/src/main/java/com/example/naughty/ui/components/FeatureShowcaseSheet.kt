package com.example.naughty.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.ui.oneui.components.OneUiBottomSheet
import com.example.naughty.ui.oneui.components.OneUiCard
import com.example.naughty.ui.oneui.components.OneUiFilledButton
import com.example.naughty.ui.oneui.components.OneUiIconBadge
import com.example.naughty.ui.oneui.theme.OneUi
import com.example.naughty.ui.theme.ElectricAmber
import com.example.naughty.ui.theme.ElectricCyan
import com.example.naughty.ui.theme.ElectricGreen
import com.example.naughty.ui.theme.ElectricPink
import com.example.naughty.ui.theme.ElectricPurple

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
    val scrollState = rememberScrollState()
    val items = remember { getFeatureShowcaseItems() }

    OneUiBottomSheet(
        onDismissRequest = onDismiss,
        title = if (isFirstLaunch) "Welcome to Naughty" else "What's New in Naughty",
        subtitle = "v$versionName ($versionCode) • ${if (isFirstLaunch) "First Launch" else "Updated"}"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = OneUi.dimens.horizontalMargin)
        ) {
            Text(
                text = if (isFirstLaunch) {
                    "Your fast, context-aware, 100% offline note-taking hub. Here are key highlights at your fingertips:"
                } else {
                    "Here is what makes your notes private, smart, and fast in this release:"
                },
                style = OneUi.typography.subhead,
                color = OneUi.colors.textSecondary,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable feature list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.forEach { item ->
                    FeatureItemRow(item = item)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Action Button
            OneUiFilledButton(
                text = if (isFirstLaunch) "Explore Naughty" else "Got It, Let's Go",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FeatureItemRow(item: FeatureShowcaseItem) {
    OneUiCard(
        shape = OneUi.shapes.inner,
        containerColor = OneUi.colors.surfaceElevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            OneUiIconBadge(
                icon = item.icon,
                iconTint = item.accentColor,
                backgroundColor = item.accentColor.copy(alpha = 0.16f),
                size = 38.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.title,
                        style = OneUi.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = OneUi.colors.textPrimary,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = OneUi.shapes.pill,
                        color = item.accentColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = item.category.uppercase(),
                            style = OneUi.typography.badge,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp,
                            color = item.accentColor,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = item.description,
                    style = OneUi.typography.bodySmall,
                    color = OneUi.colors.textSecondary,
                    lineHeight = 17.sp,
                    fontSize = 12.5.sp
                )
            }
        }
    }
}
