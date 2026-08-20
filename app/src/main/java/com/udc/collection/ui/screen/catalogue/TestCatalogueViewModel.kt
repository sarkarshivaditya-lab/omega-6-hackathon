package com.udc.collection.ui.screen.catalogue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.udc.collection.data.repository.LabTestRepository
import com.udc.collection.domain.model.LabTest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TestCatalogueViewModel @Inject constructor(
    private val labTestRepository: LabTestRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val tests: StateFlow<List<LabTest>> = _searchQuery
        .debounce(150)
        .flatMapLatest { query ->
            if (query.isBlank()) labTestRepository.getAllTests()
            else labTestRepository.searchTests(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearch(query: String) { _searchQuery.value = query }

    fun addTest(name: String, price: Double, category: String) {
        viewModelScope.launch {
            labTestRepository.addTest(
                LabTest(name = name.trim(), price = price, category = category.trim(), isCustom = true)
            )
        }
    }

    fun updateTest(test: LabTest, name: String, price: Double, category: String) {
        viewModelScope.launch {
            labTestRepository.updateTest(test.copy(name = name.trim(), price = price, category = category.trim()))
        }
    }

    fun deleteTest(test: LabTest) {
        viewModelScope.launch { labTestRepository.deleteTest(test) }
    }

    fun resetCatalogue() {
        viewModelScope.launch { labTestRepository.resetToDefault() }
    }
}
