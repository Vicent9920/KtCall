package com.contact.ktcall.core.viewmodel

import androidx.core.net.toUri
import com.contact.ktcall.core.data.ContactData
import com.contact.ktcall.core.data.record.ContactRecord
import com.contact.ktcall.core.repository.ContactRepository
import com.contact.ktcall.core.repository.PhoneRepository
import kotlinx.coroutines.flow.Flow

interface ContactsViewModel : RecordsListViewModel<ContactData, ContactRecord>


class ContactsViewModelImpl constructor(
    val phoneRepository: PhoneRepository,
    val contactRepository: ContactRepository
) : RecordsListViewModelImpl<ContactData, ContactRecord>() {
    override suspend fun convertRecordToItem(record: ContactRecord) = ContactData(
        id = record.id,
        name = record.name,
        isFavorite = record.starred,
        imageUri = record.photoUri?.let { record.photoUri.toUri() }
    )

    override fun getRecordsFlow(filter: String?): Flow<List<ContactRecord>> =
        contactRepository.getContactsFlow(filter)

    override suspend fun enrichItem(item: ContactData): ContactData {
        phoneRepository.getContactAccounts(item.id).firstOrNull()
            ?.let { item.number = it.number }
        return item
    }
}