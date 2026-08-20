package com.udc.collection.data.repository

import com.google.gson.Gson
import com.udc.collection.data.local.dao.DraftDao
import com.udc.collection.data.local.entity.DraftPatientEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DraftRepository @Inject constructor(
    private val dao: DraftDao
) {
    private val gson = Gson()

    suspend fun saveDraft(state: Any) {
        dao.saveDraft(
            DraftPatientEntity(
                json = gson.toJson(state),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun <T> loadDraft(clazz: Class<T>): T? {
        val entity = dao.getDraft() ?: return null
        return runCatching { gson.fromJson(entity.json, clazz) }.getOrNull()
    }

    suspend fun hasDraft(): Boolean = dao.getDraft() != null

    suspend fun clearDraft() = dao.clearDraft()
}
