package com.example.naughty.ui.oneui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.ui.oneui.theme.OneUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneUiConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "OK",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    dismissLabel: String = "Cancel",
    modifier: Modifier = Modifier
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Surface(
            shape = OneUi.shapes.dialog,
            color = OneUi.colors.surfaceElevated,
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header & Message
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = OneUi.type.cardTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OneUi.colors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    if (message.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = message,
                            style = OneUi.type.cardBody,
                            fontSize = 14.5.sp,
                            color = OneUi.colors.textSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = OneUi.colors.divider)

                // One UI Split Action Buttons Bar: [ Dismiss  |  Confirm ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dismissLabel,
                            style = OneUi.type.rowTitle,
                            fontWeight = FontWeight.SemiBold,
                            color = OneUi.colors.textSecondary,
                            fontSize = 15.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .background(OneUi.colors.divider)
                            .padding(horizontal = 0.25.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clickable(onClick = onConfirm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = confirmLabel,
                            style = OneUi.type.rowTitle,
                            fontWeight = FontWeight.Bold,
                            color = if (destructive) OneUi.colors.destructive else OneUi.colors.accent,
                            fontSize = 15.5.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneUiDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String = "OK",
    dismissLabel: String = "Cancel",
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Surface(
            shape = OneUi.shapes.dialog,
            color = OneUi.colors.surfaceElevated,
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 22.dp, start = 22.dp, end = 22.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = title,
                        style = OneUi.type.cardTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OneUi.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    content()
                }

                HorizontalDivider(thickness = 0.5.dp, color = OneUi.colors.divider)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dismissLabel,
                            style = OneUi.type.rowTitle,
                            fontWeight = FontWeight.SemiBold,
                            color = OneUi.colors.textSecondary,
                            fontSize = 15.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .background(OneUi.colors.divider)
                            .padding(horizontal = 0.25.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clickable(enabled = confirmEnabled, onClick = onConfirm),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = confirmLabel,
                            style = OneUi.type.rowTitle,
                            fontWeight = FontWeight.Bold,
                            color = if (!confirmEnabled) {
                                OneUi.colors.textTertiary
                            } else if (destructive) {
                                OneUi.colors.destructive
                            } else {
                                OneUi.colors.accent
                            },
                            fontSize = 15.5.sp
                        )
                    }
                }
            }
        }
    }
}

