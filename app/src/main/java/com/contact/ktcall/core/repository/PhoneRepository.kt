package com.contact.ktcall.core.repository

import com.contact.ktcall.core.contentresolver.ContentResolverFactory
import com.contact.ktcall.core.data.record.PhoneLookupRecord
import com.contact.ktcall.core.data.record.PhoneRecord
import kotlinx.coroutines.flow.Flow
import java.lang.RuntimeException

interface PhoneRepository : BaseRepository {
    suspend fun getPhones(contactId: Long? = null, filter: String? = null): List<PhoneRecord>
    fun getPhonesFlow(contactId: Long? = null, filter: String? = null): Flow<List<PhoneRecord>>

    suspend fun getPhonesByNumber(numberFilter: String): List<PhoneRecord>
    fun getPhonesByNumberFlow(numberFilter: String): Flow<List<PhoneRecord>>

    suspend fun lookupAccount(number: String?): PhoneLookupRecord?
    suspend fun getContactAccounts(contactId: Long): List<PhoneRecord>
    fun getContactAccountsFlow(contactId: Long): Flow<List<PhoneRecord>>
}

class PhoneRepositoryImpl  constructor(
    private val contentResolverFactory: ContentResolverFactory,
) : BaseRepositoryImpl(), PhoneRepository {
    override suspend fun getPhones(contactId: Long?, filter: String?): List<PhoneRecord> =
        contentResolverFactory.getPhonesContentResolver(
            contactId = if (contactId == 0L) null else contactId,
            filter = filter
        ).getItems()

    override fun getPhonesFlow(contactId: Long?, filter: String?): Flow<List<PhoneRecord>> =
        contentResolverFactory.getPhonesContentResolver(
            contactId = if (contactId == 0L) null else contactId,
            filter = filter
        ).getItemsFlow()

    override suspend fun lookupAccount(number: String?): PhoneLookupRecord? =
        if (number.isNullOrEmpty()) {
            PhoneLookupRecord.PRIVATE
        } else try {
            contentResolverFactory.getPhoneLookupContentResolver(number).getItems().getOrNull(0)
        } catch (e: RuntimeException) {
            null
        }

    override suspend fun getPhonesByNumber(numberFilter: String): List<PhoneRecord> =
        contentResolverFactory.getPhonesContentResolver(numberFilter = numberFilter).getItems()

    override fun getPhonesByNumberFlow(numberFilter: String): Flow<List<PhoneRecord>> =
        contentResolverFactory.getPhonesContentResolver(numberFilter = numberFilter).getItemsFlow()

    override suspend fun getContactAccounts(contactId: Long): List<PhoneRecord> =
        contentResolverFactory.getPhonesContentResolver(contactId = contactId).getItems()

    override fun getContactAccountsFlow(contactId: Long): Flow<List<PhoneRecord>> =
        contentResolverFactory.getPhonesContentResolver(contactId = contactId).getItemsFlow()
}