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
    private val IMAGE_REGEX = Regex("""^!\[.*?\]\(.*?\)""")

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
            // Ignore attached image markdown lines inside the body block flow
            if (IMAGE_REGEX.matches(trimmed)) {
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
            } else {
                textAccumulator.add(line)
            }
        }

        flushText()

        if (blocks.isEmpty()) {
            blocks.add(EditorBlock.Text(id = UUID.randomUUID().toString(), text = ""))
        }

        return blocks
    }

    fun toMarkdown(blocks: List<EditorBlock>, attachedImages: List<String> = emptyList()): String {
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
                    pieces.add("- [$check] ${block.text}$reminderPart")
                }
            }
        }

        val baseContent = pieces.joinToString("\n")
        return if (attachedImages.isNotEmpty()) {
            val imagePart = attachedImages.joinToString("\n\n") { "![Image]($it)" }
            if (baseContent.isBlank()) {
                imagePart
            } else {
                baseContent.trimEnd() + "\n\n" + imagePart
            }
        } else {
            baseContent
        }
    }

    fun extractImages(content: String): List<String> {
        val regex = Regex("!\\[.*?\\]\\((.*?)\\)")
        return regex.findAll(content).map { it.groupValues[1] }.toList()
    }
}
