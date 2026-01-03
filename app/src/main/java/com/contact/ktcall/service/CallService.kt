package com.contact.ktcall.service

import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import com.contact.ktcall.core.data.CallData

abstract class CallService : InCallService() {

    private val calls = mutableListOf<Call>()
    private var onCallsChangedListener: ((List<Call>) -> Unit)? = null
    private var onAudioStateChangedListener: ((CallAudioState) -> Unit)? = null

    fun setOnCallsChangedListener(listener: (List<Call>) -> Unit) {
        onCallsChangedListener = listener
    }

    fun setOnAudioStateChangedListener(listener: (CallAudioState) -> Unit) {
        onAudioStateChangedListener = listener
    }

    fun getCurrentCalls(): List<Call> {
        return calls.toList()
    }

    fun getCurrentAudioState(): android.telecom.CallAudioState? {
        return callAudioState
    }

    override fun onCallAdded(call: Call?) {
        super.onCallAdded(call)
        call?.let {
            // 检查是否已有活跃通话
            val hasActiveCall = calls.any { existingCall ->
                existingCall.state == Call.STATE_ACTIVE ||
                existingCall.state == Call.STATE_RINGING ||
                existingCall.state == Call.STATE_DIALING ||
                existingCall.state == Call.STATE_CONNECTING
            }

            if (hasActiveCall) {
                // 如果已有活跃通话，新来电自动挂断
                if (it.state == Call.STATE_RINGING) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        it.reject(Call.REJECT_REASON_DECLINED)
                    } else {
                        it.disconnect()
                    }
                }
                return@let
            }

            calls.add(it)
            registerCallCallback(it)
            notifyCallsChanged()

            // 启动通话界面
            startInCallActivity()
        }
    }

    override fun onCallRemoved(call: Call?) {
        super.onCallRemoved(call)
        call?.let {
            calls.remove(it)
            notifyCallsChanged()
        }
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        audioState?.let { onAudioStateChangedListener?.invoke(it) }
    }

    private fun startInCallActivity() {
        // 启动通话界面Activity
        val context = this
        val intent = android.content.Intent(context, com.contact.ktcall.ui.InCallActivity::class.java).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        context.startActivity(intent)
    }

    private fun registerCallCallback(call: Call) {
        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(call: Call?, state: Int) {
                notifyCallsChanged()
            }

            override fun onDetailsChanged(call: Call?, details: Call.Details?) {
                notifyCallsChanged()
            }

            override fun onCallDestroyed(call: Call?) {
                call?.let { calls.remove(it) }
                notifyCallsChanged()
            }
        })
    }

    private fun notifyCallsChanged() {
        onCallsChangedListener?.invoke(calls.toList())
    }

    // Public API methods
    fun answerMainCall() {
        calls.firstOrNull { it.state == Call.STATE_RINGING }?.answer(0)
    }

    fun hangupCall(callData: CallData) {
        calls.find { CallData.fromTelecomCall(it).id == callData.id }?.let { call ->
            when {
                call.state == Call.STATE_RINGING -> {
                    // For Android 10+ and rejecting incoming calls
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        call.reject(Call.REJECT_REASON_DECLINED)
                    } else {
                        call.disconnect()
                    }
                }
                else -> call.disconnect()
            }
        }
    }

    fun toggleMute() {
        setMuted(!callAudioState?.isMuted!!)
    }

    fun toggleHold() {
        calls.firstOrNull { it.state == Call.STATE_ACTIVE }?.hold()
            ?: calls.firstOrNull { it.state == Call.STATE_HOLDING }?.unhold()
    }
}