package com.udc.collection.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.udc.collection.data.local.dao.PatientDao
import com.udc.collection.data.local.entity.PatientEntity
import com.udc.collection.domain.model.DiscountType
import com.udc.collection.domain.model.Patient
import com.udc.collection.domain.model.PaymentMethod
import com.udc.collection.domain.model.PaymentStatus
import com.udc.collection.domain.model.SelectedTest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatientRepository @Inject constructor(
    private val dao: PatientDao
) {
    private val gson = Gson()
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun getAllPatients(): Flow<List<Patient>> =
        dao.getAllPatients().map { it.map({ it.toDomain() }) }

    fun searchPatients(query: String): Flow<List<Patient>> =
        dao.searchPatients(query).map { it.map({ it.toDomain() }) }

    suspend fun getPatientById(id: Long): Patient? =
        dao.getPatientById(id)?.toDomain()

    suspend fun savePatient(patient: Patient): Long {
        val count = dao.getPatientCount()
        val patientNumber = "PAT%06d".format(count + 1)
        val receiptNumber = generateReceiptNumber()
        val entity = patient.toEntity(patientNumber, receiptNumber)
        return dao.insertPatient(entity)
    }

    suspend fun updatePatient(patient: Patient) {
        dao.updatePatient(patient.toEntity(patient.patientNumber, patient.receiptNumber))
    }

    suspend fun deletePatient(patient: Patient) {
        dao.deletePatient(patient.toEntity(patient.patientNumber, patient.receiptNumber))
    }

    suspend fun getLatestPatient(): Patient? = dao.getLatestPatient()?.toDomain()

    fun getRecentDoctors(): Flow<List<String>> = dao.getRecentDoctors()

    fun getRecentAddresses(): Flow<List<String>> = dao.getRecentAddresses()

    suspend fun deleteAllPatients() = dao.deleteAllPatients()

    suspend fun restorePatient(patient: Patient) {
        dao.insertPatient(patient.toEntity(patient.patientNumber, patient.receiptNumber))
    }

    fun getRecentPatients(limit: Int = 5): Flow<List<Patient>> =
        dao.getRecentPatients(limit).map { it.map({ it.toDomain() }) }

    // Dashboard flows
    fun getTodayPatientCount(today: String): Flow<Int> = dao.getTodayPatientCount(today)
    fun getTodayRevenue(today: String): Flow<Double> = dao.getTodayRevenue(today)
    fun getTodayTotalBilled(today: String): Flow<Double> = dao.getTodayTotalBilled(today)
    fun getPendingPaymentCount(): Flow<Int> = dao.getPendingPaymentCount()
    fun getTotalOutstanding(): Flow<Double> = dao.getTotalOutstanding()

    private suspend fun generateReceiptNumber(): String {
        val today = LocalDate.now()
        val datePart = DateTimeFormatter.ofPattern("yyyyMMdd").format(today)
        val prefix = "REC-$datePart-"
        val count = dao.countReceiptsForDate(prefix)
        return "$prefix%03d".format(count + 1)
    }

    private fun PatientEntity.toDomain(): Patient {
        val type = object : TypeToken<List<SelectedTest>>() {}.type
        return Patient(
            id = id,
            patientNumber = patientNumber,
            name = name,
            age = age,
            gender = gender,
            phone = phone,
            address = address,
            referringDoctor = referringDoctor,
            date = runCatching { LocalDate.parse(date, dateFormatter) }.getOrDefault(LocalDate.now()),
            remarks = remarks,
            selectedTests = runCatching { gson.fromJson<List<SelectedTest>>(selectedTests, type) }.getOrDefault(emptyList()),
            discountType = runCatching { DiscountType.valueOf(discountType) }.getOrDefault(DiscountType.NONE),
            discountValue = discountValue,
            subtotal = subtotal,
            grandTotal = grandTotal,
            paymentStatus = runCatching { PaymentStatus.valueOf(paymentStatus) }.getOrDefault(PaymentStatus.UNPAID),
            paymentMethod = runCatching { PaymentMethod.valueOf(paymentMethod) }.getOrDefault(PaymentMethod.CASH),
            amountReceived = amountReceived,
            receiptNumber = receiptNumber,
            createdAt = createdAt
        )
    }

    private fun Patient.toEntity(patNum: String, receiptNum: String): PatientEntity = PatientEntity(
        id = id,
        patientNumber = patNum,
        name = name,
        age = age,
        gender = gender,
        phone = phone,
        address = address,
        referringDoctor = referringDoctor,
        date = date.format(dateFormatter),
        remarks = remarks,
        selectedTests = gson.toJson(selectedTests),
        discountType = discountType.name,
        discountValue = discountValue,
        subtotal = subtotal,
        grandTotal = grandTotal,
        paymentStatus = paymentStatus.name,
        paymentMethod = paymentMethod.name,
        amountReceived = amountReceived,
        receiptNumber = receiptNum,
        createdAt = createdAt
    )
}
