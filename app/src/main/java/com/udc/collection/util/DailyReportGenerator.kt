package com.udc.collection.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.udc.collection.data.local.dao.PatientDao
import com.udc.collection.data.repository.BackupResult
import com.udc.collection.domain.model.PaymentMethod
import com.udc.collection.domain.model.PaymentStatus
import com.udc.collection.domain.model.SelectedTest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyReportGenerator @Inject constructor(@ApplicationContext private val context: Context, private val patientDao: PatientDao) {
    private val gson = Gson()
    private val serviceListType = object : TypeToken<List<SelectedTest>>() {}.type

    suspend fun generate(agentName: String): BackupResult = withContext(Dispatchers.IO) {
        runCatching {
            val today = LocalDate.now(); val todayStr = DateTimeFormatter.ISO_LOCAL_DATE.format(today); val customers = patientDao.getPatientsForDate(todayStr)
            val totalBilled = customers.sumOf { it.grandTotal }; val totalReceived = customers.sumOf { it.amountReceived }; val paidCount = customers.count { it.paymentStatus == PaymentStatus.PAID.name }; val unpaidCount = customers.count { it.paymentStatus == PaymentStatus.UNPAID.name }; val partialCount = customers.count { it.paymentStatus == PaymentStatus.PARTIAL.name }
            val cashReceived = customers.filter { it.paymentMethod == PaymentMethod.CASH.name }.sumOf { it.amountReceived }; val upiReceived = customers.filter { it.paymentMethod == PaymentMethod.UPI.name }.sumOf { it.amountReceived }; val cardReceived = customers.filter { it.paymentMethod == PaymentMethod.CARD.name }.sumOf { it.amountReceived }; val creditCount = customers.count { it.paymentMethod == PaymentMethod.CREDIT.name }
            val displayDate = DateTimeFormatter.ofPattern("dd/MM/yyyy").format(today); val generatedAt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now())
            val report = buildString {
                appendLine("╔══════════════════════════════════════════════════╗"); appendLine("║             OMEGA 6.0 — DAILY REPORT            ║"); appendLine("╚══════════════════════════════════════════════════╝"); appendLine(); appendLine("Date      : $displayDate"); appendLine("User      : ${agentName.ifBlank { "—" }}"); appendLine("Generated : $generatedAt"); appendLine(); appendLine("══════════════════════════════════════════════════"); appendLine("SUMMARY"); appendLine("══════════════════════════════════════════════════"); appendLine("Total Customers  : ${customers.size}"); appendLine("Total Billed     : ${totalBilled.fmt()}"); appendLine("Amount Received  : ${totalReceived.fmt()}"); appendLine("Outstanding      : ${(totalBilled - totalReceived).fmt()}"); appendLine(); appendLine("Payment Status   : $paidCount Paid | $partialCount Partial | $unpaidCount Unpaid"); appendLine(); appendLine("COLLECTION BREAKDOWN"); appendLine("  Cash   : ${cashReceived.fmt()}"); appendLine("  UPI    : ${upiReceived.fmt()}"); appendLine("  Card   : ${cardReceived.fmt()}"); if (creditCount > 0) appendLine("  Credit : $creditCount customer(s) on credit"); appendLine(); appendLine("══════════════════════════════════════════════════"); appendLine("CUSTOMER LIST"); appendLine("══════════════════════════════════════════════════")
                if (customers.isEmpty()) appendLine("No customers registered today.") else customers.forEachIndexed { idx, p ->
                    val services = runCatching { gson.fromJson<List<SelectedTest>>(p.selectedTests, serviceListType) }.getOrDefault(emptyList()); val serviceNames = services.joinToString(", ") { it.testName }
                    appendLine(); appendLine("${idx + 1}. ${p.name}  [${p.receiptNumber}]"); if (p.phone.isNotBlank()) appendLine("   Phone      : ${p.phone}"); appendLine("   Services   : ${serviceNames.ifBlank { "—" }}"); appendLine("   Total      : ${p.grandTotal.fmt()}  (${p.paymentStatus}  ·  ${p.paymentMethod})"); if (p.amountReceived > 0 && p.paymentStatus != PaymentStatus.PAID.name) appendLine("   Received   : ${p.amountReceived.fmt()}")
                }
                appendLine(); appendLine("══════════════════════════════════════════════════"); appendLine("               END OF DAILY REPORT"); appendLine("══════════════════════════════════════════════════")
            }
            val dir = context.getExternalFilesDir("OMEGA6_Reports") ?: context.filesDir; val filename = "OMEGA6_Report_${DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())}.txt"; val file = File(dir, filename); file.writeText(report); BackupResult.Success("Daily report saved: ${file.name}")
        }.getOrElse { e -> BackupResult.Error("Report generation failed: ${e.localizedMessage}") }
    }
    private fun Double.fmt(): String = "₹%.2f".format(this)
}
