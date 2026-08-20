package com.udc.collection.ui.screen.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udc.collection.data.local.PreferencesDataStore
import com.udc.collection.data.repository.LabTestRepository
import com.udc.collection.data.repository.PackageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: PreferencesDataStore,
    private val labTestRepository: LabTestRepository,
    private val packageRepository: PackageRepository
) : ViewModel() {

    val onboardingDone = prefs.onboardingDone.stateIn(
        viewModelScope, SharingStarted.Eagerly, null
    )

    fun saveAgentName(name: String) {
        viewModelScope.launch {
            prefs.saveAgentName(name.trim())
            labTestRepository.seedIfEmpty()
            packageRepository.seedIfEmpty()
        }
    }
}
