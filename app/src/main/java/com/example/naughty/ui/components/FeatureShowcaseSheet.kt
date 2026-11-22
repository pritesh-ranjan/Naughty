package com.example.naughty.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.ui.oneui.components.OneUiBottomSheet
import com.example.naughty.ui.oneui.components.OneUiFilledButton
import com.example.naughty.ui.oneui.components.OneUiIconBadge
import com.example.naughty.ui.oneui.theme.OneUi

data class FeatureShowcaseItem(
    val title: String,
    val category: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
)

fun getFeatureShowcaseItems(): List<FeatureShowcaseItem> = listOf(
    FeatureShowcaseItem(
        title = "Offline Video Summarizer",
        category = "Video AI",
        description = "Share videos up to 3 minutes to Naughty to turn speech and on-screen text into crisp bullet-point notes — 100% offline in the background.",
        icon = Icons.Outlined.Movie,
        accentColor = Color(0xFF7E57C2)
    ),
    FeatureShowcaseItem(
        title = "App-Linked Notes",
        category = "Smart Context",
        description = "Notes appear automatically as alerts when you open specific apps — like grocery checklists in Instacart, or meeting notes in Zoom.",
        icon = Icons.Outlined.Apps,
        accentColor = Color(0xFF2C75FF)
    ),
    FeatureShowcaseItem(
        title = "Circle to Search Capture",
        category = "Instant Save",
        description = "Look up anything with Circle to Search or Google Lens, and Naughty promptly offers to save the result as a note the moment you exit.",
        icon = Icons.Outlined.Search,
        accentColor = Color(0xFFFF9500)
    ),
    FeatureShowcaseItem(
        title = "Timeline Tasks",
        category = "Project Tracking",
        description = "Track multi-step goals with customizable stages, ordered milestones, due dates, and real-time visual progress bars.",
        icon = Icons.Outlined.Timeline,
        accentColor = Color(0xFF34C759)
    ),
    FeatureShowcaseItem(
        title = "Photo & Audio Voice Notes",
        category = "Rich Media",
        description = "Attach photos and record studio-clarity voice memos with interactive waveform scrubbing and AI neural background noise cancellation.",
        icon = Icons.Outlined.GraphicEq,
        accentColor = Color(0xFFFF453A)
    ),
    FeatureShowcaseItem(
        title = "Offline Text Scanner (OCR)",
        category = "On-Device AI",
        description = "Extract text from photos and documents instantly on your phone — both inside note drafts and directly when sharing images to Naughty.",
        icon = Icons.Outlined.DocumentScanner,
        accentColor = Color(0xFF00C7BE)
    ),
    FeatureShowcaseItem(
        title = "Offline Speech-to-Text",
        category = "Private Voice",
        description = "Dictate live thoughts or transcribe audio files into clean text without an internet connection or cloud delay.",
        icon = Icons.Outlined.RecordVoiceOver,
        accentColor = Color(0xFF7E57C2)
    ),
    FeatureShowcaseItem(
        title = "@ Mentions & Smart Links",
        category = "Connected Notes",
        description = "Type @ while writing to tag contacts, installed apps, or calendar events with instant one-tap navigation.",
        icon = Icons.Outlined.AlternateEmail,
        accentColor = Color(0xFFFF9500)
    ),
    FeatureShowcaseItem(
        title = "100% Offline & Private",
        category = "Zero Tracking",
        description = "Zero accounts, zero cloud servers, and zero telemetry. Your notes stay strictly on your phone in standard Markdown with styled PDF export.",
        icon = Icons.Outlined.Shield,
        accentColor = Color(0xFF00C7BE)
    ),
    FeatureShowcaseItem(
        title = "Biometric Lock & Fast Search",
        category = "Security",
        description = "Lock confidential notes behind fingerprint or face authentication. Pull down from anywhere to search notes and milestones instantly.",
        icon = Icons.Outlined.Lock,
        accentColor = Color(0xFF34C759)
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
    val featuredItem = items.firstOrNull()
    val otherItems = items.drop(1)
    val islandColor = if (OneUi.colors.isDark) OneUi.colors.surface else OneUi.colors.background

    OneUiBottomSheet(
        onDismissRequest = onDismiss,
        title = if (isFirstLaunch) "Welcome to Naughty" else "What's New in Naughty",
        subtitle = "v$versionName (Build $versionCode) • ${if (isFirstLaunch) "First Launch" else "Highlights"}"
    ) {
        Text(
            text = if (isFirstLaunch) {
                "Your fast, context-aware, 100% offline note-taking hub. Here are key highlights at your fingertips:"
            } else {
                "Private, smart, and fast capabilities ready on your device in this release:"
            },
            style = OneUi.type.rowSubtitle,
            color = OneUi.colors.textSecondary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Scrollable One UI squircle islands
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(scrollState)
        ) {
            if (featuredItem != null) {
                Text(
                    text = if (isFirstLaunch) "FEATURED" else "NEW IN THIS RELEASE",
                    style = OneUi.type.groupLabel,
                    color = OneUi.colors.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )

                Surface(
                    shape = OneUi.shapes.card,
                    color = islandColor,
                    border = BorderStroke(1.dp, OneUi.colors.accent.copy(alpha = 0.28f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FeatureItemRow(item = featuredItem)
                }
            }

            if (otherItems.isNotEmpty()) {
                Text(
                    text = "KEY HIGHLIGHTS",
                    style = OneUi.type.groupLabel,
                    color = OneUi.colors.textSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 8.dp, top = 16.dp, bottom = 8.dp)
                )

                Surface(
                    shape = OneUi.shapes.card,
                    color = islandColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        otherItems.forEachIndexed { index, item ->
                            FeatureItemRow(item = item)
                            if (index < otherItems.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 66.dp, end = 16.dp),
                                    thickness = 0.5.dp,
                                    color = OneUi.colors.divider
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))

        OneUiFilledButton(
            text = if (isFirstLaunch) "Explore Naughty" else "Got It",
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FeatureItemRow(item: FeatureShowcaseItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        OneUiIconBadge(
            icon = item.icon,
            tint = item.accentColor,
            iconColor = Color.White,
            badgeSize = 36.dp,
            iconSize = 18.dp,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.title,
                    style = OneUi.type.cardTitle,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OneUi.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = OneUi.shapes.pill,
                    color = item.accentColor.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = item.category,
                        style = OneUi.type.caption,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = item.accentColor,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.description,
                style = OneUi.type.rowSubtitle,
                color = OneUi.colors.textSecondary,
                lineHeight = 19.sp
            )
        }
    }
}
