package com.contact.ktcall.core.data.record

import android.provider.ContactsContract.CommonDataKinds.Phone

data class PhoneRecord(
    val number: String,
    val contactId: Long,
    val displayName: String,
    val label: String? = null,
    val normalizedNumber: String?,
    val type: Int = Phone.TYPE_OTHER
) {
    val cleanNumber get() = number.replace("-", "")
}
