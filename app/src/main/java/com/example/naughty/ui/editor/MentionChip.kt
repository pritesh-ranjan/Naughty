package com.example.naughty.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naughty.data.mention.MentionType
import com.example.naughty.data.mention.ParsedMention
import com.example.naughty.ui.theme.ElectricAmber
import com.example.naughty.ui.theme.ElectricCyan
import com.example.naughty.ui.theme.ElectricGreen

@Composable
fun MentionChip(
    mention: ParsedMention,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily? = null
) {
    val (tintColor, iconVector) = when (mention.type) {
        MentionType.CONTACT -> ElectricGreen to Icons.Outlined.Person
        MentionType.APP -> ElectricCyan to Icons.Outlined.Apps
        MentionType.CALENDAR -> ElectricAmber to Icons.Outlined.CalendarMonth
    }

    val backgroundColor = tintColor.copy(alpha = 0.15f)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = mention.type.name,
                tint = tintColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "@${mention.title}",
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = tintColor,
                maxLines = 1
            )
        }
    }
}
