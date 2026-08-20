package com.udc.collection.ui.screen.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udc.collection.data.local.PreferencesDataStore
import com.udc.collection.data.repository.BackupRepository
import com.udc.collection.data.repository.BackupResult
import com.udc.collection.data.repository.CsvFilter
import com.udc.collection.data.repository.LabTestRepository
import com.udc.collection.data.repository.PackageRepository
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
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesDataStore,
    private val patientRepository: PatientRepository,
    private val labTestRepository: LabTestRepository,
    private val packageRepository: PackageRepository,
    private val backupRepository: BackupRepository,
    private val dailyReportGenerator: DailyReportGenerator
) : ViewModel() {

    val agentName = prefs.agentName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")
    val darkMode = prefs.darkMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _isPinVerifying = MutableStateFlow(false)
    val isPinVerifying: StateFlow<Boolean> = _isPinVerifying

    /** Verifies PIN and transparently migrates legacy SHA-256 hash to PBKDF2. */
    suspend fun verifyPin(input: String): Boolean {
        _isPinVerifying.value = true
        val ok = prefs.verifyAndMigratePin(input)
        _isPinVerifying.value = false
        return ok
    }

    fun updateAgentName(name: String) {
        viewModelScope.launch { prefs.saveAgentName(name) }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch { prefs.setDarkMode(enabled) }
    }

    fun changeAdminPin(newPin: String) {
        viewModelScope.launch {
            prefs.setAdminPin(newPin)
            _message.value = "Admin PIN updated successfully"
        }
    }

    fun exportDatabase(uri: Uri) {
        viewModelScope.launch {
            _message.value = when (val r = backupRepository.exportDatabaseFile(uri)) {
                is BackupResult.Success -> r.message
                is BackupResult.Error -> r.message
            }
        }
    }

    fun importDatabase(uri: Uri) {
        viewModelScope.launch {
            _message.value = when (val r = backupRepository.importDatabaseFile(uri)) {
                is BackupResult.Success -> r.message
                is BackupResult.Error -> r.message
            }
        }
    }

    fun exportCsv(uri: Uri, filter: CsvFilter) {
        viewModelScope.launch {
            _message.value = when (val r = backupRepository.exportPatientsToCSV(uri, filter)) {
                is BackupResult.Success -> r.message
                is BackupResult.Error -> r.message
            }
        }
    }

    fun generateDailyReport() {
        viewModelScope.launch {
            val agentName = agentName.value
            _message.value = when (val r = dailyReportGenerator.generate(agentName)) {
                is BackupResult.Success -> r.message
                is BackupResult.Error -> r.message
            }
        }
    }

    fun clearDatabase() {
        viewModelScope.launch {
            patientRepository.deleteAllPatients()
            _message.value = "All patient records deleted"
        }
    }

    fun resetTestCatalogue() {
        viewModelScope.launch {
            labTestRepository.resetToDefault()
            packageRepository.resetToDefault()
            _message.value = "Test catalogue reset to defaults"
        }
    }

    fun clearMessage() { _message.value = null }
}
