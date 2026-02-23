package id.avium.aviumnotes.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.avium.aviumnotes.BuildConfig
import id.avium.aviumnotes.R
import id.avium.aviumnotes.data.local.AppDatabase
import id.avium.aviumnotes.data.local.entity.Note
import id.avium.aviumnotes.data.preferences.PreferencesManager
import id.avium.aviumnotes.data.repository.NoteRepository
import id.avium.aviumnotes.service.FloatingBubbleService
import id.avium.aviumnotes.ui.components.ColorPickerDialog
import id.avium.aviumnotes.ui.theme.*
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
    var showAboutDialog by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            coroutineScope.launch { exportNotes(context, repository, uri) }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch { importNotes(context, repository, uri) }
        }
    }

    Scaffold(
        containerColor = IosBlack,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IosPrimaryText
                    )
                },
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onNavigateBack() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.main_back),
                            tint = IosPrimaryText
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Notes",
                            color = IosPrimaryText,
                            fontSize = 17.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = IosBlack,
                    scrolledContainerColor = IosBlack
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
            IosSettingsSection(title = stringResource(R.string.settings_appearance)) {
                IosSettingsItem(
                    icon = Icons.Outlined.Palette,
                    title = stringResource(R.string.settings_theme),
                    onClick = { showThemeDialog = true },
                    rightText = when (themeMode) {
                        "light" -> stringResource(R.string.settings_theme_light)
                        "dark" -> stringResource(R.string.settings_theme_dark)
                        else -> stringResource(R.string.settings_theme_system)
                    }
                )

                IosSettingsItem(
                    icon = Icons.Filled.SwapVert,
                    title = stringResource(R.string.settings_sort_by),
                    onClick = { showSortDialog = true },
                    rightText = when (sortBy) {
                        "date_modified" -> stringResource(R.string.settings_sort_date_modified)
                        "date_created" -> stringResource(R.string.settings_sort_date_created)
                        else -> stringResource(R.string.settings_sort_title)
                    }
                )

                IosSettingsItem(
                    icon = Icons.Outlined.ColorLens,
                    title = stringResource(R.string.settings_default_color),
                    onClick = { showColorPicker = true },
                    rightText = colorNameFor(defaultNoteColor),
                    endContent = {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(defaultNoteColor))
                        )
                    }
                )

                IosSettingsSwitchItem(
                    icon = Icons.Outlined.Visibility,
                    title = stringResource(R.string.settings_show_preview),
                    subtitle = stringResource(R.string.settings_show_preview_desc),
                    checked = showNotePreview,
                    onCheckedChange = { coroutineScope.launch { preferencesManager.setShowNotePreview(it) } },
                    showDivider = false
                )
            }

            IosSettingsSection(title = stringResource(R.string.settings_floating_bubble)) {
                IosSettingsSwitchItem(
                    icon = Icons.Outlined.ChatBubbleOutline,
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
                    },
                    showDivider = false
                )
            }

            IosSettingsSection(title = "Note Behavior") {
                IosSettingsSwitchItem(
                    icon = Icons.Outlined.Save,
                    title = stringResource(R.string.settings_auto_save),
                    subtitle = stringResource(R.string.settings_auto_save_desc),
                    checked = autoSaveEnabled,
                    onCheckedChange = { coroutineScope.launch { preferencesManager.setAutoSaveEnabled(it) } }
                )

                IosSettingsSwitchItem(
                    icon = Icons.Outlined.ReportProblem,
                    title = stringResource(R.string.settings_delete_confirm),
                    subtitle = stringResource(R.string.settings_delete_confirm_desc),
                    checked = deleteConfirmation,
                    onCheckedChange = { coroutineScope.launch { preferencesManager.setDeleteConfirmation(it) } },
                    showDivider = false
                )
            }

            IosSettingsSection(title = "Data & Backup") {
                IosSettingsItem(
                    icon = Icons.Outlined.IosShare,
                    title = stringResource(R.string.settings_export),
                    subtitle = stringResource(R.string.settings_export_all_json),
                    onClick = { exportLauncher.launch("aviumnotes_backup_${System.currentTimeMillis()}.json") }
                )

                IosSettingsItem(
                    icon = Icons.Outlined.FileDownload,
                    title = stringResource(R.string.settings_import),
                    subtitle = stringResource(R.string.settings_import_from_file),
                    onClick = { importLauncher.launch("application/json") }
                )

                IosSettingsItem(
                    icon = Icons.Outlined.DeleteOutline,
                    title = stringResource(R.string.settings_clear_data),
                    subtitle = stringResource(R.string.settings_clear_data_desc),
                    onClick = { showClearDataDialog = true },
                    showDivider = false
                )
            }

            IosSettingsSection(title = stringResource(R.string.settings_about)) {
                IosSettingsItem(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.settings_version),
                    rightText = BuildConfig.VERSION_NAME,
                    onClick = { showAboutDialog = true }
                )

                IosSettingsItem(
                    icon = Icons.Outlined.PersonOutline,
                    title = stringResource(R.string.settings_developer),
                    rightText = stringResource(R.string.settings_developer_name),
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Gustyx-Power")))
                    }
                )

                IosSettingsItem(
                    icon = Icons.Outlined.Description,
                    title = stringResource(R.string.settings_licenses),
                    rightText = "MIT License",
                    onClick = {
                        Toast.makeText(context, context.getString(R.string.settings_license_toast), Toast.LENGTH_LONG).show()
                    },
                    showDivider = false
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
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
                coroutineScope.launch { preferencesManager.setDefaultNoteColor(color.hashCode()) }
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
                    Text(stringResource(R.string.main_clear), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text(stringResource(R.string.cancel), color = IosTextGray)
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = IosDarkGray,
            title = {  }, // Clean up title to allow centering text perfectly below
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF3DDC84))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = IosPrimaryText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version " + BuildConfig.VERSION_NAME,
                        style = MaterialTheme.typography.bodyMedium,
                        color = IosTextGray,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = stringResource(R.string.settings_about_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = IosPrimaryText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Button(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Gustyx-Power/AviumNotes")))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IosSearchGray),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Code, contentDescription = null, tint = IosPrimaryText)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Source Code", color = IosPrimaryText)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showAboutDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.close), color = IosPrimaryText, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
