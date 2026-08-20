package com.udc.collection.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udc.collection.data.repository.PatientRepository
import com.udc.collection.domain.model.Patient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PatientHistoryViewModel @Inject constructor(
    private val patientRepository: PatientRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val patients: StateFlow<List<Patient>> = _searchQuery
        .debounce(200)
        .flatMapLatest { query ->
            if (query.isBlank()) patientRepository.getAllPatients()
            else patientRepository.searchPatients(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _recentlyDeleted = MutableStateFlow<Patient?>(null)
    val recentlyDeleted: StateFlow<Patient?> = _recentlyDeleted.asStateFlow()

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun deletePatient(patient: Patient) {
        viewModelScope.launch {
            patientRepository.deletePatient(patient)
            _recentlyDeleted.value = patient
        }
    }

    fun undoDelete() {
        viewModelScope.launch {
            _recentlyDeleted.value?.let { patientRepository.restorePatient(it) }
            _recentlyDeleted.value = null
        }
    }

    fun clearRecentlyDeleted() {
        _recentlyDeleted.value = null
    }
}
