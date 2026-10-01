package com.example.naughty.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

enum class PaperPattern {
    NONE,
    GRID,
    DOTS,
    LINES
}

data class NoteTheme(
    val id: String,
    val name: String,
    val subtitle: String,
    val isDark: Boolean,
    val canvasBackground: Color,
    val surface: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val secondaryAccent: Color,
    val highlightColor: Color,
    val highlightTextColor: Color,
    val linkColor: Color,
    val cursorColor: Color,
    val paperPattern: PaperPattern,
    val gridLineColor: Color,
    val fontFamily: FontFamily,
    val titleFontFamily: FontFamily
)

object NoteThemeRegistry {

    val Mononoke = NoteTheme(
        id = "mononoke",
        name = "Mononoke",
        subtitle = "Dark • Monospace",
        isDark = true,
        canvasBackground = Color(0xFF101316),
        surface = Color(0xFF181C20),
        border = Color(0xFF262C34),
        textPrimary = Color(0xFFE2E8F0),
        textSecondary = Color(0xFF8896A6),
        textMuted = Color(0xFF5D6B7B),
        accent = Color(0xFF4ADE80),
        secondaryAccent = Color(0xFFFF6B6B),
        highlightColor = Color(0xFFE5484D),
        highlightTextColor = Color(0xFFFFFFFF),
        linkColor = Color(0xFF4ADE80),
        cursorColor = Color(0xFF4ADE80),
        paperPattern = PaperPattern.GRID,
        gridLineColor = Color(0xFF1E242B),
        fontFamily = FontFamily.Monospace,
        titleFontFamily = FontFamily.Monospace
    )

    val Gundam = NoteTheme(
        id = "gundam",
        name = "Gundam",
        subtitle = "Light • Blueprint Grid",
        isDark = false,
        canvasBackground = Color(0xFFF0F3F8),
        surface = Color(0xFFFFFFFF),
        border = Color(0xFFD4DCF0),
        textPrimary = Color(0xFF1E293B),
        textSecondary = Color(0xFF64748B),
        textMuted = Color(0xFF94A3B8),
        accent = Color(0xFF2563EB),
        secondaryAccent = Color(0xFFF59E0B),
        highlightColor = Color(0xFFFDE68A),
        highlightTextColor = Color(0xFF92400E),
        linkColor = Color(0xFFF43F5E),
        cursorColor = Color(0xFF2563EB),
        paperPattern = PaperPattern.GRID,
        gridLineColor = Color(0xFFE2E8F5),
        fontFamily = FontFamily.Monospace,
        titleFontFamily = FontFamily.Monospace
    )

    val TokyoDrift = NoteTheme(
        id = "tokyo_drift",
        name = "Tokyo Drift",
        subtitle = "Dark • Cyberpunk",
        isDark = true,
        canvasBackground = Color(0xFF090A0F),
        surface = Color(0xFF11141E),
        border = Color(0xFF1E2538),
        textPrimary = Color(0xFFF0F9FF),
        textSecondary = Color(0xFF7085A0),
        textMuted = Color(0xFF475569),
        accent = Color(0xFF00F5D4),
        secondaryAccent = Color(0xFF39FF14),
        highlightColor = Color(0xFFFF007F),
        highlightTextColor = Color(0xFFFFFFFF),
        linkColor = Color(0xFF00F5D4),
        cursorColor = Color(0xFF00F5D4),
        paperPattern = PaperPattern.GRID,
        gridLineColor = Color(0xFF161B29),
        fontFamily = FontFamily.Monospace,
        titleFontFamily = FontFamily.Monospace
    )

