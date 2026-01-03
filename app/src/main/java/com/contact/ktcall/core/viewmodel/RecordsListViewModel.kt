package com.contact.ktcall.core.viewmodel

import androidx.lifecycle.viewModelScope
import com.contact.ktcall.utils.LoadingState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface RecordsListViewModel<ItemType, RecordType> : BaseViewModel {
    companion object {
        data class UiState<ItemType>(
            val filter: String? = null,
            val items: List<ItemType> = emptyList(),
            val loadingState: LoadingState = LoadingState.IDLE
        )
    }

    val uiState: StateFlow<UiState<ItemType>>

    suspend fun onFilterChanged(filter: String?)
    suspend fun enrichItem(item: ItemType): ItemType = item
    suspend fun convertRecordToItem(record: RecordType): ItemType
}

abstract class RecordsListViewModelImpl<ItemType, RecordType> :
    BaseViewModelImpl(),
    RecordsListViewModel<ItemType, RecordType> {
    private var _itemsCollectJob: Job? = null
    private var _itemsFlow: Flow<List<RecordType>>? = null

    private val _uiState = MutableStateFlow<RecordsListViewModel.Companion.UiState<ItemType>>(
        RecordsListViewModel.Companion.UiState()
    )

    override val uiState: StateFlow<RecordsListViewModel.Companion.UiState<ItemType>> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(loadingState = LoadingState.SUCCESS)
        }
    }

    fun updateItemsFlow() {
        _uiState.update {
            it.copy(loadingState = LoadingState.LOADING)
        }
        _uiState.update {
            _itemsCollectJob?.cancel()
            _itemsCollectJob = viewModelScope.launch {
                _itemsFlow = getRecordsFlow(it.filter)
                _itemsFlow?.collect(::onRecordsChanged)
            }
            it
        }
    }

    private fun onRecordsChanged(records: List<RecordType>) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loadingState = LoadingState.SUCCESS,
                    items = records.map { record -> convertRecordToItem(record) }
                )
            }
        }

//        viewModelScope.launch {
//            _uiState.update {
//                it.copy(items = it.items.map { enrichItem(it) })
//            }
//        }
    }

    override suspend fun onFilterChanged(filter: String?) {
        _uiState.update { it.copy(filter = filter) }
        updateItemsFlow()
    }

    override suspend fun enrichItem(item: ItemType): ItemType = item

    abstract override suspend fun convertRecordToItem(record: RecordType): ItemType

    abstract fun getRecordsFlow(filter: String?): Flow<List<RecordType>>?

}