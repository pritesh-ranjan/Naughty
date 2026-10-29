package com.example.naughty.ui.notelist

import com.example.naughty.data.local.NoteMetadata
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object NoteSections {

    fun bucket(
        notes: List<NoteMetadata>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<Pair<String, List<NoteMetadata>>> {
        if (notes.isEmpty()) return emptyList()

        val pinnedNotes = mutableListOf<NoteMetadata>()
        val todayNotes = mutableListOf<NoteMetadata>()
        val yesterdayNotes = mutableListOf<NoteMetadata>()
        val thisWeekNotes = mutableListOf<NoteMetadata>()
        val monthlyNotes = linkedMapOf<String, MutableList<NoteMetadata>>()

        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val nowYear = nowCal.get(Calendar.YEAR)
        val nowDayOfYear = nowCal.get(Calendar.DAY_OF_YEAR)

        val monthFormatSameYear = SimpleDateFormat("MMMM", Locale.US)
        val monthFormatDiffYear = SimpleDateFormat("MMMM yyyy", Locale.US)

        for (note in notes) {
            if (note.isPinned) {
                pinnedNotes.add(note)
                continue
            }

            val noteCal = Calendar.getInstance().apply { timeInMillis = note.modifiedAt }
            val noteYear = noteCal.get(Calendar.YEAR)
            val noteDayOfYear = noteCal.get(Calendar.DAY_OF_YEAR)

            val isSameYear = noteYear == nowYear
            val dayDiff = if (isSameYear) nowDayOfYear - noteDayOfYear else 999

            when {
                dayDiff == 0 -> todayNotes.add(note)
                dayDiff == 1 -> yesterdayNotes.add(note)
                dayDiff in 2..6 -> thisWeekNotes.add(note)
                else -> {
                    val key = if (isSameYear) {
                        monthFormatSameYear.format(Date(note.modifiedAt))
                    } else {
                        monthFormatDiffYear.format(Date(note.modifiedAt))
                    }
                    monthlyNotes.getOrPut(key) { mutableListOf() }.add(note)
                }
            }
        }

        val result = mutableListOf<Pair<String, List<NoteMetadata>>>()

        if (pinnedNotes.isNotEmpty()) {
            result.add("Pinned" to pinnedNotes)
        }
        if (todayNotes.isNotEmpty()) {
            result.add("Today" to todayNotes)
        }
        if (yesterdayNotes.isNotEmpty()) {
            result.add("Yesterday" to yesterdayNotes)
        }
        if (thisWeekNotes.isNotEmpty()) {
            result.add("This week" to thisWeekNotes)
        }
        for ((month, list) in monthlyNotes) {
            if (list.isNotEmpty()) {
                result.add(month to list)
            }
        }

        return result
    }
}
