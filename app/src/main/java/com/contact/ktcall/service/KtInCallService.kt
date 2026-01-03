package com.contact.ktcall.service

import android.telecom.Call
import android.telecom.InCallService

class KtInCallService: InCallService() {

    private val callbacks = mutableListOf<Call>()

    override fun onCallAdded(call: Call?) {
        super.onCallAdded(call)
        call?.let {
            callbacks.add(it)
        }
        call?.registerCallback(object : Call.Callback() {

        })
    }

    override fun onCallRemoved(call: Call?) {
        super.onCallRemoved(call)
        call?.let { callbacks.remove(it) }
    }
}