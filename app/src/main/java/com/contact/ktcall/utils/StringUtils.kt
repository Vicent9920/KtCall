package com.contact.ktcall.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import com.blankj.utilcode.util.ActivityUtils
import androidx.core.net.toUri


fun String.getSortKey(): String {
    return if (this == "#") "zzzzz$this" else this
}

@SuppressLint("MissingPermission")
fun String.callPhone(){
    ActivityUtils.getTopActivity()?.let { activity ->
        try {
            val uri = Uri.fromParts("tel", this, null)
            val extras = Bundle()
            val telecomManager =
                activity.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            telecomManager.placeCall(uri, extras)
        } catch (e: Exception) {
            val intent = Intent(Intent.ACTION_CALL)
            val data = ("tel:$this").toUri()
            intent.setData(data)
            activity.startActivity(intent)
        }
    }

}