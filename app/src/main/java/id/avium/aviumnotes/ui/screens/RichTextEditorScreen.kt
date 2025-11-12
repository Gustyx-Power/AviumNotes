package id.avium.aviumnotes.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import java.io.File
import java.io.FileOutputStream

private data class InlineDrawingPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RichTextEditorScreen(
    note: Note?,
    initialTitle: String? = null,
    initialContent: String? = null,
    onSaveNote: (Note) -> Unit,
    onDeleteNote: () -> Unit,
    onNavigateBack: () -> Unit,
    onOpenDrawing: () -> Unit = {}
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

    var isDrawingMode by remember { mutableStateOf(false) }
    var drawingPaths by remember { mutableStateOf(listOf<InlineDrawingPath>()) }
    var currentPath by remember { mutableStateOf(Path()) }
    var currentDrawingColor by remember { mutableStateOf(Color.Black) }
    var currentStrokeWidth by remember { mutableStateOf(5f) }
    var undoStack by remember { mutableStateOf(listOf<InlineDrawingPath>()) }

    var showColorPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showFormattingBar by remember { mutableStateOf(true) }
    var showDrawingColorPicker by remember { mutableStateOf(false) }

    val hasChanges = remember(title, richTextState.annotatedString, drawingPaths) {
        title != (initialTitle ?: note?.title ?: "") ||
                richTextState.toHtml() != (initialContent ?: note?.content ?: "") ||
                drawingPaths.isNotEmpty()
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
                        if (isDrawingMode) {
                            isDrawingMode = false
                        } else if (hasChanges && (title.isNotEmpty() || richTextState.annotatedString.text.isNotEmpty() || drawingPaths.isNotEmpty())) {
                            showDiscardDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = textColor)
                    }
                },
                actions = {
                    if (isDrawingMode) {
                        IconButton(
                            onClick = {
                                if (drawingPaths.isNotEmpty()) {
                                    val lastPath = drawingPaths.last()
                                    undoStack = undoStack + lastPath
                                    drawingPaths = drawingPaths.dropLast(1)
                                }
                            },
                            enabled = drawingPaths.isNotEmpty()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, "Undo", tint = if (drawingPaths.isNotEmpty()) textColor else textColor.copy(alpha = 0.3f))
                        }
                        IconButton(
                            onClick = {
                                if (undoStack.isNotEmpty()) {
                                    val pathToRedo = undoStack.last()
                                    drawingPaths = drawingPaths + pathToRedo
                                    undoStack = undoStack.dropLast(1)
                                }
                            },
                            enabled = undoStack.isNotEmpty()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Redo, "Redo", tint = if (undoStack.isNotEmpty()) textColor else textColor.copy(alpha = 0.3f))
                        }
                    } else {
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
                                DropdownMenuItem(text = { Text("Share") }, onClick = { showMoreMenu = false }, leadingIcon = { Icon(Icons.Outlined.Share, null) })
                                DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { showMoreMenu = false; showDeleteDialog = true }, leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) })
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = noteColor)
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = title.isNotEmpty() || richTextState.annotatedString.text.isNotEmpty() || drawingPaths.isNotEmpty(),
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        var drawingPath: String? = null
                        if (drawingPaths.isNotEmpty()) {
                            val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            canvas.drawColor(android.graphics.Color.WHITE)
                            drawingPaths.forEach { dp ->
                                val paint = android.graphics.Paint().apply {
                                    color = dp.color.toArgb()
                                    strokeWidth = dp.strokeWidth
                                    style = android.graphics.Paint.Style.STROKE
                                    strokeCap = android.graphics.Paint.Cap.ROUND
                                    isAntiAlias = true
                                }
                                canvas.drawPath(dp.path.asAndroidPath(), paint)
                            }
                            val file = File(context.filesDir, "drawing_${note?.id ?: System.currentTimeMillis()}.png")
                            FileOutputStream(file).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                            drawingPath = file.absolutePath
                        }

                        val updatedNote = Note(
                            id = note?.id ?: 0,
                            title = title.ifEmpty { "Untitled" },
                            content = richTextState.toHtml(),
                            color = noteColor.hashCode(),
                            createdAt = note?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            isPinned = note?.isPinned ?: false,
                            spanCount = note?.spanCount ?: 1,
                            hasDrawing = drawingPaths.isNotEmpty() || (note?.hasDrawing == true),
                            drawingPath = drawingPath ?: note?.drawingPath
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
            if (!isDrawingMode) {
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
                            BadgedBox(badge = { if (drawingPaths.isNotEmpty() || note?.hasDrawing == true) Badge(containerColor = MaterialTheme.colorScheme.primary, modifier = Modifier.size(8.dp)) }) {
                                IconButton(onClick = { isDrawingMode = true }) {
                                    Icon(Icons.Outlined.Brush, "Draw", tint = if (drawingPaths.isNotEmpty() || note?.hasDrawing == true) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.7f))
                                }
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

                // Drawing Preview
                val showDrawingPreview = drawingPaths.isNotEmpty() || (note?.hasDrawing == true && note.drawingPath != null)

                AnimatedVisibility(
                    visible = showDrawingPreview,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 16.dp),
                            tonalElevation = 2.dp,
                            color = Color.White
                        ) {
                            if (drawingPaths.isNotEmpty()) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawingPaths.forEach { dp ->
                                        drawPath(path = dp.path, color = dp.color, style = Stroke(width = dp.strokeWidth, cap = StrokeCap.Round))
                                    }
                                }
                            } else if (note?.hasDrawing == true && note.drawingPath != null) {
                                val bitmap = remember(note.drawingPath) {
                                    BitmapFactory.decodeFile(note.drawingPath)
                                }
                                bitmap?.let {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = "Drawing",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text("Drawing Mode - Draw with your finger", style = MaterialTheme.typography.titleMedium, color = textColor, modifier = Modifier.padding(16.dp))

                    Surface(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp), color = Color.White, shadowElevation = 4.dp) {
                        Canvas(
                            modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset -> currentPath = Path().apply { moveTo(offset.x, offset.y) } },
                                    onDrag = { change, _ -> currentPath.lineTo(change.position.x, change.position.y) },
                                    onDragEnd = {
                                        drawingPaths = drawingPaths + InlineDrawingPath(path = currentPath, color = currentDrawingColor, strokeWidth = currentStrokeWidth)
                                        currentPath = Path()
                                        undoStack = emptyList()
                                    }
                                )
                            }
                        ) {
                            drawingPaths.forEach { dp -> drawPath(path = dp.path, color = dp.color, style = Stroke(width = dp.strokeWidth, cap = StrokeCap.Round)) }
                            drawPath(path = currentPath, color = currentDrawingColor, style = Stroke(width = currentStrokeWidth, cap = StrokeCap.Round))
                        }
                    }

                    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp, shadowElevation = 8.dp) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(shape = CircleShape, color = currentDrawingColor, modifier = Modifier.size(48.dp).clickable { showDrawingColorPicker = true }, border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline)) {}
                                    OutlinedButton(onClick = { drawingPaths = emptyList(); undoStack = emptyList(); currentPath = Path() }, shape = RoundedCornerShape(12.dp)) {
                                        Icon(Icons.Outlined.Delete, "Clear", modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Clear")
                                    }
                                }
                                Button(onClick = { isDrawingMode = false }, shape = RoundedCornerShape(12.dp)) {
                                    Icon(Icons.Filled.Check, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Done")
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Column {
                                Text("Stroke: ${currentStrokeWidth.toInt()}px", style = MaterialTheme.typography.bodySmall)
                                Slider(value = currentStrokeWidth, onValueChange = { currentStrokeWidth = it }, valueRange = 1f..50f, steps = 48)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        ColorPickerBottomSheet(currentColor = noteColor, onColorSelected = { noteColor = it; showColorPicker = false }, onDismiss = { showColorPicker = false }, sheetState = sheetState)
    }

    if (showDrawingColorPicker) {
        val colors = listOf(Color.Black, Color.Red, Color(0xFFFF6B00), Color(0xFFFFC107), Color.Green, Color.Blue, Color(0xFF9C27B0), Color(0xFFE91E63), Color.Gray, Color.White)
        AlertDialog(
            onDismissRequest = { showDrawingColorPicker = false },
            title = { Text("Choose Color") },
            text = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colors.take(5).forEach { color ->
                            Surface(shape = CircleShape, color = color, modifier = Modifier.size(40.dp).weight(1f).clickable { currentDrawingColor = color; showDrawingColorPicker = false }, border = if (color == currentDrawingColor) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)) {}
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colors.drop(5).forEach { color ->
                            Surface(shape = CircleShape, color = color, modifier = Modifier.size(40.dp).weight(1f).clickable { currentDrawingColor = color; showDrawingColorPicker = false }, border = if (color == currentDrawingColor) androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)) {}
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDrawingColorPicker = false }) { Text("Close") } }
        )
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
