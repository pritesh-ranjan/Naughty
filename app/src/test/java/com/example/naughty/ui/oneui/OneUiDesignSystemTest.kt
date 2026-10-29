package com.example.naughty.ui.oneui

import androidx.compose.ui.graphics.Color
import com.example.naughty.ui.oneui.theme.OneUiDark
import com.example.naughty.ui.oneui.theme.OneUiDimens
import com.example.naughty.ui.oneui.theme.OneUiLight
import com.example.naughty.ui.oneui.theme.OneUiShapes
import com.example.naughty.ui.oneui.theme.OneUiType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OneUiDesignSystemTest {

    @Test
    fun testOneUiDarkOledCompliance() {
        // Dark theme background must be pure pitch black (AMOLED 0xFF000000)
        assertEquals(Color(0xFF000000), OneUiDark.background)
        assertTrue("Dark theme must report isDark=true", OneUiDark.isDark)
        assertNotNull(OneUiDark.surface)
        assertNotNull(OneUiDark.surfaceElevated)
        assertNotNull(OneUiDark.divider)
    }

    @Test
    fun testOneUiLightCompliance() {
        assertTrue("Light theme must report isDark=false", !OneUiLight.isDark)
        assertEquals(Color(0xFFF2F2F7), OneUiLight.background)
        assertEquals(Color(0xFFFFFFFF), OneUiLight.surface)
    }

    @Test
    fun testOneUiShapesContract() {
        assertNotNull(OneUiShapes.card)
        assertNotNull(OneUiShapes.inner)
        assertNotNull(OneUiShapes.button)
        assertNotNull(OneUiShapes.pill)
        assertNotNull(OneUiShapes.sheet)
        assertNotNull(OneUiShapes.dialog)
        assertNotNull(OneUiShapes.menu)
        assertNotNull(OneUiShapes.badge)
    }

    @Test
    fun testOneUiDimensContract() {
        assertEquals(OneUiDimens.screenGutter, OneUiDimens.horizontalMargin)
        assertTrue(OneUiDimens.dockHeight.value >= 56f)
        assertTrue(OneUiDimens.rowMinHeight.value >= 48f)
    }

    @Test
    fun testOneUiTypographyMaterialMapping() {
        val materialType = OneUiType.toMaterial()
        assertNotNull(materialType.headlineLarge)
        assertNotNull(materialType.titleLarge)
        assertNotNull(materialType.bodyLarge)
        assertNotNull(materialType.bodyMedium)
        assertNotNull(materialType.labelSmall)
    }
}
