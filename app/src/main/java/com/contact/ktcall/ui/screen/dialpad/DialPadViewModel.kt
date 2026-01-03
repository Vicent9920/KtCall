package com.contact.ktcall.ui.screen.dialpad

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.Utils
import com.contact.ktcall.core.contentresolver.ContentResolverFactoryImpl
import com.contact.ktcall.core.data.record.ContactRecord
import com.contact.ktcall.core.repository.BlockedRepositoryImpl
import com.contact.ktcall.core.repository.ContactRepositoryImpl
import com.contact.ktcall.core.repository.PhoneRepositoryImpl
import com.contact.ktcall.core.viewmodel.ContactsViewModelImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DialPadViewModel : ViewModel() {

    private val contactsViewModel by lazy { //
        ContactsViewModelImpl(
            phoneRepository = PhoneRepositoryImpl(ContentResolverFactoryImpl(Utils.getApp().contentResolver)),
            contactRepository = ContactRepositoryImpl(
                phones = PhoneRepositoryImpl(ContentResolverFactoryImpl(Utils.getApp().contentResolver)),
                blocked = BlockedRepositoryImpl(Utils.getApp()),
                Utils.getApp(),
                contentResolverFactory = ContentResolverFactoryImpl(Utils.getApp().contentResolver)


            )
        )
    }

    val colors = listOf(
        Color(0xFF9FA8DA), Color(0xFFEF9A9A), Color(0xFFFFCC80),
        Color(0xFFA5D6A7), Color(0xFFCE93D8)
    )


    fun queryContacts(query: String): Flow<List<ContactRecord>> = flow {
        LogUtils.e("queryContacts called with query: $query")
        if (query.isEmpty()){
            emit(emptyList())
        }else{
            // 通过联系人名称直接查询匹配的联系人记录
            val contactRecords = contactsViewModel.contactRepository.getContactsByNameFlow(query)

            contactRecords.collect { contacts ->
                LogUtils.e("Found ${contacts.size} contact records")
                contacts.forEachIndexed { index, contact ->
                    contact.avatarColor = colors[index % colors.size]
                }


                emit(contacts)
            }
        }


    }
}

data class ContactRecordEvent(val list: List<ContactRecord>)