package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb

data class ContactColorPreset(
    val id: String,
    val label: String,
    val argb: Long
)

object ContactColorPalette {
    val presets: List<ContactColorPreset> = listOf(
        ContactColorPreset("auto", "Automatico", 0L),
        ContactColorPreset("azure", "Blu Elettrico", 0xFF3B82F6L),
        ContactColorPreset("cyan", "Ciano", 0xFF38BDF8L),
        ContactColorPreset("gold", "Oro", 0xFFF59E0BL),
        ContactColorPreset("emerald", "Smeraldo", 0xFF10B981L),
        ContactColorPreset("violet", "Viola", 0xFFA855F7L),
        ContactColorPreset("coral", "Corallo", 0xFFF43F5EL),
        ContactColorPreset("ice", "Bianco Ghiaccio", 0xFFF8FAACL.let { 0xFFF8FAFCL }),
        ContactColorPreset("obsidian", "Nero Profondo", 0xFF070F1FL)
    )

    fun resolveNameColor(argb: Long, defaultColor: Color): Color {
        return if (argb == 0L) defaultColor else Color(argb.toInt())
    }

    /**
     * Ensures readability when rendering a custom contact name color on dark black-blue hero headers.
     */
    fun resolveHeroNameColor(argb: Long): Color {
        if (argb == 0L) return Color.White
        val color = Color(argb.toInt())
        return if (color.luminance() < 0.16f) Color.White else color
    }

    fun toArgbLong(color: Color): Long {
        return color.toArgb().toLong() and 0xFFFFFFFFL
    }
}
