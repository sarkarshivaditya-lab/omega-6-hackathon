package com.udc.collection.ui.screen.patient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.udc.collection.data.repository.DraftRepository
import com.udc.collection.data.repository.LabTestRepository
import com.udc.collection.data.repository.PackageRepository
import com.udc.collection.data.repository.PatientRepository
import com.udc.collection.domain.model.DiscountType
import com.udc.collection.domain.model.LabPackage
import com.udc.collection.domain.model.LabTest
import com.udc.collection.domain.model.Patient
import com.udc.collection.domain.model.PaymentMethod
import com.udc.collection.domain.model.PaymentStatus
import com.udc.collection.domain.model.SelectedTest
import com.udc.collection.util.SearchUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// Serialisable draft state (no lambdas / Flows)
data class WizardDraft(
    val name: String = "",
    val age: String = "",
    val gender: String = "",
    val phone: String = "",
    val address: String = "",
    val referringDoctor: String = "",
    val remarks: String = "",
    val date: String = LocalDate.now().toString(),
    val selectedTests: List<SelectedTest> = emptyList(),
    val discountType: String = DiscountType.NONE.name,
    val discountValue: String = "",
    val paymentStatus: String = PaymentStatus.UNPAID.name,
    val paymentMethod: String = PaymentMethod.CASH.name,
    val amountReceived: String = ""
)

data class WizardState(
    // Step 1
    val name: String = "",
    val age: String = "",
    val gender: String = "",
    val phone: String = "",
    val address: String = "",
    val referringDoctor: String = "",
    val remarks: String = "",
    val date: LocalDate = LocalDate.now(),
    // Step 2
    val testSearchQuery: String = "",
    val selectedTests: List<SelectedTest> = emptyList(),
    // Step 3
    val discountType: DiscountType = DiscountType.NONE,
    val discountValue: String = "",
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val amountReceived: String = "",
    // Autocomplete
    val recentDoctors: List<String> = emptyList(),
    val recentAddresses: List<String> = emptyList(),
    // Draft restore prompt
    val showDraftRestorePrompt: Boolean = false,
    // Current wizard step (0 = details, 1 = tests, 2 = billing)
    val currentStep: Int = 0,
    // Saving state
    val isSaving: Boolean = false
) {
    val subtotal: Double get() = selectedTests.sumOf { it.price }
    val discountAmount: Double
        get() = when (discountType) {
            DiscountType.PERCENTAGE -> subtotal * (discountValue.toDoubleOrNull() ?: 0.0) / 100.0
            DiscountType.FLAT -> discountValue.toDoubleOrNull() ?: 0.0
            DiscountType.NONE -> 0.0
        }.coerceAtMost(subtotal)
    val grandTotal: Double get() = (subtotal - discountAmount).coerceAtLeast(0.0)
    val balanceDue: Double get() = (grandTotal - (amountReceived.toDoubleOrNull() ?: 0.0)).coerceAtLeast(0.0)

    val step1Valid: Boolean get() = name.trim().isNotEmpty()
    val step2Valid: Boolean get() = selectedTests.isNotEmpty()
    val allValid: Boolean get() = step1Valid && step2Valid
}

