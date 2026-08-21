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

    Scaffold(topBar = { UDCTopBar(title = "OMEGA 6.0") }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccountCircle, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Welcome, ${dash.agentName.ifBlank { "User" }}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(today, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                }
            }
            Text("Today's Summary", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Modifier.weight(1f), "Customers Today", "${dash.todayPatientCount}", Icons.Filled.People, MaterialTheme.colorScheme.primary)
                StatCard(Modifier.weight(1f), "Revenue (Paid)", dash.todayRevenue.formatCurrency(), Icons.Filled.CurrencyRupee, Color(0xFF2E7D32))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(Modifier.weight(1f), "Total Billed", dash.todayTotalBilled.formatCurrency(), Icons.Filled.Receipt, MaterialTheme.colorScheme.secondary)
                StatCard(Modifier.weight(1f), "Pending Bills", "${dash.pendingPaymentCount}", Icons.Filled.Pending, Color(0xFFE65100), if (dash.totalOutstanding > 0) dash.totalOutstanding.formatCurrency() else null)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Quick Actions", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
            LargeActionButton("New Customer", Icons.Filled.PersonAdd, onNewPatient)
            LargeActionButton("Customer History", Icons.Filled.History, onPatientHistory)
            LargeActionButton("Service Catalogue", Icons.Filled.Inventory2, onTestCatalogue)
            LargeActionButton("Settings", Icons.Filled.Settings, onSettings)
            if (dash.recentPatients.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("Recent Customers", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 4.dp))
                dash.recentPatients.forEach { customer ->
                    RecentCustomerRow(customer = customer, onClick = { onPatientClick(customer.id) })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RecentCustomerRow(customer: Patient, onClick: () -> Unit) {
    val dateFormat = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH)
    val statusColor = when (customer.paymentStatus) {
        PaymentStatus.PAID -> Color(0xFF2E7D32)
        PaymentStatus.UNPAID -> MaterialTheme.colorScheme.error
        PaymentStatus.PARTIAL -> Color(0xFFE65100)
    }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(10.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(customer.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("${customer.date.format(dateFormat)}  ·  ${customer.selectedTests.size} service${if (customer.selectedTests.size == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(customer.grandTotal.formatCurrency(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(customer.paymentStatus.label, style = MaterialTheme.typography.bodySmall, color = statusColor)
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, label: String, value: String, icon: ImageVector, iconColor: Color, subLabel: String? = null) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (subLabel != null) Text(subLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}
