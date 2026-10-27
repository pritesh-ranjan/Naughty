package com.example.naughty.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.example.naughty.data.mention.MentionItem
import com.example.naughty.data.mention.MentionTriggerState
import com.example.naughty.data.mention.MentionType
import com.example.naughty.data.mention.ParsedMention
import com.example.naughty.ui.theme.ElectricAmber
import com.example.naughty.ui.theme.ElectricCyan
import com.example.naughty.ui.theme.ElectricGreen
import java.util.Locale

object MentionParser {

    private val MENTION_REGEX = Regex("""\[@([^\]]+)\]\(mention:(contact|app|calendar):([^)]+)\)""")

    fun parseMentions(text: String): List<ParsedMention> {
        if (text.isBlank()) return emptyList()
        val results = mutableListOf<ParsedMention>()
        for (match in MENTION_REGEX.findAll(text)) {
            val raw = match.value
            val title = match.groupValues[1].trim()
            val typeStr = match.groupValues[2].uppercase(Locale.ROOT)
            val type = try {
                MentionType.valueOf(typeStr)
            } catch (_: Exception) {
                continue
            }
            val id = match.groupValues[3].trim()

            results.add(
                ParsedMention(
                    rawTag = raw,
                    title = title,
                    type = type,
                    id = id,
                    startIndex = match.range.first,
                    endIndex = match.range.last + 1
                )
            )
        }
        return results
    }

    fun hasMentions(text: String): Boolean {
        return MENTION_REGEX.containsMatchIn(text)
    }

    fun detectMentionTrigger(text: String, cursorPosition: Int): MentionTriggerState? {
        if (cursorPosition <= 0 || cursorPosition > text.length) return null
        val beforeCursor = text.substring(0, cursorPosition)
        val atIndex = beforeCursor.lastIndexOf('@')
        if (atIndex < 0) return null

        // Ensure '@' is at start of text or preceded by whitespace / newline
        if (atIndex > 0 && !beforeCursor[atIndex - 1].isWhitespace()) return null

        val query = beforeCursor.substring(atIndex + 1)
        // If query starts with whitespace, has newlines, multiple spaces, punctuation, or is too long, don't trigger
        if (query.startsWith(' ') || query.contains('\n') || query.length > 30) return null
        // Max 1 space allowed (e.g. "First Last") and no double spaces
        if (query.count { it == ' ' } > 1 || query.contains("  ")) return null
        // Avoid punctuation inside mention query
        if (query.any { it in ",;!?()[]<>{}" }) return null

        // Find end index: expand to end of current word token if cursor is in middle
        val afterCursor = text.substring(cursorPosition)
        val wordEnd = afterCursor.indexOfFirst { it.isWhitespace() || it in "\n\t[]()<>,;!?" }
        val endIndex = if (wordEnd == -1) text.length else cursorPosition + wordEnd

        return MentionTriggerState(query = query, startIndex = atIndex, endIndex = endIndex)
    }

    fun createMentionTag(item: MentionItem): String {
        val sanitizedTitle = item.title.replace("[", "").replace("]", "").trim()
        val typeStr = item.type.name.lowercase(Locale.ROOT)
        return "[@$sanitizedTitle](mention:$typeStr:${item.id})"
    }

    fun cleanMentions(text: String, keepAtPrefix: Boolean = true): String {
        return text.replace(MENTION_REGEX) { matchResult ->
            val title = matchResult.groupValues[1].trim()
            if (keepAtPrefix) "@$title" else title
        }
    }

    fun buildInlineAnnotatedString(
        text: String,
        reminderAccent: Color? = null
    ): AnnotatedString {
        if (text.isEmpty()) return AnnotatedString("")

        val matches = MENTION_REGEX.findAll(text).toList()
        if (matches.isEmpty()) {
            return buildAnnotatedString {
                append(text)
                if (reminderAccent != null) {
                    val reminderRegex = Regex("""(?:🔔\s*)?\[(?:🔔\s*)?([^\]]+)\]\(reminder:(\d+)\)""")
                    for (match in reminderRegex.findAll(text)) {
                        addStyle(
                            style = SpanStyle(
                                color = reminderAccent,
                                fontWeight = FontWeight.Bold,
                                background = reminderAccent.copy(alpha = 0.14f)
                            ),
                            start = match.range.first,
                            end = match.range.last + 1
                        )
                    }
                }
            }
        }

        return buildAnnotatedString {
            var lastIdx = 0
            for (match in matches) {
                if (match.range.first > lastIdx) {
                    append(text.substring(lastIdx, match.range.first))
                }

                val title = match.groupValues[1].trim()
                val typeStr = match.groupValues[2].lowercase(Locale.ROOT)
                val id = match.groupValues[3].trim()
                val tint = when (typeStr) {
                    "contact" -> ElectricGreen
                    "app" -> ElectricCyan
                    else -> ElectricAmber
                }

                val start = length
                append("@$title")
                val end = length

                addStyle(
                    style = SpanStyle(
                        color = tint,
                        fontWeight = FontWeight.SemiBold,
                        background = tint.copy(alpha = 0.16f)
                    ),
                    start = start,
                    end = end
                )

                addStringAnnotation(
                    tag = "mention",
                    annotation = "$typeStr:$id:$title",
                    start = start,
                    end = end
                )

                lastIdx = match.range.last + 1
            }

            if (lastIdx < text.length) {
                append(text.substring(lastIdx))
            }

            if (reminderAccent != null) {
                val reminderRegex = Regex("""(?:🔔\s*)?\[(?:🔔\s*)?([^\]]+)\]\(reminder:(\d+)\)""")
                for (match in reminderRegex.findAll(toAnnotatedString().text)) {
                    addStyle(
                        style = SpanStyle(
                            color = reminderAccent,
                            fontWeight = FontWeight.Bold,
                            background = reminderAccent.copy(alpha = 0.14f)
                        ),
                        start = match.range.first,
                        end = match.range.last + 1
                    )
                }
            }
        }
    }

    fun parseAnnotation(annotation: String): ParsedMention? {
        val parts = annotation.split(":", limit = 3)
        if (parts.size < 3) return null
        val typeStr = parts[0].uppercase(Locale.ROOT)
        val id = parts[1]
        val title = parts[2]
        val type = try {
            MentionType.valueOf(typeStr)
        } catch (_: Exception) {
            return null
        }
        val rawTag = "[@$title](mention:${parts[0]}:$id)"
        return ParsedMention(
            rawTag = rawTag,
            title = title,
            type = type,
            id = id,
            startIndex = 0,
            endIndex = 0
        )
    }
}
