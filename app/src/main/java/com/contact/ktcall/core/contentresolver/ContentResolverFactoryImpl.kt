package com.contact.ktcall.core.contentresolver

import android.content.ContentResolver

class ContentResolverFactoryImpl constructor(
    private val contentResolver: ContentResolver
) : ContentResolverFactory {
    override fun getRecentsContentResolver(
        recentId: Long?,
        filter: String?
    ) = RecentsContentResolver(
        filter = filter,
        recentId = recentId,
        contentResolver = contentResolver,
    )

    override fun getPhonesContentResolver(
        filter: String?,
        contactId: Long?,
        numberFilter: String?
    ) = PhonesContentResolver(
        filter = filter,
        contactId = contactId,
        numberFilter = numberFilter,
        contentResolver = contentResolver
    )

    override fun getContactsContentResolver(
        filter: String?,
        contactId: Long?,
        nameFilter: String?
    ) = ContactsContentResolver(
        filter = filter,
        contactId = contactId,
        nameFilter = nameFilter,
        contentResolver = contentResolver
    )

    override fun getRawContactsContentResolver(
        contactId: Long,
        filter: String?
    ) = RawContactsContentResolver(
        filter = filter,
        contactId = contactId,
        contentResolver = contentResolver
    )

    override fun getPhoneLookupContentResolver(
        number: String?,
        filter: String?
    ) = PhoneLookupContentResolver(
        filter = filter,
        number = number,
        contentResolver = contentResolver
    )
}