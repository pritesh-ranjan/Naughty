package com.example.naughty.ui.oneui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object OneUiShapes {
    val card = RoundedCornerShape(24.dp)       // One UI island cards
    val inner = RoundedCornerShape(16.dp)      // Thumbnails, nested elements
    val button = RoundedCornerShape(20.dp)     // Pill buttons
    val pill = RoundedCornerShape(50)          // Fully rounded pills, dock, search
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp) // Bottom sheets
    val dialog = RoundedCornerShape(26.dp)     // One UI dialog squircle
    val menu = RoundedCornerShape(18.dp)       // Dropdown popup menus
    val badge = RoundedCornerShape(50)         // Circular icon badges

    fun toMaterialShapes(): Shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = inner,
        large = card,
        extraLarge = sheet
    )
}

object OneUiDimens {
    val screenGutter = 14.dp         // Side padding for card islands
    val horizontalMargin = screenGutter
    val cardPadding = 18.dp          // Padding inside standard card
    val groupSpacing = 14.dp         // Vertical spacing between card groups
    val rowMinHeight = 56.dp         // Standard list item row minimum height
    val dividerInsetWithIcon = 64.dp // Hairline divider indent skipping icon
    val dividerInsetWithoutIcon = 16.dp
    val dockHeight = 56.dp           // Floating search + add dock height
    val iconBadgeSize = 34.dp        // Leading icon circular badge size
    val iconSize = 18.dp             // Icon size inside badge
}
