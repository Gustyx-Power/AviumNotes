package id.avium.aviumnotes.ui.theme

import androidx.compose.ui.graphics.Color

// MIUI + OneUI inspired color palette - More vibrant and modern
object NoteColors {
    val White = Color(0xFFFFFFFF)
    val LightRed = Color(0xFFFFE5E5)        // Softer red
    val LightOrange = Color(0xFFFFEDD5)     // Warm orange
    val LightYellow = Color(0xFFFFF9C4)     // Gentle yellow
    val LightGreen = Color(0xFFD7F5DC)      // Fresh green
    val LightCyan = Color(0xFFD0F5F3)       // Cool cyan
    val LightBlue = Color(0xFFE3F2FD)       // Sky blue
    val LightPurple = Color(0xFFF3E5F5)     // Lavender
    val LightPink = Color(0xFFFCE4EC)       // Rose pink
    val LightGray = Color(0xFFF5F5F5)       // Subtle gray

    val colorsList = listOf(
        White,
        LightRed,
        LightOrange,
        LightYellow,
        LightGreen,
        LightCyan,
        LightBlue,
        LightPurple,
        LightPink,
        LightGray
    )

    fun getColorName(color: Int): String {
        return when (color) {
            White.hashCode() -> "White"
            LightRed.hashCode() -> "Red"
            LightOrange.hashCode() -> "Orange"
            LightYellow.hashCode() -> "Yellow"
            LightGreen.hashCode() -> "Green"
            LightCyan.hashCode() -> "Cyan"
            LightBlue.hashCode() -> "Blue"
            LightPurple.hashCode() -> "Purple"
            LightPink.hashCode() -> "Pink"
            LightGray.hashCode() -> "Gray"
            else -> "White"
        }
    }
}
