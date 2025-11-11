package id.avium.aviumnotes

import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import id.avium.aviumnotes.data.preferences.PreferencesManager
import id.avium.aviumnotes.ui.navigation.AppNavigation
import id.avium.aviumnotes.ui.theme.AviumNotesTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        var clipboardTitle: String? = null
        var clipboardContent: String? = null
        var shouldOpenClipboardNote: Boolean = false
    }

    private val handler = Handler(Looper.getMainLooper())
    private var pendingClipboardIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        actionBar?.hide()

        if (intent.getBooleanExtra("from_clipboard_broadcast", false)) {
            pendingClipboardIntent = intent
        } else {
            handleIntent(intent)
        }

        setContent {
            val preferencesManager = androidx.compose.runtime.remember { PreferencesManager(this) }
            val themeMode by preferencesManager.themeMode.collectAsState(initial = "system")

            AviumNotesTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        pendingClipboardIntent?.let { intent ->
            handler.postDelayed({
                Log.d(TAG, "Processing pending clipboard intent after delay")
                handleIntent(intent)
                pendingClipboardIntent = null
            }, 300)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (intent.getBooleanExtra("from_clipboard_broadcast", false)) {
            handler.postDelayed({
                Log.d(TAG, "Processing new clipboard intent after delay")
                handleIntent(intent)
            }, 300)
        } else {
            handleIntent(intent)
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            val fromClipboard = it.getBooleanExtra("from_clipboard_broadcast", false)
            val noteId = it.getLongExtra("note_id", -1L)

            Log.d(TAG, "handleIntent - fromClipboard: $fromClipboard, noteId: $noteId")

            if (fromClipboard && noteId == -2L) {
                try {
                    val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = clipboardManager.primaryClip
                    val clipText = clipData?.getItemAt(0)?.text?.toString() ?: ""

                    Log.d(TAG, "Clipboard text: '$clipText'")

                    if (clipText.isEmpty()) {
                        Log.e(TAG, "Failed to read clipboard - might be too early or access denied")
                        return
                    }

                    val trimmedText = clipText.trim()
                    val lines = trimmedText.lines()

                    Log.d(TAG, "Lines count: ${lines.size}")
                    lines.forEachIndexed { index, line ->
                        Log.d(TAG, "Line $index: '$line'")
                    }

                    val (title, content) = when {
                        trimmedText.isEmpty() -> {
                            Log.d(TAG, "Empty clipboard")
                            Pair("", "")
                        }
                        lines.size == 1 -> {
                            Log.d(TAG, "Single line -> Title")
                            Pair(trimmedText, "")
                        }
                        else -> {
                            val firstLine = lines[0].trim()
                            val restLines = lines.drop(1).joinToString("\n").trim()

                            Log.d(TAG, "First line: '$firstLine' (${firstLine.length} chars)")
                            Log.d(TAG, "Rest lines: '$restLines'")

                            if (firstLine.length > 50) {
                                Log.d(TAG, "First line too long -> All to content")
                                Pair("", trimmedText)
                            } else {
                                Log.d(TAG, "First line -> Title, Rest -> Content")
                                Pair(firstLine, restLines)
                            }
                        }
                    }

                    Log.d(TAG, "Final - Title: '$title', Content: '$content'")

                    clipboardTitle = title
                    clipboardContent = content
                    shouldOpenClipboardNote = true

                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException accessing clipboard", e)
                } catch (e: Exception) {
                    Log.e(TAG, "Error accessing clipboard", e)
                }

                it.removeExtra("from_clipboard_broadcast")
                it.removeExtra("note_id")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
