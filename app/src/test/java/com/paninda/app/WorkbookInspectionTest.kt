package com.paninda.app

import com.paninda.app.adapters.sheets.*
import com.paninda.app.domain.model.ShopSnapshot
import org.junit.Assert.*
import org.junit.Test

/** Only the warning/initialization decision rules; no layout or network tests. */
class WorkbookInspectionTest {
    private fun appPages(snapshot: ShopSnapshot = ShopSnapshot()) = SheetsMapper.toTables(snapshot).mapIndexed { index, table ->
        SpreadsheetPage(index, PanindaPages.title(table.name), listOf(table.headers) + table.rows)
    }

    @Test fun warningRequiresBothExistingDataAndMissingAppWorksheets() {
        val empty = WorkbookInspection(listOf(SpreadsheetPage(10, "Sheet1", emptyList())))
        assertFalse(empty.requiresWarning)
        assertEquals(WorkbookSchema.headers.size, empty.missingPages.size)
        val unrelated = SpreadsheetPage(10, "My existing records", listOf(listOf("name", "price"), listOf("Keep me", "9.50")))
        assertTrue(WorkbookInspection(listOf(unrelated)).requiresWarning)
        assertFalse(WorkbookInspection(appPages() + unrelated).requiresWarning)
        assertTrue(WorkbookInspection(appPages().dropLast(1) + unrelated).requiresWarning)
        assertTrue(WorkbookInspection(listOf(unrelated.copy(cells = listOf(listOf("=IF(TRUE,\"\",\"\")"))))).requiresWarning)
    }

    @Test fun initializationStagesOnlyAppTablesAndNeverAdoptsUnrelatedCells() {
        val foreign = SpreadsheetPage(20, "Products", listOf(listOf("important existing data")))
        val original = WorkbookInspection(appPages().filter { it.title != "Products (Paninda)" } + foreign)
        assertTrue(original.requiresWarning)
        assertEquals(ShopSnapshot(), SheetsMapper.fromTables(original.stagedTables()))
        assertEquals("important existing data", original.pages.last().cells.single().single())
        val collision = WorkbookInspection(listOf(foreign.copy(title = "Products (Paninda)")))
        assertThrows(WorkbookFormatException::class.java) { collision.stagedTables() }
    }

}
