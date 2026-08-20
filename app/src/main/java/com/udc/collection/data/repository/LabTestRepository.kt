package com.udc.collection.data.repository

import com.udc.collection.data.local.DefaultTestCatalogue
import com.udc.collection.data.local.dao.LabTestDao
import com.udc.collection.data.local.entity.LabTestEntity
import com.udc.collection.domain.model.LabTest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LabTestRepository @Inject constructor(
    private val dao: LabTestDao
) {
    fun getAllTests(): Flow<List<LabTest>> =
        dao.getAllTests().map { it.map(LabTestEntity::toDomain) }

    fun searchTests(query: String): Flow<List<LabTest>> =
        dao.searchTests(query).map { it.map(LabTestEntity::toDomain) }

    fun getFrequentlyUsedTests(limit: Int = 10): Flow<List<LabTest>> =
        dao.getFrequentlyUsedTests(limit).map { it.map(LabTestEntity::toDomain) }

    suspend fun addTest(test: LabTest): Long = dao.insertTest(test.toEntity())

    suspend fun updateTest(test: LabTest) = dao.updateTest(test.toEntity())

    suspend fun deleteTest(test: LabTest) = dao.deleteTest(test.toEntity())

    suspend fun incrementUseCount(testIds: List<Long>) {
        if (testIds.isNotEmpty()) dao.incrementUseCount(testIds)
    }

    suspend fun resetToDefault() {
        dao.deleteAllTests()
        dao.insertTests(DefaultTestCatalogue.tests)
    }

    suspend fun seedIfEmpty() {
        if (dao.getTestCount() == 0) {
            dao.insertTests(DefaultTestCatalogue.tests)
        }
    }
}

private fun LabTestEntity.toDomain() = LabTest(
    id = id, name = name, price = price, category = category, isCustom = isCustom, useCount = useCount
)

private fun LabTest.toEntity() = LabTestEntity(
    id = id, name = name, price = price, category = category, isCustom = isCustom, useCount = useCount
)
