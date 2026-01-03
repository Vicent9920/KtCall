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
    numberFilter: String? = null,
    contentResolver: ContentResolver
) : BaseContentResolver<PhoneRecord>(null, contentResolver) {

    private val selectionBuilder = SelectionBuilder()

    init {
        contactId?.let { selectionBuilder.addSelection(Phone.CONTACT_ID, it) }
        numberFilter?.let {
            // 同时搜索NUMBER和NORMALIZED_NUMBER字段
            selectionBuilder.addOrLike(arrayOf(Phone.NORMALIZED_NUMBER, Phone.NUMBER), it)
        }
    }

    override val uri: Uri = Phone.CONTENT_URI
    override val sortOrder: String? = null
    override val filterUri: Uri? = null // 禁用filterUri，使用自定义selection
    override val selectionArgs: Array<String>? = selectionBuilder.getSelectionArgs()
    override val selection = selectionBuilder.build()

    init {
        // 调试日志
        android.util.Log.d("PhonesContentResolver", "selection: $selection")
        android.util.Log.d("PhonesContentResolver", "selectionArgs: ${selectionArgs?.joinToString()}")
    }
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