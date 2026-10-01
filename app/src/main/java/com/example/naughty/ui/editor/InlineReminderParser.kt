package com.example.naughty.ui.editor

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class InlineReminder(
    val rawTag: String,
    val displayText: String,
    val timestampMillis: Long,
    val startIndex: Int,
    val endIndex: Int
)

object InlineReminderParser {

    private val REMINDER_REGEX = Regex("""(?:🔔\s*)?\[(?:🔔\s*)?([^\]]+)\]\(reminder:(\d+)\)""")

    fun parseReminders(text: String): List<InlineReminder> {
        if (text.isBlank()) return emptyList()
        val results = mutableListOf<InlineReminder>()
        for (match in REMINDER_REGEX.findAll(text)) {
            val raw = match.value
            val label = match.groupValues[1].replace("🔔", "").trim()
            val millis = match.groupValues[2].toLongOrNull() ?: continue
            results.add(
                InlineReminder(
                    rawTag = raw,
                    displayText = label.ifBlank { formatTimestamp(millis) },
                    timestampMillis = millis,
                    startIndex = match.range.first,
                    endIndex = match.range.last + 1
                )
            )
        }
        return results
    }

    fun hasReminder(line: String): Boolean {
        return REMINDER_REGEX.containsMatchIn(line)
    }

    fun extractFirstReminder(line: String): InlineReminder? {
        return parseReminders(line).firstOrNull()
    }

    fun createReminderTag(timestampMillis: Long, customLabel: String? = null): String {
        val label = customLabel?.trim()?.ifBlank { null } ?: formatTimestamp(timestampMillis)
        return "[🔔 $label](reminder:$timestampMillis)"
    }

    fun cleanAllReminders(text: String): String {
        return text.replace(REMINDER_REGEX, "").trim()
    }

    fun removeReminderFromLine(line: String, targetMillis: Long? = null): String {
        return if (targetMillis != null) {
            val targetRegex = Regex("""\s*(?:🔔\s*)?\[(?:🔔\s*)?[^\]]+\]\(reminder:$targetMillis\)""")
            line.replace(targetRegex, "").trimEnd()
        } else {
            line.replace(REMINDER_REGEX, "").trimEnd()
        }
    }

    fun attachOrUpdateReminderInLine(line: String, timestampMillis: Long, customLabel: String? = null): String {
        val cleanedLine = removeReminderFromLine(line)
        val tag = createReminderTag(timestampMillis, customLabel)
        return if (cleanedLine.isBlank()) {
            tag
        } else {
            "$cleanedLine $tag"
        }
    }

    fun formatTimestamp(timestampMillis: Long): String {
        val target = Calendar.getInstance().apply { timeInMillis = timestampMillis }
        val now = Calendar.getInstance()

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestampMillis))

        val isSameYear = now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
        val isToday = isSameYear && now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
        val isTomorrow = isSameYear && target.get(Calendar.DAY_OF_YEAR) - now.get(Calendar.DAY_OF_YEAR) == 1
        val isYesterday = isSameYear && now.get(Calendar.DAY_OF_YEAR) - target.get(Calendar.DAY_OF_YEAR) == 1

        return when {
            isToday -> "Today, $timeFormat"
            isTomorrow -> "Tomorrow, $timeFormat"
            isYesterday -> "Yesterday, $timeFormat"
            isSameYear -> {
                val dateFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(timestampMillis))
                "$dateFormat, $timeFormat"
            }
            else -> {
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestampMillis))
                "$dateFormat, $timeFormat"
            }
        }
    }

    fun getRelativeTimeDescription(timestampMillis: Long): String {
        val diff = timestampMillis - System.currentTimeMillis()
        if (diff <= 0) return "Due / Passed"

        val minutes = diff / (60 * 1000)
        val hours = diff / (60 * 60 * 1000)
        val days = diff / (24 * 60 * 60 * 1000)

        return when {
            minutes < 1 -> "In less than a minute"
            minutes < 60 -> "In $minutes min${if (minutes > 1) "s" else ""}"
            hours < 24 -> "In $hours hour${if (hours > 1) "s" else ""}"
            days == 1L -> "In 1 day"
            days < 30 -> "In $days days"
            else -> "In ${days / 30} month(s)"
        }
    }

    fun findUpcomingReminders(content: String): List<InlineReminder> {
        val now = System.currentTimeMillis()
        return parseReminders(content)
            .filter { it.timestampMillis > now }
            .sortedBy { it.timestampMillis }
    }
}
