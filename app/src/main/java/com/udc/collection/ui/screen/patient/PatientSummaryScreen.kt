package com.udc.collection.ui.screen.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.udc.collection.domain.model.DiscountType
import com.udc.collection.ui.components.UDCTopBar
import com.udc.collection.util.formatCurrency
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PatientSummaryScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    formViewModel: PatientFormViewModel = hiltViewModel(),
    summaryViewModel: PatientSummaryViewModel = hiltViewModel()
) {
    val state by formViewModel.state.collectAsState()
    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { UDCTopBar(title = "Patient Summary", onBack = onBack) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Back", style = MaterialTheme.typography.titleSmall)
                    }
                    Button(
                        onClick = {
                            isSaving = true
                            summaryViewModel.savePatient(formViewModel.buildPatient()) {
                                onSaved()
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save Patient", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SummaryCard(title = "Patient Information") {
                    SummaryRow("Name", state.name)
                    if (state.age.isNotBlank()) SummaryRow("Age", state.age)
                    if (state.gender.isNotBlank()) SummaryRow("Gender", state.gender)
                    if (state.phone.isNotBlank()) SummaryRow("Phone", state.phone)
                    if (state.address.isNotBlank()) SummaryRow("Address", state.address)
                    if (state.referringDoctor.isNotBlank()) SummaryRow("Doctor", state.referringDoctor)
                    SummaryRow("Date", state.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH)))
                    if (state.remarks.isNotBlank()) SummaryRow("Remarks", state.remarks)
                }
            }

            item {
                SummaryCard(title = "Tests (${state.selectedTests.size})") {
                    state.selectedTests.forEach { test ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                test.testName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                test.price.formatCurrency(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }

            item {
                SummaryCard(title = "Billing") {
                    SummaryRow("Subtotal", state.subtotal.formatCurrency())
                    if (state.discountType != DiscountType.NONE && state.discountAmount > 0) {
                        val discLabel = when (state.discountType) {
                            DiscountType.PERCENTAGE -> "Discount (${state.discountValue}%)"
                            DiscountType.FLAT -> "Discount (Flat)"
                            DiscountType.NONE -> ""
                        }
                        SummaryRow(discLabel, "- ${state.discountAmount.formatCurrency()}",
                            valueColor = MaterialTheme.colorScheme.error)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Grand Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            state.grandTotal.formatCurrency(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
            color = valueColor, modifier = Modifier.weight(0.6f))
    }
}
