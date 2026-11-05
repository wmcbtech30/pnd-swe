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
    error is FileSelectionException -> R.string.connection_selection_body
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
    var selected by remember { mutableStateOf<SelectedSpreadsheet?>(null) }
    var failure by remember { mutableIntStateOf(R.string.connection_failed_body) }
    val scope = rememberCoroutineScope()
    fun startOver() { stage = "start"; selected = null }
    fun attach(approved: Boolean) {
        if (vm.cloudBusy) return
        stage = "loading"
        scope.launch {
            try { vm.connect(requireNotNull(selected), approved, resolve) }
            catch (e: CancellationException) { throw e }
            catch (e: WorkbookApprovalRequired) {
                selected = selected?.copy(inspection = e.inspection)
                stage = "warning"
            }
            catch (e: Exception) { failure = connectionError(e); stage = "failed" }
        }
    }
    fun chooseExisting() {
        if (vm.cloudBusy) return
        selected = null
        stage = "loading"
        scope.launch {
            try {
                val result = vm.selectFile(resolve)
                selected = result
                if (result.inspection.requiresWarning) stage = "warning"
                else attach(false)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { failure = connectionError(e); stage = "failed" }
        }
    }
    when (stage) {
        "loading" -> {
            BackHandler { }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.connection_loading))
                }
            }
        }
        "source" -> FileSourceScreen(::startOver, ::chooseExisting, { stage = "new" })
        "new" -> NewFileScreen(vm.cloudBusy, { stage = "source" }) { name ->
            if (!vm.cloudBusy) scope.launch {
                try { vm.createFile(name, resolve) }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { failure = connectionError(e); stage = "failed" }
            }
        }
        "warning" -> ConnectionNoticeScreen(false, vm.cloudBusy, onStartOver = ::startOver,
            onProceed = { attach(true) }, fileName = selected?.file?.name,
            missingPages = selected?.inspection?.missingPages.orEmpty())
        "failed" -> ConnectionNoticeScreen(true, onStartOver = ::startOver,
            onProceed = { selected = null; stage = "source" }, bodyOverride = failure)
        else -> OnboardingScreen(onContinue = { stage = "source" })
    }
}
