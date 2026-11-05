package com.paninda.app

import com.paninda.app.adapters.sheets.*
import com.paninda.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class SheetsMapperTest {
    @Test fun emptyExportContainsOnlyHeadersAndMetadata() {
        val tables = SheetsMapper.toTables(ShopSnapshot())
        assertTrue(tables.all { it.startCell == "A1" })
        assertTrue(tables.filter { it.name != "Meta" }.all { it.rows.isEmpty() })
        assertEquals(ShopSnapshot(), SheetsMapper.fromTables(tables))
    }

    @Test fun roundTripAcceptsReorderedHeadersAndKeepsHistoricalPrices() {
        val original = TestFixtures.snapshot()
        val tables = SheetsMapper.toTables(original).map {
            it.copy(headers = it.headers.reversed(), rows = it.rows.map(List<String>::reversed))
        }
        val restored = SheetsMapper.fromTables(tables)
        assertEquals(original.products, restored.products)
        assertEquals(original.items, restored.items)
        assertEquals(10000L, restored.transactions.single().totalMinor)
        assertEquals(12345L, restored.products.single().unitPriceMinor)
        assertEquals(5000L, restored.items.single().unitPriceMinor)
    }

    @Test fun malformedWorkbooksAreRejectedBeforeImport() {
        val tables = SheetsMapper.toTables(TestFixtures.snapshot())
        val invalid = listOf(
            tables.map { if (it.name == "Products") it.copy(headers = listOf("product_id")) else it },
            tables.map { if (it.name == "Products") it.copy(headers = it.headers + "sku") else it },
            tables.map { if (it.name == "Products") it.copy(rows = it.rows + it.rows) else it },
            change(tables, "SaleItems", "product_id", "missing"),
            change(tables, "Products", "unit_price_minor", "123.45"),
            change(tables, "Sales", "total_minor", "9999"),
            change(tables, "Meta", "value", "2"),
        )
        invalid.forEach { workbook ->
            assertThrows(WorkbookFormatException::class.java) { SheetsMapper.fromTables(workbook) }
        }
    }

    private fun change(tables: List<WorkbookTable>, name: String, column: String, value: String) =
        tables.map { table ->
            if (table.name != name) table else table.copy(rows = table.rows.mapIndexed { index, row ->
                if (index != 0) row else row.mapIndexed { col, cell -> if (col == table.headers.indexOf(column)) value else cell }
            })
        }
}
