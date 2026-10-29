package com.example.naughty.ui.oneui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.naughty.ui.oneui.theme.OneUi

@Composable
fun OneUiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = if (label != null) { { Text(label) } } else null,
        placeholder = if (placeholder != null) { { Text(placeholder) } } else null,
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        trailingIcon = trailingIcon,
        shape = OneUi.shapes.inner,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = OneUi.colors.surface,
            unfocusedContainerColor = OneUi.colors.surface,
            focusedBorderColor = OneUi.colors.accent,
            unfocusedBorderColor = OneUi.colors.divider,
            focusedLabelColor = OneUi.colors.accent,
            unfocusedLabelColor = OneUi.colors.textSecondary,
            focusedTextColor = OneUi.colors.textPrimary,
            unfocusedTextColor = OneUi.colors.textPrimary,
            cursorColor = OneUi.colors.accent
        ),
        modifier = modifier.fillMaxWidth()
    )
}
