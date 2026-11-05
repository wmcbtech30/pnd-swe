package com.paninda.app.ui.connection

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.common.api.ApiException
import com.paninda.app.R
import com.paninda.app.adapters.google.GoogleApiException
import com.paninda.app.adapters.sheets.*
import com.paninda.app.data.sync.*
import com.paninda.app.ui.ShopViewModel
import com.paninda.app.ui.onboarding.OnboardingScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

fun connectionError(error: Exception): Int = when {
    error is SyncVerificationException -> R.string.connection_verification_body
    error is SyncConflictException -> R.string.connection_conflict_body
    error is WorkbookFormatException -> R.string.connection_format_body
    error is ApiException && error.statusCode == 10 -> R.string.connection_registration_body
    error is GoogleApiException && error.status in listOf(401, 403) -> R.string.connection_access_body
    error is GoogleApiException && error.status == 404 -> R.string.connection_missing_body
    error is GoogleApiException && error.status == 429 -> R.string.connection_quota_body
    else -> R.string.connection_failed_body
}

@Composable
fun ConnectionFlow(vm: ShopViewModel, resolve: ResolveGoogleConsent) {
    var stage by remember { mutableStateOf("start") }
    var account by remember { mutableStateOf<GoogleAccountInfo?>(null) }
    var workbooks by remember { mutableStateOf(emptyList<DriveWorkbook>()) }
    var selected by remember { mutableStateOf<DriveWorkbook?>(null) }
    var failure by remember { mutableIntStateOf(R.string.connection_failed_body) }
    val scope = rememberCoroutineScope()
    val title = stringResource(R.string.workbooks_default_name)
    fun startOver() { stage = "start"; account = null; workbooks = emptyList(); selected = null }
    fun begin() {
        if (vm.cloudBusy) return
        startOver()
        stage = "loading"
        scope.launch {
            try {
                val result = vm.discover(resolve)
                account = result.first; workbooks = result.second; stage = "choose"
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { failure = connectionError(e); stage = "failed" }
        }
    }
    when (stage) {
        "loading" -> {
            BackHandler { /* Finish or cancel the Google consent dialog before restarting. */ }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.connection_loading))
                }
            }
        }
        "choose" -> WorkbookSelectionScreen(requireNotNull(account), workbooks, vm.cloudBusy, ::startOver,
            { selected = null; stage = "warning" }, { selected = it; stage = "warning" })
        "warning" -> ConnectionNoticeScreen(false, selected != null, vm.cloudBusy, onStartOver = ::startOver, onProceed = {
            if (!vm.cloudBusy) scope.launch {
                try { vm.connect(requireNotNull(account), selected, title, resolve) }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { failure = connectionError(e); stage = "failed" }
            }
        })
        "failed" -> ConnectionNoticeScreen(true, onStartOver = ::startOver, onProceed = ::begin, bodyOverride = failure)
        else -> OnboardingScreen(onContinue = ::begin)
    }
}
