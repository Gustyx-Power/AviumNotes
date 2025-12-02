package id.avium.aviumnotes.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import id.avium.aviumnotes.R
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

    // State Drawing
    var drawingPaths by remember { mutableStateOf(listOf<DrawingPath>()) }
    var currentPath by remember { mutableStateOf(Path()) }
    var pathVersion by remember { mutableIntStateOf(0) }
    var currentDrawingColor by remember { mutableStateOf(Color.Black) }
    var currentStrokeWidth by remember { mutableStateOf(8f) }
    var undoStack by remember { mutableStateOf(listOf<DrawingPath>()) }

    var showColorPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Colors Palette for Drawing
    val drawingColors = remember {
        listOf(
            Color.Black, Color.DarkGray, Color.Red, Color(0xFFFF6B00),
            Color(0xFFFFC107), Color(0xFF4CAF50), Color(0xFF2196F3),
            Color(0xFF3F51B5), Color(0xFF9C27B0), Color(0xFFE91E63)
        )
    }

    // Load existing drawing
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
        containerColor = noteColor,
        topBar = {
            DrawingTopBar(
                textColor = textColor,
                canUndo = drawingPaths.isNotEmpty(),
                canRedo = undoStack.isNotEmpty(),
                onNavigateBack = onNavigateBack,
                onUndo = {
                    if (drawingPaths.isNotEmpty()) {
                        val lastPath = drawingPaths.last()
                        undoStack = undoStack + lastPath
                        drawingPaths = drawingPaths.dropLast(1)
                        pathVersion++
                    }
                },
                onRedo = {
                    if (undoStack.isNotEmpty()) {
                        val pathToRedo = undoStack.last()
                        drawingPaths = drawingPaths + pathToRedo
                        undoStack = undoStack.dropLast(1)
                        pathVersion++
                    }
                },
                onBackgroundClick = { showColorPicker = true },
                onMoreClick = { showMoreMenu = true },
                showMoreMenu = showMoreMenu,
                onDismissMenu = { showMoreMenu = false },
                onExportPng = {
                    // Export drawing to PNG
                    val screenWidth = context.resources.displayMetrics.widthPixels
                    val screenHeight = context.resources.displayMetrics.heightPixels
                    val density = context.resources.displayMetrics.density
                    val canvasWidth = screenWidth - (32 * density).toInt()
                    val canvasHeight = screenHeight - (200 * density).toInt()

                    val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
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

                    val uri = id.avium.aviumnotes.ui.utils.ExportUtils.exportToPng(
                        context,
                        bitmap,
                        "${context.getString(R.string.file_drawing_basename)}_${System.currentTimeMillis()}.png"
                    )

                    if (uri != null) {
                        android.widget.Toast.makeText(context, context.getString(R.string.export_png_success), android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                onExportPdf = {
                    // Export drawing to PDF
                    val screenWidth = context.resources.displayMetrics.widthPixels
                    val screenHeight = context.resources.displayMetrics.heightPixels
                    val density = context.resources.displayMetrics.density
                    val canvasWidth = screenWidth - (32 * density).toInt()
                    val canvasHeight = screenHeight - (200 * density).toInt()

                    val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
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

                    val uri = id.avium.aviumnotes.ui.utils.ExportUtils.exportToPdf(
                        context,
                        bitmap,
                        note?.title ?: context.getString(R.string.drawing_title),
                        "${context.getString(R.string.file_drawing_basename)}_${System.currentTimeMillis()}.pdf"
                    )

                    if (uri != null) {
                        android.widget.Toast.makeText(context, context.getString(R.string.export_pdf_success), android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                onDelete = { showDeleteDialog = true }
            )
        },
        bottomBar = {
            DrawingBottomToolbar(
                currentStrokeWidth = currentStrokeWidth,
                onStrokeWidthChange = { currentStrokeWidth = it },
                currentColor = currentDrawingColor,
                colors = drawingColors,
                onColorSelected = { currentDrawingColor = it },
                onClearCanvas = {
                    drawingPaths = emptyList()
                    undoStack = emptyList()
                    pathVersion++
                }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = drawingPaths.isNotEmpty(),
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        // SAVE LOGIC
                        val screenWidth = context.resources.displayMetrics.widthPixels
                        val screenHeight = context.resources.displayMetrics.heightPixels
                        val density = context.resources.displayMetrics.density
                        // Canvas dimensions (match UI padding)
                        val canvasWidth = screenWidth - (32 * density).toInt()
                        val canvasHeight = screenHeight - (200 * density).toInt()

                        val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
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

                        val file = File(context.filesDir, "drawing_${System.currentTimeMillis()}.png")
                        FileOutputStream(file).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                        }

                        val drawingData = id.avium.aviumnotes.ui.utils.DrawingSerializer.serializeDrawingPaths(drawingPaths)

                        val updatedNote = Note(
                            id = note?.id ?: 0,
                            title = context.getString(R.string.drawing_title),
                            content = "",
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
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Filled.Check, stringResource(R.string.save))
                }
            }
        }
    ) { paddingValues ->
        // CANVAS AREA
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(6.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = Color.White
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPath = Path().apply { moveTo(offset.x, offset.y) }
                                    pathVersion++
                                },
                                onDrag = { change, _ ->
                                    currentPath.lineTo(change.position.x, change.position.y)
                                    pathVersion++
                                },
                                onDragEnd = {
                                    drawingPaths = drawingPaths + DrawingPath(
                                        currentPath,
                                        currentDrawingColor,
                                        currentStrokeWidth
                                    )
                                    currentPath = Path()
                                    undoStack = emptyList() // Clear redo stack on new action
                                    pathVersion++
                                }
                            )
                        }
                ) {
                    // Force redraw
                    pathVersion.let { }

                    drawingPaths.forEach { dp ->
                        drawPath(
                            path = dp.path,
                            color = dp.color,
                            style = Stroke(width = dp.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                    drawPath(
                        path = currentPath,
                        color = currentDrawingColor,
                        style = Stroke(width = currentStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }

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
            icon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.note_delete_title)) },
            text = { Text(stringResource(R.string.note_delete_message)) },
            confirmButton = {
                Button(
                    onClick = { onDeleteDrawing(); onNavigateBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.note_delete_confirm)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.note_delete_cancel)) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingTopBar(
    textColor: Color,
    canUndo: Boolean,
    canRedo: Boolean,
    onNavigateBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onBackgroundClick: () -> Unit,
    onMoreClick: () -> Unit,
    showMoreMenu: Boolean,
    onDismissMenu: () -> Unit,
    onExportPng: () -> Unit,
    onExportPdf: () -> Unit,
    onDelete: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Surface(
                    shape = CircleShape,
                    color = textColor.copy(alpha = 0.05f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.main_back), tint = textColor)
                    }
                }
            }
        },
        actions = {
            // Undo
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo, stringResource(R.string.drawing_undo),
                    tint = if(canUndo) textColor else textColor.copy(alpha = 0.3f)
                )
            }
            // Redo
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo, stringResource(R.string.drawing_redo),
                    tint = if(canRedo) textColor else textColor.copy(alpha = 0.3f)
                )
            }
            // Background Color
            IconButton(onClick = onBackgroundClick) {
                Box(
                    modifier = Modifier.size(24.dp).clip(CircleShape).background(textColor.copy(alpha=0.1f)).border(1.5.dp, textColor.copy(alpha=0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Palette, null, tint = textColor, modifier = Modifier.size(16.dp))
                }
            }
            // More Menu
            Box {
                IconButton(onClick = onMoreClick) {
                    Icon(Icons.Outlined.MoreVert, stringResource(R.string.editor_more), tint = textColor)
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = onDismissMenu,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.export_png)) },
                        onClick = { onDismissMenu(); onExportPng() },
                        leadingIcon = { Icon(Icons.Outlined.Image, null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.export_pdf)) },
                        onClick = { onDismissMenu(); onExportPdf() },
                        leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, null) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.note_delete_title), color = MaterialTheme.colorScheme.error) },
                        onClick = { onDismissMenu(); onDelete() },
                        leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
fun DrawingBottomToolbar(
    currentStrokeWidth: Float,
    onStrokeWidthChange: (Float) -> Unit,
    currentColor: Color,
    colors: List<Color>,
    onColorSelected: (Color) -> Unit,
    onClearCanvas: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp, top = 12.dp)
        ) {
            // Handle Bar (Visual cue for sheet)
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stroke Slider Section
            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brush Preview
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size((currentStrokeWidth / 2).coerceIn(4f, 28f).dp)
                            .clip(CircleShape)
                            .background(currentColor)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Slider(
                    value = currentStrokeWidth,
                    onValueChange = onStrokeWidthChange,
                    valueRange = 1f..50f,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Clear Button
                IconButton(
                    onClick = onClearCanvas,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Outlined.DeleteSweep, stringResource(R.string.drawing_clear))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Color Palette (Horizontal Scroll)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(colors) { color ->
                    val isSelected = color == currentColor
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { onColorSelected(color) }
                            .then(
                                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape).padding(2.dp)
                                else Modifier
                            )
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = getContrastColor(color), // Reuse util to get readable checkmark
                                modifier = Modifier.size(20.dp).align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
    }
}