package com.contact.ktcall.core.data.record

import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import com.contact.ktcall.utils.fullAddress
import com.contact.ktcall.utils.fullLabel

data class SimRecord(
    val index: Int,
    val phoneAccount: PhoneAccount
) {
    val phoneAccountHandle: PhoneAccountHandle get() = phoneAccount.accountHandle
    val label: String = phoneAccount.fullLabel()
    val address: String = phoneAccount.fullAddress()
}