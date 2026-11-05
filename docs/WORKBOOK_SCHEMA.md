# Workbook contract, version 1

This is the local mapping contract in `adapters/sheets/`. Google Sheets transport
is unavailable in this build. No workbook is created, selected, read, or written.

Each table occupies its own worksheet. Headers start at A1 in row 1; records
start at row 2. One record occupies one row. No merged cells, title rows, or
subtotal rows belong inside these tables. Worksheet names and header names are
case-sensitive. The reader accepts reordered columns and additional named
columns, but rejects missing or duplicate headers.

| Worksheet | Headers, left to right from A1 |
| --- | --- |
| Meta | `key, value` |
| Vendors | `vendor_id, name` |
| Products | `product_id, sku, name, vendor_id, category, unit_price_minor, low_stock_threshold, is_active, updated_at, version` |
| Sales | `sale_id, receipt_number, sold_at, cashier_name, payment_method, total_minor, amount_tendered_minor, item_count` |
| SaleItems | `sale_item_id, sale_id, product_id, product_name_snapshot, vendor_id_snapshot, quantity, unit_price_minor` |
| StockMovements | `movement_id, product_id, quantity_delta, movement_type, source_id, occurred_at, reason` |

Required metadata:

```text
schema_version  1
currency        PHP
shop_timezone   Asia/Manila
```

- IDs are stable text identifiers, never row numbers. App-created IDs are UUIDs.
- SKU is a unique uppercase string of 1–64 letters, digits, periods, underscores,
  or hyphens. SKU is text even when it contains only digits.
- Money is a nonnegative integer number of centavos. `950` means Php9.50.
- Counts, quantities, and versions are whole numbers. Versions start at 1.
- Timestamps are ISO 8601 UTC strings, such as the output of `Instant.toString()`.
- `is_active` is TRUE or FALSE. Payment method is CASH or MANUAL_OTHER.
- Cash receipts require `amount_tendered_minor` at least equal to `total_minor`.
  A manually recorded non-cash payment may leave the tendered cell empty.
- Receipt item counts represent total units across the lines. Receipt totals
  must exactly match the sum of quantity multiplied by historical unit price.
- Stock quantity is derived from signed movement deltas; it is not an editable
  Products cell. Export rejects a snapshot whose product quantities disagree
  with its movement ledger.
- Foreign references must resolve to the corresponding vendor, product, or sale
  table. Historical vendor and product records must remain available for receipt
  references; product edits do not rewrite SaleItems snapshots.
- Local session, pending-change, and acknowledgement metadata are not shared
  workbook columns. Parsing a remote workbook record can acknowledge that local
  copy; it cannot establish that an upload succeeded.

This contract covers product metadata, stock movements, and completed receipts.
Void/refund/discount/tax/consignor-settlement rules are not defined yet. Extend
the schema explicitly when those workflows are designed.

Import validation is pure and rejects malformed data before any database write.
No import UI or transactional import-to-Room workflow is implemented yet.
