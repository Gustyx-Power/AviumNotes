package id.avium.aviumnotes.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.avium.aviumnotes.R
import id.avium.aviumnotes.ui.theme.IosBlack
import id.avium.aviumnotes.ui.theme.IosDarkGray
import id.avium.aviumnotes.ui.theme.IosPeach

@Composable
fun IosEnhancedFab(
    showOptions: Boolean,
    onToggleOptions: () -> Unit,
    onAddNote: () -> Unit,
    onAddDrawing: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedVisibility(
            visible = showOptions,
            enter = fadeIn() + slideInVertically { it / 2 } + expandVertically(),
            exit = fadeOut() + slideOutVertically { it / 2 } + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Drawing Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = IosDarkGray,
                        shadowElevation = 2.dp,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            stringResource(R.string.editor_drawing),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                    SmallFloatingActionButton(
                        onClick = onAddDrawing,
                        containerColor = IosDarkGray,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.Brush, stringResource(R.string.editor_drawing))
                    }
                }

                // Note Button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = IosDarkGray,
                        shadowElevation = 2.dp,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            stringResource(R.string.note_editor_new_note),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                    SmallFloatingActionButton(
                        onClick = onAddNote,
                        containerColor = IosDarkGray,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.Edit, stringResource(R.string.note_editor_new_note))
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onToggleOptions,
            containerColor = IosPeach,
            contentColor = IosBlack,
            shape = RoundedCornerShape(16.dp),
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
        ) {
            AnimatedContent(targetState = showOptions, label = "FabIcon") { expanded ->
                Icon(
                    imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.Add,
                    contentDescription = if (expanded) stringResource(R.string.close) else "New",
                    modifier = Modifier.size(24.dp).rotate(if (expanded) 90f else 0f)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(if (showOptions) "Close" else "New", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}
