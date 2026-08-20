package com.udc.collection.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.clickable
import com.udc.collection.domain.model.Patient
import com.udc.collection.domain.model.PaymentStatus
import com.udc.collection.ui.components.LargeActionButton
import com.udc.collection.ui.components.UDCTopBar
import com.udc.collection.util.formatCurrency
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    onNewPatient: () -> Unit,
    onPatientHistory: () -> Unit,
    onTestCatalogue: () -> Unit,
    onSettings: () -> Unit,
    onPatientClick: (Long) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val dash by viewModel.dashboardState.collectAsState()
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.ENGLISH))

    Scaffold(topBar = { UDCTopBar(title = "Urban Diagnostic Collection") }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Agent + date header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.AccountCircle, null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Welcome, ${dash.agentName.ifBlank { "Agent" }}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(today, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                }
            }

            // Dashboard stats
            Text("Today's Summary", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Patients Today",
                    value = "${dash.todayPatientCount}",
                    icon = Icons.Filled.People,
                    iconColor = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Revenue (Paid)",
                    value = dash.todayRevenue.formatCurrency(),
                    icon = Icons.Filled.CurrencyRupee,
                    iconColor = Color(0xFF2E7D32)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Total Billed",
                    value = dash.todayTotalBilled.formatCurrency(),
                    icon = Icons.Filled.Receipt,
                    iconColor = MaterialTheme.colorScheme.secondary
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label = "Pending Bills",
                    value = "${dash.pendingPaymentCount}",
                    icon = Icons.Filled.Pending,
                    iconColor = Color(0xFFE65100),
                    subLabel = if (dash.totalOutstanding > 0) dash.totalOutstanding.formatCurrency() else null
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Quick actions
            Text("Quick Actions", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp))

            LargeActionButton(
                text = "New Patient",
                icon = Icons.Filled.PersonAdd,
                onClick = onNewPatient
            )
            LargeActionButton(
                text = "Patient History",
                icon = Icons.Filled.History,
                onClick = onPatientHistory
            )
            LargeActionButton(
                text = "Test Catalogue",
                icon = Icons.Filled.Biotech,
                onClick = onTestCatalogue
            )
            LargeActionButton(
                text = "Settings",
                icon = Icons.Filled.Settings,
                onClick = onSettings
            )

            // Recent patients
            if (dash.recentPatients.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Recent Patients", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp))

                dash.recentPatients.forEach { patient ->
                    RecentPatientRow(patient = patient, onClick = { onPatientClick(patient.id) })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RecentPatientRow(patient: Patient, onClick: () -> Unit) {
    val dateFormat = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)
    val statusColor = when (patient.paymentStatus) {
        PaymentStatus.PAID -> Color(0xFF2E7D32)
        PaymentStatus.UNPAID -> MaterialTheme.colorScheme.error
        PaymentStatus.PARTIAL -> Color(0xFFE65100)
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(patient.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    "${patient.date.format(dateFormat)}  ·  ${patient.selectedTests.size} test${if (patient.selectedTests.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(patient.grandTotal.formatCurrency(), style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold)
                Text(patient.paymentStatus.label, style = MaterialTheme.typography.bodySmall, color = statusColor)
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    subLabel: String? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null,
                    tint = iconColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (subLabel != null) {
                Text(subLabel, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