private fun colorNameFor(colorHash: Int): String {
    return when (colorHash) {
        NoteColors.White.hashCode() -> stringResource(R.string.color_white)
        NoteColors.LightRed.hashCode() -> stringResource(R.string.color_red)
        NoteColors.LightOrange.hashCode() -> stringResource(R.string.color_orange)
        NoteColors.LightYellow.hashCode() -> stringResource(R.string.color_yellow)
        NoteColors.LightGreen.hashCode() -> stringResource(R.string.color_green)
        NoteColors.LightCyan.hashCode() -> stringResource(R.string.color_cyan)
        NoteColors.LightBlue.hashCode() -> stringResource(R.string.color_blue)
        NoteColors.LightPurple.hashCode() -> stringResource(R.string.color_purple)
        NoteColors.LightPink.hashCode() -> stringResource(R.string.color_pink)
        NoteColors.LightGray.hashCode() -> stringResource(R.string.color_gray)
        else -> stringResource(R.string.color_white)
    }
}

@Composable
fun IosSettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = IosPrimaryText,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = IosDarkGray
        ) {
            Column { content() }
        }
    }
}

@Composable
fun IosSettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    rightText: String? = null,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
    endContent: @Composable (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth().clickable(enabled = onClick != null) { onClick?.invoke() }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = IosPrimaryText,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = IosPrimaryText,
                    fontSize = 17.sp
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = IosTextGray,
                        fontSize = 13.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            if (endContent != null) {
                endContent()
            } else if (rightText != null) {
                Text(
                    text = rightText,
                    color = IosTextGray,
                    fontSize = 17.sp,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            
            if (onClick != null || endContent != null || rightText != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = IosTextGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(color = IosSearchGray, modifier = Modifier.padding(start = 56.dp), thickness = 1.dp)
        }
    }
}

@Composable
fun IosSettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = IosPrimaryText,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = IosPrimaryText,
                    fontSize = 17.sp
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = IosTextGray,
                        fontSize = 13.sp
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF34C759),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = IosSearchGray
                )
            )
        }
        if (showDivider) {
            HorizontalDivider(color = IosSearchGray, modifier = Modifier.padding(start = 56.dp), thickness = 1.dp)
        }
    }
}

@Composable
fun ModernThemeDialog(
    currentTheme: String,
    onThemeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val themes = listOf(
        "system" to stringResource(R.string.settings_theme_system),
        "light" to stringResource(R.string.settings_theme_light),
        "dark" to stringResource(R.string.settings_theme_dark)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IosDarkGray,
        title = { Text(stringResource(R.string.settings_theme), color = IosPrimaryText, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                themes.forEach { (value, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onThemeSelected(value) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == value,
                            onClick = { onThemeSelected(value) },
                            colors = RadioButtonDefaults.colors(selectedColor = IosPrimaryText, unselectedColor = IosTextGray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, color = IosPrimaryText)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), color = IosPrimaryText) } }
    )
}

@Composable
fun ModernSortDialog(
    currentSort: String,
    onSortSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sorts = listOf(
        "date_modified" to stringResource(R.string.settings_sort_date_modified),
        "date_created" to stringResource(R.string.settings_sort_date_created),
        "title" to stringResource(R.string.settings_sort_title)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IosDarkGray,
        title = { Text(stringResource(R.string.settings_sort_by), color = IosPrimaryText, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                sorts.forEach { (value, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSortSelected(value) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentSort == value,
                            onClick = { onSortSelected(value) },
                            colors = RadioButtonDefaults.colors(selectedColor = IosPrimaryText, unselectedColor = IosTextGray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, color = IosPrimaryText)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), color = IosPrimaryText) } }
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
        Toast.makeText(context, context.getString(R.string.export_success, notes.size), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.export_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
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
        Toast.makeText(context, context.getString(R.string.import_success, importedCount), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.import_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
    }
}
