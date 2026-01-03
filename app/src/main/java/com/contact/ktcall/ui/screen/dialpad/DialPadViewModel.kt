package com.contact.ktcall.ui.screen.dialpad

import androidx.lifecycle.ViewModel
import com.blankj.utilcode.util.Utils
import com.contact.ktcall.core.contentresolver.ContentResolverFactoryImpl
import com.contact.ktcall.core.repository.BlockedRepositoryImpl
import com.contact.ktcall.core.repository.ContactRepositoryImpl
import com.contact.ktcall.core.repository.PhoneRepositoryImpl
import com.contact.ktcall.core.viewmodel.ContactsViewModelImpl

class DialPadViewModel: ViewModel() {

    private val contactsViewModel by lazy { //
        ContactsViewModelImpl(phoneRepository = PhoneRepositoryImpl(ContentResolverFactoryImpl(Utils.getApp().contentResolver)),
            contactRepository = ContactRepositoryImpl(phones = PhoneRepositoryImpl(ContentResolverFactoryImpl(Utils.getApp().contentResolver)),
                blocked = BlockedRepositoryImpl(Utils.getApp()),
                Utils.getApp(),
                contentResolverFactory = ContentResolverFactoryImpl(Utils.getApp().contentResolver)


            )
        )
    }

    fun getContacts(filter: String? = null) = contactsViewModel.getRecordsFlow( filter)
}