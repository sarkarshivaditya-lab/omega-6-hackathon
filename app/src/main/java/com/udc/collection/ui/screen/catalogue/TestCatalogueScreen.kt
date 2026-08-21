package com.udc.collection.ui.screen.catalogue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.udc.collection.domain.model.LabTest
import com.udc.collection.ui.components.ConfirmDialog
import com.udc.collection.ui.components.EmptyState
import com.udc.collection.ui.components.UDCTopBar
import com.udc.collection.util.formatCurrency

@Composable
fun TestCatalogueScreen(onBack: () -> Unit, viewModel: TestCatalogueViewModel = hiltViewModel()) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val tests by viewModel.tests.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTest by remember { mutableStateOf<LabTest?>(null) }
    var deletingTest by remember { mutableStateOf<LabTest?>(null) }
    if (showAddDialog || editingTest != null) {
        ServiceEditDialog(editingTest, { showAddDialog = false; editingTest = null }) { name, price, category ->
            if (editingTest != null) viewModel.updateTest(editingTest!!, name, price, category) else viewModel.addTest(name, price, category)
            showAddDialog = false; editingTest = null
        }
    }
    deletingTest?.let { service -> ConfirmDialog("Delete Service", "Delete \"${service.name}\"?", "Delete", onConfirm = { viewModel.deleteTest(service); deletingTest = null }, onDismiss = { deletingTest = null }) }
    Scaffold(topBar = { UDCTopBar(title = "Service Catalogue", onBack = onBack) }, floatingActionButton = { ExtendedFloatingActionButton(onClick = { showAddDialog = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Add Service") }) }) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues)) {
            OutlinedTextField(value = searchQuery, onValueChange = viewModel::updateSearch, placeholder = { Text("Search services...") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), singleLine = true, shape = RoundedCornerShape(12.dp), leadingIcon = { Icon(Icons.Filled.Search, null) }, trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { viewModel.updateSearch("") }) { Icon(Icons.Filled.Clear, null) } })
            if (tests.isEmpty()) EmptyState("No services configured", Icons.Filled.Inventory2, Modifier.weight(1f)) else {
                Text("${tests.size} service${if (tests.size == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp))
                LazyColumn(contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    tests.groupBy { it.category.ifBlank { "Other" } }.forEach { (category, categoryTests) ->
                        item(key = "header_$category") { Text(category, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) }
                        items(categoryTests, key = { it.id }) { service -> ServiceCatalogueItem(service, { editingTest = service }, { deletingTest = service }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceCatalogueItem(service: LabTest, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(service.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium); if (service.isCustom) Text("Custom", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary) }
            Text(service.price.formatCurrency(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp)); IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Edit, "Edit", Modifier.size(18.dp)) }; IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Delete, "Delete", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun ServiceEditDialog(service: LabTest?, onDismiss: () -> Unit, onSave: (String, Double, String) -> Unit) {
    var name by remember { mutableStateOf(service?.name ?: "") }
    var price by remember { mutableStateOf(service?.price?.toString() ?: "") }
    var category by remember { mutableStateOf(service?.category ?: "") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (service == null) "Add Service" else "Edit Service", fontWeight = FontWeight.SemiBold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("Service Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
        OutlinedTextField(price, { price = it }, label = { Text("Price (₹) *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        OutlinedTextField(category, { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words))
    } }, confirmButton = { TextButton(onClick = { val p = price.toDoubleOrNull(); if (name.trim().isNotEmpty() && p != null) onSave(name, p, category) }, enabled = name.trim().isNotEmpty() && price.toDoubleOrNull() != null) { Text(if (service == null) "Add" else "Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
