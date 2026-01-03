package com.contact.ktcall.ui.screen.calllog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.Utils
import com.contact.ktcall.core.contentresolver.ContentResolverFactoryImpl
import com.contact.ktcall.core.data.RecentData
import com.contact.ktcall.core.data.record.RecentRecord
import com.contact.ktcall.core.repository.RecentsRepository
import com.contact.ktcall.core.repository.RecentsRepositoryImpl
import com.contact.ktcall.core.viewmodel.BaseViewModelImpl
import com.contact.ktcall.core.viewmodel.RecordsListViewModel
import com.contact.ktcall.utils.LoadingState
import com.contact.ktcall.utils.getElapsedTimeString
import com.contact.ktcall.utils.getRelativeDateString
import com.contact.ktcall.utils.getTimeAgo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CallLogViewModel : BaseViewModelImpl(), RecordsListViewModel<CallLogEntry, RecentRecord> {

    private val recentsRepository: RecentsRepository by lazy {
        RecentsRepositoryImpl(ContentResolverFactoryImpl(Utils.getApp().contentResolver))
    }

    private val _uiState = MutableStateFlow(
        RecordsListViewModel.Companion.UiState<CallLogEntry>(loadingState = LoadingState.SUCCESS)
    )
    override val uiState: StateFlow<RecordsListViewModel.Companion.UiState<CallLogEntry>> = _uiState.asStateFlow()

    private var recordsCollectJob: Job? = null

    init {
        // 初始化时自动查询通话记录
        queryCallLogs()
    }

    fun queryCallLogs() {
        // 查询当前用户的通话记录，当通话记录变更时自动更新
        updateItemsFlow()
    }

    private fun updateItemsFlow(filter: String? = null) {
        _uiState.update { it.copy(loadingState = LoadingState.LOADING) }

        recordsCollectJob?.cancel()
        recordsCollectJob = viewModelScope.launch {
            recentsRepository.getRecentsFlow(filter).collect { records ->
                onRecordsChanged(records)
            }
        }
    }

    private suspend fun onRecordsChanged(records: List<RecentRecord>) {
        // 将所有记录转换为RecentData
        val recentDataList = records.map { RecentData.fromRecord(it) }

        // 使用RecentData.group进行分组
        val groupedData = RecentData.group(recentDataList)

        // 将分组后的数据转换为CallLogEntry，确保每个entry有唯一key
        val callLogEntries = groupedData.mapIndexed { index, grouped ->
            val count = grouped.groupAccounts.size
            val type = when (grouped.type) {
                RecentRecord.TYPE_INCOMING -> CallType.INCOMING
                RecentRecord.TYPE_OUTGOING -> CallType.OUTGOING
                RecentRecord.TYPE_MISSED -> CallType.MISSED
                else -> CallType.MISSED // 默认当作未接来电
            }

            // 使用复合key确保唯一性：使用index和原始id的组合
            val uniqueId = "${grouped.id}_${grouped.date.time}_${index}".hashCode()
            CallLogEntry(
                id = uniqueId,
                name = grouped.name,
                nameOrNumber = grouped.name ?: grouped.number,
                count = count,
                type = type,
                time = getTimeAgo(grouped.date.time),
                dateLabel = getRelativeDateString(grouped.date),
                duration =  getElapsedTimeString(grouped.duration)
            )
        }

        _uiState.update {
            it.copy(
                loadingState = LoadingState.SUCCESS,
                items = callLogEntries
            )
        }
    }

    override suspend fun onFilterChanged(filter: String?) {
        _uiState.update { it.copy(filter = filter) }
        updateItemsFlow(filter)
    }

    override suspend fun convertRecordToItem(record: RecentRecord): CallLogEntry {
        // 这个方法现在不会被使用，因为我们重写了数据处理逻辑
        throw NotImplementedError("This method is not used in custom implementation")
    }

    fun getRecordsFlow(filter: String?): Flow<List<RecentRecord>> =
        recentsRepository.getRecentsFlow(filter)
}