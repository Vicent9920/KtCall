package com.contact.ktcall.ui.screen.contact

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.Utils
import com.contact.ktcall.core.contentresolver.ContentResolverFactoryImpl
import com.contact.ktcall.core.data.ContactData
import com.contact.ktcall.core.data.record.ContactRecord
import com.contact.ktcall.core.repository.BlockedRepositoryImpl
import com.contact.ktcall.core.repository.ContactRepositoryImpl
import com.contact.ktcall.core.repository.PhoneRepositoryImpl
import com.contact.ktcall.core.viewmodel.ContactsViewModelImpl
import com.contact.ktcall.core.viewmodel.RecordsListViewModel
import com.contact.ktcall.core.viewmodel.ContactsViewModel
import com.contact.ktcall.core.viewmodel.BaseViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ContactsViewModel : ViewModel(), RecordsListViewModel<ContactData, ContactRecord> {

    // 防抖机制：避免短时间内频繁查询联系人
    private var lastQueryTime = 0L
    private val minQueryInterval = 500L // 500ms 最小查询间隔
    private var currentQueryJob: Job? = null

    // 号码查询防抖机制
    private var lastNumberQueryTime = 0L
    private val minNumberQueryInterval = 200L // 200ms 最小号码查询间隔
    private var currentNumberQueryJob: Job? = null

    // 使用 lazy 初始化 core 层的 ViewModel 实现
    private val coreViewModel by lazy {
        ContactsViewModelImpl(
            phoneRepository = PhoneRepositoryImpl(ContentResolverFactoryImpl(Utils.getApp().contentResolver)),
            contactRepository = ContactRepositoryImpl(
                phones = PhoneRepositoryImpl(ContentResolverFactoryImpl(Utils.getApp().contentResolver)),
                blocked = BlockedRepositoryImpl(Utils.getApp()),
                context = Utils.getApp(),
                contentResolverFactory = ContentResolverFactoryImpl(Utils.getApp().contentResolver)
            )
        )
    }

    // 委托给 core ViewModel 实现
    override val uiState = coreViewModel.uiState
    override val errorFlow = coreViewModel.errorFlow

    init {
        // 初始化时启动数据加载
        viewModelScope.launch {
            coreViewModel.updateItemsFlow()
        }
    }

    fun queryContactNumber(item: ContactData) {
        val currentTime = System.currentTimeMillis()

        // 防抖检查：避免短时间内频繁查询号码
        if (currentTime - lastNumberQueryTime < minNumberQueryInterval) {
            LogUtils.e("ContactsViewModel", "Query contact number too frequently for contact ${item.id}, skipping")
            return
        }

        // 检查 READ_CONTACTS 权限
        if (!hasReadContactsPermission()) {
            LogUtils.e("ContactsViewModel", "READ_CONTACTS permission not granted, cannot query contact number")
            onError(BaseViewModel.VMError())
            return
        }

        // 如果已经有关联的号码，不需要重新查询
        if (!item.number.isNullOrEmpty()) {
            LogUtils.e("ContactsViewModel", "Contact ${item.id} already has number: ${item.number}")
            return
        }

        // 取消之前的号码查询任务
        currentNumberQueryJob?.cancel()

        currentNumberQueryJob = viewModelScope.launch {
            try {
                LogUtils.e("ContactsViewModel", "Starting number query for contact ${item.id} (${item.name})")
                lastNumberQueryTime = currentTime

                // 查询联系人的电话号码
                val phoneAccounts = coreViewModel.phoneRepository.getContactAccounts(item.id)

                // 选择第一个有效的电话号码
                val primaryNumber = phoneAccounts.firstOrNull()?.number

                if (primaryNumber != null) {
                    item.number = primaryNumber
                    LogUtils.e("ContactsViewModel", "Successfully found number for contact ${item.id}: $primaryNumber")
                } else {
                    LogUtils.e("ContactsViewModel", "No phone number found for contact ${item.id}")
                }

            } catch (e: Exception) {
                LogUtils.e("ContactsViewModel", "Error querying contact number for ${item.id}", e)
                onError(BaseViewModel.VMError())
            }
        }
    }

    private fun hasReadContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            Utils.getApp(),
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun onFilterChanged(filter: String?) {
        coreViewModel.onFilterChanged(filter)
    }

    override suspend fun enrichItem(item: ContactData): ContactData = item

    override suspend fun convertRecordToItem(record: ContactRecord): ContactData =
        coreViewModel.convertRecordToItem(record)

    override fun onError(error: com.contact.ktcall.core.viewmodel.BaseViewModel.VMError) {
        coreViewModel.onError(error)
    }
}