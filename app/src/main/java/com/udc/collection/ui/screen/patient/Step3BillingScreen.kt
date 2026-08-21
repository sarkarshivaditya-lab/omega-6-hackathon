package com.udc.collection.ui.screen.patient

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.udc.collection.domain.model.DiscountType
import com.udc.collection.domain.model.PaymentMethod
import com.udc.collection.domain.model.PaymentStatus
import com.udc.collection.util.ValidationUtils
import com.udc.collection.util.formatCurrency

@Composable
fun Step3BillingScreen(state: WizardState, vm: PatientWizardViewModel, onBack: () -> Unit, onSave: () -> Unit) {
    var showErrors by remember { mutableStateOf(false) }
    val discountError = if (showErrors && state.discountType != DiscountType.NONE) ValidationUtils.validateDiscount(state.discountValue, state.discountType, state.subtotal) else null
    val amountError = if (showErrors && state.paymentStatus == PaymentStatus.PARTIAL) ValidationUtils.validateAmountReceived(state.amountReceived, state.grandTotal) else null
    Scaffold(bottomBar = { Surface(shadowElevation = 8.dp) { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(12.dp)) { Text("Back") }; Button(onClick = { showErrors = true; val d = state.discountType != DiscountType.NONE && ValidationUtils.validateDiscount(state.discountValue, state.discountType, state.subtotal) != null; val a = state.paymentStatus == PaymentStatus.PARTIAL && ValidationUtils.validateAmountReceived(state.amountReceived, state.grandTotal) != null; if (!d && !a) onSave() }, enabled = state.allValid && !state.isSaving, modifier = Modifier.weight(2f).height(52.dp), shape = RoundedCornerShape(12.dp)) { if (state.isSaving) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp) else Text("Save Customer", fontWeight = FontWeight.SemiBold) } } } }) { inner ->
        LazyColumn(Modifier.fillMaxSize().padding(inner), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Selected Services", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) }
            items(state.selectedTests, key = { "${it.testId}_${it.isPackage}" }) { service -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { if (service.isPackage) Text("★ ", color = MaterialTheme.colorScheme.tertiary); Text(service.testName, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium); Text(service.price.formatCurrency(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary); IconButton(onClick = { vm.removeSelectedTest(service.testId, service.isPackage) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Close, "Remove", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) } } }
            item { HorizontalDivider() }; item { BillingDiscountSection(state, discountError, vm::updateDiscountType, vm::updateDiscountValue) }
            item { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { TotalRow("Subtotal", state.subtotal.formatCurrency()); if (state.discountAmount > 0) TotalRow(when (state.discountType) { DiscountType.PERCENTAGE -> "Discount (${state.discountValue}%)"; DiscountType.FLAT -> "Discount (Flat)"; DiscountType.NONE -> "Discount" }, "- ${state.discountAmount.formatCurrency()}", MaterialTheme.colorScheme.error); HorizontalDivider(); TotalRow("Grand Total", state.grandTotal.formatCurrency(), MaterialTheme.colorScheme.primary) } } }
            item { HorizontalDivider() }; item { PaymentSection(state, amountError, vm::updatePaymentStatus, vm::updatePaymentMethod, vm::updateAmountReceived) }
            if (state.paymentStatus == PaymentStatus.PARTIAL) item { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))) { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Balance Due", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold); Text(state.balanceDue.formatCurrency(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) } } }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable private fun BillingDiscountSection(state: WizardState, discountError: String?, onDiscountTypeChange: (DiscountType) -> Unit, onDiscountValueChange: (String) -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Discount", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { DiscountType.entries.forEach { type -> FilterChip(selected = state.discountType == type, onClick = { onDiscountTypeChange(type) }, label = { Text(when (type) { DiscountType.NONE -> "None"; DiscountType.PERCENTAGE -> "% Off"; DiscountType.FLAT -> "Flat ₹" }) }) } }; if (state.discountType != DiscountType.NONE) OutlinedTextField(value = state.discountValue, onValueChange = onDiscountValueChange, label = { Text(if (state.discountType == DiscountType.PERCENTAGE) "Discount %" else "Discount Amount (₹)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, isError = discountError != null, supportingText = discountError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }, shape = RoundedCornerShape(10.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)) } }
@Composable private fun PaymentSection(state: WizardState, amountError: String?, onStatusChange: (PaymentStatus) -> Unit, onMethodChange: (PaymentMethod) -> Unit, onAmountChange: (String) -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Payment", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary); Text("Payment Status", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PaymentStatus.entries.forEach { status -> FilterChip(selected = state.paymentStatus == status, onClick = { onStatusChange(status) }, label = { Text(status.label) }) } }; Text("Payment Method", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { PaymentMethod.entries.forEach { method -> FilterChip(selected = state.paymentMethod == method, onClick = { onMethodChange(method) }, label = { Text(method.label) }) } }; if (state.paymentStatus == PaymentStatus.PARTIAL) OutlinedTextField(value = state.amountReceived, onValueChange = onAmountChange, label = { Text("Amount Received (₹)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, isError = amountError != null, supportingText = amountError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }, shape = RoundedCornerShape(10.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)) } }
@Composable private fun TotalRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onPrimaryContainer) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer); Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = valueColor) } }
