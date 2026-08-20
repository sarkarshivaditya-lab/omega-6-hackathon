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
fun PatientDetailScreen(
    patientId: Long,
    onBack: () -> Unit,
    viewModel: PatientDetailViewModel = hiltViewModel()
) {
    val patient by viewModel.patient.collectAsState()
    val pdfFile by viewModel.pdfFile.collectAsState()
    val message by viewModel.message.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(patientId) { viewModel.loadPatient(patientId) }

    // Auto-open PDF share sheet when generated
    pdfFile?.let { file ->
        LaunchedEffect(file) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching { context.startActivity(intent) }
            viewModel.clearPdfFile()
        }
    }

    message?.let { msg ->
        LaunchedEffect(msg) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessage()
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete Patient",
            message = "Delete this patient record permanently? This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = { viewModel.deletePatient(onBack); showDeleteDialog = false },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Scaffold(
        topBar = {
            UDCTopBar(
                title = patient?.patientNumber ?: "Patient Detail",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { viewModel.generatePdf() }) {
                        Icon(Icons.Filled.PictureAsPdf, "Generate PDF",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, "Delete",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            )
        }
    ) { padding ->
        val p = patient
        if (p == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                message?.let { msg ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(msg, modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                PatientDetailContent(patient = p)
            }
        }
    }
}

@Composable
private fun PatientDetailContent(patient: Patient) {
    val dateFormat = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DetailCard("Patient Information") {
                DetailRow("Patient No.", patient.patientNumber)
                DetailRow("Receipt No.", patient.receiptNumber)
                DetailRow("Name", patient.name)
                if (patient.age.isNotBlank()) DetailRow("Age", patient.age)
                if (patient.gender.isNotBlank()) DetailRow("Gender", patient.gender)
                if (patient.phone.isNotBlank()) DetailRow("Phone", patient.phone)
                if (patient.address.isNotBlank()) DetailRow("Address", patient.address)
                if (patient.referringDoctor.isNotBlank()) DetailRow("Doctor", patient.referringDoctor)
                DetailRow("Date", patient.date.format(dateFormat))
                if (patient.remarks.isNotBlank()) DetailRow("Remarks", patient.remarks)
            }
        }

        if (patient.selectedTests.isNotEmpty()) {
            item {
                Text("Tests (${patient.selectedTests.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp))
            }
            items(patient.selectedTests) { test ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(1.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            if (test.isPackage) "★ ${test.testName}" else test.testName,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(test.price.formatCurrency(), style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            DetailCard("Billing") {
                DetailRow("Subtotal", patient.subtotal.formatCurrency())
                if (patient.discountType != DiscountType.NONE && patient.discountValue > 0) {
                    val discLabel = when (patient.discountType) {
                        DiscountType.PERCENTAGE -> "Discount (${patient.discountValue.toInt()}%)"
                        DiscountType.FLAT -> "Discount (Flat)"
                        DiscountType.NONE -> ""
                    }
                    val discAmt = when (patient.discountType) {
                        DiscountType.PERCENTAGE -> patient.subtotal * patient.discountValue / 100.0
                        DiscountType.FLAT -> patient.discountValue
                        DiscountType.NONE -> 0.0
                    }
                    DetailRow(discLabel, "- ${discAmt.formatCurrency()}")
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Grand Total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(patient.grandTotal.formatCurrency(), style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            DetailCard("Payment") {
                val statusColor = when (patient.paymentStatus) {
                    PaymentStatus.PAID -> MaterialTheme.colorScheme.primary
                    PaymentStatus.UNPAID -> MaterialTheme.colorScheme.error
                    PaymentStatus.PARTIAL -> MaterialTheme.colorScheme.tertiary
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Status", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Badge(containerColor = statusColor) {
                        Text(patient.paymentStatus.label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                DetailRow("Method", patient.paymentMethod.label)
                if (patient.paymentStatus == PaymentStatus.PARTIAL) {
                    DetailRow("Amount Received", patient.amountReceived.formatCurrency())
                    DetailRow(
                        "Balance Due",
                        (patient.grandTotal - patient.amountReceived).coerceAtLeast(0.0).formatCurrency()
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun DetailCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.4f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f))
    }
}
