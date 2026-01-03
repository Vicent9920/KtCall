package com.contact.ktcall.core.contentresolver

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import com.contact.ktcall.core.data.record.ContactRecord
import com.contact.ktcall.utils.SelectionBuilder

class ContactsContentResolver(
    filter: String? = null,
    contactId: Long? = null,
    nameFilter: String? = null,
    contentResolver: ContentResolver
) : BaseContentResolver<ContactRecord>(null, contentResolver) {

    private val selectionBuilder = SelectionBuilder()

    init {
        selectionBuilder.addNotNull(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
        contactId?.let { selectionBuilder.addSelection(ContactsContract.Contacts._ID, it) }
        nameFilter?.let { selectionBuilder.addLike(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY, it) }
    }

    override val selectionArgs: Array<String>? = selectionBuilder.getSelectionArgs()
    override val uri: Uri = ContactsContract.Contacts.CONTENT_URI
    override val filterUri: Uri? = null // 禁用filterUri，使用自定义selection
    override val sortOrder: String = "${ContactsContract.Contacts.SORT_KEY_PRIMARY} ASC"
    override val projection: Array<String> = arrayOf(
        ContactsContract.Contacts._ID,
        ContactsContract.Contacts.STARRED,
        ContactsContract.Contacts.LOOKUP_KEY,
        ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
        ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
    )
    override val selection = selectionBuilder.build()

    init {
        // 调试日志
        android.util.Log.d("ContactsContentResolver", "selection: $selection")
        android.util.Log.d("ContactsContentResolver", "selectionArgs: ${selectionArgs?.joinToString()}")
    }

    @SuppressLint("Range")
    override fun convertCursorToItem(cursor: Cursor) = ContactRecord(
        id = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID)),
        lookupKey = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)),
        starred = "1" == cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.STARRED)),
        name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)),
        photoUri = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI))
    )
}