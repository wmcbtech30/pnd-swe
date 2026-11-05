# Prototype status

Branch: `litmus_2`, continuing the clean local-first prototype on `litmus_1`.
Historical decisions and checks remain in `logs/log-rev000.log` through 003.

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
- Separate Paninda workbook creation, app-workbook discovery/reconnection, and
  real Sheets REST read/write through a modular adapter. Core domain models and
  Room repository do not depend on Google transport.
- A blanket Figma-style warning on every connection. Wording explicitly states
  that arbitrary-workbook compatibility detection is pending. Creation uses a
  separate spreadsheet file; it does not add a workbook inside another file.
- Failure notices use the same layout. Setup retry starts fresh authorization
  and workbook selection; sync retry reauthorizes and rereads both copies.
- Explicit local-first sync with three-way snapshot comparison, transactionally
  guarded imports/acknowledgements, conflict blocking and Settings suggestions.
- Real disconnect revokes Google's grant before closing the local session.
  Local data/pending changes and Drive files remain. Failure to revoke is shown.
- Confirmed local reset discards device business records/outbox, preserves the
  connection, and invalidates the baseline. It never deletes the remote workbook;
  later sync can restore remote data, or block if new local data conflicts.
- Google logo remains the requested blank placeholder. Excel is removed and
  Custom Database remains disabled. Business fixtures exist only under tests.

## Operational limits and pending work

| Area | Status |
| --- | --- |
| Cloud configuration | Android OAuth client registration, APIs and test users must be configured by the project owner. Exact package/SHA-1 and steps are in `GOOGLE_SHEETS_SETUP.md`. |
| Device OAuth acceptance | Not verified against a real Google account/device in this environment. The device checklist remains required. |
| Workbook compatibility | General detection/adoption of arbitrary existing spreadsheets is deferred. The live adapter still validates its own required schema before importing/writing. |
| File picker | Native list is limited to app-authorized, app-marked Paninda workbooks. Full Google Picker integration and arbitrary Sheets/Excel import are pending. |
| Sync scheduling | Manual Sync Now/Settings only; no background worker, polling, periodic sync or automatic retry. |
| Concurrency | Single writer only. Before/after checks detect many conflicts, but Sheets has no conditional-write CAS here. Do not edit Sheets or another device during sync. Reliable concurrent writes need a coordinator/protocol. |
| Conflict resolution | Warnings and suggestions exist. No automatic merge, row comparison UI, force overwrite, or automatic local-only backup export. |
| Failed creation | Timed-out work may have reached Google; incomplete or duplicate workbooks can remain. The app never automatically deletes them. |
| Selling | Catalog/search/filter only. No cart, checkout, payment, receipt creation or sale stock deduction. |
| History | Displays persisted/imported receipts; no manual receipt creation. |
| Vendors | Product vendor metadata only; vendor-management tools pending. |
| Reports | Pending. |
| Stock workflows | Physical counts, variance approval, receiving and adjustments pending. |
| Product lifecycle | No delete/archive, barcode scan, images, variants or bulk import. |
| Staff accounts | Google identity binds workbook access; staff roles/permissions are pending. |
| Financial workflows | Voids, refunds, discounts, taxes and consignor settlement are undefined. |
| Other adapters | Custom Database disabled. Excel support pending. |

## Data and design

`WORKBOOK_SCHEMA.md` defines the A1/row-1 contract. Core money, IDs, references,
stock ledger and receipt-total rules remain unchanged. The Google adapter requires
original header order; pure import mapping remains tolerant of reordered headers.
App state/outbox are device metadata. No Room schema migration is needed because
connection metadata uses the existing key/value state table.

Figma file `MAc7bCMKF4RDYbCYQetqt9`: prior eight frames plus notice `28:404`.
The warning/error layout uses its gradient, serif hero, scrolling body and fixed
bottom actions. New workbook selection and connection status details follow the
existing palette, fonts and Material icon conventions. Portrait is requested;
Android/OEM multi-window policies can override orientation constraints.

See `GOOGLE_SHEETS_SETUP.md` for integration boundaries, recovery behavior,
Google Cloud registration, official references and device acceptance steps.

## Verification

Keep iteration lean: the existing ten focused checks plus three sync data-loss
checks. No maintained screenshot baselines, layout assertions or device matrix.
Build/lint/regression results for this revision are recorded in log-rev003.
