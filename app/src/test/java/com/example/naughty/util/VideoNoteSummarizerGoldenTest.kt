package com.example.naughty.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoNoteSummarizerGoldenTest {

    @Test
    fun goldenFixture_povTreated_lexicalAndMiniLm() {
        val fixture = VideoFixtureLoader.load("pov_treated")
        for (provider in listOf(null, fixture.similarityProvider)) {
            val summary = VideoNoteSummarizer.summarizeToCrispNote(
                subject = null,
                accompanyingText = null,
                ocrFrames = fixture.frames,
                transcript = fixture.transcript,
                audioClassification = fixture.classification,
                similarityProvider = provider
            )

            assertEquals("POV: How women want to be treated", summary.title)
            val bullets = summary.content.lines().filter { it.startsWith("- ") }
            assertFalse(bullets.any { it.equals("- Be treated", ignoreCase = true) })
            assertFalse(summary.content.contains("8AMANJOT", ignoreCase = true))
            assertFalse(summary.content.contains("Nil Gilel", ignoreCase = true))
            assertFalse(summary.content.contains("that's what I'm saying", ignoreCase = true))
            assertFalse(summary.content.contains("what the f", ignoreCase = true))
            assertFalse(summary.content.contains("HowN", ignoreCase = false))
            assertTrue(summary.content.contains("controlling", ignoreCase = true))
            assertTrue(summary.content.contains("go make me a sandwich", ignoreCase = true))
            assertEquals(2, bullets.size)
        }
    }

    @Test
    fun goldenFixture_homeSmellGood_lexicalAndMiniLm() {
        val fixture = VideoFixtureLoader.load("home_smell_good")
        for (provider in listOf(null, fixture.similarityProvider)) {
            val summary = VideoNoteSummarizer.summarizeToCrispNote(
                subject = null,
                accompanyingText = null,
                ocrFrames = fixture.frames,
                transcript = fixture.transcript,
                audioClassification = fixture.classification,
                similarityProvider = provider
            )

            assertEquals("5 Ways to Make Your Home Smell Good", summary.title)

            assertFalse(summary.content.contains("HO EeoD"))
            assertFalse(summary.content.contains("Heles"))
            assertFalse(summary.content.contains("Sted Bali"))
            assertFalse(summary.content.contains("America's #1"))
            assertFalse(summary.content.contains("FUELING FUTURE SCIENTISTS"))
            assertFalse(summary.content.contains("LAUNCH YOUR BAKING SODA"))
            assertFalse(summary.content.contains("2 LB"))
            assertFalse(summary.content.contains("5 WAYS TO MAKE YOUR"))
            assertFalse(summary.content.contains("Five simple ways"))
            assertFalse(summary.content.contains("Instead of just covering"))
            assertFalse(summary.content.contains("Keeps everything contained"))
            assertFalse(summary.content.contains("hidden source of bad smells"))

            val bullets = summary.content.lines().filter { it.startsWith("- ") }
            assertEquals("Expected exactly 5 step bullets, got:\n${summary.content}", 5, bullets.size)
            assertTrue(bullets[0].contains("air purifier", ignoreCase = true))
            assertTrue(bullets[1].contains("dryer sheets", ignoreCase = true))
            assertFalse(bullets[1].contains("tucking", ignoreCase = true))
            assertFalse(bullets[1].contains("decorated", ignoreCase = true))
            assertTrue(bullets[2].contains("simmer pot", ignoreCase = true))
            assertTrue(bullets[3].contains("baking soda", ignoreCase = true))
            assertTrue(bullets[4].contains("garbage disposal", ignoreCase = true))
        }
    }
}
