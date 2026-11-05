package com.paninda.app.adapters.google

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.*
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await

/** Google Play services owns the grant/cache; no secret or refresh token in Room. */
class GoogleAuthorization(context: Context) {
    private val client = Identity.getAuthorizationClient(context.applicationContext)
    private val scopes = listOf("https://www.googleapis.com/auth/drive.file", "openid", "https://www.googleapis.com/auth/userinfo.email").map(::Scope)
    private suspend fun authorize(request: AuthorizationRequest, resolve: suspend (PendingIntent) -> Intent): AuthorizationResult {
        var result = client.authorize(request).await()
        if (result.hasResolution()) result = client.getAuthorizationResultFromIntent(resolve(requireNotNull(result.pendingIntent)))
        return result
    }

    /** Google's Android Picker request must contain drive.file only, excluding previous identity scopes. */
    suspend fun pickFile(email: String, resolve: suspend (PendingIntent) -> Intent): String {
        val request = AuthorizationRequest.builder()
            .setAccount(Account(email, "com.google"))
            .setRequestedScopes(listOf(Scope("https://www.googleapis.com/auth/drive.file")))
            .setOptOutIncludingGrantedScopes(true)
            .setPrompt(AuthorizationRequest.Prompt.CONSENT)
            .addResourceParameter(AuthorizationRequest.ResourceParameter.PICKER_OAUTH_TRIGGER, "true")
            .addResourceParameter(AuthorizationRequest.ResourceParameter.PICKER_ALLOW_MULTIPLE, "false")
            .addResourceParameter(AuthorizationRequest.ResourceParameter.PICKER_ALLOW_FOLDER_SELECTION, "false")
            .addResourceParameter(AuthorizationRequest.ResourceParameter.PICKER_MIMETYPES, "application/vnd.google-apps.spreadsheet")
            .build()
        val result = authorize(request, resolve)
        val ids = result.tokenResponseParams?.getString("picked_file_ids")
            ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty).orEmpty()
        if (ids.size != 1) throw java.io.IOException("Select one Google Sheets file in Google Picker")
        return ids.single()
    }

    suspend fun token(email: String?, resolve: suspend (PendingIntent) -> Intent): String {
        val request = AuthorizationRequest.builder().setRequestedScopes(scopes).apply {
            if (email != null) setAccount(Account(email, "com.google"))
        }.build()
        val result = authorize(request, resolve)
        check(result.grantedScopes.containsAll(scopes.map { it.scopeUri })) { "Required Google permissions were not granted" }
        return checkNotNull(result.accessToken) { "Google did not return an access token" }
    }
    suspend fun clearExpired(token: String) = client.clearToken(ClearTokenRequest.builder().setToken(token).build()).await()
    suspend fun revoke(email: String) {
        client.revokeAccess(RevokeAccessRequest.builder().setAccount(Account(email, "com.google"))
            .setScopes(scopes).build()).await()
    }
}
