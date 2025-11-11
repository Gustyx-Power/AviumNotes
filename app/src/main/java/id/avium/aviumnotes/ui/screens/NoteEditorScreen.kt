package id.avium.aviumnotes.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.avium.aviumnotes.R
import id.avium.aviumnotes.data.local.entity.Note
import id.avium.aviumnotes.data.preferences.PreferencesManager
import id.avium.aviumnotes.ui.components.ColorPickerBottomSheet
import id.avium.aviumnotes.ui.theme.NoteColors
import id.avium.aviumnotes.ui.utils.getContrastColor
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    note: Note?,
    initialTitle: String? = null,
    initialContent: String? = null,
    onSaveNote: (Note) -> Unit,
    onDeleteNote: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val defaultNoteColor by preferencesManager.defaultNoteColor.collectAsState(
        initial = NoteColors.White.hashCode()
    )

    var title by remember(note, initialTitle) {
        mutableStateOf(initialTitle ?: note?.title ?: "")
    }
    var content by remember(note, initialContent) {
        mutableStateOf(initialContent ?: note?.content ?: "")
    }
    var noteColor by remember(note, defaultNoteColor) {
        mutableStateOf(Color(note?.color ?: defaultNoteColor))
    }
    var showColorPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val hasChanges = remember(title, content, noteColor) {
        title != (initialTitle ?: note?.title ?: "") ||
                content != (initialContent ?: note?.content ?: "") ||
                noteColor.hashCode() != (note?.color ?: defaultNoteColor)
    }

    val isNewNote = note == null
    val isFromClipboard = initialTitle != null || initialContent != null
    val textColor = remember(noteColor) { getContrastColor(noteColor) }

    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            EditorTopBar(
                textColor = textColor,
                noteColor = noteColor,
                hasChanges = hasChanges,
                isNewNote = isNewNote,
                onBackClick = {
                    if (hasChanges && (title.isNotEmpty() || content.isNotEmpty())) {
                        showDiscardDialog = true
                    } else {
                        onNavigateBack()
                    }
                },
                onColorClick = { showColorPicker = true },
                onMoreClick = { showMoreMenu = true },
                showMoreMenu = showMoreMenu,
                onDismissMenu = { showMoreMenu = false },
                onDeleteClick = {
                    showMoreMenu = false
                    showDeleteDialog = true
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = title.isNotEmpty() || content.isNotEmpty(),
                enter = scaleIn(animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )),
                exit = scaleOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        val updatedNote = Note(
                            id = note?.id ?: 0,
                            title = title.ifEmpty { "Untitled" },
                            content = content,
                            color = noteColor.hashCode(),
                            createdAt = note?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            isPinned = note?.isPinned ?: false
                        )
                        onSaveNote(updatedNote)
                        onNavigateBack()
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Save",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(noteColor)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.note_title_hint),
                        style = MaterialTheme.typography.headlineMedium,
                        color = textColor.copy(alpha = 0.5f)
                    )
                },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = textColor
                ),
                singleLine = false,
                maxLines = 3
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = textColor.copy(alpha = 0.2f),
                thickness = 1.dp
            )

            TextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .defaultMinSize(minHeight = 400.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.note_content_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = textColor.copy(alpha = 0.5f)
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 26.sp,
                    color = textColor
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = textColor
                )
            )

            NoteMetadata(
                note = note,
                isNewNote = isNewNote,
                isFromClipboard = isFromClipboard,
                initialTitle = initialTitle,
                textColor = textColor,
                contentLength = content.length
            )
        }
    }

    if (showColorPicker) {
        ColorPickerBottomSheet(
            currentColor = noteColor,
            onColorSelected = {
                noteColor = it
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false },
            sheetState = sheetState
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
            title = { Text("Delete Note?") },
            text = { Text("This note will be permanently deleted. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteNote()
                        onNavigateBack()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
            title = { Text("Discard Changes?") },
            text = { Text("You have unsaved changes. Do you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Keep Editing")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
    textColor: Color,
    noteColor: Color,
    hasChanges: Boolean,
    isNewNote: Boolean,
    onBackClick: () -> Unit,
    onColorClick: () -> Unit,
    onMoreClick: () -> Unit,
    showMoreMenu: Boolean,
    onDismissMenu: () -> Unit,
    onDeleteClick: () -> Unit
) {
    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = textColor
                )
            }
        },
        actions = {
            AnimatedVisibility(
                visible = hasChanges,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Surface(
                    shape = CircleShape,
                    color = textColor.copy(alpha = 0.1f),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "Unsaved",
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            IconButton(onClick = onColorClick) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(textColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = "Color",
                        modifier = Modifier.size(16.dp),
                        tint = textColor
                    )
                }
            }

            if (!isNewNote) {
                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "More",
                        tint = textColor
                    )
                }

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = onDismissMenu
                ) {
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = { onDismissMenu() },
                        leadingIcon = {
                            Icon(Icons.Outlined.Share, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = onDeleteClick,
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = noteColor
        )
    )
}

@Composable
fun NoteMetadata(
    note: Note?,
    isNewNote: Boolean,
    isFromClipboard: Boolean,
    initialTitle: String?,
    textColor: Color,
    contentLength: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (!isNewNote && note != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Created",
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.5f)
                    )
                    Text(
                        text = formatDate(note.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Modified",
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.5f)
                    )
                    Text(
                        text = formatDate(note.updatedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = textColor.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "$contentLength characters",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            if (isFromClipboard && isNewNote) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = textColor.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (initialTitle?.isNotEmpty() == true)
                                "From clipboard → Title"
                            else
                                "From clipboard → Content",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
