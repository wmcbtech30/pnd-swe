# Workbook contract, version 1

This is the mapping contract in `adapters/sheets/`. The Google adapter uses it
to create, read and write app-owned workbooks; Room remains the working store.

Each table occupies its own worksheet, named with a ` (Paninda)` suffix (for
example, `Products (Paninda)`). Internal mapper table keys remain `Products`,
etc. Headers start at A1 in row 1; records
start at row 2. One record occupies one row. No merged cells, title rows, or
subtotal rows belong inside these tables. Worksheet names and header names are
case-sensitive. The pure mapper accepts reordered columns and additional named columns, but
rejects missing or duplicate headers. The live read/write adapter requires the
original exact header order, preventing writes from reassigning manual columns.

| Worksheet | Headers, left to right from A1 |
| --- | --- |
| Meta (Paninda) | `key, value` |
| Vendors (Paninda) | `vendor_id, name, contact_person, phone, email, address, notes, is_active, updated_at, version` |
| Products (Paninda) | `product_id, sku, name, vendor_id, category, unit_price_minor, low_stock_threshold, is_active, updated_at, version` |
| Sales (Paninda) | `sale_id, receipt_number, sold_at, cashier_name, payment_method, total_minor, amount_tendered_minor, item_count` |
| SaleItems (Paninda) | `sale_item_id, sale_id, product_id, product_name_snapshot, vendor_id_snapshot, quantity, unit_price_minor` |
| StockCounts (Paninda) | `count_id, counted_at, counted_by, notes` |
| StockCountItems (Paninda) | `count_item_id, count_id, product_id, product_name_snapshot, sku_snapshot, vendor_id_snapshot, expected_quantity, counted_quantity` |
| StockMovements (Paninda) | `movement_id, product_id, quantity_delta, movement_type, source_id, occurred_at, reason` |

Required metadata:

```text
schema_version  1
currency        PHP
shop_timezone   Asia/Manila
```

- IDs are stable text identifiers, never row numbers. App-created IDs are UUIDs.
- Vendor name is required (maximum 120 characters) and unique ignoring case,
  including archived vendors. Optional contact person: 120 characters; phone: 40;
  email: 254 with basic email-format validation; address: 300; notes: 1000.
  Text is trimmed. Archive/restore changes `is_active`; stable IDs preserve
  product links and receipt snapshots. New product links require an active vendor.
  Existing links to an archived vendor may be retained during product edits.
- SKU is a unique uppercase string of 1–64 letters, digits, periods, underscores,
  or hyphens. SKU is text even when it contains only digits.
- Money is a nonnegative integer number of centavos. `950` means Php9.50.
- Counts, quantities, and versions are whole numbers. Versions start at 1.
- Timestamps are ISO 8601 UTC strings, such as the output of `Instant.toString()`.
- `is_active` is TRUE or FALSE. Payment method is CASH, MANUAL_OTHER or NOT_RECORDED.
- Cash receipts require `amount_tendered_minor` at least equal to `total_minor`.
  MANUAL_OTHER can leave tendered empty. App-recorded completed sales use
  NOT_RECORDED with an empty tendered cell: payment is neither collected nor verified.
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

- Completed-sale recording saves Sales/SaleItems plus one negative SALE movement
  per product, with source_id equal to sale_id. Products retain stable IDs; price,
  product name and vendor are captured in SaleItems. Saving increments the product
  version and never permits sale quantity to exceed current on-hand stock.
- StockCounts stores the recorder, confirmation time and optional notes. Each
  StockCountItems row snapshots product name, SKU and vendor plus expected and
  counted quantities. Physical quantity is 0–1,000,000; variance is counted minus
  expected. A completed count has at least one distinct product, and nonzero
  variances require a note (up to 1,000 characters). Recorder: 1–120 characters.
- Each nonzero count variance has exactly one COUNT_ADJUSTMENT movement with
  source_id equal to count_id and quantity_delta equal to that variance. Matching
  counts create history without a zero adjustment. Count source/product references
  must resolve. Product version increments even for a matching count so other
  open counts must recheck. Open count baselines are local form data; saving
  requires unchanged product version and expected stock, inside one transaction.
- Current contract adds StockCounts and StockCountItems, making eight app-owned
  worksheets. Room and Sheets schema stay at fresh-install version 1. There is
  no schema migration or backward compatibility support.

This contract covers product metadata, stock movements, completed-sale records
and completed physical counts.
Void/refund/discount/tax/consignor-settlement rules are not defined yet. Extend
the schema explicitly when those workflows are designed.

Import validation is pure and rejects malformed data before any database write.
The live Google adapter imports valid app-owned workbooks into Room transactionally.
It requires the original canonical header order; the pure mapper still accepts
reordered headers for future import workflows. Existing-file setup checks for missing app worksheets and existing cell data.
It adds only app worksheets after any required warning; unrelated data is not
imported into the domain model. See GOOGLE_SHEETS_SETUP.md for sync
conflict policy, single-writer limits and connection setup.

This prototype uses a fresh version-1 schema, including all ten vendor columns.
There are no schema upgrades, migrations, unsuffixed worksheet renames or legacy
header support. Use a fresh installation and new app worksheets for testing; an
older two-column Vendors worksheet is rejected rather than converted.
