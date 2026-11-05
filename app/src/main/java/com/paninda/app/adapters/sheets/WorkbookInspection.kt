package com.paninda.app.adapters.sheets

/** Physical pages are named separately from the provider-neutral table keys. */
object PanindaPages {
    fun title(table: String) = "$table (Paninda)"
    val titles get() = WorkbookSchema.headers.keys.map(::title)
}

data class SpreadsheetPage(val id: Int, val title: String, val cells: List<List<String>>) {
    // FORMULA rendering counts formulas even when their evaluated result is empty.
    val hasData get() = cells.any { row -> row.any { it.isNotEmpty() } }
}

data class WorkbookInspection(val pages: List<SpreadsheetPage>) {
    val missingPages get() = PanindaPages.titles.filter { title -> pages.none { it.title == title } }
    val hasData get() = pages.any { it.hasData }
    val requiresWarning get() = hasData && missingPages.isNotEmpty()

    /** Stage missing/empty pages in memory first; malformed existing app data must not be rewritten. */
    fun stagedTables(): List<WorkbookTable> {
        val empty = SheetsMapper.toTables(com.paninda.app.domain.model.ShopSnapshot()).associateBy { it.name }
        return WorkbookSchema.headers.map { (name, headers) ->
            val page = pages.firstOrNull { it.title == PanindaPages.title(name) }
            if (page == null || !page.hasData) empty.getValue(name)
            else {
                if (!WorkbookSchema.acceptsLiveHeaders(name, page.cells.firstOrNull().orEmpty())) throw WorkbookFormatException("Restore row-1 headers in ${page.title}")
                WorkbookTable(name, page.cells.first(), page.cells.drop(1))
            }
        }.also { SheetsMapper.fromTables(it) }
    }
}
