package id.avium.aviumnotes.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.avium.aviumnotes.R
import id.avium.aviumnotes.data.local.entity.Note
import id.avium.aviumnotes.data.preferences.PreferencesManager
import id.avium.aviumnotes.ui.components.ColorPickerDialog
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (hasChanges && (title.isNotEmpty() || content.isNotEmpty())) {
                                showDiscardDialog = true
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showColorPicker = true }) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = stringResource(R.string.note_color),
                            tint = textColor
                        )
                    }

                    if (!isNewNote) {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = textColor
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.note_share)) },
                                onClick = {
                                    showMoreMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.note_delete)) },
                                onClick = {
                                    showMoreMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = noteColor,
                    navigationIconContentColor = textColor,
                    titleContentColor = textColor,
                    actionIconContentColor = textColor
                )
            )
        },
        floatingActionButton = {
            if (title.isNotEmpty() || content.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val updatedNote = Note(
                            id = note?.id ?: 0,
                            title = title,
                            content = content,
                            color = noteColor.hashCode(),
                            createdAt = note?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            isPinned = note?.isPinned ?: false
                        )
                        onSaveNote(updatedNote)
                        onNavigateBack()
                    },
                    icon = { Icon(Icons.Default.Check, contentDescription = null) },
                    text = { Text(stringResource(R.string.note_save)) },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
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
                        color = textColor.copy(alpha = 0.6f)
                    )
                },
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    color = textColor
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = textColor
                ),
                singleLine = false,
                maxLines = 3
            )

            Divider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = textColor.copy(alpha = 0.3f)
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
                        color = textColor.copy(alpha = 0.6f)
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = textColor
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = textColor
                )
            )

            if (!isNewNote && note != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.note_created, formatDate(note.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = stringResource(R.string.note_modified, formatDate(note.updatedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
            }

            if (isFromClipboard && isNewNote) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = textColor.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Smart Clipboard",
                                style = MaterialTheme.typography.labelMedium,
                                color = textColor.copy(alpha = 0.9f)
                            )
                            Text(
                                text = if (initialTitle?.isNotEmpty() == true)
                                    "Single line → Title"
                                else
                                    "Multiple lines → Content",
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = noteColor,
            onColorSelected = { noteColor = it },
            onDismiss = { showColorPicker = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text(stringResource(R.string.note_delete)) },
            text = { Text("Are you sure you want to delete this note? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteNote()
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null) },
            title = { Text(stringResource(R.string.note_discard_changes)) },
            text = { Text(stringResource(R.string.note_discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.discard))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text(stringResource(R.string.keep_editing))
                }
            }
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
