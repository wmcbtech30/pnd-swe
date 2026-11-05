# Local prototype status

Branch: `litmus_1`. This implementation follows `logs/log-rev000.log` with the
user's subsequent correction: **start completely clean, without sample data**.

## Implemented

- The supplied onboarding, Home, Sell, Inventory, History, Tools, Sync Status,
  and Settings layouts in Jetpack Compose.
- Five navigation pillars with shared Material icons and a persistent bottom
  bar. Subpages use a chevron-left back action. Product editing hides the bottom
  bar to prevent accidental navigation away from unsaved changes.
- Shared cards corresponding to the inspected Figma component variants.
- Fixed page headings, search, filter actions, and History summary. Lists and
  forms scroll independently. FABs include icons and lists reserve bottom space.
- Responsive portrait layouts, keyboard-aware forms, and larger-text support.
- Only the supplied screen palette is used; Material defaults are overridden.
- Page-specific strings, separate Kotlin files for pages/subpages, and shared
  components for repeated controls.
- Room-backed local persistence, version-1 schema export, repository, ViewModel,
  and shared product/vendor/receipt/item/stock-movement/change models.
- Product creation and editing, SKU uniqueness, price/quantity validation,
  opening stock, and pending-change recording in a single Room transaction.
- Inventory search by name/SKU and stock, vendor, and category filter sheets.
- Read-only product details and movement history.
- History receipt search, date filter sheet, custom date validation, totals,
  pending badges, and read-only receipt detail/line snapshots.
- Home totals and stock summaries derived from the same local records used by
  Inventory and History. Recent receipts are limited to the latest 20 records.
- Missing forms, detail pages, and filter sheets follow the existing palette
  and typography; they are additions because no Figma frames were supplied.

## Clean startup and local session

- Production code contains no fixture records, preset identity, seed function,
  fake receipts, or fictional successful sync timestamps.
- A fresh database has no products, vendors, receipts, items, stock movements,
  or pending changes. Initialization writes only an application-state flag.
- The greeting has no preset personal name. Home starts at zero totals.
- The Google button opens Home and stores a local session flag. It does not
  authenticate with Google, select a workbook, or connect to an external service.
- The session survives activity recreation and app restarts.
- The Google logo remains a blank placeholder, as requested. Excel is removed;
  Custom Database remains disabled.
- Disconnect requires confirmation, preserves business data and pending
  changes, clears the local session flag, and returns to onboarding.
- Reset requires confirmation and clears local business records and pending
  changes. It preserves the local connection and initialization flags, returns
  to Home, and never restores or generates data automatically.
- Any synthetic records used for verification exist only under `src/test` and
  are not packaged in the application.

## Local data rules

- New records use UUID identifiers. SKUs are trimmed, normalized to uppercase,
  and unique. Vendor names entered with a product reuse an existing vendor
  case-insensitively; this is metadata entry, not a vendor-management workflow.
- Money uses integer minor units. Product forms accept up to seven whole-number
  digits and two decimal places. Opening quantities and thresholds accept whole
  numbers from zero to 1,000,000.
- Expected stock is the sum of stock movements. Editing product metadata does
  not overwrite quantities or create another opening movement.
- Low stock means positive quantity at or below the threshold. Zero or negative
  expected quantity is treated as out of stock.
- Receipt lines store their original product name, vendor, quantity, and price.
  Editing a product cannot change an existing receipt.
- Times are stored as epoch milliseconds. Day boundaries and display use
  `Asia/Manila`. Custom date ranges include both endpoint dates.
- Local metadata/opening-stock writes queue UPSERT references. Repeated edits
  to one record coalesce to the latest pending reference. There is no uploader
  or retry worker yet.

## Spreadsheet contract and adapters

- `adapters/WorkbookAdapter.kt` defines the transport boundary. Google Sheets
  and Custom Database implementations explicitly return `Unavailable`.
- `WorkbookSchema.kt` and `SheetsMapper.kt` contain pure, locally testable
  conversion logic. They perform no network requests and do not modify Room.
- Version 1 defines `Meta`, `Vendors`, `Products`, `Sales`, `SaleItems`, and
  `StockMovements`, each with row-1 headers beginning at A1 and records at row 2.
