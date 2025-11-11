package id.avium.aviumnotes.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.avium.aviumnotes.data.local.AppDatabase
import id.avium.aviumnotes.data.preferences.PreferencesManager
import id.avium.aviumnotes.data.repository.NoteRepository
import id.avium.aviumnotes.ui.screens.MainScreen
import id.avium.aviumnotes.ui.screens.NoteEditorScreen
import id.avium.aviumnotes.ui.screens.OnboardingScreen
import id.avium.aviumnotes.ui.viewmodel.NoteViewModel
import id.avium.aviumnotes.ui.viewmodel.NoteViewModelFactory
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Initialize PreferencesManager
    val preferencesManager = remember { PreferencesManager(context) }
    val isOnboardingCompleted by preferencesManager.isOnboardingCompleted.collectAsState(initial = null)

    // Initialize ViewModel
    val database = remember { AppDatabase.getInstance(context) }
    val repository = remember { NoteRepository(database.noteDao()) }
    val viewModel: NoteViewModel = viewModel(
        factory = NoteViewModelFactory(repository)
    )

    val notes by viewModel.notes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Show loading while checking onboarding status
    if (isOnboardingCompleted == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
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
                    navController.navigate("editor/$noteId")
                },
                onAddNote = {
                    navController.navigate("editor/new")
                },
                onDeleteNote = { note ->
                    viewModel.deleteNote(note)
                },
                onTogglePin = { id, isPinned ->
                    viewModel.togglePinStatus(id, isPinned)
                }
            )
        }

        composable(
            route = "editor/{noteId}",
            arguments = listOf(
                navArgument("noteId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId")

            // Load note from repository
            var currentNote by remember { mutableStateOf<id.avium.aviumnotes.data.local.entity.Note?>(null) }
            var isLoading by remember { mutableStateOf(true) }

            LaunchedEffect(noteId) {
                isLoading = true
                currentNote = if (noteId != "new") {
                    repository.getNoteById(noteId?.toLongOrNull() ?: 0)
                } else {
                    null
                }
                isLoading = false
            }

            // Show loading or editor screen
            if (!isLoading) {
                NoteEditorScreen(
                    note = currentNote,
                    onSaveNote = { note ->
                        coroutineScope.launch {
                            if (note.id == 0L) {
                                viewModel.insertNote(note)
                            } else {
                                viewModel.updateNote(note)
                            }
                        }
                    },
                    onDeleteNote = {
                        currentNote?.let { viewModel.deleteNote(it) }
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            } else {
                // Loading indicator while fetching note data
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
