package id.avium.aviumnotes.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val repository = remember { NoteRepository(AppDatabase.getInstance(context).noteDao()) }
    val coroutineScope = rememberCoroutineScope()

    // Collect preferences
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

    // File picker for export
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                exportNotes(context, repository, uri)
            }
        }
    }

    // File picker for import
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
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Appearance Section
            SettingsSectionHeader(stringResource(R.string.settings_appearance))

            SettingsItem(
                icon = Icons.Default.Palette,
                title = stringResource(R.string.settings_theme),
                subtitle = when (themeMode) {
                    "light" -> stringResource(R.string.settings_theme_light)
                    "dark" -> stringResource(R.string.settings_theme_dark)
                    else -> stringResource(R.string.settings_theme_system)
                },
                onClick = { showThemeDialog = true }
            )

            SettingsItem(
                icon = Icons.Default.Sort,
                title = stringResource(R.string.settings_sort_by),
                subtitle = when (sortBy) {
                    "date_modified" -> stringResource(R.string.settings_sort_date_modified)
                    "date_created" -> stringResource(R.string.settings_sort_date_created)
                    else -> stringResource(R.string.settings_sort_title)
                },
                onClick = { showSortDialog = true }
            )

            SettingsItem(
                icon = Icons.Default.FormatPaint,
                title = stringResource(R.string.settings_default_color),
                subtitle = "Tap to change",
                onClick = { showColorPicker = true }
            )

            SettingsSwitchItem(
                icon = Icons.Default.Visibility,
                title = stringResource(R.string.settings_show_preview),
                subtitle = stringResource(R.string.settings_show_preview_desc),
                checked = showNotePreview,
                onCheckedChange = { checked ->
                    coroutineScope.launch {
                        preferencesManager.setShowNotePreview(checked)
                    }
                }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Floating Bubble Section
            SettingsSectionHeader(stringResource(R.string.settings_floating_bubble))

            SettingsSwitchItem(
                icon = Icons.Default.BubbleChart,
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

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Behavior Section
            SettingsSectionHeader(stringResource(R.string.settings_behavior))

            SettingsSwitchItem(
                icon = Icons.Default.Save,
                title = stringResource(R.string.settings_auto_save),
                subtitle = stringResource(R.string.settings_auto_save_desc),
                checked = autoSaveEnabled,
                onCheckedChange = { checked ->
                    coroutineScope.launch {
                        preferencesManager.setAutoSaveEnabled(checked)
                    }
                }
            )

            SettingsSwitchItem(
                icon = Icons.Default.Warning,
                title = stringResource(R.string.settings_delete_confirm),
                subtitle = stringResource(R.string.settings_delete_confirm_desc),
                checked = deleteConfirmation,
                onCheckedChange = { checked ->
                    coroutineScope.launch {
                        preferencesManager.setDeleteConfirmation(checked)
                    }
                }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Data & Backup Section
            SettingsSectionHeader(stringResource(R.string.settings_data))

            SettingsItem(
                icon = Icons.Default.Upload,
                title = stringResource(R.string.settings_export),
                subtitle = "Export all notes as JSON",
                onClick = {
                    exportLauncher.launch("aviumnotes_backup_${System.currentTimeMillis()}.json")
                }
            )

            SettingsItem(
                icon = Icons.Default.Download,
                title = stringResource(R.string.settings_import),
                subtitle = "Import notes from file",
                onClick = {
                    importLauncher.launch("application/json")
                }
            )

            SettingsItem(
                icon = Icons.Default.DeleteForever,
                title = stringResource(R.string.settings_clear_data),
                subtitle = "Delete all notes permanently",
                onClick = { showClearDataDialog = true }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // About Section
            SettingsSectionHeader(stringResource(R.string.settings_about))

            SettingsItem(
                icon = Icons.Default.Info,
                title = stringResource(R.string.settings_version),
                subtitle = BuildConfig.VERSION_NAME,
                onClick = { }
            )

            SettingsItem(
                icon = Icons.Default.Person,
                title = stringResource(R.string.settings_developer),
                subtitle = "Gustyx-Power",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Gustyx-Power"))
                    context.startActivity(intent)
                }
            )

            SettingsItem(
                icon = Icons.Default.Code,
                title = stringResource(R.string.settings_github),
                subtitle = "View source code",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Gustyx-Power/AviumNotes"))
                    context.startActivity(intent)
                }
            )

            SettingsItem(
                icon = Icons.Default.Description,
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

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Theme Dialog
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(R.string.settings_theme)) },
            text = {
                Column {
                    ThemeOption("System Default", "system", themeMode) {
                        coroutineScope.launch {
                            preferencesManager.setThemeMode("system")
                            showThemeDialog = false
                        }
                    }
                    ThemeOption("Light", "light", themeMode) {
                        coroutineScope.launch {
                            preferencesManager.setThemeMode("light")
                            showThemeDialog = false
                        }
                    }
                    ThemeOption("Dark", "dark", themeMode) {
                        coroutineScope.launch {
                            preferencesManager.setThemeMode("dark")
                            showThemeDialog = false
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sort Dialog
    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = { Text(stringResource(R.string.settings_sort_by)) },
            text = {
                Column {
                    SortOption("Date Modified", "date_modified", sortBy) {
                        coroutineScope.launch {
                            preferencesManager.setSortBy("date_modified")
                            showSortDialog = false
                        }
                    }
                    SortOption("Date Created", "date_created", sortBy) {
                        coroutineScope.launch {
                            preferencesManager.setSortBy("date_created")
                            showSortDialog = false
                        }
                    }
                    SortOption("Title", "title", sortBy) {
                        coroutineScope.launch {
                            preferencesManager.setSortBy("title")
                            showSortDialog = false
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Color Picker
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

    // Clear Data Dialog
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text(stringResource(R.string.clear_data_title)) },
            text = { Text(stringResource(R.string.clear_data_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            val notes = repository.getAllNotes().first()
                            notes.forEach { repository.deleteNote(it) }
                            Toast.makeText(
                                context,
                                context.getString(R.string.clear_data_success),
                                Toast.LENGTH_SHORT
                            ).show()
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

// Export function
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

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(jsonArray.toString(2).toByteArray())
        }

        Toast.makeText(context, "Exported ${notes.size} notes successfully", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// Import function
private suspend fun importNotes(context: Context, repository: NoteRepository, uri: Uri) {
    try {
        val jsonString = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        val jsonArray = JSONArray(jsonString)
        var importedCount = 0

        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val note = Note(
                id = 0, // Auto-generate new ID
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

        Toast.makeText(context, "Imported $importedCount notes successfully", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// Rest of composable functions remain the same...
@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun ThemeOption(
    label: String,
    value: String,
    currentValue: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = value == currentValue,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label)
    }
}

@Composable
fun SortOption(
    label: String,
    value: String,
    currentValue: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = value == currentValue,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label)
    }
}
