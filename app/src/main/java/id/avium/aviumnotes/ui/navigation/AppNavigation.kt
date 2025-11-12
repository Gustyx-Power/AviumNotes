package id.avium.aviumnotes.ui.navigation

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.avium.aviumnotes.MainActivity
import id.avium.aviumnotes.data.local.AppDatabase
import id.avium.aviumnotes.data.preferences.PreferencesManager
import id.avium.aviumnotes.data.repository.NoteRepository
import id.avium.aviumnotes.ui.screens.*
import id.avium.aviumnotes.ui.viewmodel.NoteViewModel
import id.avium.aviumnotes.ui.viewmodel.NoteViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    val preferencesManager = remember { PreferencesManager(context) }
    val isOnboardingCompleted by preferencesManager.isOnboardingCompleted.collectAsState(initial = null)

    val database = remember { AppDatabase.getInstance(context) }
    val repository = remember { NoteRepository(database.noteDao()) }
    val viewModel: NoteViewModel = viewModel(
        factory = NoteViewModelFactory(repository)
    )

    val notes by viewModel.notes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var hasCheckedClipboard by remember { mutableStateOf(false) }

    LaunchedEffect(isOnboardingCompleted, hasCheckedClipboard) {
        if (isOnboardingCompleted == true && !hasCheckedClipboard) {
            delay(100)
            if (MainActivity.shouldOpenClipboardNote) {
                MainActivity.shouldOpenClipboardNote = false
                hasCheckedClipboard = true
                navController.navigate("editor/clipboard")
            }
        }
    }

    if (isOnboardingCompleted == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(
        navController = navController,
        startDestination = if (isOnboardingCompleted == true) "main" else "onboarding"
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onOnboardingComplete = {
                    coroutineScope.launch {
                        preferencesManager.setOnboardingCompleted(true)
                        navController.navigate("main") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("main") {
            MainScreen(
                notes = notes,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                onNoteClick = { noteId ->
                    // Check if note is drawing or text
                    val note = notes.find { it.id == noteId }
                    if (note?.hasDrawing == true && note.content.isEmpty()) {
                        // Pure drawing note -> Open DrawingScreen
                        navController.navigate("drawing/$noteId")
                    } else {
                        // Text note -> Open RichTextEditor
                        navController.navigate("editor/$noteId")
                    }
                },
                onAddNote = { navController.navigate("editor/new") },
                onAddDrawing = { navController.navigate("drawing/new") },
                onDeleteNote = { note -> viewModel.deleteNote(note) },
                onTogglePin = { id, isPinned -> viewModel.togglePinStatus(id, isPinned) },
                onResizeCard = { note, newSpan ->
                    coroutineScope.launch {
                        val updatedNote = note.copy(
                            spanCount = newSpan,
                            updatedAt = System.currentTimeMillis()
                        )
                        viewModel.updateNote(updatedNote)
                    }
                },
                onNavigateToSettings = { navController.navigate("settings") }
            )
        }

        // Note Editor Route (Text only)
        composable(
            route = "editor/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")

            var currentNote by remember { mutableStateOf<id.avium.aviumnotes.data.local.entity.Note?>(null) }
            var isLoading by remember { mutableStateOf(true) }
            var editorClipboardTitle by remember { mutableStateOf<String?>(null) }
            var editorClipboardContent by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(noteId) {
                isLoading = true
                when (noteId) {
                    "clipboard" -> {
                        editorClipboardTitle = MainActivity.clipboardTitle
                        editorClipboardContent = MainActivity.clipboardContent
                        currentNote = null
                        MainActivity.clipboardTitle = null
                        MainActivity.clipboardContent = null
                    }
                    "new" -> {
                        currentNote = null
                        editorClipboardTitle = null
                        editorClipboardContent = null
                    }
                    else -> {
                        currentNote = repository.getNoteById(noteId?.toLongOrNull() ?: 0)
                        editorClipboardTitle = null
                        editorClipboardContent = null
                    }
                }
                isLoading = false
            }

            if (!isLoading) {
                RichTextEditorScreen(
                    note = currentNote,
                    initialTitle = editorClipboardTitle,
                    initialContent = editorClipboardContent,
                    onSaveNote = { note ->
                        coroutineScope.launch {
                            if (note.id == 0L) {
                                viewModel.insertNote(note)
                            } else {
                                viewModel.updateNote(note)
                            }
                        }
                    },
                    onDeleteNote = { currentNote?.let { viewModel.deleteNote(it) } },
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        // Drawing Screen Route (Drawing only)
        composable(
            route = "drawing/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.StringType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")

            var currentNote by remember { mutableStateOf<id.avium.aviumnotes.data.local.entity.Note?>(null) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(noteId) {
                isLoading = true
                when (noteId) {
                    "new" -> {
                        currentNote = null
                    }
                    else -> {
                        currentNote = repository.getNoteById(noteId?.toLongOrNull() ?: 0)
                    }
                }
                isLoading = false
            }

            if (!isLoading) {
                DrawingScreen(
                    note = currentNote,
                    onSaveDrawing = { updatedNote ->
                        coroutineScope.launch {
                            if (updatedNote.id == 0L) {
                                viewModel.insertNote(updatedNote)
                            } else {
                                viewModel.updateNote(updatedNote)
                            }
                        }
                    },
                    onDeleteDrawing = { currentNote?.let { viewModel.deleteNote(it) } },
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        composable("settings") {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
