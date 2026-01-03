package com.contact.ktcall.core.repository

import android.telecom.Call
import com.contact.ktcall.core.data.*
import com.contact.ktcall.service.CallService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class CallRepositoryImpl(
    private val callService: CallService?
) : CallRepository {

    private val _calls = MutableStateFlow<List<CallData>>(emptyList())
    override val calls: StateFlow<List<CallData>> = _calls

    private val _mainCall = MutableStateFlow<CallData?>(null)
    override val mainCall: StateFlow<CallData?> = _mainCall

    private val _callsState = MutableStateFlow<CallsState>(CallsState.NO_CALL)
    override val callsState: StateFlow<CallsState> = _callsState

    private val _audioRoute = MutableStateFlow(AudioRoute.EARPIECE)
    override val audioRoute: StateFlow<AudioRoute> = _audioRoute

    init {
        // 先获取当前的通话状态
        val currentCalls = callService?.getCurrentCalls() ?: emptyList()
        if (currentCalls.isNotEmpty()) {
            val callDataList = currentCalls.map { CallData.fromTelecomCall(it) }
            _calls.value = callDataList
            _mainCall.value = filterPrimaryCall(callDataList)
            _callsState.value = getCallsState(callDataList, _mainCall.value)
        }

        // 获取当前的音频状态
        val currentAudioState = callService?.getCurrentAudioState()
        if (currentAudioState != null) {
            _audioRoute.value = AudioRoute.values().find { it.route == currentAudioState.route } ?: AudioRoute.EARPIECE
        }

        callService?.setOnCallsChangedListener { telecomCalls ->
            val callDataList = telecomCalls.map { CallData.fromTelecomCall(it) }
            _calls.value = callDataList
            // 立即更新mainCall和callsState
            val mainCall = filterPrimaryCall(callDataList)
            val callsState = getCallsState(callDataList, mainCall)
            _mainCall.value = mainCall
            _callsState.value = callsState
        }

        callService?.setOnAudioStateChangedListener { audioState ->
            _audioRoute.value = AudioRoute.values().find { it.route == audioState.route } ?: AudioRoute.EARPIECE
        }

        // Listen to calls changes and update mainCall and callsState
        _calls.onEach { calls ->
            val mainCall = filterPrimaryCall(calls)
            val callsState = getCallsState(calls, mainCall)
            _mainCall.value = mainCall
            _callsState.value = callsState
        }.launchIn(CoroutineScope(Dispatchers.Default))
    }

    override fun answerMainCall() {
        callService?.answerMainCall()
    }

    override fun hangup(call: CallData) {
        callService?.hangupCall(call)
    }

    override fun toggleMute() {
        callService?.toggleMute()
    }

    override fun toggleSpeaker() {
        val newRoute = if (audioRoute.value == AudioRoute.SPEAKER) {
            AudioRoute.EARPIECE
        } else {
            AudioRoute.SPEAKER
        }
        setAudioRoute(newRoute)
    }

    override fun toggleHold() {
        callService?.toggleHold()
    }

    override fun setAudioRoute(route: AudioRoute) {
        callService?.setAudioRoute(route.route)
    }

    private fun filterPrimaryCall(calls: List<CallData>): CallData? {
        return calls.firstOrNull { it.state == CallState.RINGING }
            ?: calls.firstOrNull { it.state == CallState.ACTIVE }
            ?: calls.firstOrNull()
    }

    private fun getCallsState(calls: List<CallData>, mainCall: CallData?): CallsState {
        if (calls.isEmpty()) return CallsState.NO_CALL

        // 如果有mainCall，根据其状态返回相应的CallsState
        mainCall?.let {
            return when (it.state) {
                CallState.RINGING -> CallsState.INCOMING
                CallState.DIALING, CallState.CONNECTING -> CallsState.OUTGOING
                CallState.ACTIVE -> CallsState.ACTIVE
                CallState.HOLDING -> CallsState.HOLDING
                else -> CallsState.NO_CALL
            }
        }

        // 如果没有mainCall但有calls，检查calls中的状态
        val firstCall = calls.firstOrNull()
        return when (firstCall?.state) {
            CallState.RINGING -> CallsState.INCOMING
            CallState.DIALING, CallState.CONNECTING -> CallsState.OUTGOING
            CallState.ACTIVE -> CallsState.ACTIVE
            CallState.HOLDING -> CallsState.HOLDING
            else -> CallsState.NO_CALL
        }
    }
}