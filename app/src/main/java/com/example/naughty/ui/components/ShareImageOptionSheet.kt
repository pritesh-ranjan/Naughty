package com.example.naughty.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.ui.notelist.AttachedImageThumbnail
import com.example.naughty.ui.oneui.components.OneUiBottomSheet
import com.example.naughty.ui.oneui.theme.OneUi
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
    OneUiBottomSheet(
        title = "Save Shared Image",
        subtitle = if (imageUris.size == 1) "1 image received" else "${imageUris.size} images received",
        onDismiss = { if (!isProcessing) onDismiss() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Thumbnail Preview
            if (imageUris.isNotEmpty()) {
                if (imageUris.size == 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(OneUi.shapes.inner)
                            .background(OneUi.colors.surfaceElevated)
                    ) {
                        AttachedImageThumbnail(
                            imageRef = imageUris.first().toString(),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        imageUris.forEach { uri ->
                            Box(
                                modifier = Modifier
                                    .size(width = 110.dp, height = 110.dp)
                                    .clip(OneUi.shapes.inner)
                                    .background(OneUi.colors.surfaceElevated)
                            ) {
                                AttachedImageThumbnail(
                                    imageRef = uri.toString(),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(width = 110.dp, height = 110.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Processing State
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
                            text = processingText.ifBlank { "Processing image..." },
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
                    ShareOptionCard(
                        icon = Icons.Outlined.Image,
                        accentColor = OneUi.badgePalette.blue,
                        title = "Attach Image",
                        description = "Create note with image attached as markdown",
                        onClick = { onSelectMode(ImageShareMode.IMAGE_ONLY) }
                    )

                    ShareOptionCard(
                        icon = Icons.Outlined.DocumentScanner,
                        accentColor = OneUi.badgePalette.orange,
                        title = "Extract Text (OCR)",
                        description = "Transcribe on-device with ML Kit into a text note",
                        onClick = { onSelectMode(ImageShareMode.OCR_ONLY) }
                    )

                    ShareOptionCard(
                        icon = Icons.Outlined.AutoAwesome,
                        accentColor = OneUi.badgePalette.green,
                        title = "Both (Image & OCR Text)",
                        description = "Transcribe text and keep image attached at bottom",
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
