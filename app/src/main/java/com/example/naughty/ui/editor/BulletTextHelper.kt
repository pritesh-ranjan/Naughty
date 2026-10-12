package com.example.naughty.ui.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Helper for smart bullet list editing in notes.
 *
 * Rules:
 * 1. Pressing Enter on a bullet point row creates a new bullet point on the next line.
 * 2. Pressing Enter on an empty bullet point row (double enter) exits bullet points.
 * 3. Pressing Backspace on an empty bullet point row removes the bullet point.
 */
object BulletTextHelper {

    val BULLET_PREFIX_REGEX = Regex("""^(\s*(?:•|-|\*)\s*)(?!\[[ xX]?\])""")
    val EMPTY_BULLET_LINE_REGEX = Regex("""^\s*(?:•|-|\*)\s*$""")

    /**
     * Handles text value change from IME / soft keyboards.
     * Returns the transformed [TextFieldValue] if a bullet rule was applied, or null for default behavior.
     */
    fun handleValueChange(
        oldValue: TextFieldValue,
        newValue: TextFieldValue
    ): TextFieldValue? {
        val oldText = oldValue.text
        val newText = newValue.text

        val oldMin = oldValue.selection.min
        val oldMax = oldValue.selection.max
        val charsRemoved = oldMax - oldMin
        val charsAdded = newText.length - (oldText.length - charsRemoved)

        // Case 1: Enter pressed (newline inserted at cursor position)
        val isNewlineInserted = (charsAdded == 1 && oldMin < newText.length && newText[oldMin] == '\n') ||
            (charsAdded == 2 && oldMin + 1 < newText.length && newText.substring(oldMin, oldMin + 2) == "\r\n")

        if (isNewlineInserted) {
            val oldCursor = oldMin
            if (oldCursor <= oldText.length) {
                val lineStart = if (oldCursor == 0) 0 else {
                    val idx = oldText.lastIndexOf('\n', oldCursor - 1)
                    if (idx == -1) 0 else idx + 1
                }
                val lineEnd = oldText.indexOf('\n', oldCursor).let {
                    if (it == -1) oldText.length else it
                }
                val lineBeforeCursor = oldText.substring(lineStart, oldCursor)
                val entireLine = oldText.substring(lineStart, lineEnd)

                val bulletMatch = BULLET_PREFIX_REGEX.find(lineBeforeCursor)
                if (bulletMatch != null) {
                    val prefix = bulletMatch.value

                    // Rule 2: Double Enter on empty bullet point -> exit bullet points
                    if (EMPTY_BULLET_LINE_REGEX.matches(entireLine)) {
                        val before = oldText.substring(0, lineStart)
                        val after = oldText.substring(lineEnd)
                        val updatedText = before + after
                        return TextFieldValue(
                            text = updatedText,
                            selection = TextRange(lineStart)
                        )
                    }

                    // Rule 1: Enter on bullet point row -> create a new bullet point
                    val normalizedBullet = if (prefix.endsWith(" ")) prefix else "$prefix "
                    val textBeforeCursor = oldText.substring(0, oldCursor)
                    val textAfterCursor = oldText.substring(oldMax)
                    val updatedText = textBeforeCursor + "\n" + normalizedBullet + textAfterCursor
                    val newCursor = oldCursor + 1 + normalizedBullet.length
                    return TextFieldValue(
                        text = updatedText,
                        selection = TextRange(newCursor)
                    )
                }
            }
        }

        // Case 2: Backspace pressed on an empty bullet line
        if (newText.length < oldText.length && oldValue.selection.collapsed) {
            val oldCursor = oldValue.selection.min
            val lineStart = if (oldCursor == 0) 0 else {
                val idx = oldText.lastIndexOf('\n', oldCursor - 1)
                if (idx == -1) 0 else idx + 1
            }
            val lineEnd = oldText.indexOf('\n', oldCursor).let {
                if (it == -1) oldText.length else it
            }
            val entireLine = oldText.substring(lineStart, lineEnd)

            // Rule 3: Backspace on empty bullet row must remove it
            if (EMPTY_BULLET_LINE_REGEX.matches(entireLine) && oldCursor > lineStart) {
                val before = oldText.substring(0, lineStart)
                val after = oldText.substring(lineEnd)
                val updatedText = before + after
                return TextFieldValue(
                    text = updatedText,
                    selection = TextRange(lineStart)
                )
            }
        }

        return null
    }

    /**
     * Handles key events for hardware and emulator keyboards.
     * Returns true if the key event was consumed.
     */
    fun handleKeyEvent(
        event: KeyEvent,
        currentValue: TextFieldValue,
        onUpdateValue: (TextFieldValue) -> Unit
    ): Boolean {
        if (event.type != KeyEventType.KeyDown) return false

        val text = currentValue.text
        val cursor = currentValue.selection.min
        val lineStart = if (cursor == 0) 0 else {
            val idx = text.lastIndexOf('\n', cursor - 1)
            if (idx == -1) 0 else idx + 1
        }
        val lineEnd = text.indexOf('\n', cursor).let {
            if (it == -1) text.length else it
        }
        val currentLine = text.substring(lineStart, lineEnd)

        // Rule 3: Backspace on empty bullet row
        if (event.key == Key.Backspace) {
            if (EMPTY_BULLET_LINE_REGEX.matches(currentLine) && cursor > lineStart) {
                val before = text.substring(0, lineStart)
                val after = text.substring(lineEnd)
                onUpdateValue(
                    TextFieldValue(
                        text = before + after,
                        selection = TextRange(lineStart)
                    )
                )
                return true
            }
        }

        // Rule 1 & 2: Enter on bullet point row
        if (event.key == Key.Enter || event.key == Key.NumPadEnter) {
            if (!event.isShiftPressed) {
                val lineBeforeCursor = text.substring(lineStart, cursor)
                val bulletMatch = BULLET_PREFIX_REGEX.find(lineBeforeCursor)
                if (bulletMatch != null) {
                    val prefix = bulletMatch.value

                    // Rule 2: Double enter on empty bullet point -> exit bullet points
                    if (EMPTY_BULLET_LINE_REGEX.matches(currentLine)) {
                        val before = text.substring(0, lineStart)
                        val after = text.substring(lineEnd)
                        onUpdateValue(
                            TextFieldValue(
                                text = before + after,
                                selection = TextRange(lineStart)
                            )
                        )
                        return true
                    }

                    // Rule 1: Enter creates new bullet point
                    val normalizedBullet = if (prefix.endsWith(" ")) prefix else "$prefix "
                    val textBefore = text.substring(0, cursor)
                    val textAfter = text.substring(currentValue.selection.max)
                    val updatedText = textBefore + "\n" + normalizedBullet + textAfter
                    val newCursor = cursor + 1 + normalizedBullet.length
                    onUpdateValue(
                        TextFieldValue(
                            text = updatedText,
                            selection = TextRange(newCursor)
                        )
                    )
                    return true
                }
            }
        }

        return false
    }
}