    val Vendetta = NoteTheme(
        id = "vendetta",
        name = "Vendetta",
        subtitle = "Dark • Typewriter",
        isDark = true,
        canvasBackground = Color(0xFF0F0F0F),
        surface = Color(0xFF171717),
        border = Color(0xFF2B2B2B),
        textPrimary = Color(0xFFEBE6DC),
        textSecondary = Color(0xFF8C877D),
        textMuted = Color(0xFF5A564E),
        accent = Color(0xFFDC2626),
        secondaryAccent = Color(0xFFEF4444),
        highlightColor = Color(0xFF7F1D1D),
        highlightTextColor = Color(0xFFFEE2E2),
        linkColor = Color(0xFFEF4444),
        cursorColor = Color(0xFFDC2626),
        paperPattern = PaperPattern.GRID,
        gridLineColor = Color(0xFF1F1F1F),
        fontFamily = FontFamily.Serif,
        titleFontFamily = FontFamily.Serif
    )

    val Piccolo = NoteTheme(
        id = "piccolo",
        name = "Piccolo",
        subtitle = "Light • Anime Mint",
        isDark = false,
        canvasBackground = Color(0xFFEBF5EE),
        surface = Color(0xFFF8FCF9),
        border = Color(0xFFC8E2D1),
        textPrimary = Color(0xFF162E20),
        textSecondary = Color(0xFF4F755E),
        textMuted = Color(0xFF82A692),
        accent = Color(0xFF7C3AED),
        secondaryAccent = Color(0xFF16A34A),
        highlightColor = Color(0xFFCCFF00),
        highlightTextColor = Color(0xFF162E20),
        linkColor = Color(0xFF7C3AED),
        cursorColor = Color(0xFF7C3AED),
        paperPattern = PaperPattern.GRID,
        gridLineColor = Color(0xFFD6EDE0),
        fontFamily = FontFamily.Monospace,
        titleFontFamily = FontFamily.Monospace
    )

    val A24 = NoteTheme(
        id = "a24",
        name = "A24",
        subtitle = "Dark • Indie Noir",
        isDark = true,
        canvasBackground = Color(0xFF0E0E10),
        surface = Color(0xFF16171B),
        border = Color(0xFF262832),
        textPrimary = Color(0xFFF3F4F6),
        textSecondary = Color(0xFF868B99),
        textMuted = Color(0xFF555966),
        accent = Color(0xFF10B981),
        secondaryAccent = Color(0xFFF59E0B),
        highlightColor = Color(0xFFD97706),
        highlightTextColor = Color(0xFFFFFFFF),
        linkColor = Color(0xFFF59E0B),
        cursorColor = Color(0xFFF59E0B),
        paperPattern = PaperPattern.GRID,
        gridLineColor = Color(0xFF1E2028),
        fontFamily = FontFamily.Monospace,
        titleFontFamily = FontFamily.Monospace
    )

    val Brave = NoteTheme(
        id = "brave",
        name = "Brave",
        subtitle = "Light • Editorial Serif",
        isDark = false,
        canvasBackground = Color(0xFFF4EFE6),
        surface = Color(0xFFFCFAF6),
        border = Color(0xFFDED5C5),
        textPrimary = Color(0xFF231B15),
        textSecondary = Color(0xFF6E5F52),
        textMuted = Color(0xFF9E8E80),
        accent = Color(0xFFC2410C),
        secondaryAccent = Color(0xFFD97706),
        highlightColor = Color(0xFFFFD1BD),
        highlightTextColor = Color(0xFF431407),
        linkColor = Color(0xFFC2410C),
        cursorColor = Color(0xFFC2410C),
        paperPattern = PaperPattern.GRID,
        gridLineColor = Color(0xFFEBE3D3),
        fontFamily = FontFamily.Serif,
        titleFontFamily = FontFamily.Serif
    )

    val Agrabah = NoteTheme(
        id = "agrabah",
        name = "Agrabah",
        subtitle = "Dark • Arabian Night",
        isDark = true,
        canvasBackground = Color(0xFF120C21),
        surface = Color(0xFF1A1230),
        border = Color(0xFF2F2154),
        textPrimary = Color(0xFFF7F5FC),
        textSecondary = Color(0xFF9F8DBF),
        textMuted = Color(0xFF645380),
        accent = Color(0xFFFBBF24),
        secondaryAccent = Color(0xFF06B6D4),
        highlightColor = Color(0xFFE11D48),
        highlightTextColor = Color(0xFFFFFFFF),
        linkColor = Color(0xFF06B6D4),
        cursorColor = Color(0xFFFBBF24),
        paperPattern = PaperPattern.GRID,
        gridLineColor = Color(0xFF241944),
        fontFamily = FontFamily.Monospace,
        titleFontFamily = FontFamily.Monospace
    )

