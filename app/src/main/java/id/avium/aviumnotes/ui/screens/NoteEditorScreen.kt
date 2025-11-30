package id.avium.aviumnotes.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.toArgb
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
    val textColor = remember(noteColor) { getContrastColor(noteColor) }

    val sheetState = rememberModalBottomSheetState()
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = noteColor,
        contentWindowInsets = WindowInsets.ime,
        topBar = {
            EditorTopBar(
                textColor = textColor,
                noteColor = noteColor,
                title = title,
                hasChanges = hasChanges,
                isNewNote = isNewNote,
                scrollElevation = if (scrollState.value > 0) 4.dp else 0.dp,
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
                },
                onExportPng = {
                    val bitmap = id.avium.aviumnotes.ui.utils.ExportUtils.createBitmapFromText(
                        text = content,
                        title = title.ifEmpty { "Untitled" },
                        backgroundColor = noteColor.toArgb()
                    )

                    val uri = id.avium.aviumnotes.ui.utils.ExportUtils.exportToPng(
                        context,
                        bitmap,
                        "${title.ifEmpty { "note" }}_${System.currentTimeMillis()}.png"
                    )

                    if (uri != null) {
                        Toast.makeText(context, "Exported to Pictures/AviumNotes", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                    }
                },
                onExportPdf = {
                    // Export plain text note as PDF
                    val bitmap = id.avium.aviumnotes.ui.utils.ExportUtils.createBitmapFromText(
                        text = content,
                        title = title.ifEmpty { "Untitled" },
                        backgroundColor = noteColor.toArgb()
                    )

                    val uri = id.avium.aviumnotes.ui.utils.ExportUtils.exportToPdf(
                        context,
                        bitmap,
                        title.ifEmpty { "Untitled" },
                        "${title.ifEmpty { "note" }}_${System.currentTimeMillis()}.pdf"
                    )

                    if (uri != null) {
                        Toast.makeText(context, "Exported to Documents/AviumNotes", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = hasChanges || (isNewNote && (title.isNotEmpty() || content.isNotEmpty())),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val updatedNote = Note(
                            id = note?.id ?: 0,
                            title = title.ifEmpty { "Untitled" },
                            content = content,
                            color = noteColor.toArgb(),
                            createdAt = note?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            isPinned = note?.isPinned ?: false
                        )
                        onSaveNote(updatedNote)
                        onNavigateBack()
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp),
                    icon = { Icon(Icons.Filled.Check, null) },
                    text = { Text("Save") }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // Last Edited Info (iOS Style - Subtle at top)
            if (!isNewNote && note != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Last modified ${formatDate(note.updatedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.5f)
                    )
                    if (content.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(3.dp).background(textColor.copy(alpha=0.3f), CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${content.length} chars",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Title Field
            TextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.note_title_hint),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor.copy(alpha = 0.4f)
                        )
                    )
                },
                textStyle = MaterialTheme.typography.headlineLarge.copy(
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
                maxLines = 3
            )

            // Content Field
            TextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 400.dp)
                    .padding(horizontal = 8.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.note_content_hint),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 28.sp
                        ),
                        color = textColor.copy(alpha = 0.4f)
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    lineHeight = 28.sp, // Comfortable reading spacing
                    color = textColor.copy(alpha = 0.9f)
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

            Spacer(modifier = Modifier.height(100.dp)) // Bottom padding for FAB
        }
    }

    // --- DIALOGS & BOTTOM SHEETS ---

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
            icon = { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Note?") },
            text = { Text("This action cannot be undone. Are you sure you want to delete this note?") },
            confirmButton = {
                Button(
                    onClick = { onDeleteNote(); onNavigateBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            icon = { Icon(Icons.Outlined.SaveAs, null) },
            title = { Text("Unsaved Changes") },
            text = { Text("You have unsaved changes. Do you want to save before exiting?") },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedNote = Note(
                            id = note?.id ?: 0,
                            title = title.ifEmpty { "Untitled" },
                            content = content,
                            color = noteColor.toArgb(),
                            createdAt = note?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            isPinned = note?.isPinned ?: false
                        )
                        onSaveNote(updatedNote)
                        onNavigateBack()
                    }
                ) {
                    Text("Save & Exit")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDiscardDialog = false; onNavigateBack() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Discard")
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
    title: String,
    hasChanges: Boolean,
    isNewNote: Boolean,
    scrollElevation: androidx.compose.ui.unit.Dp,
    onBackClick: () -> Unit,
    onColorClick: () -> Unit,
    onMoreClick: () -> Unit,
    showMoreMenu: Boolean,
    onDismissMenu: () -> Unit,
    onDeleteClick: () -> Unit,
    onExportPng: () -> Unit,
    onExportPdf: () -> Unit
) {
    // Top Bar with seamless transition
    Surface(
        color = noteColor,
        shadowElevation = scrollElevation
    ) {
        CenterAlignedTopAppBar(
            title = {
                // Show title in top bar only when scrolled down (Optional logic)
                // For now, keep empty for clean look or show "Edit Note"
            },
            navigationIcon = {
                Surface(
                    onClick = onBackClick,
                    shape = CircleShape,
                    color = textColor.copy(alpha = 0.05f), // Subtle circle background
                    modifier = Modifier.padding(start = 12.dp).size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            actions = {
                // Color Picker Button
                IconButton(onClick = onColorClick) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(textColor.copy(alpha = 0.1f))
                            .border(1.5.dp, textColor.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Palette,
                            null,
                            tint = textColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (!isNewNote) {
                    Box {
                        IconButton(onClick = onMoreClick) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = "More",
                                tint = textColor
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = onDismissMenu,
                            shape = RoundedCornerShape(16.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export as PNG") },
                                onClick = { onDismissMenu(); onExportPng() },
                                leadingIcon = { Icon(Icons.Outlined.Image, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as PDF") },
                                onClick = { onDismissMenu(); onExportPdf() },
                                leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, null) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            DropdownMenuItem(
                                text = { Text("Delete Note", color = MaterialTheme.colorScheme.error) },
                                onClick = onDeleteClick,
                                leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent // Important for seamless look
            )
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}