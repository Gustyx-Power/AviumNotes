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
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import id.avium.aviumnotes.R
import id.avium.aviumnotes.data.local.entity.Note
import id.avium.aviumnotes.data.preferences.PreferencesManager
import id.avium.aviumnotes.ui.components.ColorPickerBottomSheet
import id.avium.aviumnotes.ui.theme.NoteColors
import id.avium.aviumnotes.ui.utils.getContrastColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RichTextEditorScreen(
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

    val richTextState = rememberRichTextState()

    LaunchedEffect(note, initialContent) {
        if (initialContent != null) {
            richTextState.setHtml(initialContent)
        } else if (note?.content != null) {
            richTextState.setHtml(note.content)
        }
    }

    var noteColor by remember(note, defaultNoteColor) {
        mutableStateOf(Color(note?.color ?: defaultNoteColor))
    }

    var showColorPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showFormattingBar by remember { mutableStateOf(true) }

    val hasChanges = remember(title, richTextState.annotatedString) {
        title != (initialTitle ?: note?.title ?: "") ||
                richTextState.toHtml() != (initialContent ?: note?.content ?: "")
    }

    val isNewNote = note == null
    val textColor = remember(noteColor) { getContrastColor(noteColor) }
    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasChanges && (title.isNotEmpty() || richTextState.annotatedString.text.isNotEmpty())) {
                            showDiscardDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = textColor)
                    }
                },
                actions = {
                    AnimatedVisibility(visible = hasChanges, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                        Surface(shape = CircleShape, color = textColor.copy(alpha = 0.1f), modifier = Modifier.padding(end = 8.dp)) {
                            Text("Unsaved", style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.7f), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                    IconButton(onClick = { showFormattingBar = !showFormattingBar }) {
                        Icon(Icons.Outlined.TextFields, "Format", tint = textColor)
                    }
                    IconButton(onClick = { showColorPicker = true }) {
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(textColor.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Palette, "Color", modifier = Modifier.size(16.dp), tint = textColor)
                        }
                    }
                    if (!isNewNote) {
                        IconButton(onClick = { showMoreMenu = true }) { Icon(Icons.Outlined.MoreVert, "More", tint = textColor) }
                        DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Export as PNG") },
                                onClick = {
                                    showMoreMenu = false
                                    // Export note as PNG
                                    val bitmap = id.avium.aviumnotes.ui.utils.ExportUtils.createBitmapFromText(
                                        text = richTextState.annotatedString.text,
                                        title = title.ifEmpty { "Untitled" },
                                        backgroundColor = noteColor.hashCode()
                                    )

                                    val uri = id.avium.aviumnotes.ui.utils.ExportUtils.exportToPng(
                                        context,
                                        bitmap,
                                        "${title.ifEmpty { "note" }}_${System.currentTimeMillis()}.png"
                                    )

                                    if (uri != null) {
                                        android.widget.Toast.makeText(context, "Exported to Pictures/AviumNotes", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                leadingIcon = { Icon(Icons.Outlined.Image, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Export as PDF") },
                                onClick = {
                                    showMoreMenu = false
                                    // Export note as PDF
                                    val bitmap = id.avium.aviumnotes.ui.utils.ExportUtils.createBitmapFromText(
                                        text = richTextState.annotatedString.text,
                                        title = title.ifEmpty { "Untitled" },
                                        backgroundColor = noteColor.hashCode()
                                    )

                                    val uri = id.avium.aviumnotes.ui.utils.ExportUtils.exportToPdf(
                                        context,
                                        bitmap,
                                        title.ifEmpty { "Untitled" },
                                        "${title.ifEmpty { "note" }}_${System.currentTimeMillis()}.pdf"
                                    )

                                    if (uri != null) {
                                        android.widget.Toast.makeText(context, "Exported to Documents/AviumNotes", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                leadingIcon = { Icon(Icons.Outlined.Description, null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text("Share") }, onClick = { showMoreMenu = false }, leadingIcon = { Icon(Icons.Outlined.Share, null) })
                            DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { showMoreMenu = false; showDeleteDialog = true }, leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) })
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = noteColor)
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = title.isNotEmpty() || richTextState.annotatedString.text.isNotEmpty(),
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        val updatedNote = Note(
                            id = note?.id ?: 0,
                            title = title.ifEmpty { "Untitled" },
                            content = richTextState.toHtml(),
                            color = noteColor.hashCode(),
                            createdAt = note?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            isPinned = note?.isPinned ?: false,
                            spanCount = note?.spanCount ?: 1,
                            hasDrawing = false,
                            drawingPath = null
                        )
                        onSaveNote(updatedNote)
                        onNavigateBack()
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Check, "Save", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(noteColor)
                .padding(paddingValues)
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.note_title_hint), style = MaterialTheme.typography.headlineMedium, color = textColor.copy(alpha = 0.5f)) },
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = textColor),
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

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = textColor.copy(alpha = 0.2f), thickness = 1.dp)

            AnimatedVisibility(visible = showFormattingBar, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Surface(color = textColor.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        IconButton(onClick = { richTextState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) }) {
                            Icon(Icons.Outlined.FormatBold, "Bold", tint = if (richTextState.currentSpanStyle.fontWeight == FontWeight.Bold) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.7f))
                        }
                        IconButton(onClick = { richTextState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) }) {
                            Icon(Icons.Outlined.FormatItalic, "Italic", tint = if (richTextState.currentSpanStyle.fontStyle == androidx.compose.ui.text.font.FontStyle.Italic) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.7f))
                        }
                        IconButton(onClick = { richTextState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)) }) {
                            Icon(Icons.Outlined.FormatUnderlined, "Underline", tint = if (richTextState.currentSpanStyle.textDecoration == androidx.compose.ui.text.style.TextDecoration.Underline) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.7f))
                        }
                        IconButton(onClick = { richTextState.toggleSpanStyle(androidx.compose.ui.text.SpanStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)) }) {
                            Icon(Icons.Outlined.FormatStrikethrough, "Strike", tint = if (richTextState.currentSpanStyle.textDecoration == androidx.compose.ui.text.style.TextDecoration.LineThrough) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            RichTextEditor(
                state = richTextState,
                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
                placeholder = { Text(stringResource(R.string.note_content_hint), color = textColor.copy(alpha = 0.5f)) },
                colors = RichTextEditorDefaults.richTextEditorColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = textColor
                )
            )
        }
    }

    if (showColorPicker) {
        ColorPickerBottomSheet(currentColor = noteColor, onColorSelected = { noteColor = it; showColorPicker = false }, onDismiss = { showColorPicker = false }, sheetState = sheetState)
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Outlined.Delete, null) },
            title = { Text("Delete Note?") },
            text = { Text("This note will be permanently deleted.") },
            confirmButton = { TextButton(onClick = { onDeleteNote(); onNavigateBack() }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            icon = { Icon(Icons.Outlined.Info, null) },
            title = { Text("Discard Changes?") },
            text = { Text("You have unsaved changes.") },
            confirmButton = { TextButton(onClick = { showDiscardDialog = false; onNavigateBack() }) { Text("Discard", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDiscardDialog = false }) { Text("Keep Editing") } }
        )
    }
}
