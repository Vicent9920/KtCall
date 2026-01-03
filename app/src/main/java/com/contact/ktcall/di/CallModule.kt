package com.contact.ktcall.di

import com.contact.ktcall.core.repository.CallRepository
import com.contact.ktcall.core.repository.CallRepositoryImpl
import com.contact.ktcall.core.viewmodel.CallViewModelImpl
import com.contact.ktcall.service.CallService

object CallModule {

    private var callService: CallService? = null
    private var callRepository: CallRepository? = null

    fun setCallService(service: CallService) {
        callService = service
    }

    fun provideCallRepository(): CallRepository {
        return callRepository ?: CallRepositoryImpl(callService).also {
            callRepository = it
        }
    }

    fun provideCallViewModel(): CallViewModelImpl {
        return CallViewModelImpl(provideCallRepository())
    }
}
