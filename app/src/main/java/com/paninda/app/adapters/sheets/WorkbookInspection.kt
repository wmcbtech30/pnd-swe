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
    val requiresWarning get() = hasData && missingPages.isNotEmpty() && !canRenameLegacy

    /** Preserve earlier prototype records: rename a complete, valid legacy layout in place. */
    val canRenameLegacy: Boolean get() {
        if (pages.any { it.title in PanindaPages.titles }) return false
        if (!WorkbookSchema.headers.keys.all { name -> pages.any { it.title == name } }) return false
        return runCatching {
            SheetsMapper.fromTables(WorkbookSchema.headers.keys.map { name ->
                val page = pages.first { it.title == name }
                WorkbookTable(name, page.cells.firstOrNull().orEmpty(), page.cells.drop(1))
            })
        }.isSuccess
    }

    /** Stage missing/empty pages in memory first; malformed existing app data must not be rewritten. */
    fun stagedTables(): List<WorkbookTable> {
        val empty = SheetsMapper.toTables(com.paninda.app.domain.model.ShopSnapshot()).associateBy { it.name }
        return WorkbookSchema.headers.map { (name, headers) ->
            val page = pages.firstOrNull { it.title == if (canRenameLegacy) name else PanindaPages.title(name) }
            if (page == null || !page.hasData) empty.getValue(name)
            else {
                if (page.cells.firstOrNull() != headers) throw WorkbookFormatException("Restore row-1 headers in ${page.title}")
                WorkbookTable(name, headers, page.cells.drop(1))
            }
        }.also { SheetsMapper.fromTables(it) }
    }
}
