package id.avium.aviumnotes.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import id.avium.aviumnotes.BuildConfig
import id.avium.aviumnotes.R
import id.avium.aviumnotes.data.local.AppDatabase
import id.avium.aviumnotes.data.local.entity.Note
import id.avium.aviumnotes.data.preferences.PreferencesManager
import id.avium.aviumnotes.data.repository.NoteRepository
import id.avium.aviumnotes.service.FloatingBubbleService
import id.avium.aviumnotes.ui.components.ColorPickerDialog
import id.avium.aviumnotes.ui.theme.NoteColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val repository = remember { NoteRepository(AppDatabase.getInstance(context).noteDao()) }
    val coroutineScope = rememberCoroutineScope()

    val themeMode by preferencesManager.themeMode.collectAsState(initial = "system")
    val sortBy by preferencesManager.sortBy.collectAsState(initial = "date_modified")
    val defaultNoteColor by preferencesManager.defaultNoteColor.collectAsState(initial = NoteColors.White.hashCode())
    val showNotePreview by preferencesManager.showNotePreview.collectAsState(initial = true)
    val floatingBubbleEnabled by preferencesManager.floatingBubbleEnabled.collectAsState(initial = false)
    val autoSaveEnabled by preferencesManager.autoSaveEnabled.collectAsState(initial = true)
    val deleteConfirmation by preferencesManager.deleteConfirmation.collectAsState(initial = true)

    var showThemeDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                exportNotes(context, repository, uri)
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                importNotes(context, repository, uri)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            ModernSettingsSection(title = stringResource(R.string.settings_appearance)) {
                ModernSettingsItem(
                    icon = Icons.Outlined.Palette,
                    title = stringResource(R.string.settings_theme),
                    subtitle = when (themeMode) {
                        "light" -> stringResource(R.string.settings_theme_light)
                        "dark" -> stringResource(R.string.settings_theme_dark)
                        else -> stringResource(R.string.settings_theme_system)
                    },
                    onClick = { showThemeDialog = true }
                )

                ModernSettingsItem(
                    icon = Icons.Outlined.Sort,
                    title = stringResource(R.string.settings_sort_by),
                    subtitle = when (sortBy) {
                        "date_modified" -> stringResource(R.string.settings_sort_date_modified)
                        "date_created" -> stringResource(R.string.settings_sort_date_created)
                        else -> stringResource(R.string.settings_sort_title)
                    },
                    onClick = { showSortDialog = true }
                )

                ModernSettingsItem(
                    icon = Icons.Outlined.ColorLens,
                    title = stringResource(R.string.settings_default_color),
                    subtitle = NoteColors.getColorName(defaultNoteColor),
                    onClick = { showColorPicker = true },
                    endContent = {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(defaultNoteColor))
                                .padding(1.dp)
                        )
                    }
                )

                ModernSettingsSwitchItem(
                    icon = Icons.Outlined.Visibility,
                    title = stringResource(R.string.settings_show_preview),
                    subtitle = stringResource(R.string.settings_show_preview_desc),
                    checked = showNotePreview,
                    onCheckedChange = { coroutineScope.launch { preferencesManager.setShowNotePreview(it) } }
                )
            }

            ModernSettingsSection(title = stringResource(R.string.settings_floating_bubble)) {
                ModernSettingsSwitchItem(
                    icon = Icons.Outlined.BubbleChart,
                    title = stringResource(R.string.settings_floating_bubble),
                    subtitle = stringResource(R.string.settings_floating_bubble_desc),
                    checked = floatingBubbleEnabled,
                    onCheckedChange = { checked ->
                        coroutineScope.launch {
                            preferencesManager.setFloatingBubbleEnabled(checked)
                            if (checked) {
                                if (Settings.canDrawOverlays(context)) {
                                    val intent = Intent(context, FloatingBubbleService::class.java)
                                    intent.action = FloatingBubbleService.ACTION_START
                                    context.startService(intent)
                                } else {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            } else {
                                val intent = Intent(context, FloatingBubbleService::class.java)
                                intent.action = FloatingBubbleService.ACTION_STOP
                                context.startService(intent)
                            }
                        }
                    }
                )
            }

            ModernSettingsSection(title = stringResource(R.string.settings_behavior)) {
                ModernSettingsSwitchItem(
                    icon = Icons.Outlined.Save,
                    title = stringResource(R.string.settings_auto_save),
                    subtitle = stringResource(R.string.settings_auto_save_desc),
                    checked = autoSaveEnabled,
                    onCheckedChange = { coroutineScope.launch { preferencesManager.setAutoSaveEnabled(it) } }
                )

                ModernSettingsSwitchItem(
                    icon = Icons.Outlined.Warning,
                    title = stringResource(R.string.settings_delete_confirm),
                    subtitle = stringResource(R.string.settings_delete_confirm_desc),
                    checked = deleteConfirmation,
                    onCheckedChange = { coroutineScope.launch { preferencesManager.setDeleteConfirmation(it) } }
                )
            }

            ModernSettingsSection(title = stringResource(R.string.settings_data)) {
                ModernSettingsItem(
                    icon = Icons.Outlined.Upload,
                    title = stringResource(R.string.settings_export),
                    subtitle = "Export all notes as JSON",
                    onClick = {
                        exportLauncher.launch("aviumnotes_backup_${System.currentTimeMillis()}.json")
                    }
                )

                ModernSettingsItem(
                    icon = Icons.Outlined.Download,
                    title = stringResource(R.string.settings_import),
                    subtitle = "Import notes from file",
                    onClick = { importLauncher.launch("application/json") }
                )

                ModernSettingsItem(
                    icon = Icons.Outlined.DeleteForever,
                    title = stringResource(R.string.settings_clear_data),
                    subtitle = "Delete all notes permanently",
                    onClick = { showClearDataDialog = true },
                    isDestructive = true
                )
            }

            ModernSettingsSection(title = stringResource(R.string.settings_about)) {
                ModernSettingsItem(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.settings_version),
                    subtitle = BuildConfig.VERSION_NAME,
                    onClick = { }
                )

                ModernSettingsItem(
                    icon = Icons.Outlined.Person,
                    title = stringResource(R.string.settings_developer),
                    subtitle = "Gustyx-Power",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Gustyx-Power"))
                        context.startActivity(intent)
                    }
                )

                ModernSettingsItem(
                    icon = Icons.Outlined.Code,
                    title = stringResource(R.string.settings_github),
                    subtitle = "View source code",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Gustyx-Power/AviumNotes"))
                        context.startActivity(intent)
                    }
                )

                ModernSettingsItem(
                    icon = Icons.Outlined.Description,
                    title = stringResource(R.string.settings_licenses),
                    subtitle = "MIT License",
                    onClick = {
                        Toast.makeText(
                            context,
                            "AviumNotes is licensed under MIT License\n© 2025 Gustyx-Power",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Made with Spirit for Avium OS",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .wrapContentWidth(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showThemeDialog) {
        ModernThemeDialog(
            currentTheme = themeMode,
            onThemeSelected = { theme ->
                coroutineScope.launch {
                    preferencesManager.setThemeMode(theme)
                    showThemeDialog = false
                }
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showSortDialog) {
        ModernSortDialog(
            currentSort = sortBy,
            onSortSelected = { sort ->
                coroutineScope.launch {
                    preferencesManager.setSortBy(sort)
                    showSortDialog = false
                }
            },
            onDismiss = { showSortDialog = false }
        )
    }

    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = Color(defaultNoteColor),
            onColorSelected = { color ->
                coroutineScope.launch {
                    preferencesManager.setDefaultNoteColor(color.hashCode())
                }
            },
            onDismiss = { showColorPicker = false }
        )
    }

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            icon = { Icon(Icons.Outlined.Warning, contentDescription = null) },
            title = { Text(stringResource(R.string.clear_data_title)) },
            text = { Text(stringResource(R.string.clear_data_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val notes = repository.getAllNotes().first()
                            notes.forEach { repository.deleteNote(it) }
                            Toast.makeText(context, context.getString(R.string.clear_data_success), Toast.LENGTH_SHORT).show()
                            showClearDataDialog = false
                        }
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ModernSettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 1.dp
        ) {
            Column { content() }
        }
    }
}

@Composable
fun ModernSettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    endContent: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isDestructive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (endContent != null) {
                endContent()
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ModernSettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color.Transparent) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun ModernThemeDialog(
    currentTheme: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val themes = listOf("system" to "System Default", "light" to "Light", "dark" to "Dark")
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Palette, contentDescription = null) },
        title = { Text("Choose Theme") },
        text = {
            Column {
                themes.forEach { (value, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onThemeSelected(value) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentTheme == value, onClick = { onThemeSelected(value) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun ModernSortDialog(
    currentSort: String,
    onSortSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sorts = listOf("date_modified" to "Date Modified", "date_created" to "Date Created", "title" to "Title")
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Sort, contentDescription = null) },
        title = { Text("Sort By") },
        text = {
            Column {
                sorts.forEach { (value, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSortSelected(value) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentSort == value, onClick = { onSortSelected(value) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

private suspend fun exportNotes(context: Context, repository: NoteRepository, uri: Uri) {
    try {
        val notes = repository.getAllNotes().first()
        val jsonArray = JSONArray()
        notes.forEach { note ->
            val jsonObject = JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("content", note.content)
                put("color", note.color)
                put("createdAt", note.createdAt)
                put("updatedAt", note.updatedAt)
                put("isPinned", note.isPinned)
            }
            jsonArray.put(jsonObject)
        }
        context.contentResolver.openOutputStream(uri)?.use { it.write(jsonArray.toString(2).toByteArray()) }
        Toast.makeText(context, "Exported ${notes.size} notes", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private suspend fun importNotes(context: Context, repository: NoteRepository, uri: Uri) {
    try {
        val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        val jsonArray = JSONArray(jsonString)
        var importedCount = 0
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val note = Note(
                id = 0,
                title = jsonObject.getString("title"),
                content = jsonObject.getString("content"),
                color = jsonObject.getInt("color"),
                createdAt = jsonObject.getLong("createdAt"),
                updatedAt = jsonObject.getLong("updatedAt"),
                isPinned = jsonObject.getBoolean("isPinned")
            )
            repository.insertNote(note)
            importedCount++
        }
        Toast.makeText(context, "Imported $importedCount notes", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
