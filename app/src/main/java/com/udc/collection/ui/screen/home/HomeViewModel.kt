package com.udc.collection.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udc.collection.data.local.PreferencesDataStore
import com.udc.collection.data.repository.PatientRepository
import com.udc.collection.domain.model.Patient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DashboardState(
    val agentName: String = "",
    val todayPatientCount: Int = 0,
    val todayRevenue: Double = 0.0,
    val todayTotalBilled: Double = 0.0,
    val pendingPaymentCount: Int = 0,
    val totalOutstanding: Double = 0.0,
    val recentPatients: List<Patient> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    prefs: PreferencesDataStore,
    patientRepository: PatientRepository
) : ViewModel() {

    private val today: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    val dashboardState: StateFlow<DashboardState> = combine(
        prefs.agentName,
        patientRepository.getTodayPatientCount(today),
        patientRepository.getTodayRevenue(today),
        patientRepository.getTodayTotalBilled(today),
        patientRepository.getPendingPaymentCount()
    ) { agentName, todayCount, todayRevenue, todayBilled, pendingCount ->
        DashboardState(
            agentName = agentName,
            todayPatientCount = todayCount,
            todayRevenue = todayRevenue,
            todayTotalBilled = todayBilled,
            pendingPaymentCount = pendingCount
        )
    }.combine(patientRepository.getTotalOutstanding()) { dash, outstanding ->
        dash.copy(totalOutstanding = outstanding)
    }.combine(patientRepository.getRecentPatients(5)) { dash, recent ->
        dash.copy(recentPatients = recent)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())
}
