package com.udc.collection.ui.screen.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.udc.collection.data.repository.CsvFilter
import com.udc.collection.ui.components.AdminPinDialog
import com.udc.collection.ui.components.ChangePinDialog
import com.udc.collection.ui.components.ConfirmDialog
import com.udc.collection.ui.components.UDCTopBar
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val agentName by viewModel.agentName.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    val message by viewModel.message.collectAsState()

    var editingName by remember { mutableStateOf(false) }
    var nameInput by remember(agentName) { mutableStateOf(agentName) }

    var pendingAction by remember { mutableStateOf<PinGatedAction?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }

    var showClearDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    var selectedCsvFilter by remember { mutableStateOf(CsvFilter.TODAY) }

    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? -> uri?.let { viewModel.exportDatabase(it) } }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { viewModel.importDatabase(it) } }

    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? -> uri?.let { viewModel.exportCsv(it, selectedCsvFilter) } }

    message?.let {
        LaunchedEffect(it) {
            kotlinx.coroutines.delay(3500)
            viewModel.clearMessage()
        }
    }

    if (showPinDialog) {
        AdminPinDialog(
            verifyPin = viewModel::verifyPin,
            onVerified = {
                showPinDialog = false
                when (pendingAction) {
                    PinGatedAction.CLEAR_DB -> showClearDialog = true
                    PinGatedAction.RESET_CATALOGUE -> showResetDialog = true
                    PinGatedAction.IMPORT_DB -> importLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                    PinGatedAction.CHANGE_PIN -> showChangePinDialog = true
                    null -> Unit
                }
                pendingAction = null
            },
            onDismiss = { showPinDialog = false; pendingAction = null }
        )
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            verifyCurrentPin = viewModel::verifyPin,
            onSave = { viewModel.changeAdminPin(it); showChangePinDialog = false },
            onDismiss = { showChangePinDialog = false }
        )
    }

    if (showClearDialog) {
        ConfirmDialog(
            title = "Clear All Patient Records",
            message = "Permanently delete all patient records? Test catalogue is unaffected. This cannot be undone.",
            confirmLabel = "Clear All",
            onConfirm = { viewModel.clearDatabase(); showClearDialog = false },
            onDismiss = { showClearDialog = false }
        )
    }

    if (showResetDialog) {
        ConfirmDialog(
            title = "Reset Test Catalogue",
            message = "Delete all tests and packages and restore defaults? Custom tests will be lost.",
            confirmLabel = "Reset",
            onConfirm = { viewModel.resetTestCatalogue(); showResetDialog = false },
            onDismiss = { showResetDialog = false }
        )
    }

    Scaffold(topBar = { UDCTopBar(title = "Settings", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Toast-style message
            message?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(msg, modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            // ── Profile ──────────────────────────────────────────────────────
            SectionHeader("Profile")

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text("Collection Agent Name", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (editingName) {
                            OutlinedTextField(
                                value = nameInput, onValueChange = { nameInput = it },
                                modifier = Modifier.fillMaxWidth(), singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { editingName = false; nameInput = agentName }) { Text("Cancel") }
                                Button(onClick = { viewModel.updateAgentName(nameInput); editingName = false }) { Text("Save") }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(agentName.ifBlank { "Not set" }, style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                TextButton(onClick = { editingName = true; nameInput = agentName }) { Text("Edit") }
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dark Mode", style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium)
                            Text("Switch between light and dark theme",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = darkMode, onCheckedChange = viewModel::toggleDarkMode)
                    }
                }
            }

            // ── Security ─────────────────────────────────────────────────────
            SectionHeader("Security")

            SettingsItem(
                icon = Icons.Filled.Lock,
                title = "Change Admin PIN",
                subtitle = "Default PIN: 1234",
                onClick = { pendingAction = PinGatedAction.CHANGE_PIN; showPinDialog = true }
            )

            // ── Reports & Export ──────────────────────────────────────────────
            SectionHeader("Reports & Export")

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CSV Export Filter", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CsvFilter.entries.forEach { f ->
                            FilterChip(
                                selected = selectedCsvFilter == f,
                                onClick = { selectedCsvFilter = f },
                                label = {
                                    Text(when (f) {
                                        CsvFilter.TODAY -> "Today"
                                        CsvFilter.ALL -> "All Records"
                                        CsvFilter.PENDING -> "Pending"
                                    })
                                }
                            )
                        }
                    }
                }
            }

            SettingsItem(
                icon = Icons.Filled.Share,
                title = "Export as CSV",
                subtitle = "Export patient data as a spreadsheet",
                onClick = { csvExportLauncher.launch("UDC_Patients_$timestamp.csv") }
            )

            SettingsItem(
                icon = Icons.Filled.CalendarToday,
                title = "Generate Daily Report",
                subtitle = "Save today's patient & collection summary as text",
                onClick = { viewModel.generateDailyReport() }
            )

            // ── Backup & Restore ──────────────────────────────────────────────
            SectionHeader("Backup & Restore")

            SettingsItem(
                icon = Icons.Filled.Upload,
                title = "Export Database",
                subtitle = "Save a full backup of the database file",
                onClick = { exportLauncher.launch("UDC_Backup_$timestamp.db") }
            )

            SettingsItem(
                icon = Icons.Filled.Download,
                title = "Import Database",
                subtitle = "Restore from a backup (requires PIN)",
                onClick = { pendingAction = PinGatedAction.IMPORT_DB; showPinDialog = true }
            )

            // ── Danger Zone ───────────────────────────────────────────────────
            SectionHeader("Danger Zone")

            SettingsItem(
                icon = Icons.Filled.Refresh,
                title = "Reset Test Catalogue",
                subtitle = "Restore default tests and packages (requires PIN)",
                onClick = { pendingAction = PinGatedAction.RESET_CATALOGUE; showPinDialog = true },
                isDangerous = true
            )

            SettingsItem(
                icon = Icons.Filled.DeleteForever,
                title = "Clear All Patient Records",
                subtitle = "Permanently delete all patient data (requires PIN)",
                onClick = { pendingAction = PinGatedAction.CLEAR_DB; showPinDialog = true },
                isDangerous = true
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Urban Diagnostic Collection v1.0\nAll data stored locally on this device.\nNo internet connection is used.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

private enum class PinGatedAction { CLEAR_DB, RESET_CATALOGUE, IMPORT_DB, CHANGE_PIN }

@Composable
private fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDangerous: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null,
                tint = if (isDangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                    color = if (isDangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
