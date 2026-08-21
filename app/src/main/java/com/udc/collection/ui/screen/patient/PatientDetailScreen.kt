package com.udc.collection.ui.screen.patient

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.udc.collection.domain.model.DiscountType
import com.udc.collection.domain.model.Patient
import com.udc.collection.domain.model.PaymentStatus
import com.udc.collection.ui.components.ConfirmDialog
import com.udc.collection.ui.components.UDCTopBar
import com.udc.collection.util.formatCurrency
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PatientDetailScreen(patientId: Long, onBack: () -> Unit, viewModel: PatientDetailViewModel = hiltViewModel()) {
    val customer by viewModel.patient.collectAsState(); val pdfFile by viewModel.pdfFile.collectAsState(); val message by viewModel.message.collectAsState(); var showDeleteDialog by remember { mutableStateOf(false) }; val context = LocalContext.current
    LaunchedEffect(patientId) { viewModel.loadPatient(patientId) }
    pdfFile?.let { file -> LaunchedEffect(file) { val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file); val intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/pdf"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }; runCatching { context.startActivity(intent) }; viewModel.clearPdfFile() } }
    message?.let { LaunchedEffect(it) { kotlinx.coroutines.delay(3000); viewModel.clearMessage() } }
    if (showDeleteDialog) ConfirmDialog("Delete Customer", "Delete this customer record permanently? This cannot be undone.", "Delete", onConfirm = { viewModel.deletePatient(onBack); showDeleteDialog = false }, onDismiss = { showDeleteDialog = false })
    Scaffold(topBar = { UDCTopBar(customer?.patientNumber ?: "Customer Detail", onBack, actions = { IconButton({ viewModel.generatePdf() }) { Icon(Icons.Filled.PictureAsPdf, "Generate PDF", tint = MaterialTheme.colorScheme.onPrimary) }; IconButton({ showDeleteDialog = true }) { Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.onPrimary) } }) }) { padding ->
        val p = customer
        if (p == null) Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) { CircularProgressIndicator() }
        else Column(Modifier.fillMaxSize().padding(padding)) { message?.let { Card(Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) { Text(it, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall) } }; CustomerDetailContent(p) }
    }
}

@Composable private fun CustomerDetailContent(customer: Patient) {
    val dateFormat = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { DetailCard("Customer Information") { DetailRow("Customer No.", customer.patientNumber); DetailRow("Receipt No.", customer.receiptNumber); DetailRow("Name", customer.name); if (customer.phone.isNotBlank()) DetailRow("Phone", customer.phone); if (customer.address.isNotBlank()) DetailRow("Address", customer.address); DetailRow("Date", customer.date.format(dateFormat)); if (customer.remarks.isNotBlank()) DetailRow("Notes", customer.remarks) } }
        if (customer.selectedTests.isNotEmpty()) { item { Text("Services (${customer.selectedTests.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp)) }; items(customer.selectedTests) { service -> Card(Modifier.fillMaxWidth(), RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(1.dp)) { Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(if (service.isPackage) "★ ${service.testName}" else service.testName, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium); Text(service.price.formatCurrency(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary) } } } }
        item { DetailCard("Billing") { DetailRow("Subtotal", customer.subtotal.formatCurrency()); if (customer.discountType != DiscountType.NONE && customer.discountValue > 0) { val discLabel = when (customer.discountType) { DiscountType.PERCENTAGE -> "Discount (${customer.discountValue.toInt()}%)"; DiscountType.FLAT -> "Discount (Flat)"; DiscountType.NONE -> "" }; val discAmt = when (customer.discountType) { DiscountType.PERCENTAGE -> customer.subtotal * customer.discountValue / 100.0; DiscountType.FLAT -> customer.discountValue; DiscountType.NONE -> 0.0 }; DetailRow(discLabel, "- ${discAmt.formatCurrency()}") }; HorizontalDivider(Modifier.padding(vertical = 4.dp)); Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Grand Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(customer.grandTotal.formatCurrency(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) } } }
        item { DetailCard("Payment") { val statusColor = when (customer.paymentStatus) { PaymentStatus.PAID -> MaterialTheme.colorScheme.primary; PaymentStatus.UNPAID -> MaterialTheme.colorScheme.error; PaymentStatus.PARTIAL -> MaterialTheme.colorScheme.tertiary }; Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Status", color = MaterialTheme.colorScheme.onSurfaceVariant); Badge(containerColor = statusColor) { Text(customer.paymentStatus.label, Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) } }; Spacer(Modifier.height(4.dp)); DetailRow("Method", customer.paymentMethod.label); if (customer.paymentStatus == PaymentStatus.PARTIAL) { DetailRow("Amount Received", customer.amountReceived.formatCurrency()); DetailRow("Balance Due", (customer.grandTotal - customer.amountReceived).coerceAtLeast(0.0).formatCurrency()) } } }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable private fun DetailCard(title: String, content: @Composable ColumnScope.() -> Unit) { Card(Modifier.fillMaxWidth(), RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) { Column(Modifier.padding(16.dp)) { Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(12.dp)); content() } } }
@Composable private fun DetailRow(label: String, value: String) { if (value.isBlank()) return; Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), Arrangement.SpaceBetween) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.4f)); Text(value, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.6f)) } }
