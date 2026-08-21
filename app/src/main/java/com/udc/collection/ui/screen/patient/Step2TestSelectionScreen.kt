package com.udc.collection.ui.screen.patient

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
import com.udc.collection.domain.model.LabPackage
import com.udc.collection.domain.model.LabTest
import com.udc.collection.util.formatCurrency

@Composable
fun Step2TestSelectionScreen(state: WizardState, vm: PatientWizardViewModel, allTests: List<LabTest>, allPackages: List<LabPackage>, frequentTests: List<LabTest>, onNext: () -> Unit, onBack: () -> Unit) {
    val selectedTestIds = remember(state.selectedTests) { state.selectedTests.filter { !it.isPackage }.map { it.testId }.toSet() }
    val selectedPackageIds = remember(state.selectedTests) { state.selectedTests.filter { it.isPackage }.map { it.testId }.toSet() }
    val grouped = remember(allTests) { allTests.groupBy { it.category.ifBlank { "Other" } }.toSortedMap() }
    Scaffold(bottomBar = { BillingSummaryBar(state, onNext, onBack) }) { inner ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(inner), contentPadding = PaddingValues(bottom = 8.dp)) {
            item {
                OutlinedTextField(value = state.testSearchQuery, onValueChange = vm::updateTestSearch, placeholder = { Text("Search services…") }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), singleLine = true, shape = RoundedCornerShape(12.dp), leadingIcon = { Icon(Icons.Filled.Search, null) }, trailingIcon = { if (state.testSearchQuery.isNotEmpty()) IconButton(onClick = { vm.updateTestSearch("") }) { Icon(Icons.Filled.Clear, null) } })
            }
            val isSearching = state.testSearchQuery.isNotBlank()
            if (isSearching) {
                if (allPackages.isNotEmpty()) {
                    item { CategoryHeader("Packages", Icons.Filled.Inventory2) }
                    items(allPackages, key = { "pkg_${it.id}" }) { pkg -> PackageListItem(pkg, pkg.id in selectedPackageIds) { vm.togglePackage(pkg) } }
                }
                if (allTests.isNotEmpty()) {
                    item { CategoryHeader("Services", Icons.Filled.Inventory2) }
                    items(allTests, key = { "test_${it.id}" }) { test -> TestListItem(test, test.id in selectedTestIds) { vm.toggleTest(test) } }
                }
                if (allTests.isEmpty() && allPackages.isEmpty()) item { EmptyCatalogueMessage() }
            } else {
                if (allPackages.isNotEmpty()) {
                    item { CategoryHeader("Packages", Icons.Filled.Inventory2) }
                    items(allPackages, key = { "pkg_${it.id}" }) { pkg -> PackageListItem(pkg, pkg.id in selectedPackageIds) { vm.togglePackage(pkg) } }
                }
                if (frequentTests.isNotEmpty()) {
                    item { CategoryHeader("Frequently Used Services", Icons.Filled.Star) }
                    items(frequentTests, key = { "freq_${it.id}" }) { test -> TestListItem(test, test.id in selectedTestIds) { vm.toggleTest(test) } }
                }
                if (grouped.isNotEmpty()) grouped.forEach { (category, tests) -> item(key = "cat_$category") { CollapsibleCategory(category, tests, selectedTestIds) { vm.toggleTest(it) } } }
                if (allTests.isEmpty() && allPackages.isEmpty()) item { EmptyCatalogueMessage() }
            }
        }
    }
}

@Composable private fun EmptyCatalogueMessage() {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text("No services configured yet. Add services from Service Catalogue.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable private fun CollapsibleCategory(title: String, tests: List<LabTest>, selectedIds: Set<Long>, onToggle: (LabTest) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCount = tests.count { it.id in selectedIds }
    Column {
        Row(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            if (selectedCount > 0) Badge { Text("$selectedCount") }
            Text("${tests.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        if (expanded) Column(modifier = Modifier.padding(horizontal = 16.dp)) { tests.forEach { test -> TestListItem(test, test.id in selectedIds) { onToggle(test) } } }
    }
}

@Composable private fun CategoryHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable private fun TestListItem(test: LabTest, isSelected: Boolean, onToggle: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).clickable(onClick = onToggle), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(if (isSelected) 2.dp else 1.dp)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isSelected, onCheckedChange = { onToggle() }); Spacer(Modifier.width(6.dp)); Text(test.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium); Text(test.price.formatCurrency(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable private fun PackageListItem(pkg: LabPackage, isSelected: Boolean, onToggle: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable(onClick = onToggle), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant), elevation = CardDefaults.cardElevation(if (isSelected) 3.dp else 1.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isSelected, onCheckedChange = { onToggle() }); Spacer(Modifier.width(6.dp)); Column(Modifier.weight(1f)) { Text(pkg.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold); if (pkg.description.isNotBlank()) Text(pkg.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(pkg.price.formatCurrency(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable private fun BillingSummaryBar(state: WizardState, onNext: () -> Unit, onBack: () -> Unit) {
    Surface(shadowElevation = 12.dp) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("${state.selectedTests.size} service${if (state.selectedTests.size == 1) "" else "s"} selected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); if (state.discountAmount > 0) Text("Discount: ${state.discountAmount.formatCurrency()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Column { Text("Subtotal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(state.subtotal.formatCurrency(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }; Column(horizontalAlignment = Alignment.End) { Text("Total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary); Text(state.grandTotal.formatCurrency(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) } }
            Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(10.dp)) { Text("Back") }; Button(onClick = onNext, enabled = state.step2Valid, modifier = Modifier.weight(2f).height(48.dp), shape = RoundedCornerShape(10.dp)) { Text("Next: Billing") } }
        }
    }
}
