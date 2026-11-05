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

    suspend fun readSnapshot(): ShopSnapshot {
        val builder = Uri.parse("$url/values:batchGet").buildUpon()
            .appendQueryParameter("valueRenderOption", "UNFORMATTED_VALUE")
            .appendQueryParameter("dateTimeRenderOption", "FORMATTED_STRING")
        WorkbookSchema.headers.forEach { (name, _) -> builder.appendQueryParameter("ranges", "'$name'") }
        val ranges = http.request(builder.build().toString()).getJSONArray("valueRanges")
        val tables = WorkbookSchema.headers.keys.mapIndexed { i, name ->
            val rows = ranges.getJSONObject(i).optJSONArray("values") ?: JSONArray()
            fun row(index: Int): List<String> {
                val cells = rows.getJSONArray(index)
                return (0 until cells.length()).map { col -> cells.optString(col, "") }
            }
            WorkbookTable(name, if (rows.length() == 0) emptyList() else row(0), (1 until rows.length()).map(::row))
        }
        // This is contract validation, not the deferred arbitrary-workbook compatibility detector.
        tables.forEach { table ->
            if (table.headers != WorkbookSchema.headers.getValue(table.name))
                throw WorkbookFormatException("Restore the original row-1 headers in ${table.name}")
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
            val sheet = properties[table.name] ?: throw WorkbookFormatException("Missing worksheet: ${table.name}")
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
            val rows = JSONArray()
            (listOf(table.headers) + table.rows).forEach { row ->
                rows.put(JSONObject().put("values", JSONArray(row.map { value ->
                    // stringValue preserves IDs and integer money and cannot execute formulas.
                    JSONObject().put("userEnteredValue", JSONObject().put("stringValue", value))
                })))
            }
            // One atomic batch for canonical columns. Extra tabs and columns are left intact.
            requests.put(JSONObject().put("updateCells", JSONObject().put("range", JSONObject()
                .put("sheetId", sheetId).put("startRowIndex", 0).put("startColumnIndex", 0)
                .put("endRowIndex", maxOf(existingRows, neededRows)).put("endColumnIndex", table.headers.size))
                .put("rows", rows).put("fields", "userEnteredValue")))
        }
        http.request("$url:batchUpdate", "POST", JSONObject().put("requests", requests))
        return AdapterResult.Available(Unit)
    }

    companion object {
        suspend fun create(http: GoogleHttp, title: String): DriveWorkbook {
            val sheets = JSONArray(WorkbookSchema.headers.keys.mapIndexed { index, name ->
                JSONObject().put("properties", JSONObject().put("sheetId", index).put("title", name))
            })
            val result = http.request("https://sheets.googleapis.com/v4/spreadsheets", "POST",
                JSONObject().put("properties", JSONObject().put("title", title)).put("sheets", sheets))
            return DriveWorkbook(result.getString("spreadsheetId"), title)
        }
    }
}
