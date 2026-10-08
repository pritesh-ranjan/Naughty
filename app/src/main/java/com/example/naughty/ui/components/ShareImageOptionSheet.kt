package com.example.naughty.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.ui.notelist.AttachedImageThumbnail
import com.example.naughty.ui.theme.DarkElevatedSurface
import com.example.naughty.ui.theme.ElectricAmber
import com.example.naughty.ui.theme.ElectricCyan
import com.example.naughty.ui.theme.ElectricGreen
import com.example.naughty.ui.theme.StealthCardBorder
import com.example.naughty.ui.theme.isAppInDarkTheme
import com.example.naughty.util.ImageShareMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareImageOptionSheet(
    imageUris: List<Uri>,
    isProcessing: Boolean = false,
    processingText: String = "",
    onSelectMode: (ImageShareMode) -> Unit,
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
                        text = "Save Shared Image",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (imageUris.size == 1) "1 image received" else "${imageUris.size} images received",
                        fontSize = 13.sp,
                        color = textSecondary
                    )
                }

                // Count Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardColor,
                    border = BorderStroke(0.8.dp, borderColor)
                ) {
                    Text(
                        text = if (imageUris.size == 1) "Single Photo" else "${imageUris.size} Photos",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Thumbnail Preview
            if (imageUris.isNotEmpty()) {
                if (imageUris.size == 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                            .background(cardColor)
                    ) {
                        AttachedImageThumbnail(
                            imageRef = imageUris[0].toString(),
                            modifier = Modifier.matchParentSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        imageUris.forEach { uri ->
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                                    .background(cardColor)
                            ) {
                                AttachedImageThumbnail(
                                    imageRef = uri.toString(),
                                    modifier = Modifier.matchParentSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Processing state or Option buttons
            AnimatedVisibility(
                visible = isProcessing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = cardColor,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = ElectricGreen
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = processingText.ifBlank { "Processing..." },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textPrimary
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !isProcessing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Option 1: Attach Image
                    ShareOptionCard(
                        icon = Icons.Outlined.Image,
                        accentColor = ElectricCyan,
                        title = "Attach Image",
                        description = "Create note with image attached as markdown",
                        cardColor = cardColor,
                        borderColor = borderColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onClick = { onSelectMode(ImageShareMode.IMAGE_ONLY) }
                    )

                    // Option 2: Extract Text via OCR
                    ShareOptionCard(
                        icon = Icons.Outlined.DocumentScanner,
                        accentColor = ElectricAmber,
                        title = "Extract Text (OCR)",
                        description = "Transcribe on-device with ML Kit into a text note",
                        cardColor = cardColor,
                        borderColor = borderColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onClick = { onSelectMode(ImageShareMode.OCR_ONLY) }
                    )

                    // Option 3: Both (Image + OCR Text)
                    ShareOptionCard(
                        icon = Icons.Outlined.AutoAwesome,
                        accentColor = ElectricGreen,
                        title = "Both (Image & OCR Text)",
                        description = "Transcribe text and keep image attached at bottom",
                        cardColor = cardColor,
                        borderColor = borderColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onClick = { onSelectMode(ImageShareMode.BOTH) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareOptionCard(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    description: String,
    cardColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = cardColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = textSecondary
                )
            }
        }
    }
}
