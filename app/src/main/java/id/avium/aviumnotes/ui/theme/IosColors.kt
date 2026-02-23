package id.avium.aviumnotes.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val IosPeach = Color(0xFFFFD5C6)

val IosBlack: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF000000) else Color(0xFFF2F2F7)

val IosDarkGray: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)

val IosSearchGray: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFF2C2C2E) else Color(0xFFE3E3E8)

val IosTextGray: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFEBEBF5) else Color(0xFF3C3C43)

val IosPrimaryText: Color
    @Composable get() = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color.White else Color.Black

val HashtagColors = listOf(
    Color(0xFFB1D4FF),
    Color(0xFFB5E4CA),
    Color(0xFFD3BFFF),
    Color(0xFFFFD1AA),
    Color(0xFFFFC0E1),
    Color(0xFFFFEFA6)
)
