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
class DailyReportGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val patientDao: PatientDao
) {
    private val gson = Gson()
    private val testListType = object : TypeToken<List<SelectedTest>>() {}.type

    suspend fun generate(agentName: String): BackupResult = withContext(Dispatchers.IO) {
        runCatching {
            val today = LocalDate.now()
            val todayStr = DateTimeFormatter.ISO_LOCAL_DATE.format(today)
            val patients = patientDao.getPatientsForDate(todayStr)

            val totalBilled = patients.sumOf { it.grandTotal }
            val totalReceived = patients.sumOf { it.amountReceived }
            val paidCount = patients.count { it.paymentStatus == PaymentStatus.PAID.name }
            val unpaidCount = patients.count { it.paymentStatus == PaymentStatus.UNPAID.name }
            val partialCount = patients.count { it.paymentStatus == PaymentStatus.PARTIAL.name }

            val cashReceived = patients.filter { it.paymentMethod == PaymentMethod.CASH.name }.sumOf { it.amountReceived }
            val upiReceived = patients.filter { it.paymentMethod == PaymentMethod.UPI.name }.sumOf { it.amountReceived }
            val cardReceived = patients.filter { it.paymentMethod == PaymentMethod.CARD.name }.sumOf { it.amountReceived }
            val creditCount = patients.count { it.paymentMethod == PaymentMethod.CREDIT.name }

            val displayDate = DateTimeFormatter.ofPattern("dd/MM/yyyy").format(today)
            val generatedAt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(LocalDateTime.now())

            val report = buildString {
                appendLine("╔══════════════════════════════════════════════════╗")
                appendLine("║     URBAN DIAGNOSTIC CENTRE  —  DAILY REPORT    ║")
                appendLine("╚══════════════════════════════════════════════════╝")
                appendLine()
                appendLine("Date      : $displayDate")
                appendLine("Agent     : ${agentName.ifBlank { "—" }}")
                appendLine("Generated : $generatedAt")
                appendLine()
                appendLine("══════════════════════════════════════════════════")
                appendLine("SUMMARY")
                appendLine("══════════════════════════════════════════════════")
                appendLine("Total Patients   : ${patients.size}")
                appendLine("Total Billed     : ${totalBilled.fmt()}")
                appendLine("Amount Received  : ${totalReceived.fmt()}")
                appendLine("Outstanding      : ${(totalBilled - totalReceived).fmt()}")
                appendLine()
                appendLine("Payment Status   : $paidCount Paid | $partialCount Partial | $unpaidCount Unpaid")
                appendLine()
                appendLine("COLLECTION BREAKDOWN")
                appendLine("  Cash   : ${cashReceived.fmt()}")
                appendLine("  UPI    : ${upiReceived.fmt()}")
                appendLine("  Card   : ${cardReceived.fmt()}")
                if (creditCount > 0) appendLine("  Credit : $creditCount patient(s) on credit")
                appendLine()
                appendLine("══════════════════════════════════════════════════")
                appendLine("PATIENT LIST")
                appendLine("══════════════════════════════════════════════════")
                if (patients.isEmpty()) {
                    appendLine("No patients registered today.")
                } else {
                    patients.forEachIndexed { idx, p ->
                        val tests = runCatching {
                            gson.fromJson<List<SelectedTest>>(p.selectedTests, testListType)
                        }.getOrDefault(emptyList())
                        val testNames = tests.joinToString(", ") { it.testName }
                        appendLine()
                        appendLine("${idx + 1}. ${p.name}  [${p.receiptNumber}]")
                        appendLine("   Age/Gender : ${p.age.ifBlank { "—" }} / ${p.gender.ifBlank { "—" }}")
                        if (p.phone.isNotBlank()) appendLine("   Phone      : ${p.phone}")
                        appendLine("   Tests      : ${testNames.ifBlank { "—" }}")
                        appendLine("   Total      : ${p.grandTotal.fmt()}  (${p.paymentStatus}  ·  ${p.paymentMethod})")
                        if (p.amountReceived > 0 && p.paymentStatus != PaymentStatus.PAID.name) {
                            appendLine("   Received   : ${p.amountReceived.fmt()}")
                        }
                    }
                }
                appendLine()
                appendLine("══════════════════════════════════════════════════")
                appendLine("               END OF DAILY REPORT")
                appendLine("══════════════════════════════════════════════════")
            }

            val dir = context.getExternalFilesDir("UDC_Reports") ?: context.filesDir
            val filename = "UDC_Report_${DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now())}.txt"
            val file = File(dir, filename)
            file.writeText(report)

            BackupResult.Success("Daily report saved: ${file.name}")
        }.getOrElse { e -> BackupResult.Error("Report generation failed: ${e.localizedMessage}") }
    }

    private fun Double.fmt(): String = "₹%.2f".format(this)
}
