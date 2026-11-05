package com.paninda.app.adapters.sheets

import android.net.Uri
import com.paninda.app.adapters.google.GoogleHttp
import org.json.JSONObject

data class GoogleAccountInfo(val subject: String, val email: String)
data class DriveWorkbook(val id: String, val name: String)

class DriveWorkbooks(private val http: GoogleHttp) {
    suspend fun account(): GoogleAccountInfo {
        val user = http.request("https://www.googleapis.com/oauth2/v3/userinfo")
        check(user.optBoolean("email_verified")) { "Google account email is not verified" }
        return GoogleAccountInfo(user.getString("sub"), user.getString("email"))
    }
    /** drive.file lists only app-authorized files; arbitrary Drive browsing is deferred. */
    suspend fun list(): List<DriveWorkbook> {
        val files = mutableListOf<DriveWorkbook>()
        var page = ""
        do {
            val query = "trashed = false and mimeType = 'application/vnd.google-apps.spreadsheet' and appProperties has { key='panindaSchema' and value='1' }"
            val url = Uri.parse("https://www.googleapis.com/drive/v3/files").buildUpon()
                .appendQueryParameter("q", query).appendQueryParameter("spaces", "drive")
                .appendQueryParameter("fields", "nextPageToken,files(id,name)")
                .appendQueryParameter("pageSize", "100").apply { if (page.isNotEmpty()) appendQueryParameter("pageToken", page) }.build()
            val result = http.request(url.toString())
            val rows = result.optJSONArray("files")
            if (rows != null) for (i in 0 until rows.length()) {
                val file = rows.getJSONObject(i)
                files += DriveWorkbook(file.getString("id"), file.getString("name"))
            }
            page = result.optString("nextPageToken")
        } while (page.isNotEmpty())
        return files.sortedBy { it.name }
    }
    suspend fun mark(id: String) {
        http.request("https://www.googleapis.com/drive/v3/files/${Uri.encode(id)}", "PATCH",
            JSONObject().put("appProperties", JSONObject().put("panindaSchema", WorkbookSchema.VERSION)))
    }
}
