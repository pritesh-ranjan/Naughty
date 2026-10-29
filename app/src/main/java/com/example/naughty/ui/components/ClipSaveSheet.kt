package com.example.naughty.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.naughty.ui.oneui.components.OneUiBottomSheet
import com.example.naughty.ui.oneui.components.OneUiFilledButton
import com.example.naughty.ui.oneui.theme.OneUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipSaveSheet(
    clipText: String,
    sourceAppName: String,
    onSaveToCtsNote: () -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    OneUiBottomSheet(
        title = "Circle to Search",
        subtitle = "Captured from $sourceAppName",
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Surface(
                shape = OneUi.shapes.inner,
                color = OneUi.colors.surfaceElevated,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(16.dp)
                ) {
                    Text(
                        text = clipText,
                        style = OneUi.typography.body,
                        color = OneUi.colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OneUiFilledButton(
                text = "Save to CTS Note",
                icon = Icons.Outlined.Search,
                onClick = {
                    onSaveToCtsNote()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
