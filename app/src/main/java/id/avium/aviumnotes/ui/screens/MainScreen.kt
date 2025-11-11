package id.avium.aviumnotes.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.avium.aviumnotes.R
import id.avium.aviumnotes.data.local.entity.Note
import id.avium.aviumnotes.data.preferences.PreferencesManager
import id.avium.aviumnotes.service.FloatingBubbleService
import id.avium.aviumnotes.ui.utils.getContrastColor
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    notes: List<Note>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNoteClick: (Long) -> Unit,
    onAddNote: () -> Unit,
    onDeleteNote: (Note) -> Unit,
    onTogglePin: (Long, Boolean) -> Unit,
    onResizeCard: (Note, Int) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val showNotePreview by preferencesManager.showNotePreview.collectAsState(initial = true)

    var showMenu by remember { mutableStateOf(false) }
    var isBubbleEnabled by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedViewMode by remember { mutableStateOf("grid") }

    val pinnedNotes = remember(notes) { notes.filter { it.isPinned } }
    val unpinnedNotes = remember(notes) { notes.filter { !it.isPinned } }

    Scaffold(
        topBar = {
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                }
            ) { searching ->
                if (searching) {
                    SearchTopBar(
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        onCloseClick = {
                            onSearchQueryChange("")
                            isSearchActive = false
                        }
                    )
                } else {
                    MainTopBar(
                        noteCount = notes.size,
                        onSearchClick = { isSearchActive = true },
                        onMenuClick = { showMenu = true },
                        showMenu = showMenu,
                        onDismissMenu = { showMenu = false },
                        onBubbleToggle = {
                            if (Settings.canDrawOverlays(context)) {
                                val intent = Intent(context, FloatingBubbleService::class.java)
                                intent.action = if (isBubbleEnabled) {
                                    FloatingBubbleService.ACTION_STOP
                                } else {
                                    FloatingBubbleService.ACTION_START
                                }
                                context.startService(intent)
                                isBubbleEnabled = !isBubbleEnabled
                            }
                            showMenu = false
                        },
                        onSettingsClick = {
                            showMenu = false
                            onNavigateToSettings()
                        },
                        onViewModeChange = { mode -> selectedViewMode = mode },
                        currentViewMode = selectedViewMode
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNote,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.main_add_note),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        if (notes.isEmpty()) {
            EmptyNotesView(modifier = Modifier.padding(paddingValues))
        } else {
            NotesGrid(
                pinnedNotes = pinnedNotes,
                unpinnedNotes = unpinnedNotes,
                showPreview = showNotePreview,
                viewMode = selectedViewMode,
                onNoteClick = onNoteClick,
                onDeleteNote = onDeleteNote,
                onTogglePin = onTogglePin,
                onResizeCard = onResizeCard,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    noteCount: Int,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit,
    showMenu: Boolean,
    onDismissMenu: () -> Unit,
    onBubbleToggle: () -> Unit,
    onSettingsClick: () -> Unit,
    onViewModeChange: (String) -> Unit,
    currentViewMode: String
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$noteCount notes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Outlined.Search, contentDescription = "Search")
            }
            IconButton(onClick = { onViewModeChange(if (currentViewMode == "grid") "list" else "grid") }) {
                Icon(
                    imageVector = if (currentViewMode == "grid") Icons.Outlined.ViewAgenda else Icons.Outlined.GridView,
                    contentDescription = "View Mode"
                )
            }
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "Menu")
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = onDismissMenu) {
                DropdownMenuItem(
                    text = { Text("Floating Bubble") },
                    onClick = onBubbleToggle,
                    leadingIcon = { Icon(Icons.Outlined.BubbleChart, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = onSettingsClick,
                    leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(query: String, onQueryChange: (String) -> Unit, onCloseClick: () -> Unit) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.main_search), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
        },
        navigationIcon = { IconButton(onClick = onCloseClick) { Icon(Icons.Filled.ArrowBack, contentDescription = "Close") } },
        actions = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Filled.Close, contentDescription = "Clear") }
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotesGrid(
    pinnedNotes: List<Note>,
    unpinnedNotes: List<Note>,
    showPreview: Boolean,
    viewMode: String,
    onNoteClick: (Long) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onTogglePin: (Long, Boolean) -> Unit,
    onResizeCard: (Note, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(if (viewMode == "grid") 2 else 1),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp
    ) {
        if (pinnedNotes.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Text(
                    text = "📌 Pinned",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(
                items = pinnedNotes,
                key = { it.id },
                span = { note ->
                    if (viewMode == "grid" && note.spanCount == 2) StaggeredGridItemSpan.FullLine else StaggeredGridItemSpan.SingleLane
                }
            ) { note ->
                ModernNoteCard(
                    note = note,
                    showPreview = showPreview,
                    viewMode = viewMode,
                    onClick = { onNoteClick(note.id) },
                    onDelete = { onDeleteNote(note) },
                    onTogglePin = { onTogglePin(note.id, !note.isPinned) },
                    onResizeCard = { span -> onResizeCard(note, span) }
                )
            }
        }

        if (unpinnedNotes.isNotEmpty() && pinnedNotes.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Others",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
        items(
            items = unpinnedNotes,
            key = { it.id },
            span = { note ->
                if (viewMode == "grid" && note.spanCount == 2) StaggeredGridItemSpan.FullLine else StaggeredGridItemSpan.SingleLane
            }
        ) { note ->
            ModernNoteCard(
                note = note,
                showPreview = showPreview,
                viewMode = viewMode,
                onClick = { onNoteClick(note.id) },
                onDelete = { onDeleteNote(note) },
                onTogglePin = { onTogglePin(note.id, !note.isPinned) },
                onResizeCard = { span -> onResizeCard(note, span) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModernNoteCard(
    note: Note,
    showPreview: Boolean,
    viewMode: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onResizeCard: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isResizeMode by remember { mutableStateOf(false) }
    val backgroundColor = Color(note.color)
    val textColor = remember(backgroundColor) { getContrastColor(backgroundColor) }

    val scale by animateFloatAsState(
        targetValue = if (isResizeMode) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    Box(modifier = modifier.fillMaxWidth().scale(scale)) {
        Card(
            modifier = Modifier.fillMaxWidth().combinedClickable(
                onClick = { if (!isResizeMode) onClick() },
                onLongClick = { if (viewMode == "grid") isResizeMode = !isResizeMode }
            ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isResizeMode) 8.dp else 2.dp),
            border = if (isResizeMode) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = note.title.ifEmpty { "Untitled" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = if (note.spanCount == 2) 3 else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = textColor
                    )
                    AnimatedVisibility(visible = !isResizeMode) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(onClick = onTogglePin, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = "Pin",
                                    modifier = Modifier.size(18.dp),
                                    tint = if (note.isPinned) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.6f)
                                )
                            }
                            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete",
                                    modifier = Modifier.size(18.dp),
                                    tint = textColor.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
                if (note.content.isNotEmpty() && showPreview) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (note.spanCount == 2) 8 else 4,
                        overflow = TextOverflow.Ellipsis,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDate(note.updatedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.6f)
                    )
                    Surface(shape = CircleShape, color = textColor.copy(alpha = 0.1f)) {
                        Text(
                            text = "${note.content.length} chars",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Resize Handle - Right (Expand)
        AnimatedVisibility(
            visible = isResizeMode && viewMode == "grid" && note.spanCount == 1,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd).offset(x = 20.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp).clickable {
                    onResizeCard(2)
                    isResizeMode = false
                },
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardDoubleArrowRight,
                        contentDescription = "Expand",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Resize Handle - Left (Collapse)
        AnimatedVisibility(
            visible = isResizeMode && viewMode == "grid" && note.spanCount == 2,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart).offset(x = (-20).dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp).clickable {
                    onResizeCard(1)
                    isResizeMode = false
                },
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardDoubleArrowLeft,
                        contentDescription = "Collapse",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text("Delete Note?") },
            text = { Text("This note will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun EmptyNotesView(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(120.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "No notes yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Tap + to create your first note", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