    val Dracula = NoteTheme(
        id = "dracula",
        name = "Dracula",
        subtitle = "Dark • Gothic Slate",
        isDark = true,
        canvasBackground = Color(0xFF1E1F29),
        surface = Color(0xFF282A36),
        border = Color(0xFF44475A),
        textPrimary = Color(0xFFF8F8F2),
        textSecondary = Color(0xFF9CA3AF),
        textMuted = Color(0xFF6272A4),
        accent = Color(0xFFFF79C6),
        secondaryAccent = Color(0xFF8BE9FD),
        highlightColor = Color(0xFFBD93F9),
        highlightTextColor = Color(0xFF1E1F29),
        linkColor = Color(0xFF50FA7B),
        cursorColor = Color(0xFF50FA7B),
        paperPattern = PaperPattern.GRID,
        gridLineColor = Color(0xFF333647),
        fontFamily = FontFamily.Monospace,
        titleFontFamily = FontFamily.Monospace
    )

    val Nord = NoteTheme(
        id = "nord",
        name = "Nord",
        subtitle = "Dark • Arctic Frost",
        isDark = true,
        canvasBackground = Color(0xFF20242C),
        surface = Color(0xFF2E3440),
        border = Color(0xFF3B4252),
        textPrimary = Color(0xFFECEFF4),
        textSecondary = Color(0xFF909DB6),
        textMuted = Color(0xFF616E88),
        accent = Color(0xFF88C0D0),
        secondaryAccent = Color(0xFFA3BE8C),
        highlightColor = Color(0xFFEBCB8B),
        highlightTextColor = Color(0xFF2E3440),
        linkColor = Color(0xFF81A1C1),
        cursorColor = Color(0xFF88C0D0),
        paperPattern = PaperPattern.GRID,
        gridLineColor = Color(0xFF384050),
        fontFamily = FontFamily.Monospace,
        titleFontFamily = FontFamily.Monospace
    )

    val Gruvbox = NoteTheme(
        id = "gruvbox",
        name = "Gruvbox",
        subtitle = "Dark • Retro Groove",
        isDark = true,
        canvasBackground = Color(0xFF1D2021),
        surface = Color(0xFF282828),
        border = Color(0xFF3C3836),
        textPrimary = Color(0xFFEBDBB2),
        textSecondary = Color(0xFFA89984),
        textMuted = Color(0xFF665C54),
        accent = Color(0xFFFE8019),
        secondaryAccent = Color(0xFFFABD2F),
        highlightColor = Color(0xFFB8BB26),
        highlightTextColor = Color(0xFF282828),
        linkColor = Color(0xFF83A598),
        cursorColor = Color(0xFFFE8019),
        paperPattern = PaperPattern.GRID,
        gridLineColor = Color(0xFF32302F),
        fontFamily = FontFamily.Monospace,
        titleFontFamily = FontFamily.Monospace
    )

    val Solarized = NoteTheme(
        id = "solarized",
        name = "Solarized",
        subtitle = "Light • Terminal Paper",
        isDark = false,
        canvasBackground = Color(0xFFF3EDD9),
        surface = Color(0xFFFDF6E3),
        border = Color(0xFFE6DEC9),
        textPrimary = Color(0xFF073642),
        textSecondary = Color(0xFF586E75),
        textMuted = Color(0xFF93A1A1),
        accent = Color(0xFFB58900),
        secondaryAccent = Color(0xFFCB4B16),
        highlightColor = Color(0xFF2AA198),
        highlightTextColor = Color(0xFFFFFFFF),
        linkColor = Color(0xFF268BD2),
        cursorColor = Color(0xFF268BD2),
        paperPattern = PaperPattern.GRID,
        gridLineColor = Color(0xFFEFE7D0),
        fontFamily = FontFamily.Monospace,
        titleFontFamily = FontFamily.Monospace
    )

