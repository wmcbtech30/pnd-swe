# DFD implementation review

Reviewed against branch `litmus_4`, source commit `cc26d4a` (including the original Mermaid diagrams). Both `.drawio` files contain editable, uncompressed mxGraphModel XML. Open them in diagrams.net. The connector also creates interactive versions with obstacle-avoiding edge routing. The large Level 1 canvas preserves all flow labels; repeated Staff and D1 symbols denote the same entity/store.

## Corrections

- Broadened the external Sheets boundary to Sheets / Drive, and authorization to Google Identity / Picker. `GoogleWorkbookSync.selectFile`, `DriveWorkbooks.account/selected`, and `GoogleAuthorization.pickFile` exchange account identity, selected file IDs and file metadata, beyond authorization tokens and worksheet records.
- Added workbook creation/setup and grant revocation flows, balanced at both levels. Evidence: `GoogleWorkbookSync.createFile/connect/disconnect`. Disconnect revokes access before changing the local connection state; it retains business records and pending changes.
- Included confirmed local reset in process 5's connection/sync administration responsibility. `ShopViewModel.resetLocalData` calls `ShopRepository.resetLocalData`, which clears local business records/outbox and invalidates the baseline while preserving the connection. This is an implemented settings action, not a proposed recovery workflow. No remote deletion is represented.
- Made vendor archive/restore explicit within product/vendor management (`ShopRepository.setVendorActive`). Level 0 summarizes these as vendor details. Product archiving remains excluded.
- Added product versions and pending changes to sale/count writes (`RecordingRepository.recordSale/recordCount`). These writes are atomic. Stock counts preserve item snapshots and only nonzero variances produce adjustments. Sales record completed sales and do not collect payments.
- Added connection state to D1 → stock alerts (`StockNotifications.start`). Alerts read committed Room data and device settings; only device preferences/delivery markers are written by this process. Android permission/channel enforcement is abstracted within the local alert-delivery process, not modeled as another business actor.

## Preserved behavior and evidence

`WorkbookSynchronizer.sync` performs explicit snapshot comparison, upload verification and conflict blocking. D1 includes connection/sync state and the pending queue, but only business snapshots go to Sheets. D2 never syncs. `InventoryReport` and `MovementReport` derive reports from existing local records without writing a report store.

Reviewed revision logs 006–009, 011 and 012 alongside current implementation. Historical statements that counts/reports were pending are superseded by revisions 008/009. Revision 012 documents local notifications and their delivery limits. These are development/change logs; no runtime device trace or live Google session was exercised for this diagram review.

## Validation

Both XML files parse successfully, have unique cell IDs and valid edge endpoints, and preserve all 6 context and 28 Level 1 directed flows from the revised Mermaid sources. Level 0 has one circular process and no stores. Level 1 has six circular processes and two logical stores, represented by parallel horizontal lines. External service flows match verbatim between levels. No entity-to-store, store-to-store or entity-to-entity flows are introduced. No application code changed; application build/tests were not rerun for documentation-only changes.

For publication, open a `.drawio` file in diagrams.net and use File → Export as → SVG after checking label placement at the desired page size. SVG exports have not been generated or visually verified in this review.

## Receiving revision note

Revision 013 adds stock receiving. The Mermaid sources now include delivery inputs/outputs and process 7.0 with its four flows (32 Level 1 flows total). The editable `.drawio` files above are preserved as the reviewed pre-receiving snapshots of `cc26d4a`; regenerate them from the updated Mermaid sources before using them to describe revision 013.
