package com.udc.collection.ui.screen.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.udc.collection.domain.model.Patient
import com.udc.collection.ui.components.EmptyState
import com.udc.collection.ui.components.UDCTopBar
import com.udc.collection.util.formatCurrency
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PatientHistoryScreen(onBack: () -> Unit, onPatientClick: (Long) -> Unit, viewModel: PatientHistoryViewModel = hiltViewModel()) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val customers by viewModel.patients.collectAsState()
    val recentlyDeleted by viewModel.recentlyDeleted.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(recentlyDeleted) {
        val deleted = recentlyDeleted ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar("${deleted.name} deleted", "Undo", SnackbarDuration.Long)
        when (result) { SnackbarResult.ActionPerformed -> viewModel.undoDelete(); SnackbarResult.Dismissed -> viewModel.clearRecentlyDeleted() }
    }
    Scaffold(topBar = { UDCTopBar("Customer History", onBack) }, snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues)) {
            OutlinedTextField(value = searchQuery, onValueChange = viewModel::updateSearch, placeholder = { Text("Search by name, phone, number, date...") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), singleLine = true, shape = RoundedCornerShape(12.dp), leadingIcon = { Icon(Icons.Filled.Search, null) }, trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { viewModel.updateSearch("") }) { Icon(Icons.Filled.Clear, null) } })
            if (customers.isEmpty()) EmptyState(if (searchQuery.isBlank()) "No customers yet" else "No customers found", Icons.Filled.Person, Modifier.weight(1f))
            else {
                Text("${customers.size} customer${if (customers.size == 1) "" else "s"}  •  swipe left to delete", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(customers, key = { it.id }) { customer -> SwipeToDeleteCustomerItem(customer, { viewModel.deletePatient(customer) }) { onPatientClick(customer.id) } }; item { Spacer(Modifier.height(8.dp)) } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SwipeToDeleteCustomerItem(customer: Patient, onDelete: () -> Unit, onClick: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { value -> if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false })
    SwipeToDismissBox(state = dismissState, enableDismissFromStartToEnd = false, backgroundContent = { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp)).padding(end = 24.dp), Alignment.CenterEnd) { Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.onErrorContainer) } }) { CustomerHistoryItem(customer, onClick) }
}

@Composable private fun CustomerHistoryItem(customer: Patient, onClick: () -> Unit) {
    val dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(customer.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp)); Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Text(customer.patientNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium); Text(customer.date.format(dateFormat), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); if (customer.phone.isNotBlank()) Text(customer.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (customer.selectedTests.isNotEmpty()) Text("${customer.selectedTests.size} service${if (customer.selectedTests.size == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) { Text(customer.grandTotal.formatCurrency(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
