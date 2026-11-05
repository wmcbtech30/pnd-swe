package com.paninda.app.ui.connection

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.paninda.app.data.sync.ResolveGoogleConsent
import kotlinx.coroutines.CompletableDeferred
import java.io.IOException

@Composable
fun rememberGoogleConsent(): ResolveGoogleConsent {
    val pending = remember { arrayOfNulls<CompletableDeferred<Intent>>(1) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val waiting = pending[0]
        pending[0] = null
        if (result.resultCode == Activity.RESULT_OK && result.data != null) waiting?.complete(result.data!!)
        else waiting?.completeExceptionally(IOException("Google authorization was cancelled"))
    }
    DisposableEffect(Unit) { onDispose { pending[0]?.cancel(); pending[0] = null } }
    return remember(launcher) {
        { intent ->
            check(pending[0] == null)
            val waiting = CompletableDeferred<Intent>()
            pending[0] = waiting
            try {
                launcher.launch(IntentSenderRequest.Builder(intent.intentSender).build())
                waiting.await()
            } finally { if (pending[0] === waiting) pending[0] = null }
        }
    }
}
