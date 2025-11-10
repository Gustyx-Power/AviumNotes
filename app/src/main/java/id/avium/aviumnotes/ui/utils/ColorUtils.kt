package id.avium.aviumnotes.ui.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Determine text color based on background luminance
 * Returns black for light backgrounds, white for dark backgrounds
 */
fun getContrastColor(backgroundColor: Color): Color {
    return if (backgroundColor.luminance() > 0.5f) {
        Color.Black  // Light background, use dark text
    } else {
        Color.White  // Dark background, use light text
    }
}
