package com.udc.collection.ui.screen.catalogue

import androidx.compose.foundation.clickable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestCatalogueScreen(
    onBack: () -> Unit,
    viewModel: TestCatalogueViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val tests by viewModel.tests.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTest by remember { mutableStateOf<LabTest?>(null) }
    var deletingTest by remember { mutableStateOf<LabTest?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    if (showAddDialog || editingTest != null) {
        TestEditDialog(
            test = editingTest,
            onDismiss = { showAddDialog = false; editingTest = null },
            onSave = { name, price, category ->
                if (editingTest != null) {
                    viewModel.updateTest(editingTest!!, name, price, category)
                } else {
                    viewModel.addTest(name, price, category)
                }
                showAddDialog = false
                editingTest = null
            }
        )
    }

    deletingTest?.let { test ->
        ConfirmDialog(
            title = "Delete Test",
            message = "Delete \"${test.name}\"?",
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.deleteTest(test)
                deletingTest = null
            },
            onDismiss = { deletingTest = null }
        )
    }

    if (showResetDialog) {
        ConfirmDialog(
            title = "Reset Catalogue",
            message = "This will delete all tests and restore the default catalogue. Custom tests will be lost.",
            confirmLabel = "Reset",
            onConfirm = {
                viewModel.resetCatalogue()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }

    Scaffold(
        topBar = {
            UDCTopBar(
                title = "Test Catalogue",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reset to defaults",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Add Test") }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::updateSearch,
                placeholder = { Text("Search tests...") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearch("") }) {
                            Icon(Icons.Filled.Clear, null)
                        }
                    }
                }
            )

            if (tests.isEmpty()) {
                EmptyState(
                    message = "No tests found",
                    icon = Icons.Filled.Biotech,
                    modifier = Modifier.weight(1f)
                )
            } else {
                val grouped = tests.groupBy { it.category.ifBlank { "Other" } }

                Text(
                    text = "${tests.size} tests",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                )

                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    grouped.forEach { (category, categoryTests) ->
                        item(key = "header_$category") {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(categoryTests, key = { it.id }) { test ->
                            TestCatalogueItem(
                                test = test,
                                onEdit = { editingTest = test },
                                onDelete = { deletingTest = test }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TestCatalogueItem(
    test: LabTest,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(test.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                if (test.isCustom) {
                    Text("Custom", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            Text(test.price.formatCurrency(), style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestEditDialog(
    test: LabTest?,
    onDismiss: () -> Unit,
    onSave: (String, Double, String) -> Unit
) {
    var name by remember { mutableStateOf(test?.name ?: "") }
    var price by remember { mutableStateOf(test?.price?.toString() ?: "") }
    var category by remember { mutableStateOf(test?.category ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (test == null) "Add Test" else "Edit Test", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Test Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price (₹) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val p = price.toDoubleOrNull()
                    if (name.trim().isNotEmpty() && p != null) {
                        onSave(name, p, category)
                    }
                },
                enabled = name.trim().isNotEmpty() && price.toDoubleOrNull() != null
            ) {
                Text(if (test == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
