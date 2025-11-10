package id.avium.aviumnotes.ui.navigation

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import id.avium.aviumnotes.data.local.AppDatabase
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
    var isFirstLaunch by remember { mutableStateOf(true) }

    // Initialize ViewModel
    val database = remember { AppDatabase.getInstance(context) }
    val repository = remember { NoteRepository(database.noteDao()) }
    val viewModel: NoteViewModel = viewModel(
        factory = NoteViewModelFactory(repository)
    )

    val notes by viewModel.notes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = if (isFirstLaunch) "onboarding" else "main"
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onOnboardingComplete = {
                    isFirstLaunch = false
                    navController.navigate("main") {
                        popUpTo("onboarding") { inclusive = true }
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
            var currentNote by remember { mutableStateOf<id.avium.aviumnotes.data.local.entity.Note?>(null) }

            LaunchedEffect(noteId) {
                if (noteId != "new") {
                    currentNote = repository.getNoteById(noteId?.toLongOrNull() ?: 0)
                }
            }

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
        }
    }
}
