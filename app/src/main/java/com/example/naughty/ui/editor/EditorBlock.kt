package com.example.naughty.ui.editor

import java.util.UUID

sealed class EditorBlock {
    abstract val id: String

    data class Text(
        override val id: String = UUID.randomUUID().toString(),
        val text: String = ""
    ) : EditorBlock()

    data class Checklist(
        override val id: String = UUID.randomUUID().toString(),
        val isChecked: Boolean = false,
        val text: String = "",
        val reminder: InlineReminder? = null
    ) : EditorBlock()
}

object EditorBlockParser {

    private val CHECKLIST_LINE_REGEX = Regex("""^\s*-\s*\[([ xX])\](?:[ ](.*))?$""")
    private val CHECKLIST_CONTINUATION_REGEX = Regex("""^\s{2,}(.*)$""")
    private val IMAGE_REGEX = Regex("""^!\[.*?\]\(.*?\)""")
    private val AUDIO_REGEX = Regex("""^!?\[(?:🎤\s*)?Voice Note.*?\]\((.*?)\)""", RegexOption.IGNORE_CASE)

    fun parse(content: String): List<EditorBlock> {
        if (content.isEmpty()) {
            return listOf(EditorBlock.Text(id = UUID.randomUUID().toString(), text = ""))
        }

        val blocks = mutableListOf<EditorBlock>()
        val textAccumulator = mutableListOf<String>()

        fun flushText() {
            if (textAccumulator.isNotEmpty()) {
                blocks.add(
                    EditorBlock.Text(
                        id = UUID.randomUUID().toString(),
                        text = textAccumulator.joinToString("\n")
                    )
                )
                textAccumulator.clear()
            }
        }

        for (line in content.lines()) {
            val trimmed = line.trim()
            // Ignore attached image and voice note markdown lines inside the body block flow
            if (IMAGE_REGEX.matches(trimmed) || AUDIO_REGEX.matches(trimmed)) {
                if (textAccumulator.all { it.isBlank() }) {
                    textAccumulator.clear()
                }
                continue
            }

            val match = CHECKLIST_LINE_REGEX.find(line)
            if (match != null) {
                flushText()
                val isChecked = match.groupValues[1].equals("x", ignoreCase = true)
                val rawAfter = match.groupValues[2]
                val reminder = InlineReminderParser.extractFirstReminder(rawAfter)
                val cleanText = if (reminder != null) {
                    InlineReminderParser.removeReminderFromLine(rawAfter)
                } else {
                    rawAfter
                }
                blocks.add(
                    EditorBlock.Checklist(
                        id = UUID.randomUUID().toString(),
                        isChecked = isChecked,
                        text = cleanText,
                        reminder = reminder
                    )
                )
            } else if (textAccumulator.isEmpty() && blocks.lastOrNull() is EditorBlock.Checklist && CHECKLIST_CONTINUATION_REGEX.matches(line)) {
                val continuationMatch = CHECKLIST_CONTINUATION_REGEX.find(line)!!
                val rawContinuation = continuationMatch.groupValues[1]
                val reminder = InlineReminderParser.extractFirstReminder(rawContinuation)
                val cleanContinuation = if (reminder != null) {
                    InlineReminderParser.removeReminderFromLine(rawContinuation)
                } else {
                    rawContinuation
                }
                val lastChecklist = blocks.last() as EditorBlock.Checklist
                val updatedReminder = lastChecklist.reminder ?: reminder
                blocks[blocks.lastIndex] = lastChecklist.copy(
                    text = "${lastChecklist.text}\n$cleanContinuation",
                    reminder = updatedReminder
                )
            } else {
                textAccumulator.add(line)
            }
        }

        if (textAccumulator.all { it.isBlank() } && blocks.isNotEmpty()) {
            textAccumulator.clear()
        }
        flushText()

        if (blocks.isEmpty()) {
            blocks.add(EditorBlock.Text(id = UUID.randomUUID().toString(), text = ""))
        }

        return blocks
    }

    fun toMarkdown(
        blocks: List<EditorBlock>,
        attachedImages: List<String> = emptyList(),
        attachedVoiceNotes: List<String> = emptyList()
    ): String {
        val pieces = mutableListOf<String>()
        for (block in blocks) {
            when (block) {
                is EditorBlock.Text -> {
                    // If it's the only block and text is empty, omit from output
                    if (blocks.size == 1 && block.text.isEmpty()) {
                        // blank
                    } else {
                        pieces.add(block.text)
                    }
                }
                is EditorBlock.Checklist -> {
                    val check = if (block.isChecked) "x" else " "
                    val reminderPart = if (block.reminder != null) " ${block.reminder.rawTag}" else ""
                    val lines = block.text.split("\n")
                    if (lines.size <= 1) {
                        pieces.add("- [$check] ${block.text}$reminderPart")
                    } else {
                        val first = "- [$check] ${lines[0]}"
                        val rest = lines.drop(1).joinToString("\n") { "  $it" }
                        pieces.add("$first\n$rest$reminderPart")
                    }
                }
            }
        }

        var result = pieces.joinToString("\n")
        if (attachedImages.isNotEmpty()) {
            val imagePart = attachedImages.joinToString("\n\n") { "![Image]($it)" }
            result = if (result.isBlank()) {
                imagePart
            } else {
                result.trimEnd() + "\n\n" + imagePart
            }
        }
        if (attachedVoiceNotes.isNotEmpty()) {
            val audioPart = attachedVoiceNotes.joinToString("\n\n") { "[🎤 Voice Note]($it)" }
            result = if (result.isBlank()) {
                audioPart
            } else {
                result.trimEnd() + "\n\n" + audioPart
            }
        }
        return result
    }

    fun extractImages(content: String): List<String> {
        val regex = Regex("!\\[.*?\\]\\((.*?)\\)")
        return regex.findAll(content).map { it.groupValues[1] }.toList()
    }

    fun extractVoiceNotes(content: String): List<String> {
        val regex = Regex("""!?\[(?:🎤\s*)?Voice Note.*?\]\((.*?)\)""", RegexOption.IGNORE_CASE)
        return regex.findAll(content).map { it.groupValues[1] }.toList()
    }
}