@HiltViewModel
class PatientWizardViewModel @Inject constructor(
    private val patientRepository: PatientRepository,
    private val labTestRepository: LabTestRepository,
    private val packageRepository: PackageRepository,
    private val draftRepository: DraftRepository
) : ViewModel() {

    private val gson = Gson()

    private val _state = MutableStateFlow(WizardState())
    val state: StateFlow<WizardState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private var autoSaveJob: Job? = null

    private val _allTests: StateFlow<List<LabTest>> = labTestRepository.getAllTests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _allPackages: StateFlow<List<LabPackage>> = packageRepository.getAllPackages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTests: StateFlow<List<LabTest>> = combine(_allTests, _searchQuery) { tests, q ->
        SearchUtils.filter(tests, q) { it.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredPackages: StateFlow<List<LabPackage>> = combine(_allPackages, _searchQuery) { pkgs, q ->
        SearchUtils.filter(pkgs, q) { it.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val frequentTests: StateFlow<List<LabTest>> = _allTests
        .map { tests -> tests.filter { it.useCount > 0 }.sortedByDescending { it.useCount }.take(10) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            patientRepository.getRecentDoctors().collect { docs ->
                _state.update { it.copy(recentDoctors = docs) }
            }
        }
        viewModelScope.launch {
            patientRepository.getRecentAddresses().collect { addrs ->
                _state.update { it.copy(recentAddresses = addrs) }
            }
        }
        viewModelScope.launch {
            if (draftRepository.hasDraft()) {
                _state.update { it.copy(showDraftRestorePrompt = true) }
            }
        }
    }

    // ── Draft ────────────────────────────────────────────────────────────────

    fun restoreDraft() {
        viewModelScope.launch {
            val draft = draftRepository.loadDraft(WizardDraft::class.java) ?: return@launch
            _state.update { current ->
                current.copy(
                    name = draft.name,
                    age = draft.age,
                    gender = draft.gender,
                    phone = draft.phone,
                    address = draft.address,
                    referringDoctor = draft.referringDoctor,
                    remarks = draft.remarks,
                    date = runCatching { LocalDate.parse(draft.date) }.getOrDefault(LocalDate.now()),
                    selectedTests = draft.selectedTests,
                    discountType = runCatching { DiscountType.valueOf(draft.discountType) }.getOrDefault(DiscountType.NONE),
                    discountValue = draft.discountValue,
                    paymentStatus = runCatching { PaymentStatus.valueOf(draft.paymentStatus) }.getOrDefault(PaymentStatus.UNPAID),
                    paymentMethod = runCatching { PaymentMethod.valueOf(draft.paymentMethod) }.getOrDefault(PaymentMethod.CASH),
                    amountReceived = draft.amountReceived,
                    showDraftRestorePrompt = false
                )
            }
        }
    }

    fun dismissDraft() {
        viewModelScope.launch {
            draftRepository.clearDraft()
            _state.update { it.copy(showDraftRestorePrompt = false) }
        }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(2000) // debounce 2s
            saveCurrentDraft()
        }
    }

    private suspend fun saveCurrentDraft() {
        val s = _state.value
        if (s.name.isBlank() && s.selectedTests.isEmpty()) return // nothing to save
        val draft = WizardDraft(
            name = s.name, age = s.age, gender = s.gender, phone = s.phone,
            address = s.address, referringDoctor = s.referringDoctor, remarks = s.remarks,
            date = s.date.toString(), selectedTests = s.selectedTests,
            discountType = s.discountType.name, discountValue = s.discountValue,
            paymentStatus = s.paymentStatus.name, paymentMethod = s.paymentMethod.name,
            amountReceived = s.amountReceived
        )
        draftRepository.saveDraft(draft)
    }

    // ── Step 1 ───────────────────────────────────────────────────────────────

    fun updateName(v: String) { _state.update { it.copy(name = v) }; scheduleAutoSave() }
    fun updateAge(v: String) { _state.update { it.copy(age = v) }; scheduleAutoSave() }
    fun updateGender(v: String) { _state.update { it.copy(gender = v) } }
    fun updatePhone(v: String) { _state.update { it.copy(phone = v) }; scheduleAutoSave() }
    fun updateAddress(v: String) { _state.update { it.copy(address = v) }; scheduleAutoSave() }
    fun updateReferringDoctor(v: String) { _state.update { it.copy(referringDoctor = v) }; scheduleAutoSave() }
    fun updateRemarks(v: String) { _state.update { it.copy(remarks = v) } }
    fun updateDate(v: LocalDate) { _state.update { it.copy(date = v) } }

    // ── Step 2 ───────────────────────────────────────────────────────────────

    fun updateTestSearch(q: String) {
        _searchQuery.value = q
        _state.update { it.copy(testSearchQuery = q) }
    }

    fun toggleTest(test: LabTest) {
        _state.update { current ->
            val already = current.selectedTests.any { it.testId == test.id && !it.isPackage }
            val updated = if (already) {
                current.selectedTests.filter { it.testId != test.id || it.isPackage }
            } else {
                current.selectedTests + SelectedTest(test.id, test.name, test.price)
            }
            current.copy(selectedTests = updated)
        }
        scheduleAutoSave()
    }

    fun togglePackage(pkg: LabPackage) {
        _state.update { current ->
            val alreadySelected = current.selectedTests.any { it.testId == pkg.id && it.isPackage }
            val updated = if (alreadySelected) {
                current.selectedTests.filter { it.testId != pkg.id || !it.isPackage }
            } else {
                current.selectedTests + SelectedTest(
                    testId = pkg.id,
                    testName = pkg.name,
                    price = pkg.price,
                    isPackage = true
                )
            }
            current.copy(selectedTests = updated)
        }
        scheduleAutoSave()
    }

    fun removeSelectedTest(testId: Long, isPackage: Boolean) {
        _state.update { current ->
            current.copy(selectedTests = current.selectedTests.filter {
                !(it.testId == testId && it.isPackage == isPackage)
            })
        }
        scheduleAutoSave()
    }

    // ── Step 3 ───────────────────────────────────────────────────────────────

    fun updateDiscountType(t: DiscountType) { _state.update { it.copy(discountType = t, discountValue = "") } }
    fun updateDiscountValue(v: String) { _state.update { it.copy(discountValue = v) } }
    fun updatePaymentStatus(s: PaymentStatus) { _state.update { it.copy(paymentStatus = s) } }
    fun updatePaymentMethod(m: PaymentMethod) { _state.update { it.copy(paymentMethod = m) } }
    fun updateAmountReceived(v: String) { _state.update { it.copy(amountReceived = v) } }

    // ── Navigation ───────────────────────────────────────────────────────────

    fun goToStep(step: Int) { _state.update { it.copy(currentStep = step.coerceIn(0, 2)) } }
    fun nextStep() { _state.update { it.copy(currentStep = (it.currentStep + 1).coerceAtMost(2)) } }
    fun previousStep() { _state.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0)) } }

    // ── Save ─────────────────────────────────────────────────────────────────

    fun savePatient(onComplete: (patientId: Long) -> Unit) {
        if (_state.value.isSaving) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val s = _state.value
            val patient = Patient(
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
                grandTotal = s.grandTotal,
                paymentStatus = s.paymentStatus,
                paymentMethod = s.paymentMethod,
                amountReceived = s.amountReceived.toDoubleOrNull() ?: 0.0
            )
            val id = patientRepository.savePatient(patient)

            // Increment use counts for non-package test selections
            val testIds = s.selectedTests.filter { !it.isPackage }.map { it.testId }
            labTestRepository.incrementUseCount(testIds)

            draftRepository.clearDraft()
            _state.update { it.copy(isSaving = false) }
            onComplete(id)
        }
    }
}
