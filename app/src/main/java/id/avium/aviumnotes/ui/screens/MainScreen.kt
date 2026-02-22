package id.avium.aviumnotes.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.avium.aviumnotes.data.local.entity.Note
import id.avium.aviumnotes.data.preferences.PreferencesManager
import id.avium.aviumnotes.ui.components.IosEnhancedFab
import id.avium.aviumnotes.ui.components.IosNoteCard
import id.avium.aviumnotes.ui.theme.IosBlack
import id.avium.aviumnotes.ui.theme.IosSearchGray
import id.avium.aviumnotes.ui.theme.IosTextGray

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    notes: List<Note>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNoteClick: (Long) -> Unit,
    onAddNote: () -> Unit,
    onAddDrawing: () -> Unit = {},
    onDeleteNote: (Note) -> Unit,
    onTogglePin: (Long, Boolean) -> Unit,
    onResizeCard: (Note, Int) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val showNotePreview by preferencesManager.showNotePreview.collectAsState(initial = true)

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val pinnedNotes = remember(notes) { notes.filter { it.isPinned } }
    val unpinnedNotes = remember(notes) { notes.filter { !it.isPinned } }
    
    var showFabOptions by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = IosBlack,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = IosBlack,
                    scrolledContainerColor = IosBlack,
                    titleContentColor = Color.White
                ),
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = IosTextGray)
                    }
                }
            )
        },
        floatingActionButton = {
            IosEnhancedFab(
                showOptions = showFabOptions,
                onToggleOptions = { showFabOptions = !showFabOptions },
                onAddNote = {
                    showFabOptions = false
                    onAddNote()
                },
                onAddDrawing = {
                    showFabOptions = false
                    onAddDrawing()
                }
            )
        }
    ) { paddingValues ->
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(1),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalItemSpacing = 16.dp
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = IosSearchGray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Search your notes", color = Color.Gray, fontSize = 16.sp)
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            if (notes.isEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(modifier = Modifier.fillMaxWidth().height(400.dp).padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No Notes",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.Gray
                        )
                    }
                }
            } else {
                if (pinnedNotes.isNotEmpty()) {
                    items(items = pinnedNotes, key = { it.id }) { note ->
                        IosNoteCard(
                            note = note,
                            showPreview = showNotePreview,
                            onClick = { onNoteClick(note.id) },
                            onDelete = { onDeleteNote(note) },
                            onTogglePin = { onTogglePin(note.id, !note.isPinned) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                items(items = unpinnedNotes, key = { it.id }) { note ->
                    IosNoteCard(
                        note = note,
                        showPreview = showNotePreview,
                        onClick = { onNoteClick(note.id) },
                        onDelete = { onDeleteNote(note) },
                        onTogglePin = { onTogglePin(note.id, !note.isPinned) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}