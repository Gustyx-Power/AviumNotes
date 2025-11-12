package id.avium.aviumnotes.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    noteId: Long,
    onSaveDrawing: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    var paths by remember { mutableStateOf(listOf<DrawingPath>()) }
    var currentPath by remember { mutableStateOf(Path()) }
    var currentColor by remember { mutableStateOf(Color.Black) }
    var currentStrokeWidth by remember { mutableStateOf(5f) }

    var undoStack by remember { mutableStateOf(listOf<DrawingPath>()) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showStrokeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Draw", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Undo
                    IconButton(
                        onClick = {
                            if (paths.isNotEmpty()) {
                                val lastPath = paths.last()
                                undoStack = undoStack + lastPath
                                paths = paths.dropLast(1)
                            }
                        },
                        enabled = paths.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }

                    // Redo
                    IconButton(
                        onClick = {
                            if (undoStack.isNotEmpty()) {
                                val pathToRedo = undoStack.last()
                                paths = paths + pathToRedo
                                undoStack = undoStack.dropLast(1)
                            }
                        },
                        enabled = undoStack.isNotEmpty()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo")
                    }

                    // Clear all
                    IconButton(
                        onClick = {
                            paths = emptyList()
                            undoStack = emptyList()
                            currentPath = Path()
                        },
                        enabled = paths.isNotEmpty()
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Clear")
                    }
                }
            )
        },
        bottomBar = {
            DrawingToolbar(
                currentColor = currentColor,
                currentStrokeWidth = currentStrokeWidth,
                onColorClick = { showColorPicker = true },
                onStrokeClick = { showStrokeDialog = true },
                onSave = {
                    // Save bitmap to file
                    val bitmap = createBitmapFromPaths(paths, 800, 600)
                    val file = File(context.filesDir, "drawing_$noteId.png")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    onSaveDrawing(file.absolutePath)
                    onNavigateBack()
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath = Path().apply {
                                    moveTo(offset.x, offset.y)
                                }
                            },
                            onDrag = { change, _ ->
                                currentPath.lineTo(change.position.x, change.position.y)
                            },
                            onDragEnd = {
                                paths = paths + DrawingPath(
                                    path = currentPath,
                                    color = currentColor,
                                    strokeWidth = currentStrokeWidth
                                )
                                currentPath = Path()
                                undoStack = emptyList()
                            }
                        )
                    }
            ) {
                // Draw all paths
                paths.forEach { drawPath ->
                    drawPath(
                        path = drawPath.path,
                        color = drawPath.color,
                        style = Stroke(width = drawPath.strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Draw current path
                drawPath(
                    path = currentPath,
                    color = currentColor,
                    style = Stroke(width = currentStrokeWidth, cap = StrokeCap.Round)
                )
            }
        }
    }

    // Color Picker Dialog
    if (showColorPicker) {
        DrawingColorPickerDialog(
            currentColor = currentColor,
            onColorSelected = {
                currentColor = it
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    // Stroke Width Dialog
    if (showStrokeDialog) {
        StrokeWidthDialog(
            currentWidth = currentStrokeWidth,
            onWidthSelected = {
                currentStrokeWidth = it
                showStrokeDialog = false
            },
            onDismiss = { showStrokeDialog = false }
        )
    }
}

@Composable
fun DrawingToolbar(
    currentColor: Color,
    currentStrokeWidth: Float,
    onColorClick: () -> Unit,
    onStrokeClick: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Color indicator
                Surface(
                    shape = CircleShape,
                    color = currentColor,
                    modifier = Modifier
                        .size(48.dp),
                    onClick = onColorClick,
                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                ) {}

                // Stroke width
                OutlinedButton(
                    onClick = onStrokeClick,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.LineWeight, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${currentStrokeWidth.toInt()}px")
                }
            }

            // Save button
            Button(
                onClick = onSave,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Drawing")
            }
        }
    }
}

@Composable
fun DrawingColorPickerDialog(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = listOf(
        Color.Black, Color.Red, Color(0xFFFF6B00), Color(0xFFFFC107),
        Color.Green, Color.Blue, Color(0xFF9C27B0), Color(0xFFE91E63),
        Color.Gray, Color.White
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Color") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colors.forEach { color ->
                    Surface(
                        shape = CircleShape,
                        color = color,
                        modifier = Modifier
                            .size(40.dp)
                            .weight(1f),
                        onClick = { onColorSelected(color) },
                        border = if (color == currentColor)
                            androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                        else
                            androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                    ) {}
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun StrokeWidthDialog(
    currentWidth: Float,
    onWidthSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderValue by remember { mutableStateOf(currentWidth) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stroke Width") },
        text = {
            Column {
                Text("${sliderValue.toInt()}px", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 1f..50f,
                    steps = 48
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onWidthSelected(sliderValue) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper function to convert paths to bitmap
fun createBitmapFromPaths(paths: List<DrawingPath>, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    paths.forEach { drawPath ->
        val paint = android.graphics.Paint().apply {
            color = drawPath.color.toArgb()
            strokeWidth = drawPath.strokeWidth
            style = android.graphics.Paint.Style.STROKE
            strokeCap = android.graphics.Paint.Cap.ROUND
            isAntiAlias = true
        }
        canvas.drawPath(drawPath.path.asAndroidPath(), paint)
    }

    return bitmap
}
