package com.example.naughty.ui.components

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.naughty.ui.notelist.AttachedImageThumbnail
import java.io.File

@Composable
fun FullscreenImageViewer(
    imageUri: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.8f, 5f)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // Full image fitted to screen with zoom and pan
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 48.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .transformable(state = transformableState)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        // If zoomed in, tap image resets to 1x
                        if (scale > 1.1f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                AttachedImageThumbnail(
                    imageRef = imageUri,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Top action buttons: Share and Close
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Share button
                IconButton(
                    onClick = { shareAttachedImage(context, imageUri) },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share image",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Close button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * Resolves local image reference into a content URI via FileProvider and launches Android share sheet.
 */
fun shareAttachedImage(context: Context, imageUri: String) {
    try {
        val uri: Uri = when {
            imageUri.startsWith("file://") -> {
                val file = File(imageUri.removePrefix("file://"))
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
            imageUri.startsWith("/") -> {
                val file = File(imageUri)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
            imageUri.startsWith("content://") -> {
                Uri.parse(imageUri)
            }
            else -> {
                val file = File(imageUri)
                if (file.exists()) {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } else {
                    Uri.parse(imageUri)
                }
            }
        }

        val mimeType = try {
            context.contentResolver.getType(uri) ?: when {
                imageUri.endsWith(".png", ignoreCase = true) -> "image/png"
                imageUri.endsWith(".webp", ignoreCase = true) -> "image/webp"
                imageUri.endsWith(".gif", ignoreCase = true) -> "image/gif"
                else -> "image/jpeg"
            }
        } catch (_: Exception) {
            "image/*"
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("Shared image", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share Image").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to share image: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
