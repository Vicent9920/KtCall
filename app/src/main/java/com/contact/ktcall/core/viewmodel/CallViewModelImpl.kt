package com.contact.ktcall.core.viewmodel

import androidx.lifecycle.viewModelScope
import com.contact.ktcall.core.repository.CallRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CallViewModelImpl(
    callRepository: CallRepository
) : BaseCallViewModel(callRepository) {

    init {
        // 启动时长更新协程
        viewModelScope.launch {
            while (true) {
                updateCallDuration()
                delay(1000) // 每秒更新一次
            }
        }
    }

    private fun updateCallDuration() {
        val currentState = _uiState.value
        val startTime = currentState.callStartTime
        val callsState = currentState.callsState

        // 只有在ACTIVE状态且有开始时间时才更新时长
        if (startTime != null && callsState == com.contact.ktcall.core.data.CallsState.ACTIVE) {
            val duration = System.currentTimeMillis() - startTime
            val formattedDuration = formatDuration(duration)

            // 只有时长发生变化时才更新状态，避免不必要的重组
            if (currentState.callDuration != formattedDuration) {
                _uiState.value = currentState.copy(callDuration = formattedDuration)
            }
        }
    }

}
