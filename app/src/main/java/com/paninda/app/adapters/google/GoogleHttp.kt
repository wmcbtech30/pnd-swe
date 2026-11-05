package com.paninda.app.adapters.google

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class GoogleApiException(val status: Int) : IOException("Google API request failed ($status)")

/** Never logs tokens, response bodies or URLs. No implicit mutation retries. */
class GoogleHttp(private val token: String) {
    suspend fun request(url: String, method: String = "GET", body: JSONObject? = null): JSONObject = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            if (status !in 200..299) throw GoogleApiException(status)
            val text = connection.inputStream.bufferedReader().use { it.readText() }
            if (text.isBlank()) JSONObject() else JSONObject(text)
        } finally { connection.disconnect() }
    }
}
