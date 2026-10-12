package com.example.naughty.ui.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BulletTextHelperTest {

    @Test
    fun testEnterOnBulletRowCreatesNewBulletPoint() {
        val oldVal = TextFieldValue(text = "• First point", selection = TextRange(13))
        val newVal = TextFieldValue(text = "• First point\n", selection = TextRange(14))

        val result = BulletTextHelper.handleValueChange(oldVal, newVal)
        assertNotNull(result)
        assertEquals("• First point\n• ", result!!.text)
        assertEquals(16, result.selection.start)
        assertEquals(16, result.selection.end)
    }

    @Test
    fun testEnterOnDashBulletRowCreatesNewBulletPoint() {
        val oldVal = TextFieldValue(text = "- First point", selection = TextRange(13))
        val newVal = TextFieldValue(text = "- First point\n", selection = TextRange(14))

        val result = BulletTextHelper.handleValueChange(oldVal, newVal)
        assertNotNull(result)
        assertEquals("- First point\n- ", result!!.text)
        assertEquals(16, result.selection.start)
    }

    @Test
    fun testEnterInMiddleOfBulletRowSplitsAndCreatesNewBullet() {
        val oldVal = TextFieldValue(text = "• First point", selection = TextRange(7)) // after "• First"
        val newVal = TextFieldValue(text = "• First\n point", selection = TextRange(8))

        val result = BulletTextHelper.handleValueChange(oldVal, newVal)
        assertNotNull(result)
        assertEquals("• First\n•  point", result!!.text)
        assertEquals(10, result.selection.start)
    }

    @Test
    fun testDoubleEnterOnEmptyBulletExitsBulletPoints() {
        // Line 1 has text, Line 2 is an empty bullet "• "
        val oldVal = TextFieldValue(text = "• First point\n• ", selection = TextRange(16))
        val newVal = TextFieldValue(text = "• First point\n• \n", selection = TextRange(17))

        val result = BulletTextHelper.handleValueChange(oldVal, newVal)
        assertNotNull(result)
        // Empty bullet is removed, exiting bullet mode!
        assertEquals("• First point\n", result!!.text)
        assertEquals(14, result.selection.start)
    }

    @Test
    fun testDoubleEnterOnSingleEmptyBulletLineExits() {
        val oldVal = TextFieldValue(text = "• ", selection = TextRange(2))
        val newVal = TextFieldValue(text = "• \n", selection = TextRange(3))

        val result = BulletTextHelper.handleValueChange(oldVal, newVal)
        assertNotNull(result)
        assertEquals("", result!!.text)
        assertEquals(0, result.selection.start)
    }

    @Test
    fun testBackspaceOnEmptyBulletRowRemovesIt() {
        val oldVal = TextFieldValue(text = "• First point\n• ", selection = TextRange(16))
        val newVal = TextFieldValue(text = "• First point\n•", selection = TextRange(15))

        val result = BulletTextHelper.handleValueChange(oldVal, newVal)
        assertNotNull(result)
        assertEquals("• First point\n", result!!.text)
        assertEquals(14, result.selection.start)
    }

    @Test
    fun testBackspaceOnSingleEmptyBulletRemovesIt() {
        val oldVal = TextFieldValue(text = "• ", selection = TextRange(2))
        val newVal = TextFieldValue(text = "•", selection = TextRange(1))

        val result = BulletTextHelper.handleValueChange(oldVal, newVal)
        assertNotNull(result)
        assertEquals("", result!!.text)
        assertEquals(0, result.selection.start)
    }

    @Test
    fun testNormalTextNotAffectedByBulletHandler() {
        val oldVal = TextFieldValue(text = "Hello world", selection = TextRange(11))
        val newVal = TextFieldValue(text = "Hello world\n", selection = TextRange(12))

        val result = BulletTextHelper.handleValueChange(oldVal, newVal)
        assertNull(result)
    }

    @Test
    fun testTypingCharacterOnEmptyBulletDoesNotTriggerBulletRule() {
        val oldVal = TextFieldValue(text = "• First point\n• ", selection = TextRange(16))
        val newVal = TextFieldValue(text = "• First point\n• a", selection = TextRange(17))

        val result = BulletTextHelper.handleValueChange(oldVal, newVal)
        assertNull(result)
    }

    @Test
    fun testTypingCharacterOnSingleBulletDoesNotTriggerBulletRule() {
        val oldVal = TextFieldValue(text = "• ", selection = TextRange(2))
        val newVal = TextFieldValue(text = "• h", selection = TextRange(3))

        val result = BulletTextHelper.handleValueChange(oldVal, newVal)
        assertNull(result)
    }

    @Test
    fun testAddingMultipleBulletPointsInSequence() {
        // Step 1: Start on bullet point 1
        var current = TextFieldValue(text = "• Point 1", selection = TextRange(9))

        // Step 2: Press Enter on bullet 1
        var next = TextFieldValue(text = "• Point 1\n", selection = TextRange(10))
        var transformed = BulletTextHelper.handleValueChange(current, next)
        assertNotNull(transformed)
        assertEquals("• Point 1\n• ", transformed!!.text)
        assertEquals(12, transformed.selection.start)
        current = transformed

        // Step 3: Type character 'P' on bullet 2
        next = TextFieldValue(text = "• Point 1\n• P", selection = TextRange(13))
        transformed = BulletTextHelper.handleValueChange(current, next)
        assertNull(transformed) // Must NOT eat 'P' or delete bullet!
        current = next

        // Step 4: Complete word "Point 2"
        current = TextFieldValue(text = "• Point 1\n• Point 2", selection = TextRange(19))

        // Step 5: Press Enter on bullet 2
        next = TextFieldValue(text = "• Point 1\n• Point 2\n", selection = TextRange(20))
        transformed = BulletTextHelper.handleValueChange(current, next)
        assertNotNull(transformed)
        assertEquals("• Point 1\n• Point 2\n• ", transformed!!.text)
        assertEquals(22, transformed.selection.start)
        current = transformed

        // Step 6: Press Enter again on empty bullet 3 (double Enter to exit)
        next = TextFieldValue(text = "• Point 1\n• Point 2\n• \n", selection = TextRange(23))
        transformed = BulletTextHelper.handleValueChange(current, next)
        assertNotNull(transformed)
        assertEquals("• Point 1\n• Point 2\n", transformed!!.text)
        assertEquals(20, transformed.selection.start)
    }
}
