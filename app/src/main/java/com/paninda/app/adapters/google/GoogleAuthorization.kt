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
    suspend fun token(email: String?, resolve: suspend (PendingIntent) -> Intent): String {
        val request = AuthorizationRequest.builder().setRequestedScopes(scopes).apply {
            if (email != null) setAccount(Account(email, "com.google"))
        }.build()
        var result = client.authorize(request).await()
        if (result.hasResolution()) result = client.getAuthorizationResultFromIntent(resolve(requireNotNull(result.pendingIntent)))
        check(result.grantedScopes.containsAll(scopes.map { it.scopeUri })) { "Required Google permissions were not granted" }
        return checkNotNull(result.accessToken) { "Google did not return an access token" }
    }
    suspend fun clearExpired(token: String) = client.clearToken(ClearTokenRequest.builder().setToken(token).build()).await()
    suspend fun revoke(email: String) {
        client.revokeAccess(RevokeAccessRequest.builder().setAccount(Account(email, "com.google"))
            .setScopes(scopes).build()).await()
    }
}
