package com.contact.ktcall.core.contentresolver

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.contact.ktcall.core.data.record.PhoneRecord
import com.contact.ktcall.utils.SelectionBuilder

class PhonesContentResolver(
    filter: String? = null,
    contactId: Long? = null,
    contentResolver: ContentResolver
) : BaseContentResolver<PhoneRecord>(filter, contentResolver) {

    override val uri: Uri = Phone.CONTENT_URI
    override val sortOrder: String? = null
    override val filterUri: Uri = Phone.CONTENT_FILTER_URI
    override val selectionArgs: Array<String>? = null
    override val selection = SelectionBuilder().addSelection(Phone.CONTACT_ID, contactId).build()
    override val projection: Array<String> = arrayOf(
        Phone.TYPE,
        Phone.LABEL,
        Phone.NUMBER,
        Phone.CONTACT_ID,
        Phone.NORMALIZED_NUMBER,
        Phone.DISPLAY_NAME_PRIMARY
    )

    @SuppressLint("Range")
    override fun convertCursorToItem(cursor: Cursor) = PhoneRecord(
        type = cursor.getInt(cursor.getColumnIndex(Phone.TYPE)),
        label = cursor.getString(cursor.getColumnIndex(Phone.LABEL)),
        number = cursor.getString(cursor.getColumnIndex(Phone.NUMBER)),
        contactId = cursor.getLong(cursor.getColumnIndex(Phone.CONTACT_ID)),
        displayName = cursor.getString(cursor.getColumnIndex(Phone.DISPLAY_NAME_PRIMARY)),
        normalizedNumber = cursor.getString(cursor.getColumnIndex(Phone.NORMALIZED_NUMBER))
    )
}