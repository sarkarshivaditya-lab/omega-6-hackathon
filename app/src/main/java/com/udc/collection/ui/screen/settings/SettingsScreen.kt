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
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val userName by viewModel.agentName.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    val message by viewModel.message.collectAsState()
    var editingName by remember { mutableStateOf(false) }
    var nameInput by remember(userName) { mutableStateOf(userName) }
    var pendingAction by remember { mutableStateOf<PinGatedAction?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var selectedCsvFilter by remember { mutableStateOf(CsvFilter.TODAY) }
    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { it?.let(viewModel::exportDatabase) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(viewModel::importDatabase) }
    val csvExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let { viewModel.exportCsv(it, selectedCsvFilter) } }
    message?.let { LaunchedEffect(it) { kotlinx.coroutines.delay(3500); viewModel.clearMessage() } }

    if (showPinDialog) {
        AdminPinDialog(
            verifyPin = viewModel::verifyPin,
            onVerified = {
                showPinDialog = false
                when (pendingAction) {
                    PinGatedAction.CLEAR_DB -> showClearDialog = true
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
            title = "Clear All Customer Records",
            message = "Permanently delete all customer records? This cannot be undone.",
            confirmLabel = "Clear All",
            onConfirm = { viewModel.clearDatabase(); showClearDialog = false },
            onDismiss = { showClearDialog = false }
        )
    }

    Scaffold(topBar = { UDCTopBar(title = "Settings", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            message?.let { msg ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(msg, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            SectionHeader("Profile")
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("User Name", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (editingName) {
                        OutlinedTextField(nameInput, { nameInput = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { editingName = false; nameInput = userName }) { Text("Cancel") }
                            Button(onClick = { viewModel.updateAgentName(nameInput); editingName = false }) { Text("Save") }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(userName.ifBlank { "Not set" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            TextButton(onClick = { editingName = true; nameInput = userName }) { Text("Edit") }
                        }
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Dark Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("Switch between light and dark theme", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = darkMode, onCheckedChange = viewModel::toggleDarkMode)
                    }
                }
            }
            SectionHeader("Security")
            SettingsItem(Icons.Filled.Lock, "Change Admin PIN", "Default PIN: 1234") { pendingAction = PinGatedAction.CHANGE_PIN; showPinDialog = true }
            SectionHeader("Reports & Export")
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CSV Export Filter", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CsvFilter.entries.forEach { f ->
                            FilterChip(
                                selected = selectedCsvFilter == f,
                                onClick = { selectedCsvFilter = f },
                                label = { Text(when (f) { CsvFilter.TODAY -> "Today"; CsvFilter.ALL -> "All Records"; CsvFilter.PENDING -> "Pending" }) }
                            )
                        }
                    }
                }
            }
            SettingsItem(Icons.Filled.Share, "Export as CSV", "Export customer data as a spreadsheet") { csvExportLauncher.launch("OMEGA6_Customers_$timestamp.csv") }
            SettingsItem(Icons.Filled.CalendarToday, "Generate Daily Report", "Save today's customer and collection summary as text") { viewModel.generateDailyReport() }
            SectionHeader("Backup & Restore")
            SettingsItem(Icons.Filled.Upload, "Export Database", "Save a full backup of the database file") { exportLauncher.launch("OMEGA6_Backup_$timestamp.db") }
            SettingsItem(Icons.Filled.Download, "Import Database", "Restore from a backup (requires PIN)") { pendingAction = PinGatedAction.IMPORT_DB; showPinDialog = true }
            SectionHeader("Data Management")
            SettingsItem(Icons.Filled.DeleteForever, "Clear All Customer Records", "Permanently delete all customer data (requires PIN)", isDangerous = true) { pendingAction = PinGatedAction.CLEAR_DB; showPinDialog = true }
            Spacer(Modifier.height(8.dp))
            Text("OMEGA 6.0\nAll data stored locally on this device.\nNo internet connection is used.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

private enum class PinGatedAction { CLEAR_DB, IMPORT_DB, CHANGE_PIN }

@Composable
private fun SectionHeader(title: String) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp, start = 4.dp))
}

@Composable
private fun SettingsItem(icon: ImageVector, title: String, subtitle: String, isDangerous: Boolean = false, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (isDangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = if (isDangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
