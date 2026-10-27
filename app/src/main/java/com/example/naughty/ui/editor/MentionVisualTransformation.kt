package com.example.naughty.ui.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.example.naughty.ui.theme.ElectricAmber
import com.example.naughty.ui.theme.ElectricCyan
import com.example.naughty.ui.theme.ElectricGreen
import java.util.Locale

class MentionVisualTransformation(
    private val reminderAccentColor: Color? = null
) : VisualTransformation {

    private data class MentionSpan(
        val origStart: Int,
        val origEnd: Int,
        val transStart: Int,
        val transEnd: Int,
        val titleLen: Int
    )

    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val mentionRegex = Regex("""\[@([^\]]+)\]\(mention:(contact|app|calendar):([^)]+)\)""")
        val matches = mentionRegex.findAll(originalText).toList()

        // If no mentions, apply only reminder styling if present, with Identity mapping
        if (matches.isEmpty()) {
            if (reminderAccentColor != null && originalText.contains("(reminder:")) {
                val reminderRegex = Regex("""(?:🔔\s*)?\[(?:🔔\s*)?([^\]]+)\]\(reminder:(\d+)\)""")
                val builder = buildAnnotatedString {
                    append(originalText)
                    for (match in reminderRegex.findAll(originalText)) {
                        addStyle(
                            style = SpanStyle(
                                color = reminderAccentColor,
                                fontWeight = FontWeight.Bold,
                                background = reminderAccentColor.copy(alpha = 0.14f)
                            ),
                            start = match.range.first,
                            end = match.range.last + 1
                        )
                    }
                }
                return TransformedText(builder, OffsetMapping.Identity)
            }
            return TransformedText(text, OffsetMapping.Identity)
        }

        val spans = mutableListOf<MentionSpan>()
        val builder = buildAnnotatedString {
            var lastOriginalIndex = 0
            for (match in matches) {
                // Append plain text before mention
                if (match.range.first > lastOriginalIndex) {
                    append(originalText.substring(lastOriginalIndex, match.range.first))
                }

                val title = match.groupValues[1].trim()
                val type = match.groupValues[2].lowercase(Locale.ROOT)
                val tint = when (type) {
                    "contact" -> ElectricGreen
                    "app" -> ElectricCyan
                    else -> ElectricAmber
                }

                val transStart = length
                append("@$title")
                val transEnd = length

                addStyle(
                    style = SpanStyle(
                        color = tint,
                        fontWeight = FontWeight.SemiBold,
                        background = tint.copy(alpha = 0.16f)
                    ),
                    start = transStart,
                    end = transEnd
                )

                spans.add(
                    MentionSpan(
                        origStart = match.range.first,
                        origEnd = match.range.last + 1,
                        transStart = transStart,
                        transEnd = transEnd,
                        titleLen = title.length
                    )
                )

                lastOriginalIndex = match.range.last + 1
            }

            // Append remaining plain text
            if (lastOriginalIndex < originalText.length) {
                append(originalText.substring(lastOriginalIndex))
            }

            // Also style reminders if present
            if (reminderAccentColor != null) {
                val reminderRegex = Regex("""(?:🔔\s*)?\[(?:🔔\s*)?([^\]]+)\]\(reminder:(\d+)\)""")
                for (match in reminderRegex.findAll(toAnnotatedString().text)) {
                    addStyle(
                        style = SpanStyle(
                            color = reminderAccentColor,
                            fontWeight = FontWeight.Bold,
                            background = reminderAccentColor.copy(alpha = 0.14f)
                        ),
                        start = match.range.first,
                        end = match.range.last + 1
                    )
                }
            }
        }

        val transformedLength = builder.length
        val originalLength = originalText.length

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, originalLength)
                var cumDelta = 0
                for (span in spans) {
                    if (clamped < span.origStart) {
                        return (clamped - cumDelta).coerceIn(0, transformedLength)
                    } else if (clamped <= span.origEnd) {
                        val titleOffset = clamped - (span.origStart + 1)
                        val res = when {
                            titleOffset <= 0 -> span.transStart
                            titleOffset <= span.titleLen -> span.transStart + titleOffset
                            else -> span.transEnd
                        }
                        return res.coerceIn(0, transformedLength)
                    } else {
                        cumDelta += (span.origEnd - span.origStart) - (span.transEnd - span.transStart)
                    }
                }
                return (clamped - cumDelta).coerceIn(0, transformedLength)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, transformedLength)
                var cumDelta = 0
                for (span in spans) {
                    if (clamped < span.transStart) {
                        return (clamped + cumDelta).coerceIn(0, originalLength)
                    } else if (clamped <= span.transEnd) {
                        val rel = clamped - span.transStart
                        val res = when {
                            rel == 0 -> span.origStart
                            rel >= (span.transEnd - span.transStart) -> span.origEnd
                            else -> span.origStart + 1 + rel
                        }
                        return res.coerceIn(0, originalLength)
                    } else {
                        cumDelta += (span.origEnd - span.origStart) - (span.transEnd - span.transStart)
                    }
                }
                return (clamped + cumDelta).coerceIn(0, originalLength)
            }
        }

        return TransformedText(builder, offsetMapping)
    }
}
