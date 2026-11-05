package com.paninda.app.data.sync

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.paninda.app.adapters.google.GoogleApiException
import com.paninda.app.adapters.sheets.WorkbookFormatException
import com.paninda.app.domain.model.NetworkStatus
import java.io.IOException

class OfflineException : IOException("Internet is unavailable; local session is retained")

object SyncFailures {
    fun status(error: Exception, network: NetworkStatus): String = when {
        error is OfflineException || network == NetworkStatus.OFFLINE -> "offline"
        error is SyncConflictException -> "conflict"
        error is WorkbookFormatException -> "incompatible"
        error is GoogleApiException && error.status == 401 -> "access"
        error is GoogleApiException && error.status in listOf(403, 404) -> "unreachable"
        error is IOException -> "unreachable"
        error is ApiException && error.statusCode == CommonStatusCodes.NETWORK_ERROR -> "unreachable"
        else -> "failed"
    }
}
