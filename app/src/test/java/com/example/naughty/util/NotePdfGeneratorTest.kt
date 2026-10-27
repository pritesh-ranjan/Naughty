package com.example.naughty.util

import androidx.compose.ui.graphics.toArgb
import com.example.naughty.ui.theme.NoteThemeRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotePdfGeneratorTest {

    @Test
    fun testSanitizeFileName_defaultAndBlankTitles() {
        assertEquals("Naughty_Note", NotePdfGenerator.sanitizeFileName(""))
        assertEquals("Naughty_Note", NotePdfGenerator.sanitizeFileName("   "))
        assertEquals("Naughty_Note", NotePdfGenerator.sanitizeFileName("Untitled"))
        assertEquals("Naughty_Note", NotePdfGenerator.sanitizeFileName("untitled"))
        assertEquals("Naughty_Note", NotePdfGenerator.sanitizeFileName("New thought"))
        assertEquals("Naughty_Note", NotePdfGenerator.sanitizeFileName("new thought"))
    }

    @Test
    fun testSanitizeFileName_specialCharactersAndSpaces() {
        assertEquals(
            "Meeting_Notes_with_Team",
            NotePdfGenerator.sanitizeFileName("Meeting Notes with Team")
        )
        assertEquals(
            "Grocery_List_Apples_Oranges_2026",
            NotePdfGenerator.sanitizeFileName("Grocery List: Apples & Oranges! (2026)")
        )
        assertEquals(
            "Project_Alpha_v12",
            NotePdfGenerator.sanitizeFileName("Project #Alpha / v1.2*")
        )
    }

    @Test
    fun testSanitizeFileName_lengthCapped() {
        val longTitle = "This is an extraordinarily long note title that definitely exceeds forty characters by quite a bit"
        val sanitized = NotePdfGenerator.sanitizeFileName(longTitle)
        assertTrue(sanitized.length <= 40)
        assertFalse(sanitized.endsWith("_"))
    }

    @Test
    fun testEnsurePrintContrast_darkOrMidTonesPreserved() {
        // Red color #DC2626 has luminance ~0.24, well below 0.60 threshold
        val redColor = 0xFFDC2626.toInt()
        assertEquals(redColor, NotePdfGenerator.ensurePrintContrast(redColor))

        // Deep blue #2563EB has luminance ~0.17
        val blueColor = 0xFF2563EB.toInt()
        assertEquals(blueColor, NotePdfGenerator.ensurePrintContrast(blueColor))
    }

    @Test
    fun testEnsurePrintContrast_brightColorsDarkenedForPrint() {
        // Neon cyan #00F5D4 has luminance ~0.74, exceeds 0.60 threshold
        val neonCyan = 0xFF00F5D4.toInt()
        val darkenedCyan = NotePdfGenerator.ensurePrintContrast(neonCyan)
        assertTrue("Darkened color must differ from bright neon", darkenedCyan != neonCyan)

        // Verify luminance of darkened color is within print-readable bounds
        val r = (darkenedCyan ushr 16) and 0xFF
        val g = (darkenedCyan ushr 8) and 0xFF
        val b = darkenedCyan and 0xFF
        val lum = (0.2126 * (r / 255.0)) + (0.7152 * (g / 255.0)) + (0.0722 * (b / 255.0))
        assertTrue("Luminance after print adaptation should be <= 0.60, got $lum", lum <= 0.60)
    }

    @Test
    fun testResolvePdfPalette_lightThemePreservesSurface() {
        val lightTheme = NoteThemeRegistry.Gundam
        val palette = NotePdfGenerator.resolvePdfPalette(lightTheme)

        assertFalse(palette.isDarkAdapted)
        // Background should match theme's surface
        assertEquals(lightTheme.surface.toArgb(), palette.background)
        assertEquals(lightTheme.textPrimary.toArgb(), palette.textPrimary)
    }

    @Test
    fun testResolvePdfPalette_darkThemeAdaptsToPrintFriendly() {
        val darkTheme = NoteThemeRegistry.Mononoke
        val palette = NotePdfGenerator.resolvePdfPalette(darkTheme)

        assertTrue(palette.isDarkAdapted)
        // Warm paper white background
        assertEquals(0xFFFCFCFA.toInt(), palette.background)
        // Slate dark text for readability
        assertEquals(0xFF1E293B.toInt(), palette.textPrimary)
        // Muted gray
        assertEquals(0xFF94A3B8.toInt(), palette.textMuted)
    }
}
