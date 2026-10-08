package com.example.naughty.ui.timeline

import androidx.compose.ui.graphics.Color
import com.example.naughty.data.local.timeline.StageCategory
import com.example.naughty.data.local.timeline.TrackerPriority
import com.example.naughty.ui.theme.ElectricAmber
import com.example.naughty.ui.theme.ElectricCyan
import com.example.naughty.ui.theme.ElectricGreen
import com.example.naughty.ui.theme.ElectricPink
import com.example.naughty.ui.theme.ElectricPurple

object TimelineColors {

    fun accentColor(accentKey: String): Color {
        return when (accentKey.lowercase()) {
            "green" -> ElectricGreen
            "cyan" -> ElectricCyan
            "pink" -> ElectricPink
            "amber" -> ElectricAmber
            "purple" -> ElectricPurple
            else -> ElectricGreen
        }
    }

    fun categoryColor(category: StageCategory): Color {
        return when (category) {
            StageCategory.NOT_STARTED -> Color(0xFF71717A)
            StageCategory.IN_PROGRESS -> ElectricCyan
            StageCategory.BLOCKED -> ElectricPink
            StageCategory.DONE -> ElectricGreen
            StageCategory.CANCELLED -> Color(0xFF52525B)
        }
    }

    fun stageColor(colorKey: String, fallbackCategory: StageCategory): Color {
        return when (colorKey.lowercase()) {
            "green" -> ElectricGreen
            "cyan" -> ElectricCyan
            "pink" -> ElectricPink
            "amber" -> ElectricAmber
            "purple" -> ElectricPurple
            "gray", "muted" -> Color(0xFF71717A)
            else -> categoryColor(fallbackCategory)
        }
    }

    fun priorityColor(priority: TrackerPriority): Color {
        return when (priority) {
            TrackerPriority.LOW -> Color(0xFF71717A)
            TrackerPriority.MEDIUM -> ElectricCyan
            TrackerPriority.HIGH -> ElectricAmber
            TrackerPriority.CRITICAL -> ElectricPink
        }
    }

    val availableAccents = listOf("green", "cyan", "amber", "pink", "purple")
}
