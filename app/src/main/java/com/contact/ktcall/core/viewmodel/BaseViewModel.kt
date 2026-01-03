package com.contact.ktcall.core.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

interface BaseViewModel {
    val errorFlow: Flow<VMError>

    fun onError(error: VMError)

    open class VMError(@StringRes val strRes: Int? = null)
}

open class BaseViewModelImpl : ViewModel(), BaseViewModel {
    private val _errorChannel = Channel<BaseViewModel.VMError>(Channel.BUFFERED)

    override val errorFlow = _errorChannel.receiveAsFlow()

    override fun onError(error: BaseViewModel.VMError) {
        viewModelScope.launch {
            _errorChannel.send(error)
        }
    }
}