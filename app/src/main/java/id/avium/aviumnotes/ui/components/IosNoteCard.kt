package id.avium.aviumnotes.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.avium.aviumnotes.R
import id.avium.aviumnotes.data.local.entity.Note
import id.avium.aviumnotes.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

fun parseNoteContent(rawContent: String): Pair<String, List<String>> {
    val stripHtml = rawContent.replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .trim()
    val words = stripHtml.split(Regex("\\s+"))
    val hashtags = words.filter { it.startsWith("#") && it.length > 1 }
    val normalWordStr = words.filter { !it.startsWith("#") }.joinToString(" ")
    return Pair(normalWordStr, hashtags)
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    return sdf.format(Date(timestamp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IosNoteCard(
    note: Note,
    showPreview: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isResizeMode by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isResizeMode) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "CardScale"
    )

    val (contentStr, hashtags) = parseNoteContent(note.content)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                onClick = { if (isResizeMode) isResizeMode = false else onClick() },
                onLongClick = { isResizeMode = !isResizeMode }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = IosDarkGray)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = note.title.ifEmpty { stringResource(R.string.note_untitled) },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    
                    if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = IosPeach,
                            modifier = Modifier.size(20.dp).rotate(45f)
                        )
                    }
                }

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
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }

                if (contentStr.isNotEmpty() && showPreview) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = contentStr,
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp, fontSize = 16.sp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = IosTextGray.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        hashtags.take(3).forEachIndexed { index, tag ->
                            val bgColor = HashtagColors[Math.abs(tag.hashCode()) % HashtagColors.size]
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = bgColor
                            ) {
                                Text(
                                    text = tag,
                                    color = Color.Black.copy(alpha=0.8f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = formatDate(note.updatedAt),
                        color = IosTextGray.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }

            if (isResizeMode) {
                Surface(
                    modifier = Modifier.matchParentSize(),
                    color = IosBlack.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = { showDeleteDialog = true },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Icon(Icons.Outlined.Delete, stringResource(R.string.note_delete), tint = MaterialTheme.colorScheme.onErrorContainer)
                        }

                        FilledIconButton(
                            onClick = { onTogglePin(); isResizeMode = false },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = IosSearchGray)
                        ) {
                            Icon(
                                if(note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                stringResource(R.string.note_pin),
                                tint = Color.White
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