- The canonical header order is used for writing. Reading matches exact header
  names and accepts reordered columns. Missing/duplicate headers, duplicate IDs
  or SKUs, broken references, invalid types, and inconsistent receipt totals or
  item counts are rejected.
- Currency is PHP. Money cells contain integer minor units, flags use TRUE/FALSE,
  and timestamps use ISO 8601 UTC. Stock is reconstructed from movement rows.
- Pending-change and local-session tables are device metadata and are not
  exported. A parsed workbook receipt is marked acknowledged locally because
  it is a workbook record; the disabled adapter never claims an upload occurred.
- This is the first contract for product metadata, stock movements, and completed
  receipt records. Returns, voids, settlement, and discount rules are not defined.
  Future contract changes require an explicit schema version/migration.
- There is no user-facing workbook import/export yet. Having mapping functions
  does not establish authentication, remote access, synchronization, or backups.

## Pending or unavailable

| Area | Current behavior / missing work |
| --- | --- |
| Selling | Catalog/search/filter only. Add and cart actions explain that selling is unavailable. No stock deduction, cart, payment, checkout, or receipt creation. |
| History | Starts empty. Receipts can be displayed when records exist, but there is no manual receipt entry or import UI in this phase. |
| Vendors | Tools action explains that vendor management is pending. Vendor names can be entered as product metadata. |
| Reports | Tools action explains that reports are pending. |
| Stock counts | Tools action explains that physical counting and variance approval are pending. |
| Receiving/adjustments | No stock-receiving or adjustment workflow yet; product details explain this. |
| Product lifecycle | No delete/archive, barcode scan, image upload, variant editor, or bulk import. One product represents one saleable SKU. |
| Authentication | No Google OAuth, user accounts, staff roles, permissions, or actual workbook selection. |
| Google Sheets | Required future integration. Schema/mappers exist; transport and conflict handling do not. |
| Custom Database | Disabled and returns unavailable. |
| Sync | Permanently offline for this build. Sync Now records a local check and reports that nothing was uploaded; pending changes remain intact. |
| Toast variants | Offline feedback is supported. Online/resumed-sync states remain unused until a real adapter exists. |
| Financial workflows | No void/refund/discount/tax/consignor-settlement behavior. |
| Error recovery | Local load/action errors are surfaced. There is no remote retry or conflict-resolution workflow. |

## Design references and limits

Figma file: `MAc7bCMKF4RDYbCYQetqt9`.
Frames: `3:2`, `1:3`, `9:131`, `14:53`, `14:128`, `18:250`, `19:305`, `19:373`.

The source frames use fixed positioning at 412 x 917. Code uses native layout
constraints and isolated scrolling instead of scaling the whole canvas. Empty
states replace Figma's example business records. Offline wording reflects the
actual unavailable adapter, and bottom navigation uses the existing blue to
identify the selected pillar. Icon-bearing FABs and Material icon substitutions
follow the user's explicit requirements.

Portrait orientation is requested in the manifest, with the Android 16 large-
screen compatibility property retained. Some Android multi-window, OEM, or
device-policy contexts can override orientation restrictions.

Automatic Figma Code Connect mapping is unavailable with the inspected seat;
components are mapped manually. IBM Plex, Roboto, and Roboto Condensed assets
are bundled with their licenses. Roboto Condensed static weight instances are
generated from the Google Fonts variable font for API-24 compatibility.

## Verification

The regression suite is intentionally small: three money/filter checks, three
workbook-mapping checks, three repository checks, and one UI behavior smoke
check. The user requested removal of unnecessary layout tests; there are no
screenshot baselines, device-size matrix tests, or automated layout assertions.

The ten focused regression checks passed, including empty startup, validated
product persistence, receipt snapshot integrity, disconnect/reset behavior, and
workbook validation. The debug APK builds and Android lint reports zero errors;
the remaining notices concern available dependency updates. Initial one-off
renders were inspected against the supplied frames; no visual test suite or
baselines are retained in the project.

For UI iteration, use `./gradlew assembleDebug`. For the focused regression
suite, use `./gradlew testDebugUnitTest`. Actual-device acceptance testing remains
pending; this environment has no configured emulator.
