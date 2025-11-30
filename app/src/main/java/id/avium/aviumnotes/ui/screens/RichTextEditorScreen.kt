package id.avium.aviumnotes.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
import java.text.SimpleDateFormat
import java.util.*

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
                richTextState.toHtml() != (initialContent ?: note?.content ?: "") ||
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
            RichEditorTopBar(
                textColor = textColor,
                noteColor = noteColor,
                hasChanges = hasChanges,
                isNewNote = isNewNote,
                showFormattingBar = showFormattingBar,
                onToggleFormat = { showFormattingBar = !showFormattingBar },
                onBackClick = {
                    if (hasChanges && (title.isNotEmpty() || richTextState.annotatedString.text.isNotEmpty())) {
                        showDiscardDialog = true
                    } else {
                        onNavigateBack()
                    }
                },
                onColorClick = { showColorPicker = true },
                onMoreClick = { showMoreMenu = true },
                showMoreMenu = showMoreMenu,
                onDismissMenu = { showMoreMenu = false },
                onDeleteClick = { showMoreMenu = false; showDeleteDialog = true },
                onExportPng = {
                    // Export rich text note as PNG
                    val bitmap = id.avium.aviumnotes.ui.utils.ExportUtils.createBitmapFromText(
                        text = richTextState.annotatedString.text,
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
                    // Export rich text note as PDF
                    val bitmap = id.avium.aviumnotes.ui.utils.ExportUtils.createBitmapFromText(
                        text = richTextState.annotatedString.text,
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
                visible = title.isNotEmpty() || richTextState.annotatedString.text.isNotEmpty(),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val updatedNote = Note(
                            id = note?.id ?: 0,
                            title = title.ifEmpty { "Untitled" },
                            content = richTextState.toHtml(),
                            color = noteColor.toArgb(),
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
            // Metadata Header (Last Modified)
            if (!isNewNote && note != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rich Text • ${formatDate(note.updatedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.5f)
                    )
                }
            }

            // Title Field
            TextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                placeholder = {
                    Text(
                        stringResource(R.string.note_title_hint),
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
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = textColor
                ),
                singleLine = false,
                maxLines = 3
            )

            // Formatting Toolbar (Animated)
            AnimatedVisibility(
                visible = showFormattingBar,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = textColor.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(24.dp), // Pill shape
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bold
                        FormatButton(
                            icon = Icons.Outlined.FormatBold,
                            isActive = richTextState.currentSpanStyle.fontWeight == FontWeight.Bold,
                            textColor = textColor,
                            onClick = { richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) }
                        )
                        // Italic
                        FormatButton(
                            icon = Icons.Outlined.FormatItalic,
                            isActive = richTextState.currentSpanStyle.fontStyle == FontStyle.Italic,
                            textColor = textColor,
                            onClick = { richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) }
                        )
                        // Underline
                        FormatButton(
                            icon = Icons.Outlined.FormatUnderlined,
                            isActive = richTextState.currentSpanStyle.textDecoration == TextDecoration.Underline,
                            textColor = textColor,
                            onClick = { richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) }
                        )
                        // Strikethrough
                        FormatButton(
                            icon = Icons.Outlined.FormatStrikethrough,
                            isActive = richTextState.currentSpanStyle.textDecoration == TextDecoration.LineThrough,
                            textColor = textColor,
                            onClick = { richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) }
                        )
                    }
                }
            }

            // Rich Text Editor
            RichTextEditor(
                state = richTextState,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 400.dp)
                    .padding(horizontal = 16.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = textColor.copy(alpha = 0.9f),
                    lineHeight = 28.sp,
                    fontSize = 17.sp
                ),
                placeholder = {
                    Text(
                        stringResource(R.string.note_content_hint),
                        color = textColor.copy(alpha = 0.4f),
                        fontSize = 17.sp
                    )
                },
                colors = RichTextEditorDefaults.richTextEditorColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = textColor
                )
            )

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // --- Dialogs & Bottom Sheet ---
    if (showColorPicker) {
        ColorPickerBottomSheet(
            currentColor = noteColor,
            onColorSelected = { noteColor = it; showColorPicker = false },
            onDismiss = { showColorPicker = false },
            sheetState = sheetState
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Outlined.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Note?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { onDeleteNote(); onNavigateBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            icon = { Icon(Icons.Outlined.SaveAs, null) },
            title = { Text("Unsaved Changes") },
            text = { Text("Do you want to save before exiting?") },
            confirmButton = {
                Button(
                    onClick = {
                        // Logic save same as FAB
                        val updatedNote = Note(
                            id = note?.id ?: 0,
                            title = title.ifEmpty { "Untitled" },
                            content = richTextState.toHtml(),
                            color = noteColor.toArgb(),
                            createdAt = note?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            isPinned = note?.isPinned ?: false
                        )
                        onSaveNote(updatedNote)
                        onNavigateBack()
                    }
                ) { Text("Save & Exit") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false; onNavigateBack() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Discard") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RichEditorTopBar(
    textColor: Color,
    noteColor: Color,
    hasChanges: Boolean,
    isNewNote: Boolean,
    showFormattingBar: Boolean,
    onToggleFormat: () -> Unit,
    onBackClick: () -> Unit,
    onColorClick: () -> Unit,
    onMoreClick: () -> Unit,
    showMoreMenu: Boolean,
    onDismissMenu: () -> Unit,
    onDeleteClick: () -> Unit,
    onExportPng: () -> Unit,
    onExportPdf: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {},
        navigationIcon = {
            Surface(
                onClick = onBackClick,
                shape = CircleShape,
                color = textColor.copy(alpha = 0.05f),
                modifier = Modifier.padding(start = 12.dp).size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor
                    )
                }
            }
        },
        actions = {
            // Formatting Toggle
            IconButton(onClick = onToggleFormat) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if(showFormattingBar) textColor.copy(alpha=0.15f) else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.TextFields,
                        "Format",
                        tint = if(showFormattingBar) textColor else textColor.copy(alpha=0.6f)
                    )
                }
            }

            // Color Picker
            IconButton(onClick = onColorClick) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(textColor.copy(alpha = 0.1f))
                        .border(1.5.dp, textColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Palette, null, modifier = Modifier.size(16.dp), tint = textColor)
                }
            }

            if (!isNewNote) {
                Box {
                    IconButton(onClick = onMoreClick) {
                        Icon(Icons.Outlined.MoreVert, "More", tint = textColor)
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = onDismissMenu,
                        shape = RoundedCornerShape(16.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export PNG") },
                            onClick = { onDismissMenu(); onExportPng() },
                            leadingIcon = { Icon(Icons.Outlined.Image, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Export PDF") },
                            onClick = { onDismissMenu(); onExportPdf() },
                            leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, null) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = onDeleteClick,
                            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun FormatButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    textColor: Color,
    onClick: () -> Unit
) {
    val backgroundColor = if (isActive) textColor.copy(alpha = 0.15f) else Color.Transparent
    val contentColor = if (isActive) textColor else textColor.copy(alpha = 0.4f)

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(12.dp))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}