# Prototype status

Branch: `litmus_3`, continuing the local-first prototype from `litmus_2`.
Historical decisions and checks remain in `logs/log-rev000.log` through 009.

## Implemented

- Compose onboarding, Home, Sell catalog, Inventory, History, Tools, Sync Status
  and Settings. Five navigation pillars; tapping Home returns to the dashboard.
- Responsive portrait layouts, fixed headers/search/filter actions, isolated
  scrolling and FAB clearance. Per-page Kotlin files and string resources.
- Product creation/editing with SKU validation, integer-centavo prices, vendor
  metadata, opening stock and movement-based quantity. Receipt detail snapshots.
- Room persistence and a coalescing pending-change queue. Clean install starts
  empty: no business fixtures, fake identity or fictional successful sync.
- Google Play services OAuth authorization with limited `drive.file`, OpenID and
  email scopes. Onboarding cannot bypass real authorization/workbook connection.
- Onboarding choice between an existing file through Google’s own Picker and
  creating a named file. Eight `(Paninda)` worksheets live inside that one file.
  Real Sheets REST read/write remains behind a modular adapter. Core domain models and
  Room repository do not depend on Google transport.
- Conditional Figma-style warning only when a selected file has cell data and
  is missing required Paninda worksheets. Empty/compliant files connect directly.
  Proceed adds worksheets inside the selected file and preserves unrelated data.
- Failure notices share the gradient layout; retry returns to file setup. Native
  Google Picker handles selection/authorization, limited to one editable file.
- Explicit local-first sync with three-way snapshot comparison, transactionally
  guarded imports/acknowledgements, conflict blocking and Settings suggestions.
- Real disconnect revokes Google's grant before closing the local session.
  Local data/pending changes and Drive files remain. Failure to revoke is shown.
- Confirmed local reset discards device business records/outbox, preserves the
  connection, and invalidates the baseline. It never deletes the remote workbook;
  later sync can restore remote data, or block if new local data conflicts.
- Google logo remains the requested blank placeholder. Excel is removed and
  Custom Database remains disabled. Business fixtures exist only under tests.

- Home shows the last successful sync date/time through a shared read-only
  `ShopState.syncSummary` getter and common sync presentation helpers. Successful
  sync uses a checkmark; pending/in-progress states use sync icons, and warnings
  are reserved for errors/conflicts.
- Time-based Home microcopy cycles across cold launches, stays stable during
  navigation/recreation, and switches to the appropriate Manila-time pool when
  the time period changes. Editable phrases live in `strings_microcopies.xml`;
  pools/selection rules live in `ui/home/Microcopies.kt`.
- System chrome follows the app’s light theme even when the device uses dark
  mode. Dark status/navigation controls remain visible against the light app;
  older Android versions without dark navigation icons use a dark fallback bar.

- Live validated-internet status is observed separately from the saved local
  session/file binding. Losing internet shows Offline with an X and leaves the
  current page and local work available. Failed sync routes to the normal Sync
  Status page, preserving bottom navigation; it never opens onboarding.
- Missing/deleted/inaccessible files and reachable-internet transport failures
  show Sync failed with a retry explanation. They retain the selected file,
  local records, pending changes and last successful sync. Only a successful
  explicit Settings disconnect closes the local session.

- Vendors under Tools: searchable active/archived lists, add/edit contact details
  and notes, linked products and stock summaries, archive/restore confirmation.
  Product forms select saved active vendors or create one by name. Archived
  vendors retain existing links/history and cannot receive new product links.
- Vendor IDs remain stable across renames; edits queue for explicit Sheets sync.
  Contacts, archive state, timestamps and versions round-trip through the adapter.

- Tools → Reports: current active-product stock, low/out-of-stock counts, a
  selling-price stock estimate, and a labeled horizontal bar chart by vendor.
  Stock/vendor/category filters apply to cards, bars and linked product rows.
- Separate Stock Movements report: added/removed/net units and recorded rows,
  with Manila-time date ranges and current-vendor filters. Includes opening stock
  and valid imported movements, including archived products/vendors. Both reports
  react to Room changes and work offline; no report fixtures are packaged.

