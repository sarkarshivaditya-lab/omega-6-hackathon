package com.udc.collection.ui.screen.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udc.collection.data.repository.LabTestRepository
import com.udc.collection.data.repository.PatientRepository
import com.udc.collection.domain.model.DiscountType
import com.udc.collection.domain.model.LabTest
import com.udc.collection.domain.model.Patient
import com.udc.collection.domain.model.SelectedTest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class PatientFormState(
    val name: String = "",
    val age: String = "",
    val gender: String = "",
    val phone: String = "",
    val address: String = "",
    val referringDoctor: String = "",
    val date: LocalDate = LocalDate.now(),
    val remarks: String = "",
    val testSearchQuery: String = "",
    val selectedTests: List<SelectedTest> = emptyList(),
    val discountType: DiscountType = DiscountType.NONE,
    val discountValue: String = "",
    val recentDoctors: List<String> = emptyList(),
    val recentAddresses: List<String> = emptyList()
) {
    val subtotal: Double get() = selectedTests.sumOf { it.price }
    val discountAmount: Double
        get() = when (discountType) {
            DiscountType.PERCENTAGE -> subtotal * (discountValue.toDoubleOrNull() ?: 0.0) / 100.0
            DiscountType.FLAT -> discountValue.toDoubleOrNull() ?: 0.0
            DiscountType.NONE -> 0.0
        }.coerceAtMost(subtotal)
    val grandTotal: Double get() = (subtotal - discountAmount).coerceAtLeast(0.0)
    val isValid: Boolean get() = name.trim().isNotEmpty() && selectedTests.isNotEmpty()
}

@HiltViewModel
class PatientFormViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    private val labTestRepository: LabTestRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PatientFormState())
    val state: StateFlow<PatientFormState> = _state.asStateFlow()

    private val _testSearchQuery = MutableStateFlow("")

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val filteredTests: StateFlow<List<LabTest>> = _testSearchQuery
        .debounce(150)
        .flatMapLatest { query ->
            if (query.isBlank()) labTestRepository.getAllTests()
            else labTestRepository.searchTests(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            patientRepository.getRecentDoctors().collect { doctors ->
                _state.update { it.copy(recentDoctors = doctors) }
            }
        }
        viewModelScope.launch {
            patientRepository.getRecentAddresses().collect { addresses ->
                _state.update { it.copy(recentAddresses = addresses) }
            }
        }
    }

    fun updateName(value: String) = _state.update { it.copy(name = value) }
    fun updateAge(value: String) = _state.update { it.copy(age = value) }
    fun updateGender(value: String) = _state.update { it.copy(gender = value) }
    fun updatePhone(value: String) = _state.update { it.copy(phone = value) }
    fun updateAddress(value: String) = _state.update { it.copy(address = value) }
    fun updateReferringDoctor(value: String) = _state.update { it.copy(referringDoctor = value) }
    fun updateDate(value: LocalDate) = _state.update { it.copy(date = value) }
    fun updateRemarks(value: String) = _state.update { it.copy(remarks = value) }
    fun updateDiscountType(type: DiscountType) = _state.update { it.copy(discountType = type, discountValue = "") }
    fun updateDiscountValue(value: String) = _state.update { it.copy(discountValue = value) }

    fun updateTestSearch(query: String) {
        _testSearchQuery.value = query
        _state.update { it.copy(testSearchQuery = query) }
    }

    fun toggleTest(test: LabTest) {
        _state.update { current ->
            val isSelected = current.selectedTests.any { it.testId == test.id }
            val updated = if (isSelected) {
                current.selectedTests.filter { it.testId != test.id }
            } else {
                current.selectedTests + SelectedTest(test.id, test.name, test.price)
            }
            current.copy(selectedTests = updated)
        }
    }

    fun removeTest(testId: Long) {
        _state.update { it.copy(selectedTests = it.selectedTests.filter { t -> t.testId != testId }) }
    }

    fun buildPatient(): Patient {
        val s = _state.value
        return Patient(
            name = s.name.trim(),
            age = s.age.trim(),
            gender = s.gender,
            phone = s.phone.trim(),
            address = s.address.trim(),
            referringDoctor = s.referringDoctor.trim(),
            date = s.date,
            remarks = s.remarks.trim(),
            selectedTests = s.selectedTests,
            discountType = s.discountType,
            discountValue = s.discountValue.toDoubleOrNull() ?: 0.0,
            subtotal = s.subtotal,
            grandTotal = s.grandTotal
        )
    }

    fun duplicateLatestPatient() {
        viewModelScope.launch {
            val latest = patientRepository.getLatestPatient() ?: return@launch
            _state.update { current ->
                current.copy(
                    referringDoctor = latest.referringDoctor,
                    address = latest.address
                )
            }
        }
    }
}
