package com.paninda.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.paninda.app.data.repository.ShopRepository
import com.paninda.app.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShopViewModel(private val repository: ShopRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(ShopState())
    val state = mutableState.asStateFlow()
    private var loadingJob: Job? = null
    init { load() }

    fun load() {
        loadingJob?.cancel()
        mutableState.value = ShopState()
        loadingJob = viewModelScope.launch {
            try {
                repository.initialize()
                repository.state.collect { mutableState.value = it }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.value = ShopState(loading = false, loadFailed = true)
            }
        }
    }

    suspend fun connectLocal() = repository.connectLocal()
    suspend fun disconnect() = repository.disconnect()
    suspend fun resetLocalData() = repository.resetLocalData()
    suspend fun checkOffline() = repository.checkOffline()
    suspend fun saveProduct(id: String?, draft: ProductDraft) = repository.saveProduct(id, draft)
    suspend fun items(id: String) = repository.items(id)
    suspend fun movements(id: String) = repository.movements(id)

    class Factory(private val repository: ShopRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ShopViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return ShopViewModel(repository) as T
        }
    }
}