- Sell and Home open a completed-sale recording form: product lines, quantities,
  recorded unit prices, recorder and final confirmation. No cart/checkout,
  tendered amount, payment collection or payment verification. New records use
  NOT_RECORDED payment metadata, appear in History/Home, and deduct stock once.
- Tools → Stock Counts: count history, physical-count entry, expected quantities,
  per-product variances, required discrepancy explanation and confirmation.
  Blank products are skipped; zero is explicit. Stale baselines block saving.
  Count history includes matching/no-change counts and persists across restart.
- Recording writes are atomic and idempotent by record ID. Stock movements,
  item snapshots, product versions and pending changes save together. Sheets
  maps count history through StockCounts and StockCountItems worksheets.

## Operational limits and pending work

| Area | Status |
| --- | --- |
| Cloud configuration | Android OAuth client registration, Drive/Sheets/Picker APIs and test users must be configured by the project owner. Exact package/SHA-1 and steps are in `GOOGLE_SHEETS_SETUP.md`. |
| Device OAuth acceptance | Not verified against a real Google account/device in this environment. The device checklist remains required. |
| Worksheet compatibility | Detects cell data and missing app worksheets before setup. Existing app headers/records are validated; unrelated worksheets are not imported. No automatic repair of malformed app data. |
| File picker | Google’s own Android Picker selects one editable Sheets file. General Excel/table import remains pending. |
| Sync scheduling | Manual Sync Now/Settings only; automatic sync is the last priority. No background worker, polling, periodic sync or automatic retry. |
| Concurrency | Single writer only. Before/after checks detect many conflicts, but Sheets has no conditional-write CAS here. Do not edit Sheets or another device during sync. Reliable concurrent writes need a coordinator/protocol. |
| Conflict resolution | Warnings and suggestions exist. No automatic merge, row comparison UI, force overwrite, or automatic local-only backup export. |
| Failed creation | Timed-out work may have reached Google; incomplete or duplicate workbooks can remain. The app never automatically deletes them. |
| Selling | Completed-sale recording implemented with stock deduction. POS/payment processing, checkout, voids/refunds and backdated entry are excluded/not implemented. |
| History | Displays recorded/imported sales and immutable item/price snapshots. |
| Vendors | Management implemented. Vendor payments, settlement, purchase orders and receiving remain undefined/pending. |
| Reports | Current inventory and recorded stock movements implemented. Historical stock balances, cost/profit, receiving summaries, aggregate count-variance analysis and export remain pending. Count details show individual variances. No POS analytics. |
| Stock workflows | Physical counts, user confirmation, variance history and count adjustments implemented. Receiving, standalone adjustments and role-based count approval remain pending. |
| Product lifecycle | No delete/archive, barcode scan, images, variants or bulk import. |
| Staff accounts | Google identity binds workbook access; staff roles/permissions are pending. |
| Financial workflows | Voids, refunds, discounts, taxes and consignor settlement are undefined. |
| Other adapters | Custom Database disabled. Excel support pending. |

## Data and design

`WORKBOOK_SCHEMA.md` defines the A1/row-1 contract. Core money, IDs, references,
stock ledger and receipt-total rules remain unchanged. The Google adapter requires
original header order; pure import mapping remains tolerant of reordered headers.
App state/outbox are device metadata. This branch uses a fresh Room/schema-1
contract with expanded vendor columns and stock-count tables. Per the requested testing workflow, no
schema upgrades, migrations or backward compatibility are provided. Uninstall
and use a new file or freshly initialized app worksheets when testing this branch.

Figma file `MAc7bCMKF4RDYbCYQetqt9`: prior eight frames plus notice `28:404`.
The warning/error layout uses its gradient, serif hero, scrolling body and fixed
bottom actions. New workbook selection and connection status details follow the
existing palette, fonts and Material icon conventions. Portrait is requested;
Android/OEM multi-window policies can override orientation constraints.

See `GOOGLE_SHEETS_SETUP.md` for integration boundaries, recovery behavior,
Google Cloud registration, official references and device acceptance steps.

## Verification

Keep iteration lean: twenty-five focused checks covering existing data flows, worksheet inspection,
sync presentation, launch-based greeting rotation, vendor lifecycle rules and
inventory-report calculations and atomic sale/count recording. No maintained screenshot baselines, layout
assertions or device matrix.
Build/lint/regression results for this revision are recorded in log-rev009.
