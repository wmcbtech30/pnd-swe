package com.paninda.app.adapters.sheets

import android.net.Uri
import com.paninda.app.adapters.*
import com.paninda.app.adapters.google.GoogleHttp
import com.paninda.app.domain.model.ShopSnapshot
import org.json.JSONArray
import org.json.JSONObject

class GoogleSheetsAdapter(private val http: GoogleHttp, private val workbookId: String) : WorkbookAdapter {
    private val url = "https://sheets.googleapis.com/v4/spreadsheets/${Uri.encode(workbookId)}"
    override suspend fun read(): AdapterResult<ShopSnapshot> = AdapterResult.Available(readSnapshot())

    suspend fun inspect(): WorkbookInspection {
        val properties = http.request("$url?fields=sheets.properties").getJSONArray("sheets")
        val pages = (0 until properties.length()).map { properties.getJSONObject(it).getJSONObject("properties") }
        val cells = mutableMapOf<String, List<List<String>>>()
        // Bound URL size when a selected file has many unrelated worksheets.
        val allPresent = PanindaPages.titles.all { title -> pages.any { it.getString("title") == title } }
        val toRead = if (allPresent) pages.filter { it.getString("title") in PanindaPages.titles } else pages
        toRead.chunked(40).forEach { chunk ->
            val builder = Uri.parse("$url/values:batchGet").buildUpon().appendQueryParameter("valueRenderOption", "FORMULA")
            chunk.forEach { builder.appendQueryParameter("ranges", quoted(it.getString("title"))) }
            val values = http.request(builder.build().toString()).getJSONArray("valueRanges")
            chunk.forEachIndexed { index, page -> cells[page.getString("title")] = readCells(values.getJSONObject(index)) }
        }
        return WorkbookInspection(pages.map { SpreadsheetPage(it.getInt("sheetId"), it.getString("title"), cells[it.getString("title")].orEmpty()) })
    }

    /** Add only missing app worksheets/headers. Never clear or repurpose existing unrelated data. */
    suspend fun initializePages(inspection: WorkbookInspection) {
        val staged = inspection.stagedTables() // Validate existing app data before any mutation.
        val requests = JSONArray()
        var nextId = (inspection.pages.maxOfOrNull { it.id } ?: 0) + 1
        for (table in staged) {
            val title = PanindaPages.title(table.name)
            val existing = inspection.pages.firstOrNull { it.title == title }
            if (existing?.hasData == true) continue
            val id = existing?.id ?: nextId++
            if (existing == null) requests.put(JSONObject().put("addSheet", JSONObject().put("properties",
                JSONObject().put("sheetId", id).put("title", title))))
            requests.put(JSONObject().put("updateCells", JSONObject().put("start", JSONObject()
                .put("sheetId", id).put("rowIndex", 0).put("columnIndex", 0))
                .put("rows", rows(listOf(table.headers) + table.rows)).put("fields", "userEnteredValue")))
        }
        if (requests.length() > 0) http.request("$url:batchUpdate", "POST", JSONObject().put("requests", requests))
    }

    suspend fun readSnapshot(): ShopSnapshot {
        val builder = Uri.parse("$url/values:batchGet").buildUpon()
            .appendQueryParameter("valueRenderOption", "UNFORMATTED_VALUE")
            .appendQueryParameter("dateTimeRenderOption", "FORMATTED_STRING")
        WorkbookSchema.headers.keys.forEach { builder.appendQueryParameter("ranges", quoted(PanindaPages.title(it))) }
        val ranges = http.request(builder.build().toString()).getJSONArray("valueRanges")
        val tables = WorkbookSchema.headers.keys.mapIndexed { i, name ->
            val cells = readCells(ranges.getJSONObject(i))
            WorkbookTable(name, cells.firstOrNull().orEmpty(), cells.drop(1))
        }
        tables.forEach { table ->
            if (!WorkbookSchema.acceptsLiveHeaders(table.name, table.headers))
                throw WorkbookFormatException("Restore row-1 headers in ${PanindaPages.title(table.name)}")
        }
        return SheetsMapper.fromTables(tables)
    }

    override suspend fun write(snapshot: ShopSnapshot): AdapterResult<Unit> {
        val tables = SheetsMapper.toTables(snapshot)
        val sheets = http.request("$url?fields=sheets.properties").getJSONArray("sheets")
        val properties = (0 until sheets.length()).map { sheets.getJSONObject(it).getJSONObject("properties") }
            .associateBy { it.getString("title") }
        val requests = JSONArray()
        for (table in tables) {
            val title = PanindaPages.title(table.name)
            val sheet = properties[title] ?: throw WorkbookFormatException("Missing worksheet: $title")
            val sheetId = sheet.getInt("sheetId")
            val neededRows = table.rows.size + 1
            val existingRows = sheet.getJSONObject("gridProperties").getInt("rowCount")
            val existingCols = sheet.getJSONObject("gridProperties").getInt("columnCount")
            if (neededRows > existingRows || table.headers.size > existingCols) {
                requests.put(JSONObject().put("updateSheetProperties", JSONObject()
                    .put("properties", JSONObject().put("sheetId", sheetId).put("gridProperties", JSONObject()
                        .put("rowCount", maxOf(neededRows, existingRows)).put("columnCount", maxOf(table.headers.size, existingCols))))
                    .put("fields", "gridProperties.rowCount,gridProperties.columnCount")))
            }
            requests.put(JSONObject().put("updateCells", JSONObject().put("range", JSONObject()
                .put("sheetId", sheetId).put("startRowIndex", 0).put("startColumnIndex", 0)
                .put("endRowIndex", maxOf(existingRows, neededRows)).put("endColumnIndex", table.headers.size))
                .put("rows", rows(listOf(table.headers) + table.rows)).put("fields", "userEnteredValue")))
        }
        http.request("$url:batchUpdate", "POST", JSONObject().put("requests", requests))
        return AdapterResult.Available(Unit)
    }

    companion object {
        private fun quoted(title: String) = "'${title.replace("'", "''")}'"
        private fun readCells(range: JSONObject): List<List<String>> {
            val cells = range.optJSONArray("values") ?: return emptyList()
            return (0 until cells.length()).map { row ->
                val values = cells.getJSONArray(row)
                (0 until values.length()).map { values.optString(it, "") }
            }
        }
        private fun rows(values: List<List<String>>) = JSONArray(values.map { row ->
            JSONObject().put("values", JSONArray(row.map { value ->
                JSONObject().put("userEnteredValue", JSONObject().put("stringValue", value))
            }))
        })
        suspend fun create(http: GoogleHttp, title: String): DriveWorkbook {
            val sheets = JSONArray(WorkbookSchema.headers.keys.mapIndexed { index, name ->
                JSONObject().put("properties", JSONObject().put("sheetId", index).put("title", PanindaPages.title(name)))
            })
            val result = http.request("https://sheets.googleapis.com/v4/spreadsheets", "POST",
                JSONObject().put("properties", JSONObject().put("title", title)).put("sheets", sheets))
            return DriveWorkbook(result.getString("spreadsheetId"), title)
        }
    }
}
