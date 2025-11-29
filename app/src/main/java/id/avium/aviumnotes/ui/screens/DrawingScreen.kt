package id.avium.aviumnotes.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.avium.aviumnotes.data.local.entity.Note
import id.avium.aviumnotes.data.preferences.PreferencesManager
import id.avium.aviumnotes.ui.components.ColorPickerBottomSheet
import id.avium.aviumnotes.ui.theme.NoteColors
import id.avium.aviumnotes.ui.utils.getContrastColor
import java.io.File
import java.io.FileOutputStream

data class DrawingPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(
    note: Note?,
    onSaveDrawing: (Note) -> Unit,
    onDeleteDrawing: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val defaultNoteColor by preferencesManager.defaultNoteColor.collectAsState(
        initial = NoteColors.White.hashCode()
    )

    var noteColor by remember(note, defaultNoteColor) {
        mutableStateOf(Color(note?.color ?: defaultNoteColor))
    }

    var drawingPaths by remember { mutableStateOf(listOf<DrawingPath>()) }
    var currentPath by remember { mutableStateOf(Path()) }
    var pathVersion by remember { mutableIntStateOf(0) } // Trigger recomposition
    var currentDrawingColor by remember { mutableStateOf(Color.Black) }
    var currentStrokeWidth by remember { mutableStateOf(5f) }
    var undoStack by remember { mutableStateOf(listOf<DrawingPath>()) }

    var showColorPicker by remember { mutableStateOf(false) }
    var showDrawingColorPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Load existing drawing data for editing
    LaunchedEffect(note?.drawingData) {
        if (note?.drawingData != null && drawingPaths.isEmpty()) {
            try {
                val loadedPaths = id.avium.aviumnotes.ui.utils.DrawingSerializer
                    .deserializeDrawingPaths(note.drawingData)
                drawingPaths = loadedPaths
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val textColor = remember(noteColor) { getContrastColor(noteColor) }
    val sheetState = rememberModalBottomSheetState()
    val isNewDrawing = note == null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = textColor)
                    }
                },
                actions = {
                    // Always show undo/redo for editing
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
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                "Undo",
                                tint = if (drawingPaths.isNotEmpty()) textColor else textColor.copy(alpha = 0.3f)
                            )
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
                            Icon(
                                Icons.AutoMirrored.Filled.Redo,
                                "Redo",
                                tint = if (undoStack.isNotEmpty()) textColor else textColor.copy(alpha = 0.3f)
                            )
                        }

                    IconButton(onClick = { showColorPicker = true }) {
                        Icon(Icons.Outlined.Palette, "Background", tint = textColor)
                    }

                    if (!isNewDrawing) {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, "More", tint = textColor)
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMoreMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = noteColor)
            )
        },
        floatingActionButton = {
            if (drawingPaths.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        // Save drawing with editable data
                        val screenWidth = context.resources.displayMetrics.widthPixels
                        val screenHeight = context.resources.displayMetrics.heightPixels
                        val density = context.resources.displayMetrics.density

                        val canvasWidth = screenWidth - (32 * density).toInt()
                        val canvasHeight = screenHeight - (200 * density).toInt()

                        // Create bitmap preview
                        val bitmap = Bitmap.createBitmap(
                            canvasWidth,
                            canvasHeight,
                            Bitmap.Config.ARGB_8888
                        )
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

                        val file = File(
                            context.filesDir,
                            "drawing_${note?.id ?: System.currentTimeMillis()}.png"
                        )
                        FileOutputStream(file).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                        }

                        // Serialize drawing data for editing
                        val drawingData = id.avium.aviumnotes.ui.utils.DrawingSerializer
                            .serializeDrawingPaths(drawingPaths)

                        val updatedNote = Note(
                            id = note?.id ?: 0,
                            title = "Drawing",
                            content = "",  // No rich text content
                            color = noteColor.hashCode(),
                            createdAt = note?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            isPinned = note?.isPinned ?: false,
                            spanCount = note?.spanCount ?: 1,
                            hasDrawing = true,
                            drawingPath = file.absolutePath,
                            drawingData = drawingData
                        )
                        onSaveDrawing(updatedNote)
                        onNavigateBack()
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Check, "Save")
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
            Surface(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                // Canvas for drawing (editable)
                Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentPath = Path().apply {
                                            moveTo(offset.x, offset.y)
                                        }
                                        pathVersion++ // Trigger recomposition
                                    },
                                    onDrag = { change, _ ->
                                        currentPath.lineTo(
                                            change.position.x,
                                            change.position.y
                                        )
                                        pathVersion++ // Trigger recomposition
                                    },
                                    onDragEnd = {
                                        drawingPaths = drawingPaths + DrawingPath(
                                            currentPath,
                                            currentDrawingColor,
                                            currentStrokeWidth
                                        )
                                        currentPath = Path()
                                        undoStack = emptyList()
                                    }
                                )
                            }
                    ) {
                        // Reference pathVersion to ensure recomposition on path changes
                        pathVersion.let { }

                        drawingPaths.forEach { dp ->
                            drawPath(
                                path = dp.path,
                                color = dp.color,
                                style = Stroke(
                                    width = dp.strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            )
                        }
                        drawPath(
                            path = currentPath,
                            color = currentDrawingColor,
                            style = Stroke(
                                width = currentStrokeWidth,
                                cap = StrokeCap.Round
                            )
                        )
                    }
            }

            // Drawing toolbar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = CircleShape,
                                    color = currentDrawingColor,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clickable { showDrawingColorPicker = true },
                                    border = androidx.compose.foundation.BorderStroke(
                                        2.dp,
                                        MaterialTheme.colorScheme.outline
                                    )
                                ) {}
                                OutlinedButton(
                                    onClick = {
                                        drawingPaths = emptyList()
                                        undoStack = emptyList()
                                        currentPath = Path()
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        "Clear",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Stroke: ${currentStrokeWidth.toInt()}px",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Slider(
                            value = currentStrokeWidth,
                            onValueChange = { currentStrokeWidth = it },
                            valueRange = 1f..50f,
                            steps = 48
                        )
                    }
                }
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

    if (showDrawingColorPicker) {
        val colors = listOf(
            Color.Black, Color.Red, Color(0xFFFF6B00), Color(0xFFFFC107),
            Color.Green, Color.Blue, Color(0xFF9C27B0), Color(0xFFE91E63),
            Color.Gray, Color.White
        )
        AlertDialog(
            onDismissRequest = { showDrawingColorPicker = false },
            title = { Text("Choose Color") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colors.take(5).forEach { color ->
                            Surface(
                                shape = CircleShape,
                                color = color,
                                modifier = Modifier
                                    .size(40.dp)
                                    .weight(1f)
                                    .clickable {
                                        currentDrawingColor = color
                                        showDrawingColorPicker = false
                                    },
                                border = if (color == currentDrawingColor)
                                    androidx.compose.foundation.BorderStroke(
                                        3.dp,
                                        MaterialTheme.colorScheme.primary
                                    )
                                else
                                    androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                            ) {}
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colors.drop(5).forEach { color ->
                            Surface(
                                shape = CircleShape,
                                color = color,
                                modifier = Modifier
                                    .size(40.dp)
                                    .weight(1f)
                                    .clickable {
                                        currentDrawingColor = color
                                        showDrawingColorPicker = false
                                    },
                                border = if (color == currentDrawingColor)
                                    androidx.compose.foundation.BorderStroke(
                                        3.dp,
                                        MaterialTheme.colorScheme.primary
                                    )
                                else
                                    androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                            ) {}
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDrawingColorPicker = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Outlined.Delete, null) },
            title = { Text("Delete Drawing?") },
            text = { Text("This drawing will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteDrawing()
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
}
