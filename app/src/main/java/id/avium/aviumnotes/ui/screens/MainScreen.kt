package id.avium.aviumnotes.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onAddDrawing: () -> Unit = {},
    onDeleteNote: (Note) -> Unit,
    onTogglePin: (Long, Boolean) -> Unit,
    onResizeCard: (Note, Int) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val showNotePreview by preferencesManager.showNotePreview.collectAsState(initial = true)

    // Scroll behavior untuk efek collapsing toolbar
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    var showMenu by remember { mutableStateOf(false) }
    var isBubbleEnabled by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var showFabOptions by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf("grid") } // grid or list

    val pinnedNotes = remember(notes) { notes.filter { it.isPinned } }
    val unpinnedNotes = remember(notes) { notes.filter { !it.isPinned } }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = {
                    (fadeIn() + slideInVertically { -it }) togetherWith (fadeOut() + slideOutVertically { -it })
                },
                label = "TopBarTransition"
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
                        scrollBehavior = scrollBehavior,
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
                        viewMode = viewMode,
                        onViewModeChange = { viewMode = it }
                    )
                }
            }
        },
        floatingActionButton = {
            EnhancedFab(
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
        if (notes.isEmpty()) {
            EmptyNotesView(modifier = Modifier.padding(paddingValues))
        } else {
            NotesGrid(
                pinnedNotes = pinnedNotes,
                unpinnedNotes = unpinnedNotes,
                showPreview = showNotePreview,
                viewMode = viewMode,
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
    scrollBehavior: TopAppBarScrollBehavior,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit,
    showMenu: Boolean,
    onDismissMenu: () -> Unit,
    onBubbleToggle: () -> Unit,
    onSettingsClick: () -> Unit,
    viewMode: String,
    onViewModeChange: (String) -> Unit
) {
    LargeTopAppBar(
        title = {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                // Subtitle yang memudar saat di-scroll
                val alpha = 1f - scrollBehavior.state.collapsedFraction
                if (alpha > 0.1f) {
                    Text(
                        text = stringResource(R.string.main_notes_count, noteCount),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.graphicsLayer { this.alpha = alpha }
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = onSearchClick,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.main_search))
            }

            IconButton(onClick = { onViewModeChange(if (viewMode == "grid") "list" else "grid") }) {
                Icon(
                    imageVector = if (viewMode == "grid") Icons.Outlined.ViewAgenda else Icons.Outlined.GridView,
                    contentDescription = "Toggle view"
                )
            }

            Box {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.main_menu))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = onDismissMenu,
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.main_floating_bubble)) },
                        onClick = onBubbleToggle,
                        leadingIcon = { Icon(Icons.Outlined.BubbleChart, null) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.main_settings)) },
                        onClick = onSettingsClick,
                        leadingIcon = { Icon(Icons.Outlined.Settings, null) }
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(query: String, onQueryChange: (String) -> Unit, onCloseClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .statusBarsPadding(), // Handle notch overlap
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCloseClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Floating Pill Search Field
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.weight(1f)
            ) {
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search notes...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.6f)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Filled.Close, null, modifier = Modifier.size(20.dp))
                            }
                        }
                    } else null,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
