package com.contact.ktcall.core.repository

import com.contact.ktcall.core.contentresolver.ContentResolverFactory
import com.contact.ktcall.core.data.record.RecentRecord
import kotlinx.coroutines.flow.Flow

interface RecentsRepository : BaseRepository {
    fun getRecentsFlow(filter: String? = null): Flow<List<RecentRecord>>
    suspend fun getRecents(filter: String? = null): List<RecentRecord>
}

class RecentsRepositoryImpl(
    private val contentResolverFactory: ContentResolverFactory
) : BaseRepositoryImpl(), RecentsRepository {

    override fun getRecentsFlow(filter: String?): Flow<List<RecentRecord>> =
        contentResolverFactory.getRecentsContentResolver(filter = filter).getItemsFlow()

    override suspend fun getRecents(filter: String?): List<RecentRecord> =
        contentResolverFactory.getRecentsContentResolver(filter = filter).getItems()
}
