package com.udc.collection.ui.screen.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udc.collection.data.local.PreferencesDataStore
import com.udc.collection.data.repository.BackupRepository
import com.udc.collection.data.repository.BackupResult
import com.udc.collection.data.repository.CsvFilter
import com.udc.collection.data.repository.PatientRepository
import com.udc.collection.util.DailyReportGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val prefs: PreferencesDataStore, private val customerRepository: PatientRepository, private val backupRepository: BackupRepository, private val dailyReportGenerator: DailyReportGenerator) : ViewModel() {
    val agentName = prefs.agentName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")
    val darkMode = prefs.darkMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    private val _message = MutableStateFlow<String?>(null); val message: StateFlow<String?> = _message
    private val _isPinVerifying = MutableStateFlow(false); val isPinVerifying: StateFlow<Boolean> = _isPinVerifying
    suspend fun verifyPin(input: String): Boolean { _isPinVerifying.value = true; val ok = prefs.verifyAndMigratePin(input); _isPinVerifying.value = false; return ok }
    fun updateAgentName(name: String) { viewModelScope.launch { prefs.saveAgentName(name) } }
    fun toggleDarkMode(enabled: Boolean) { viewModelScope.launch { prefs.setDarkMode(enabled) } }
    fun changeAdminPin(newPin: String) { viewModelScope.launch { prefs.setAdminPin(newPin); _message.value = "Admin PIN updated successfully" } }
    fun exportDatabase(uri: Uri) { viewModelScope.launch { _message.value = when (val r = backupRepository.exportDatabaseFile(uri)) { is BackupResult.Success -> r.message; is BackupResult.Error -> r.message } } }
    fun importDatabase(uri: Uri) { viewModelScope.launch { _message.value = when (val r = backupRepository.importDatabaseFile(uri)) { is BackupResult.Success -> r.message; is BackupResult.Error -> r.message } } }
    fun exportCsv(uri: Uri, filter: CsvFilter) { viewModelScope.launch { _message.value = when (val r = backupRepository.exportPatientsToCSV(uri, filter)) { is BackupResult.Success -> r.message; is BackupResult.Error -> r.message } } }
    fun generateDailyReport() { viewModelScope.launch { _message.value = when (val r = dailyReportGenerator.generate(agentName.value)) { is BackupResult.Success -> r.message; is BackupResult.Error -> r.message } } }
    fun clearDatabase() { viewModelScope.launch { customerRepository.deleteAllPatients(); _message.value = "All customer records deleted" } }
    fun clearMessage() { _message.value = null }
}
