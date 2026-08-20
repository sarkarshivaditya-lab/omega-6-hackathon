package com.udc.collection.ui.screen.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udc.collection.data.local.PreferencesDataStore
import com.udc.collection.data.repository.PatientRepository
import com.udc.collection.domain.model.Patient
import com.udc.collection.util.PdfReceiptGenerator
import com.udc.collection.util.PdfResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    private val pdfGenerator: PdfReceiptGenerator,
    private val prefs: PreferencesDataStore
) : ViewModel() {

    private val _patient = MutableStateFlow<Patient?>(null)
    val patient: StateFlow<Patient?> = _patient

    private val _pdfFile = MutableStateFlow<File?>(null)
    val pdfFile: StateFlow<File?> = _pdfFile

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun loadPatient(id: Long) {
        viewModelScope.launch {
            _patient.value = patientRepository.getPatientById(id)
        }
    }

    fun deletePatient(onComplete: () -> Unit) {
        viewModelScope.launch {
            _patient.value?.let {
                patientRepository.deletePatient(it)
                onComplete()
            }
        }
    }

    fun generatePdf() {
        viewModelScope.launch {
            val p = _patient.value ?: return@launch
            val agentName = prefs.agentName.first()
            when (val result = pdfGenerator.generate(p, agentName)) {
                is PdfResult.Success -> {
                    _pdfFile.value = result.file
                    _message.value = "Receipt saved: ${result.file.name}"
                }
                is PdfResult.Error -> _message.value = result.message
            }
        }
    }

    fun clearPdfFile() { _pdfFile.value = null }
    fun clearMessage() { _message.value = null }
}