fun EnhancedFab(
    showOptions: Boolean,
    onToggleOptions: () -> Unit,
    onAddNote: () -> Unit,
    onAddDrawing: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Expanded Options
        AnimatedVisibility(
            visible = showOptions,
            enter = fadeIn() + slideInVertically { it / 2 } + expandVertically(),
            exit = fadeOut() + slideOutVertically { it / 2 } + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Drawing Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 2.dp,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            "Drawing",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    SmallFloatingActionButton(
                        onClick = onAddDrawing,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(Icons.Outlined.Brush, "Drawing")
                    }
                }

                // Note Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 2.dp,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            "Text Note",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    SmallFloatingActionButton(
                        onClick = onAddNote,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Icon(Icons.Outlined.Edit, "Note")
                    }
                }
            }
        }

        // Main FAB
        FloatingActionButton(
            onClick = onToggleOptions,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.size(64.dp)
        ) {
            AnimatedContent(targetState = showOptions, label = "FabIcon") { expanded ->
                Icon(
                    imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.Add,
                    contentDescription = if (expanded) "Close" else "Add",
                    modifier = Modifier.size(32.dp).rotate(if (expanded) 90f else 0f)
                )
            }
        }
    }
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
        columns = if (viewMode == "list") StaggeredGridCells.Fixed(1) else StaggeredGridCells.Adaptive(160.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp
    ) {
        if (pinnedNotes.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Text(
                    text = stringResource(R.string.main_pinned).uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                )
            }
            items(items = pinnedNotes, key = { it.id }, span = { if (it.spanCount == 2) StaggeredGridItemSpan.FullLine else StaggeredGridItemSpan.SingleLane }) { note ->
                ModernNoteCard(
                    note = note,
                    showPreview = showPreview,
                    onClick = { onNoteClick(note.id) },
                    onDelete = { onDeleteNote(note) },
                    onTogglePin = { onTogglePin(note.id, !note.isPinned) },
                    onResizeCard = { span -> onResizeCard(note, span) }
                )
            }
        }

        if (unpinnedNotes.isNotEmpty() && pinnedNotes.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.main_others).uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                )
            }
        }

        items(items = unpinnedNotes, key = { it.id }, span = { if (it.spanCount == 2) StaggeredGridItemSpan.FullLine else StaggeredGridItemSpan.SingleLane }) { note ->
            ModernNoteCard(
                note = note,
                showPreview = showPreview,
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
        targetValue = if (isResizeMode) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "CardScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                onClick = {
                    if (isResizeMode) isResizeMode = false else onClick()
                },
                onLongClick = { isResizeMode = !isResizeMode }
            ),
        shape = RoundedCornerShape(26.dp), // Sudut lebih halus
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isResizeMode) 12.dp else 2.dp
        ),
        border = if (isResizeMode) BorderStroke(3.dp, MaterialTheme.colorScheme.primary.copy(alpha=0.6f)) else null
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header: Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = note.title.ifEmpty { stringResource(R.string.note_untitled) },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = textColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )

                    if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = textColor.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp).rotate(45f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Metadata Pill (Date & Count)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = textColor.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDate(note.updatedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.size(3.dp).background(textColor.copy(alpha=0.4f), CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${note.content.length}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = textColor.copy(alpha = 0.6f)
                        )
                    }
                }

                // Drawing Preview
                if (note.hasDrawing && note.drawingPath != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    val bitmap = remember(note.drawingPath) {
                        BitmapFactory.decodeFile(note.drawingPath)
                    }
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = stringResource(R.string.note_drawing),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if(note.spanCount == 2) 180.dp else 140.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.dp, textColor.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                        )
                    }
                }

                // Text Content
                if (note.content.isNotEmpty() && showPreview) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stripHtmlTags(note.content),
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        maxLines = if (note.spanCount == 2) 8 else 5,
                        overflow = TextOverflow.Ellipsis,
                        color = textColor.copy(alpha = 0.85f)
                    )
                }
            }

            // OVERLAY: Resize Mode Actions (Visible only on Long Press)
            if (isResizeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Delete Button
                        FilledIconButton(
                            onClick = { showDeleteDialog = true },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.onErrorContainer)
                        }

                        // Resize Button
                        FilledIconButton(
                            onClick = {
                                onResizeCard(if(note.spanCount == 1) 2 else 1)
                                isResizeMode = false
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                if(note.spanCount == 1) Icons.Outlined.OpenInFull else Icons.Outlined.CloseFullscreen,
                                "Resize",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Pin Button
                        FilledIconButton(
                            onClick = { onTogglePin(); isResizeMode = false },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Icon(
                                if(note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                "Pin",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Outlined.DeleteForever, contentDescription = null) },
            title = { Text(stringResource(R.string.note_delete_title)) },
            text = { Text(stringResource(R.string.note_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = { onDelete(); showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.note_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.note_delete_cancel))
                }
            }
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
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.size(140.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.NoteAdd,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.main_empty_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.main_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun stripHtmlTags(html: String): String {
    return html
        .replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .trim()
}