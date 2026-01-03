package com.contact.ktcall.core.viewmodel

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.contact.ktcall.core.data.CallActionType
import com.contact.ktcall.core.data.CallData
import com.contact.ktcall.core.data.CallsState
import com.contact.ktcall.core.repository.CallRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import com.contact.ktcall.core.data.AudioRoute
import kotlinx.coroutines.launch

data class CallUiState(
    val call: CallData? = null,
    val callsState: CallsState = CallsState.NO_CALL,
    val isMuted: Boolean = false,
    val isOnHold: Boolean = false,
    val audioRoute: AudioRoute = AudioRoute.EARPIECE,
    val callDuration: String = "00:00",
    val callStartTime: Long? = null
)

abstract class BaseCallViewModel(
    protected val callRepository: CallRepository
) : ViewModel() {

    protected val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState

    init {
        viewModelScope.launch {
            combine(
                callRepository.mainCall,
                callRepository.callsState,
                callRepository.audioRoute
            ) { mainCall, callsState, audioRoute ->
                val currentState = _uiState.value
                val callStartTime = when {
                    callsState == CallsState.ACTIVE && currentState.callStartTime == null -> {
                        // 通话刚变为活跃状态，开始计时
                        System.currentTimeMillis()
                    }
                    callsState != CallsState.ACTIVE -> {
                        // 通话结束，重置计时
                        null
                    }
                    else -> {
                        // 保持现有的开始时间
                        currentState.callStartTime
                    }
                }

                // 计算通话时长
                val callDuration = if (callStartTime != null) {
                    val duration = System.currentTimeMillis() - callStartTime
                    formatDuration(duration)
                } else {
                    "00:00"
                }

                CallUiState(
                    call = mainCall,
                    callsState = callsState,
                    isMuted = mainCall?.isMuted ?: false,
                    isOnHold = mainCall?.isOnHold ?: false,
                    audioRoute = audioRoute,
                    callStartTime = callStartTime,
                    callDuration = callDuration
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun onCallActionClick(action: CallActionType) {
        when (action) {
            CallActionType.ANSWER -> callRepository.answerMainCall()
            CallActionType.DENY, CallActionType.HANGUP -> {
                uiState.value.call?.let { callRepository.hangup(it) }
            }
            CallActionType.TOGGLE_MUTE -> callRepository.toggleMute()
            CallActionType.TOGGLE_SPEAKER -> callRepository.toggleSpeaker()
            CallActionType.TOGGLE_HOLD -> callRepository.toggleHold()
        }
    }

    @SuppressLint("DefaultLocale")
    protected fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = durationMs / (1000 * 60 * 60)

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
