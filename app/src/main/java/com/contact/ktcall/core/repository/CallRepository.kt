package com.contact.ktcall.core.repository

import com.contact.ktcall.core.data.AudioRoute
import com.contact.ktcall.core.data.CallData
import com.contact.ktcall.core.data.CallsState
import kotlinx.coroutines.flow.StateFlow

interface CallRepository {
    val calls: StateFlow<List<CallData>>
    val mainCall: StateFlow<CallData?>
    val callsState: StateFlow<CallsState>
    val audioRoute: StateFlow<AudioRoute>

    fun answerMainCall()
    fun hangup(call: CallData)
    fun toggleMute()
    fun toggleSpeaker()
    fun toggleHold()
    fun setAudioRoute(route: AudioRoute)
}
