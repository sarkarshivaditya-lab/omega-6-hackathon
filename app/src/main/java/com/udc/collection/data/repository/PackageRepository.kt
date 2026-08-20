package com.udc.collection.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.udc.collection.data.local.DefaultPackages
import com.udc.collection.data.local.dao.LabPackageDao
import com.udc.collection.data.local.entity.LabPackageEntity
import com.udc.collection.domain.model.LabPackage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackageRepository @Inject constructor(
    private val dao: LabPackageDao
) {
    private val gson = Gson()

    fun getAllPackages(): Flow<List<LabPackage>> =
        dao.getAllPackages().map { it.map { e -> e.toDomain() } }

    fun searchPackages(query: String): Flow<List<LabPackage>> =
        dao.searchPackages(query).map { it.map { e -> e.toDomain() } }

    suspend fun addPackage(pkg: LabPackage): Long = dao.insertPackage(pkg.toEntity())

    suspend fun updatePackage(pkg: LabPackage) = dao.updatePackage(pkg.toEntity())

    suspend fun deletePackage(pkg: LabPackage) = dao.deletePackage(pkg.toEntity())

    suspend fun seedIfEmpty() {
        if (dao.getPackageCount() == 0) {
            dao.insertPackages(DefaultPackages.packages)
        }
    }

    suspend fun resetToDefault() {
        dao.deleteAllPackages()
        dao.insertPackages(DefaultPackages.packages)
    }

    private fun LabPackageEntity.toDomain(): LabPackage {
        val idsType = object : TypeToken<List<Long>>() {}.type
        val namesType = object : TypeToken<List<String>>() {}.type
        return LabPackage(
            id = id,
            name = name,
            description = description,
            price = price,
            includedTestIds = runCatching { gson.fromJson<List<Long>>(testIds, idsType) }.getOrDefault(emptyList()),
            includedTestNames = runCatching { gson.fromJson<List<String>>(testNames, namesType) }.getOrDefault(emptyList()),
            isActive = isActive
        )
    }

    private fun LabPackage.toEntity(): LabPackageEntity = LabPackageEntity(
        id = id,
        name = name,
        description = description,
        price = price,
        testIds = gson.toJson(includedTestIds),
        testNames = gson.toJson(includedTestNames),
        isActive = isActive
    )
}
