package com.paninda.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.paninda.app.data.repository.ShopRepository
import com.paninda.app.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.paninda.app.adapters.sheets.*
import com.paninda.app.data.sync.*

class ShopViewModel(private val repository: ShopRepository, private val google: () -> GoogleWorkbookSync,
    private val network: Flow<NetworkStatus> = flowOf(NetworkStatus.UNKNOWN)) : ViewModel() {
    var cloudBusy by mutableStateOf(false)
        private set
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
                // Connectivity observation must never make the local database/session unavailable.
                combine(repository.state, network.catch { emit(NetworkStatus.UNKNOWN) }) { local, internet -> local.copy(networkStatus = internet) }
                    .collect { mutableState.value = it }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.value = ShopState(loading = false, loadFailed = true)
            }
        }
    }

    private suspend fun <T> cloud(block: suspend () -> T): T {
        check(!cloudBusy) { "A Google operation is already running" }
        cloudBusy = true
        try { return block() } finally { cloudBusy = false }
    }
    suspend fun selectFile(resolve: ResolveGoogleConsent) = cloud { google().selectFile(resolve) }
    suspend fun connect(selected: SelectedSpreadsheet, allowMissingPages: Boolean, resolve: ResolveGoogleConsent) =
        cloud { google().connect(selected, allowMissingPages, resolve) }
    suspend fun createFile(title: String, resolve: ResolveGoogleConsent) = cloud { google().createFile(title, resolve) }
    suspend fun sync(resolve: ResolveGoogleConsent) = cloud {
        try {
            if (state.value.networkStatus == NetworkStatus.OFFLINE) throw OfflineException()
            google().sync(resolve)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            repository.syncStatus(SyncFailures.status(e, state.value.networkStatus))
            throw e
        }
    }
    suspend fun disconnect() = cloud { google().disconnect() }
    suspend fun resetLocalData() { check(!cloudBusy); repository.resetLocalData() }
    suspend fun saveProduct(id: String?, draft: ProductDraft) = repository.saveProduct(id, draft)
    suspend fun saveVendor(id: String?, draft: VendorDraft) = repository.saveVendor(id, draft)
    suspend fun setVendorActive(id: String, active: Boolean) = repository.setVendorActive(id, active)
    suspend fun recordSale(draft: SaleDraft) = repository.recordSale(draft)
    suspend fun recordCount(draft: CountDraft) = repository.recordCount(draft)
    suspend fun countItems(id: String) = repository.countItems(id)
    suspend fun items(id: String) = repository.items(id)
    suspend fun movements(id: String) = repository.movements(id)

    class Factory(private val repository: ShopRepository, private val google: () -> GoogleWorkbookSync,
        private val network: Flow<NetworkStatus> = flowOf(NetworkStatus.UNKNOWN)) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ShopViewModel::class.java))
            @Suppress("UNCHECKED_CAST")
            return ShopViewModel(repository, google, network) as T
        }
    }
}