    val DotMatrix = NoteTheme(
        id = "dot_matrix",
        name = "Dot Matrix",
        subtitle = "Dark • CRT Phosphor",
        isDark = true,
        canvasBackground = Color(0xFF070B08),
        surface = Color(0xFF0D140E),
        border = Color(0xFF1B2D1E),
        textPrimary = Color(0xFF33FF66),
        textSecondary = Color(0xFF22AA44),
        textMuted = Color(0xFF135E26),
        accent = Color(0xFF00FF99),
        secondaryAccent = Color(0xFF66FFB2),
        highlightColor = Color(0xFF005522),
        highlightTextColor = Color(0xFF33FF66),
        linkColor = Color(0xFF00FF99),
        cursorColor = Color(0xFF33FF66),
        paperPattern = PaperPattern.DOTS,
        gridLineColor = Color(0xFF142417),
        fontFamily = FontFamily.Monospace,
        titleFontFamily = FontFamily.Monospace
    )

    val Flexoki = NoteTheme(
        id = "flexoki",
        name = "Flexoki",
        subtitle = "Light • Inky Wove Paper",
        isDark = false,
        canvasBackground = Color(0xFFEBE7DE),
        surface = Color(0xFFF7F5EE),
        border = Color(0xFFD6D1C4),
        textPrimary = Color(0xFF100F0F),
        textSecondary = Color(0xFF6F6E69),
        textMuted = Color(0xFF878580),
        accent = Color(0xFF24837B),
        secondaryAccent = Color(0xFFAD8301),
        highlightColor = Color(0xFFF8D6D1),
        highlightTextColor = Color(0xFF5A1D16),
        linkColor = Color(0xFF205EA6),
        cursorColor = Color(0xFF205EA6),
        paperPattern = PaperPattern.LINES,
        gridLineColor = Color(0xFFE0DDD1),
        fontFamily = FontFamily.Serif,
        titleFontFamily = FontFamily.Serif
    )

    val allThemes: List<NoteTheme> = listOf(
        Mononoke,
        Gundam,
        TokyoDrift,
        Vendetta,
        Piccolo,
        A24,
        Brave,
        Agrabah,
        Dracula,
        Nord,
        Gruvbox,
        Solarized,
        DotMatrix,
        Flexoki
    )

    private var lastAssignedThemeId: String? = null

    /**
     * Returns a random theme ID from available themes, guaranteeing it differs from the last assigned theme.
     */
    @Synchronized
    fun getRandomThemeId(excludeThemeId: String? = lastAssignedThemeId): String {
        val candidates = allThemes.filter { it.id != excludeThemeId }
        val chosen = (candidates.ifEmpty { allThemes }).random().id
        lastAssignedThemeId = chosen
        return chosen
    }

    /**
     * Resolves a theme by id, with smooth fallback mapping for legacy color-based themes.
     */
    fun getTheme(id: String?, isDarkDefault: Boolean = true): NoteTheme {
        val key = id?.lowercase()?.trim() ?: ""
        return when (key) {
            "mononoke" -> Mononoke
            "gundam" -> Gundam
            "tokyo_drift", "tokyodrift" -> TokyoDrift
            "vendetta" -> Vendetta
            "piccolo" -> Piccolo
            "a24" -> A24
            "brave" -> Brave
            "agrabah" -> Agrabah
            "dracula" -> Dracula
            "nord" -> Nord
            "gruvbox" -> Gruvbox
            "solarized" -> Solarized
            "dot_matrix", "dotmatrix" -> DotMatrix
            "flexoki" -> Flexoki

            // Legacy backward-compatibility mappings
            "peach" -> Brave
            "mint", "green", "sage" -> Piccolo
            "coral", "red", "melon" -> TokyoDrift
            "blue", "sky" -> Gundam
            "yellow", "butter" -> A24
            "stone", "gray", "putty" -> Vendetta
            "lavender", "purple" -> Agrabah

            else -> if (isDarkDefault) Mononoke else Gundam
        }
    }
}
