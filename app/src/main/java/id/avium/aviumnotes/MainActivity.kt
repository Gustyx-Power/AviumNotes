package id.avium.aviumnotes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import id.avium.aviumnotes.data.preferences.PreferencesManager
import id.avium.aviumnotes.ui.navigation.AppNavigation
import id.avium.aviumnotes.ui.theme.AviumNotesTheme

class MainActivity : ComponentActivity() {

    private var initialNoteId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initialNoteId = intent?.getLongExtra("note_id", -1L)?.takeIf { it != -1L }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        actionBar?.hide()

        setContent {
            val preferencesManager = remember { PreferencesManager(this) }
            val themeMode by preferencesManager.themeMode.collectAsState(initial = "system")

            AviumNotesTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(initialNoteId = initialNoteId)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val noteId = intent.getLongExtra("note_id", -1L).takeIf { it != -1L }
        if (noteId != null) {
            initialNoteId = noteId
            recreate()
        }
    }
}
