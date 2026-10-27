package com.example.naughty.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureShowcaseTest {

    @Test
    fun shouldShowFeatureShowcase_onFirstEverInstall_returnsTrue() {
        // When lastSeenVersionCode is null (app freshly installed), it must always show
        assertTrue(shouldShowFeatureShowcase(lastSeenVersionCode = null, currentVersionCode = 1L))
        assertTrue(shouldShowFeatureShowcase(lastSeenVersionCode = null, currentVersionCode = 34L))
    }

    @Test
    fun shouldShowFeatureShowcase_afterAppUpdate_returnsTrue() {
        // When app is updated from versionCode 33 to 34, it must show
        assertTrue(shouldShowFeatureShowcase(lastSeenVersionCode = 33L, currentVersionCode = 34L))
        assertTrue(shouldShowFeatureShowcase(lastSeenVersionCode = 1L, currentVersionCode = 2L))
        assertTrue(shouldShowFeatureShowcase(lastSeenVersionCode = 20L, currentVersionCode = 34L))
    }

    @Test
    fun shouldShowFeatureShowcase_onSubsequentOpensOfSameVersion_returnsFalse() {
        // When user opens the app again on the same version, it must not pop up again
        assertFalse(shouldShowFeatureShowcase(lastSeenVersionCode = 34L, currentVersionCode = 34L))
        assertFalse(shouldShowFeatureShowcase(lastSeenVersionCode = 2L, currentVersionCode = 2L))
    }

    @Test
    fun shouldShowFeatureShowcase_onDowngradeOrOlderCode_returnsFalse() {
        // In the edge case where an older APK is installed over a newer version
        assertFalse(shouldShowFeatureShowcase(lastSeenVersionCode = 35L, currentVersionCode = 34L))
    }

    @Test
    fun featureShowcaseItems_containsAllRequiredFeatures() {
        val items = getFeatureShowcaseItems()

        // Verify all 11 items are present
        assertTrue("Expected at least 9 showcase items, found ${items.size}", items.size >= 9)

        // 1. App binding
        assertTrue(
            "Missing App-linked notes / app binding",
            items.any { it.title.contains("App-Linked", ignoreCase = true) || it.description.contains("open specific apps", ignoreCase = true) }
        )

        // 2. Circle to search notes
        assertTrue(
            "Missing Circle to Search",
            items.any { it.title.contains("Circle to Search", ignoreCase = true) }
        )

        // 3. Timeline tasks
        assertTrue(
            "Missing Timeline tasks",
            items.any { it.title.contains("Timeline", ignoreCase = true) }
        )

        // 4. Image and audio notes
        assertTrue(
            "Missing Image / Audio notes",
            items.any { it.title.contains("Photo", ignoreCase = true) && it.title.contains("Voice", ignoreCase = true) }
        )

        // 5. Offline OCR within notes & via share
        val ocrItem = items.find { it.title.contains("OCR", ignoreCase = true) }
        println("FOUND OCR ITEM: ${ocrItem?.title} | ${ocrItem?.description}")
        assertTrue(
            "Missing Offline OCR",
            ocrItem != null && (ocrItem.description.contains("share", ignoreCase = true) || ocrItem.description.contains("sharing", ignoreCase = true))
        )

        // 6. Offline voice transcription
        assertTrue(
            "Missing Offline speech to text",
            items.any { it.title.contains("Speech-to-Text", ignoreCase = true) || it.description.contains("transcribe", ignoreCase = true) }
        )

        // 7. @ mentions
        assertTrue(
            "Missing @ mentions",
            items.any { it.title.contains("@", ignoreCase = true) }
        )

        // 8. 100% Offline and Private, no data collection
        assertTrue(
            "Missing 100% Offline and Private",
            items.any { it.title.contains("100% Offline", ignoreCase = true) && it.description.contains("telemetry", ignoreCase = true) }
        )

        // 9. Notable feature: Biometric lock & fast search
        assertTrue(
            "Missing Biometric Lock",
            items.any { it.title.contains("Lock", ignoreCase = true) }
        )

        // Ensure "Floating Quick Notes" and "14 Themes" were removed as requested
        assertFalse(
            "Floating Quick Notes should be removed",
            items.any { it.title.contains("Floating", ignoreCase = true) }
        )
        assertFalse(
            "14 Themes should be removed",
            items.any { it.title.contains("14", ignoreCase = true) || it.title.contains("Themes", ignoreCase = true) }
        )
    }

    @Test
    fun featureShowcaseItems_haveCrispNonEmptyDescriptions() {
        val items = getFeatureShowcaseItems()
        for (item in items) {
            assertTrue("Item ${item.title} has empty title", item.title.isNotBlank())
            assertTrue("Item ${item.title} has empty category", item.category.isNotBlank())
            assertTrue("Item ${item.title} has empty description", item.description.isNotBlank())
            // Ensure descriptions are crisp, concise (under 250 characters), yet meaningful
            assertTrue("Item ${item.title} description is too long (${item.description.length} chars)", item.description.length <= 250)
            assertTrue("Item ${item.title} description is too short (${item.description.length} chars)", item.description.length >= 30)
        }
    }
}
