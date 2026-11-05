package com.paninda.app.adapters.sheets

import android.net.Uri
import com.paninda.app.adapters.google.GoogleHttp

data class GoogleAccountInfo(val subject: String, val email: String)
data class DriveWorkbook(val id: String, val name: String)

class DriveWorkbooks(private val http: GoogleHttp) {
    suspend fun account(): GoogleAccountInfo {
        val user = http.request("https://www.googleapis.com/oauth2/v3/userinfo")
        check(user.optBoolean("email_verified")) { "Google account email is not verified" }
        return GoogleAccountInfo(user.getString("sub"), user.getString("email"))
    }
    /** Read only the exact file returned by Google's Picker, never an app-marker filtered list. */
    suspend fun selected(id: String): DriveWorkbook {
        val file = http.request("https://www.googleapis.com/drive/v3/files/${Uri.encode(id)}?fields=id,name,mimeType,trashed,capabilities(canEdit)&supportsAllDrives=true")
        if (file.optBoolean("trashed") || file.optString("mimeType") != "application/vnd.google-apps.spreadsheet" ||
            !file.getJSONObject("capabilities").optBoolean("canEdit")) throw FileSelectionException()
        return DriveWorkbook(file.getString("id"), file.getString("name"))
    }
}

class FileSelectionException : java.io.IOException("Select an editable Google Sheets file")
