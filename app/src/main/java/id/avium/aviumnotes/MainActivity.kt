package id.avium.aviumnotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import id.avium.aviumnotes.ui.navigation.AppNavigation
import id.avium.aviumnotes.ui.theme.AviumNotesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide system bars untuk edge-to-edge experience
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        // Pastikan tidak ada action bar
        actionBar?.hide()

        setContent {
            AviumNotesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
