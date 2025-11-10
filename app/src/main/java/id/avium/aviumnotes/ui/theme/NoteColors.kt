package id.avium.aviumnotes.ui.theme

import androidx.compose.ui.graphics.Color

object NoteColors {
    val White = Color(0xFFFFFFFF)
    val LightRed = Color(0xFFF28B82)
    val LightOrange = Color(0xFFFBBC04)
    val LightYellow = Color(0xFFFFF475)
    val LightGreen = Color(0xFFCCFF90)
    val LightCyan = Color(0xFFA7FFEB)
    val LightBlue = Color(0xFFCBF0F8)
    val LightPurple = Color(0xFFAECBFA)
    val LightPink = Color(0xFFFDCFE8)
    val LightGray = Color(0xFFE8EAED)

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
