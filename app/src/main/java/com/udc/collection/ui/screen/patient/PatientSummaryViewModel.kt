package com.udc.collection.ui.screen.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udc.collection.data.repository.PatientRepository
import com.udc.collection.domain.model.Patient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PatientSummaryViewModel @Inject constructor(
    private val patientRepository: PatientRepository
) : ViewModel() {

    fun savePatient(patient: Patient, onComplete: () -> Unit) {
        viewModelScope.launch {
            patientRepository.savePatient(patient)
            onComplete()
        }
    }
}
