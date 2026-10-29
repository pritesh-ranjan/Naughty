package com.example.naughty.ui.oneui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.ui.oneui.theme.OneUi

@Composable
fun OneUiSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = OneUi.type.sectionTitle,
        color = OneUi.colors.textPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        modifier = modifier.padding(
            start = OneUi.dimens.screenGutter + 4.dp,
            end = OneUi.dimens.screenGutter + 4.dp,
            top = 18.dp,
            bottom = 8.dp
        )
    )
}
